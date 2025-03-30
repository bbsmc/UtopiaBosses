package lt.utopiabosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;

/**
 * 小树人实体类
 * 发现玩家后会震惊并冲向玩家，接近后自爆
 */
public class LittleTreeMan extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private final Random random = new Random();
    
    // 动画定义
    private static final RawAnimation IDLE_ANIM_1 = RawAnimation.begin().then("idle1", Animation.LoopType.LOOP);
    private static final RawAnimation IDLE_ANIM_2 = RawAnimation.begin().then("idle2", Animation.LoopType.LOOP);
    private static final RawAnimation RUN_ANIM = RawAnimation.begin().then("run", Animation.LoopType.LOOP);
    private static final RawAnimation SURPRISED_ANIM = RawAnimation.begin().then("surprised", Animation.LoopType.PLAY_ONCE);

    // 实体状态
    private static final TrackedData<Boolean> IS_SURPRISED = DataTracker.registerData(LittleTreeMan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_EXPLODING = DataTracker.registerData(LittleTreeMan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_TARGET = DataTracker.registerData(LittleTreeMan.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 计时器和状态
    private int surprisedTicks = 0;
    private int explodingTicks = 0;
    private boolean hasSeenPlayer = false;
    private int idleAnimationChoice = 0; // 0 = idle_1, 1 = idle_2
    
    // 自爆范围和伤害
    private static final float EXPLOSION_RADIUS = 3.0F; // 爆炸范围
    private static final float EXPLOSION_DAMAGE = 8.0F; // 爆炸伤害 (4颗心)

    public LittleTreeMan(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.idleAnimationChoice = random.nextInt(2); // 随机选择待机动画
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0D) // 10颗心
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D) // 普通攻击伤害
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D) // 普通移动速度
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D); // 较小的追踪范围
    }

    @Override
    protected void initGoals() {
        // 基础生存AI
        this.goalSelector.add(0, new SwimGoal(this));
        
        // 攻击AI - 只有在没有被惊讶状态和没有爆炸状态时才追击玩家
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.2D, false) {
            @Override
            public boolean canStart() {
                return !LittleTreeMan.this.dataTracker.get(IS_SURPRISED) 
                    && !LittleTreeMan.this.dataTracker.get(IS_EXPLODING) 
                    && super.canStart()
                    && LittleTreeMan.this.getTarget() instanceof PlayerEntity; // 确保目标是玩家
            }
        });
        
        // 移动AI
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8D));
        
        // 基本观察AI
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        
        // 目标选择AI - 只针对玩家
        this.targetSelector.add(1, new RevengeGoal(this) {
            @Override
            public boolean shouldContinue() {
                return super.shouldContinue() && this.mob.getAttacker() instanceof PlayerEntity;
            }
        });
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(IS_SURPRISED, false);
        this.dataTracker.startTracking(IS_EXPLODING, false);
        this.dataTracker.startTracking(HAS_TARGET, false);
    }

    @Override
    public void tick() {
        super.tick();
        
        // 确保目标只能是玩家
        if (this.getTarget() != null && !(this.getTarget() instanceof PlayerEntity)) {
            this.setTarget(null);
        }
        
        // 更新实体状态
        boolean hasTarget = this.getTarget() != null;
        this.dataTracker.set(HAS_TARGET, hasTarget);
        
        // 处理惊讶状态
        if (this.dataTracker.get(IS_SURPRISED)) {
            surprisedTicks++;
            // 设置实体静止
            this.setVelocity(0, this.getVelocity().y, 0);
            
            // 0.75秒后结束惊讶状态
            if (surprisedTicks >= 15) {
                this.dataTracker.set(IS_SURPRISED, false);
                surprisedTicks = 0;
            }
        } 
        // 处理爆炸状态
        else if (this.dataTracker.get(IS_EXPLODING)) {
            explodingTicks++;
            // 设置实体静止
            this.setVelocity(0, this.getVelocity().y, 0);
            
            // 增强爆炸前的视觉效果
            if (!this.getWorld().isClient()) {
                ServerWorld serverWorld = (ServerWorld)this.getWorld();
                
                // 简化粒子效果逻辑，因为爆炸时间更短
                // 每tick都产生粒子效果
                if (explodingTicks % 2 == 0) {
                    // 烟雾粒子
                    serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX() + random.nextFloat() * 0.4f - 0.2f,
                        this.getY() + 0.7 + random.nextFloat() * 0.3f,
                        this.getZ() + random.nextFloat() * 0.4f - 0.2f,
                        3, 0.1, 0.1, 0.1, 0.01
                    );
                    
                    // 火焰粒子
                    serverWorld.spawnParticles(
                        ParticleTypes.FLAME,
                        this.getX() + random.nextFloat() * 0.4f - 0.2f,
                        this.getY() + 0.5 + random.nextFloat() * 0.3f,
                        this.getZ() + random.nextFloat() * 0.4f - 0.2f,
                        2, 0.05, 0.05, 0.05, 0.01
                    );
                }
            }
            
            // 0.5秒后爆炸
            if (explodingTicks >= 10) {
                explode();
            }
        }
        // 处理发现玩家状态
        else if (hasTarget && !hasSeenPlayer) {
            // 第一次发现玩家，进入惊讶状态 - 只有当目标是玩家时才震惊
            if (this.getTarget() instanceof PlayerEntity) {
                this.dataTracker.set(IS_SURPRISED, true);
                surprisedTicks = 0;
                hasSeenPlayer = true;
                
                // 播放惊讶音效
                this.getWorld().playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_VILLAGER_HURT,
                    SoundCategory.HOSTILE,
                    1.0F,
                    1.5F // 较高的音调
                );
            } else {
                // 如果目标不是玩家，直接标记为已见过玩家，跳过惊讶阶段
                hasSeenPlayer = true;
            }
        }
        // 如果目标在2.5格内且已经看到过玩家，开始自爆
        else if (hasTarget && hasSeenPlayer && !this.dataTracker.get(IS_EXPLODING)) {
            LivingEntity target = this.getTarget();
            // 检查距离条件，无论目标是什么类型
            if (this.squaredDistanceTo(target) < 6.25) { // 2.5^2 = 6.25
                startExploding();
            }
        }
        
        // 如果失去目标，过一段时间后重置hasSeenPlayer状态
        if (!hasTarget && hasSeenPlayer && random.nextInt(100) == 0) {
            hasSeenPlayer = false;
            // 随机选择新的待机动画
            idleAnimationChoice = random.nextInt(2);
        }
    }
    
    /**
     * 开始自爆倒计时
     */
    private void startExploding() {
        if (!this.getWorld().isClient()) {
            this.dataTracker.set(IS_EXPLODING, true);
            explodingTicks = 0;
            
            // 播放爆炸前的嘶嘶声
            this.getWorld().playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_CREEPER_PRIMED,
                SoundCategory.HOSTILE,
                1.0F,
                1.0F
            );
        }
    }
    
    /**
     * 执行爆炸效果
     */
    private void explode() {
        if (!this.getWorld().isClient()) {
            // 播放爆炸音效
            this.getWorld().playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.BLOCKS,
                4.0F,
                (1.0F + (this.getWorld().random.nextFloat() - this.getWorld().random.nextFloat()) * 0.2F) * 0.7F
            );
            
            ServerWorld serverWorld = (ServerWorld)this.getWorld();
            
            // 创建爆炸粒子效果但不破坏方块
            // 爆炸核心
            serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION,
                this.getX(), this.getY() + 0.5, this.getZ(),
                2, 0.0, 0.0, 0.0, 0.0
            );
            
            // 爆炸火焰
            serverWorld.spawnParticles(
                ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 0.5, this.getZ(),
                1, 0.0, 0.0, 0.0, 0.0
            );
            
            // 烟雾云
            serverWorld.spawnParticles(
                ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY() + 0.5, this.getZ(),
                20, 1.0, 0.5, 1.0, 0.0
            );
            
            // 火花
            for (int i = 0; i < 15; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = random.nextDouble() * 1.5;
                double x = this.getX() + Math.sin(angle) * distance;
                double z = this.getZ() + Math.cos(angle) * distance;
                
                serverWorld.spawnParticles(
                    ParticleTypes.FLAME,
                    x, this.getY() + 0.1 + random.nextDouble() * 1.0, z,
                    0, 0, 0.1, 0, 0.1
                );
                
                if (i % 3 == 0) {
                    serverWorld.spawnParticles(
                        ParticleTypes.LAVA,
                        x, this.getY() + 0.1, z,
                        1, 0.1, 0.1, 0.1, 0.0
                    );
                }
            }
            
            // 对周围生物造成伤害
            Box damageBox = this.getBoundingBox().expand(EXPLOSION_RADIUS);
            List<LivingEntity> entities = this.getWorld().getNonSpectatingEntities(LivingEntity.class, damageBox);
            
            for (LivingEntity entity : entities) {
                if (entity != this) {
                    // 计算基于距离的伤害
                    double distance = this.squaredDistanceTo(entity);
                    double maxDistance = EXPLOSION_RADIUS * EXPLOSION_RADIUS;
                    
                    if (distance <= maxDistance) {
                        double damageMultiplier = 1.0 - Math.sqrt(distance) / EXPLOSION_RADIUS;
                        float damage = (float)(damageMultiplier * EXPLOSION_DAMAGE);
                        
                        entity.damage(this.getDamageSources().explosion(null, this), damage);
                        
                        // 计算击退
                        double knockbackFactor = 1.0 - distance / maxDistance;
                        Vec3d knockback = entity.getPos().subtract(this.getPos()).normalize().multiply(knockbackFactor);
                        entity.addVelocity(knockback.x, knockbackFactor * 0.5, knockback.z);
                    }
                }
            }
            
            // 移除实体
            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<LittleTreeMan> state) {
        // 爆炸和惊讶动画共用同一个动画
        if (this.dataTracker.get(IS_EXPLODING) || this.dataTracker.get(IS_SURPRISED)) {
            state.getController().setAnimation(SURPRISED_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 如果有目标，播放奔跑动画
        if (this.dataTracker.get(HAS_TARGET) && hasSeenPlayer) {
            state.getController().setAnimation(RUN_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 根据随机选择播放不同的待机动画
        if (idleAnimationChoice == 0) {
            state.getController().setAnimation(IDLE_ANIM_1);
        } else {
            state.getController().setAnimation(IDLE_ANIM_2);
        }
        
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }
    
    // 覆盖掉落物方法，可以根据需要修改掉落物
    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        super.dropLoot(source, causedByPlayer);
        // 可以在这里添加自定义掉落物
    }

    // 覆盖setTarget方法，确保只有玩家才能成为目标
    @Override
    public void setTarget(LivingEntity target) {
        // 如果目标不是玩家，直接忽略
        if (target != null && !(target instanceof PlayerEntity)) {
            return;
        }
        super.setTarget(target);
    }
} 