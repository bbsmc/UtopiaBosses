package lt.utopiabosses.item;

import lt.utopiabosses.client.renderer.item.SunflowerGatlingRenderer;
import lt.utopiabosses.entity.SunflowerSeedEntity;
import lt.utopiabosses.network.NetworkHandler;
import lt.utopiabosses.registry.SoundRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 向日葵加特林枪 - 基于AzureAnimatedGunItem实现
 */
public class SunflowerGatlingItem extends Item implements GeoItem {

    // 常量定义
    public static final String CONTROLLER_NAME = "controller";
    public static final String FIRING_ANIM = "fire";
    public static final String IDLE_ANIM = "idle";

    // 发射冷却时间，单位tick
    private static final int FIRE_COOLDOWN = 2;

    // GeckoLib动画缓存
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    // 预定义动画
    private static final RawAnimation IDLE_ANIMATION = RawAnimation.begin().thenLoop(IDLE_ANIM);
    private static final RawAnimation FIRING_ANIMATION = RawAnimation.begin().then(FIRING_ANIM, Animation.LoopType.PLAY_ONCE);

    // 添加静态KeyBinding用于检测使用键
    private static KeyBinding fireKeyBinding;

    public SunflowerGatlingItem(Settings settings) {
        super(settings);
        // 注册为可同步的动画实体
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private SunflowerGatlingRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new SunflowerGatlingRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, CONTROLLER_NAME, 0, event -> {
            // 默认情况下播放空闲动画
            return event.setAndContinue(IDLE_ANIMATION);
        })
        // 添加可触发的射击动画
        .triggerableAnim(FIRING_ANIM, FIRING_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * 使用物品时的处理 - 现在只在服务端处理发射逻辑，客户端通过网络包发送
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // 不再直接处理射击，而是通过网络包和inventoryTick
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
    
    /**
     * 发射武器的逻辑
     */
    public void fireWeapon(ItemStack stack, World world, LivingEntity user) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        
        // 检查耐久度并发射
        if (stack.getDamage() < stack.getMaxDamage() - 1) {
            // 创建并发射种子
            SunflowerSeedEntity projectile = createProjectile(world, user);
            world.spawnEntity(projectile);
            
            // 播放射击音效
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundRegistry.ITEM_GATLING_GUN_FIRE, SoundCategory.PLAYERS, 0.5F, 
                0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            
            // 触发射击动画
            if (user instanceof PlayerEntity player) {
                triggerAnim(player, GeoItem.getOrAssignId(stack, serverWorld), CONTROLLER_NAME, FIRING_ANIM);
            }
            
            // 消耗耐久并添加冷却
            stack.damage(1, user, p -> p.sendToolBreakStatus(user.getActiveHand()));
            if (user instanceof PlayerEntity player) {
                player.getItemCooldownManager().set(this, FIRE_COOLDOWN);
            }
        } else {
            // 如果弹药耗尽则播放点击音效
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.25F, 1.3F);
        }
    }
    
    /**
     * 创建子弹实体
     */
    private SunflowerSeedEntity createProjectile(World world, LivingEntity user) {
        SunflowerSeedEntity projectile = new SunflowerSeedEntity(world, user);
        
        // 计算发射位置和方向
        Vec3d muzzlePos = calculateMuzzlePosition(user);
        Vec3d velocity = calculateProjectileVelocity(user);
        
        // 设置属性
        projectile.setPosition(muzzlePos);
        projectile.setVelocity(velocity.multiply(3.0)); // 速度因子
        projectile.setHomingTarget(null); // 不追踪
        projectile.setDamage(2.0f);
        
        return projectile;
    }
    
    /**
     * 计算枪口位置
     */
    private Vec3d calculateMuzzlePosition(LivingEntity user) {
        // 获取玩家视线方向
        float yaw = user.getYaw();
        float pitch = user.getPitch();
        
        // 转换为弧度
        float yawRad = yaw * 0.017453292F;
        float pitchRad = pitch * 0.017453292F;
        
        // 计算方向向量
        Vec3d forward = new Vec3d(
            -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad),
            -MathHelper.sin(pitchRad),
            MathHelper.cos(yawRad) * MathHelper.cos(pitchRad)
        ).normalize();
        
        Vec3d right = new Vec3d(
            MathHelper.cos(yawRad),
            0,
            MathHelper.sin(yawRad)
        ).normalize();
        
        // 确定水平偏移方向
        boolean isMainHand = user.getActiveHand() == Hand.MAIN_HAND;
        boolean isLeftHanded = user instanceof PlayerEntity && ((PlayerEntity)user).getMainArm() == Arm.LEFT;
        
        float horizontalOffset = 0.25f; // 减小水平偏移以减少视觉抖动
        if (isLeftHanded) {
            horizontalOffset = isMainHand ? horizontalOffset : -horizontalOffset;
        } else {
            horizontalOffset = isMainHand ? -horizontalOffset : horizontalOffset;
        }
        
        // 计算枪口位置
        return user.getEyePos()
            .add(forward.multiply(1.0)) // 前方位移
            .add(0, -0.15, 0) // 垂直位移 - 减小以减少抖动
            .add(right.multiply(horizontalOffset));
    }
    
    /**
     * 计算子弹速度方向
     */
    private Vec3d calculateProjectileVelocity(LivingEntity user) {
        float yaw = user.getYaw();
        float pitch = user.getPitch();
        
        float yawRad = yaw * 0.017453292F;
        float pitchRad = pitch * 0.017453292F;
        
        double x = -MathHelper.sin(yawRad) * MathHelper.cos(pitchRad);
        double y = -MathHelper.sin(pitchRad);
        double z = MathHelper.cos(yawRad) * MathHelper.cos(pitchRad);
        
        return new Vec3d(x, y, z).normalize();
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        // 客户端处理
        if (world.isClient && entity instanceof PlayerEntity player) {
            MinecraftClient client = MinecraftClient.getInstance();
            
            // 只处理选中的物品或刚刚被取消选中的物品
            boolean wasSelected = stack.getOrCreateNbt().getBoolean("WasSelected");
            
            if (selected) {
                // 物品被选中时的处理
                boolean isFirstPerson = client.options.getPerspective().isFirstPerson();
                
                // 物品刚被选中或视角切换时，强制初始化渲染
                boolean needsInit = !wasSelected || 
                                   isFirstPerson != stack.getOrCreateNbt().getBoolean("WasFirstPerson");
                
                // 获取正确的animationID - 客户端使用随机ID
                long animationID = selected ? getClientAnimationId(stack) : 0;
                
                // 当选中时标记为已选中
                if (!wasSelected) {
                    stack.getOrCreateNbt().putBoolean("WasSelected", true);
                }
                
                // 初始化或视角切换时更新渲染状态
                if (needsInit) {
                    // 记录当前视角状态
                    stack.getOrCreateNbt().putBoolean("WasFirstPerson", isFirstPerson);
                    
                    // 强制触发动画
                    triggerAnim(player, animationID, CONTROLLER_NAME, IDLE_ANIM);
                    
                    // 重置渲染状态
                    client.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND);
                    client.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.OFF_HAND);
                }
                
                // 检测按键输入并发送网络包
                if (client.options.useKey.isPressed()) {
                    // 创建并发送射击网络包
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeBoolean(true);
                    ClientPlayNetworking.send(NetworkHandler.SHOOT_GATLING_PACKET_ID, buf);
                }
            } else if (wasSelected) {
                // 物品刚被取消选中，更新状态
                stack.getOrCreateNbt().putBoolean("WasSelected", false);
            }
        }
    }
    
    /**
     * 获取客户端动画ID - 确保ID一致性
     */
    private long getClientAnimationId(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (!nbt.contains("ClientAnimID")) {
            // 为这个物品实例分配唯一ID
            nbt.putLong("ClientAnimID", stack.hashCode());
        }
        return nbt.getLong("ClientAnimID");
    }
    
    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        super.onStoppedUsing(stack, world, user, remainingUseTicks);
        
        // 确保在停止使用物品时动画回到空闲状态
        if (world.isClient && user instanceof PlayerEntity player) {
            long animationID = getClientAnimationId(stack);
            triggerAnim(player, animationID, CONTROLLER_NAME, IDLE_ANIM);
        }
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE; // 避免视角变化
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        // 当物品实体被销毁时的处理，例如掉入岩浆
        super.onItemEntityDestroyed(entity);
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isOf(Items.SUNFLOWER);
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    /**
     * 服务端射击方法，由网络包调用
     */
    public static void shoot(PlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        if (stack.getItem() instanceof SunflowerGatlingItem gatling && 
            !player.getItemCooldownManager().isCoolingDown(gatling)) {
            
            gatling.fireWeapon(stack, player.getWorld(), player);
        }
    }

    // 客户端初始化方法，注册按键事件
    public static void initClient() {
        // 注册与原版使用键（右键）相同的按键绑定
        fireKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.utopiabosses.fire_gatling",
            InputUtil.Type.MOUSE,
            1, // 右键的代码是1
            "category.utopiabosses.weapons"
        ));
        
        // 注册客户端tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            if (player != null && !player.isSpectator()) {
                ItemStack stack = player.getMainHandStack();
                if (stack.getItem() instanceof SunflowerGatlingItem) {
                    // 避免重复发送网络包，已在inventoryTick中处理
                }
            }
        });
    }

    // 添加物品切换事件
    @Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        // 确保物品切换时能够正确触发物品重置状态
        if (oldStack.getItem() == this && !oldStack.equals(newStack)) {
            if (player.getWorld().isClient) {
                // 清除状态标记，确保再次被选中时重新初始化
                oldStack.getOrCreateNbt().putBoolean("WasSelected", false);
            }
        }
        return super.allowNbtUpdateAnimation(player, hand, oldStack, newStack);
    }
}
