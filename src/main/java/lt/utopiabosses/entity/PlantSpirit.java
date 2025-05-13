package lt.utopiabosses.entity;

import lt.utopiabosses.registry.ItemRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Random;

/**
 * 植物精灵实体类
 * 敌对生物，具有和僵尸相同的生命值和攻击力
 */
public class PlantSpirit extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private final Random random = new Random();
    
    // 动画定义
    private static final RawAnimation IDEL_ANIM = RawAnimation.begin().then("idel", Animation.LoopType.LOOP);
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().then("walk", Animation.LoopType.LOOP);
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);
    
    // 实体状态
    private static final TrackedData<Boolean> IS_ATTACKING = DataTracker.registerData(PlantSpirit.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 攻击相关
    private int attackCooldown = 0;
    private static final int ATTACK_ANIMATION_LENGTH = 16; // 0.78秒攻击动画 ≈ 16 ticks
    private static final int ATTACK_DAMAGE_POINT = 10; // 0.5秒 = 10 ticks，攻击动画中造成伤害的时刻
    private int attackTicks = 0;
    private boolean hasDealtDamage = false; // 标记是否已经造成伤害
    
    // 存储当前的攻击目标
    private Entity attackTarget = null;

    public PlantSpirit(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * 创建实体属性，使用与僵尸相同的生命值和攻击力
     */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D) // 与僵尸相同的生命值 (10颗心)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D) // 与僵尸相同的攻击力
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D) // 与僵尸相同的移动速度
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D); // 追踪范围
    }
    
    @Override
    protected void initGoals() {
        // 攻击目标
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        
        // 行为目标
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.goalSelector.add(2, new RevengeGoal(this));
    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(IS_ATTACKING, false);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 处理攻击动画
        if (this.dataTracker.get(IS_ATTACKING)) {
            attackTicks++;
            
            // 在攻击动画的0.5秒处造成伤害
            if (attackTicks >= ATTACK_DAMAGE_POINT && !hasDealtDamage) {
                hasDealtDamage = true;
                if (attackTarget != null && attackTarget.isAlive() && attackTarget instanceof LivingEntity) {
                    // 执行实际的攻击逻辑
                    float damage = (float)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
                    
                    // 使用原版伤害系统造成伤害
                    boolean damaged = attackTarget.damage(this.getDamageSources().mobAttack(this), damage);
                    
                    if (damaged) {
                        this.applyDamageEffects(this, attackTarget);
                    }
                }
            }
            
            if (attackTicks >= ATTACK_ANIMATION_LENGTH) {
                // 攻击动画播放完毕，重置状态
                this.dataTracker.set(IS_ATTACKING, false);
                attackTicks = 0;
                hasDealtDamage = false;
                attackTarget = null;
            }
        }
        
        // 处理攻击冷却
        if (attackCooldown > 0) {
            attackCooldown--;
        }
    }
    
    @Override
    public boolean tryAttack(Entity target) {
        // 如果已经在攻击中则不执行新的攻击
        if (this.dataTracker.get(IS_ATTACKING)) {
            return false;
        }
        
        // 设置为攻击状态，但不立即造成伤害
        this.dataTracker.set(IS_ATTACKING, true);
        attackTicks = 0;
        attackCooldown = 20; // 1秒冷却
        
        // 记录要攻击的目标，在动画完成时再实际造成伤害
        this.attackTarget = target;
        
        return true; // 返回true表示开始攻击动作
    }
    
    /**
     * 动画控制器
     */
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 2, event -> {
            // 添加动画切换混合时间为2 (更流畅的动画过渡)
            
            // 如果正在攻击，播放攻击动画
            if (this.dataTracker.get(IS_ATTACKING)) {
                event.getController().setAnimation(ATTACK_ANIM);
                return PlayState.CONTINUE;
            }
            
            // 如果实体移动速度超过阈值，播放行走动画
            if (event.isMoving() || this.getVelocity().horizontalLengthSquared() > 0.0025) {
                event.getController().setAnimation(WALK_ANIM);
                return PlayState.CONTINUE;
            }
            
            // 否则播放待机动画
            event.getController().setAnimation(IDEL_ANIM);
            return PlayState.CONTINUE;
        }));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }
    
    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        // 死亡时掉落一个钻石
        this.dropItem(ItemRegistry.NATURAL_ESSENCE);
    }
} 