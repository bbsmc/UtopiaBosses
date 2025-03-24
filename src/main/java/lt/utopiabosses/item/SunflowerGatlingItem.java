package lt.utopiabosses.item;

import lt.utopiabosses.client.renderer.item.SunflowerGatlingRenderer;
import lt.utopiabosses.entity.SunflowerSeedEntity;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
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

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SunflowerGatlingItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    // 发射冷却时间，单位tick，控制射速
    // 10发/秒 = 每0.1秒1发 = 每2tick发射一次
    // Minecraft一秒20tick
    private static final int FIRE_COOLDOWN = 2;

    // 将静态变量改为常量，用于NBT标签
    private static final String USING_KEY = "IsBeingUsed";

    // 添加静态字段来跟踪任何加特林的使用状态
    private static boolean anyItemInUse = false;

    public SunflowerGatlingItem(Settings settings) {
        super(settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);

    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private SunflowerGatlingRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new SunflowerGatlingRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "gatling_controller", 0, event -> {
            if (anyItemInUse) {
                event.getController().setAnimation(RawAnimation.begin().then("fire", Animation.LoopType.LOOP));
            } else {
               event.getController().setAnimation(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
            }
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE; // 使用弩的动作，双手握住但姿势更自然
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000; // 与弓一样的最大使用时间
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // 开始使用（持续射击）
        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        // 当物品实体被销毁时的处理，例如掉入岩浆
        super.onItemEntityDestroyed(entity);
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        super.usageTick(world, user, stack, remainingUseTicks);
        
        // 设置全局标志
        anyItemInUse = true;
        
        // 在NBT中设置使用状态
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putBoolean(USING_KEY, true);
        
        if (world.isClient) {
            return; // 客户端不处理射击逻辑
        }

        // 获取或初始化冷却时间
        int cooldown = nbt.getInt("Cooldown");

        // 检查冷却时间和耐久度
        if (cooldown <= 0 && stack.getDamage() < stack.getMaxDamage()) {
            // 创建并发射向日葵种子
            fireProjectile(world, user, stack);

            // 重置冷却时间
            nbt.putInt("Cooldown", FIRE_COOLDOWN);

            // 消耗耐久度
            stack.damage(1, user, (player) -> player.sendToolBreakStatus(user.getActiveHand()));
        } else if (cooldown > 0) {
            // 减少冷却时间
            nbt.putInt("Cooldown", cooldown - 1);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);
        
        // 检查实体是否正在使用这个物品
        boolean stillUsing = false;
        if (entity instanceof LivingEntity livingEntity && 
                livingEntity.isUsingItem() && 
                livingEntity.getActiveItem() == stack) {
            stillUsing = true;
        }
        
        // 只有不再使用时才重置标志
        if (!stillUsing) {
            anyItemInUse = false;
        }
        
        // 冷却时间逻辑保持不变...
    }

    // 发射种子弹
    private void fireProjectile(World world, LivingEntity user, ItemStack stack) {
        // 创建向日葵种子实体
        SunflowerSeedEntity seedEntity = new SunflowerSeedEntity(world, user);

        // 计算发射角度和速度
        // 获取玩家视线方向，确保直线发射
        float yaw = user.getYaw();
        float pitch = user.getPitch();
        float roll = 0.0F;

        // 转换为弧度
        float yawRadians = yaw * 0.017453292F;
        float pitchRadians = pitch * 0.017453292F;

        // 计算发射方向，基于玩家的视线方向
        double motionX = -MathHelper.sin(yawRadians) * MathHelper.cos(pitchRadians);
        double motionY = -MathHelper.sin(pitchRadians);
        double motionZ = MathHelper.cos(yawRadians) * MathHelper.cos(pitchRadians);
        
        // 设置种子的位置 - 从枪口位置发射
        Vec3d eyePos = user.getEyePos();
        Vec3d direction = new Vec3d(motionX, motionY, motionZ).normalize();
        
        // 调整发射位置，使其看起来像从武器枪口发射
        // 向前移动1.2格，向下移动0.3格，向右/左移动0.3格（根据主手/副手）
        Vec3d rightVector = new Vec3d(
            MathHelper.cos(yawRadians), 
            0, 
            MathHelper.sin(yawRadians)
        ).normalize();
        
        // 判断使用的是主手还是副手，相应调整水平偏移方向
        boolean isMainHand = user.getActiveHand() == Hand.MAIN_HAND;
        // 如果是左手模式的玩家，需要反转
        boolean isLeftHanded = user instanceof PlayerEntity && ((PlayerEntity)user).getMainArm() == Arm.LEFT;
        
        // 确定最终的水平偏移方向
        float horizontalOffset = 0.35f; // 减少水平偏移
        if (isLeftHanded) {
            // 左撇子玩家
            horizontalOffset = isMainHand ? horizontalOffset : -horizontalOffset;
        } else {
            // 右撇子玩家
            horizontalOffset = isMainHand ? -horizontalOffset : horizontalOffset;
        }
        
        // 计算最终的枪口位置
        Vec3d muzzlePos = eyePos
            .add(direction.multiply(1.3)) // 减少向前偏移
            .add(new Vec3d(0, -0.3, 0))   // 减少向下偏移
            .add(rightVector.multiply(horizontalOffset)); // 更适中的水平偏移
            
        seedEntity.setPosition(muzzlePos);

        // 设置种子的速度
        float velocity = 3.0f; // 速度因子，调整为合适的值
        seedEntity.setVelocity(motionX * velocity, motionY * velocity, motionZ * velocity);

        // 设置种子不追踪目标（直线发射）
        seedEntity.setHomingTarget(null);
        
        // 设置种子的伤害值为0.5
        seedEntity.setDamage(2f);

        // 将种子添加到世界
        world.spawnEntity(seedEntity);

        // 播放发射音效
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        // 可以使用向日葵进行修复
        return ingredient.isOf(Items.SUNFLOWER);
    }
}
