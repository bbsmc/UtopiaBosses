package lt.utopiabosses.item;

import lt.utopiabosses.client.renderer.item.SunflowerGatlingRenderer;
import lt.utopiabosses.entity.SunflowerSeedEntity;
import lt.utopiabosses.network.NetworkHandler;
import lt.utopiabosses.registry.SoundRegistry;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
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
     * 使用物品时的处理 - 确保第一视角模型正确显示
     */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // 客户端初始化第一视角渲染
        if (world.isClient) {
            // 确保动画状态正确
            long animationID = getClientAnimationId(stack);
            triggerAnim(user, animationID, CONTROLLER_NAME, IDLE_ANIM);
        }
        
        return TypedActionResult.pass(stack);
    }
    
    /**
     * 发射武器的逻辑
     */
    public void fireWeapon(ItemStack stack, World world, LivingEntity user) {
        if (!(world instanceof ServerWorld serverWorld)) return;
        
        // 检查玩家背包是否有葵花籽
        if (user instanceof PlayerEntity player) {
            ItemStack sunflowerSeedStack = findSunflowerSeeds(player);
            if (sunflowerSeedStack.isEmpty()) {
                // 没有葵花籽，播放失败音效
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.BLOCK_DISPENSER_FAIL, SoundCategory.PLAYERS, 0.25F, 1.3F);
                return;
            }
            
            // 创建并发射种子
            SunflowerSeedEntity projectile = createProjectile(world, user);
            world.spawnEntity(projectile);
            
            // 播放射击音效
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundRegistry.ENTITY_SUNFLOWER_SHOOT, SoundCategory.PLAYERS, 0.5F,
                0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            
            // 触发射击动画
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverWorld), CONTROLLER_NAME, FIRING_ANIM);
            
            // 消耗葵花籽而不是耐久度
            if (!player.isCreative()) {
                sunflowerSeedStack.decrement(1);
            }
            
            // 添加冷却
            player.getItemCooldownManager().set(this, FIRE_COOLDOWN);
        }
    }
    
    /**
     * 在玩家背包中寻找葵花籽
     */
    private ItemStack findSunflowerSeeds(PlayerEntity player) {
        // 检查主手和副手
        if (player.getMainHandStack().isOf(lt.utopiabosses.registry.ItemRegistry.SUNFLOWER_SEED)) {
            return player.getMainHandStack();
        }
        if (player.getOffHandStack().isOf(lt.utopiabosses.registry.ItemRegistry.SUNFLOWER_SEED)) {
            return player.getOffHandStack();
        }
        
        // 检查背包
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(lt.utopiabosses.registry.ItemRegistry.SUNFLOWER_SEED)) {
                return stack;
            }
        }
        
        return ItemStack.EMPTY;
    }
    
    /**
     * 创建子弹实体 - 简化版本
     */
    private SunflowerSeedEntity createProjectile(World world, LivingEntity user) {
        SunflowerSeedEntity projectile = new SunflowerSeedEntity(world, user);
        
        // 简化位置和速度计算
        Vec3d eyePos = user.getEyePos();
        Vec3d lookDirection = user.getRotationVec(1.0F);
        
        // 设置发射位置（稍微向前偏移）
        Vec3d startPos = eyePos.add(lookDirection.multiply(0.3));
        projectile.setPosition(startPos);
        
        // 设置速度
        projectile.setVelocity(lookDirection.multiply(3.0));
        projectile.setHomingTarget(null);
        projectile.setDamage(6.0f);
        
        return projectile;
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
                
                // 获取正确的animationID - 客户端使用唯一ID
                long animationID = getClientAnimationId(stack);
                
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
                    
                    // 只在必要时重置渲染状态（减少频率）
                    if (!wasSelected) {
                        client.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.MAIN_HAND);
                        client.gameRenderer.firstPersonRenderer.resetEquipProgress(Hand.OFF_HAND);
                    }
                }
                
                // 检测按键输入并发送网络包 - 添加间隔控制
                if (client.options.useKey.isPressed()) {
                    // 检查是否可以射击（避免频繁发送）
                    long lastShotTime = stack.getOrCreateNbt().getLong("LastShotTime");
                    long currentTime = world.getTime();
                    
                    if (currentTime - lastShotTime >= FIRE_COOLDOWN) {
                        // 更新最后射击时间
                        stack.getOrCreateNbt().putLong("LastShotTime", currentTime);
                        
                        // 创建并发送射击网络包
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeBoolean(true);
                        ClientPlayNetworking.send(NetworkHandler.SHOOT_GATLING_PACKET_ID, buf);
                    }
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
        
        // 确保在停止使用物品时动画回到空闲状态，但避免频繁触发
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
        // 向日葵加特林无限耐久，不需要修复
        return false;
    }

    @Override
    public boolean isDamageable() {
        // 向日葵加特林无限耐久
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("无限耐久")
                .formatted(Formatting.GOLD));
        tooltip.add(Text.literal("右键发射：需要消耗背包中的葵花籽")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.literal("击败向日葵BOSS掉落")
                .formatted(Formatting.YELLOW));
    }

    @Override
    public boolean isPerspectiveAware() {
        return true; // 确保支持第一视角渲染
    }

    /**
     * 强制刷新第一视角渲染状态
     */
    private void forceRefreshFirstPersonRender(ItemStack stack, PlayerEntity player) {
        if (player.getWorld().isClient) {
            MinecraftClient client = MinecraftClient.getInstance();
            
            // 获取动画ID
            long animationID = getClientAnimationId(stack);
            
            // 触发空闲动画
            triggerAnim(player, animationID, CONTROLLER_NAME, IDLE_ANIM);
            
            // 标记状态已刷新
            stack.getOrCreateNbt().putBoolean("WasFirstPerson", client.options.getPerspective().isFirstPerson());
        }
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
        
        // 移除不必要的客户端tick事件监听，通过inventoryTick处理即可
    }

    // 改进物品切换事件处理，确保第一视角模型正确显示
    @Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        // 确保物品切换时能够正确触发物品重置状态
        if (oldStack.getItem() == this && !oldStack.equals(newStack)) {
            if (player.getWorld().isClient) {
                // 清除状态标记，确保再次被选中时重新初始化
                oldStack.getOrCreateNbt().putBoolean("WasSelected", false);
                oldStack.getOrCreateNbt().remove("WasFirstPerson"); // 强制重新检测视角
            }
        }
        return super.allowNbtUpdateAnimation(player, hand, oldStack, newStack);
    }
}
