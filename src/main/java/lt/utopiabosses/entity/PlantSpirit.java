package lt.utopiabosses.entity;

import lt.utopiabosses.registry.ItemRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * 植物精灵实体类
 * 中立生物，只有在被攻击时才会反击，类似猪灵的反击机制
 */
public class PlantSpirit extends PathAwareEntity implements GeoEntity, Angerable {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    
    // 动画定义
    private static final RawAnimation IDEL_ANIM = RawAnimation.begin().then("idel", Animation.LoopType.LOOP);
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().then("walk", Animation.LoopType.LOOP);
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);
    
    // 实体状态
    private static final TrackedData<Boolean> IS_ATTACKING = DataTracker.registerData(PlantSpirit.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ANGER_TIME = DataTracker.registerData(PlantSpirit.class, TrackedDataHandlerRegistry.INTEGER);
    
    // 攻击相关
    private int attackCooldown = 0;
    private static final int ATTACK_ANIMATION_LENGTH = 16; // 0.78秒攻击动画 ≈ 16 ticks
    private static final int ATTACK_DAMAGE_POINT = 10; // 0.5秒 = 10 ticks，攻击动画中造成伤害的时刻
    private int attackTicks = 0;
    private boolean hasDealtDamage = false; // 标记是否已经造成伤害
    
    // 存储当前的攻击目标
    private Entity attackTarget = null;
    
    // 愤怒参数（类似猪灵）
    private static final UniformIntProvider ANGER_TIME_RANGE = TimeHelper.betweenSeconds(20, 39);
    private UUID targetUuid;

    public PlantSpirit(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * 创建实体属性，使用与僵尸相同的生命值和攻击力
     */
    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D) // 与僵尸相同的生命值 (10颗心)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D) // 与僵尸相同的攻击力
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D) // 与僵尸相同的移动速度
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D); // 追踪范围
    }
    
    /**
     * 判断植物精灵是否可以在特定位置生成
     */
    public static boolean canSpawn(EntityType<? extends PlantSpirit> type, ServerWorldAccess world, 
                                   SpawnReason reason, BlockPos pos, Random random) {
        return true;
    }
    
    @Override
    protected void initGoals() {
        // 行为目标
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        
        // 愤怒目标 - 猪灵风格的AI
        this.targetSelector.add(1, new UniversalAngerGoal<>(this, true));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(3, new RevengeGoal(this));
    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(IS_ATTACKING, false);
        this.dataTracker.startTracking(ANGER_TIME, 0);
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
        
        // 猪灵风格的愤怒机制
        if (!this.getWorld().isClient()) {
            this.tickAngerLogic((ServerWorld)this.getWorld(), true);
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
     * 当实体受到伤害时被调用
     */
    @Override
    public boolean damage(DamageSource source, float amount) {
        boolean damaged = super.damage(source, amount);
        
        if (damaged && !this.getWorld().isClient) {
            if (source.getAttacker() instanceof LivingEntity attacker) {
                this.setAngryAt(attacker.getUuid());
                this.setTarget((LivingEntity)attacker);
                
                // 让同区域的其他植物精灵也生气
                if (attacker instanceof PlayerEntity) {
                    this.getWorld().getNonSpectatingEntities(PlantSpirit.class, 
                            this.getBoundingBox().expand(16.0, 8.0, 16.0)).forEach(spirit -> {
                        if (spirit != this && spirit.getTarget() == null) {
                            spirit.setAngryAt(attacker.getUuid());
                            spirit.setTarget(attacker);
                        }
                    });
                }
            }
        }
        
        return damaged;
    }
    
    // 实现Angerable接口的方法
    
    @Override
    public int getAngerTime() {
        return this.dataTracker.get(ANGER_TIME);
    }

    @Override
    public void setAngerTime(int ticks) {
        this.dataTracker.set(ANGER_TIME, ticks);
    }

    @Override
    @Nullable
    public UUID getAngryAt() {
        return this.targetUuid;
    }

    @Override
    public void setAngryAt(@Nullable UUID uuid) {
        this.targetUuid = uuid;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME_RANGE.get(this.getRandom()));
    }
    
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        this.attackTarget = target;
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
    public Identifier getLootTableId() {
        return new Identifier("utopiabosses", "entities/plant_spirit");
    }
} 