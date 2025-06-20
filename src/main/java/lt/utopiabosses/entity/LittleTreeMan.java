package lt.utopiabosses.entity;

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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
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

    private static final RawAnimation BOOM_ANIM = RawAnimation.begin().then("boom", Animation.LoopType.PLAY_ONCE);

    // 实体状态
    private static final TrackedData<Boolean> IS_SURPRISED = DataTracker.registerData(LittleTreeMan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_EXPLODING = DataTracker.registerData(LittleTreeMan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> HAS_TARGET = DataTracker.registerData(LittleTreeMan.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> FLASH_INTENSITY = DataTracker.registerData(LittleTreeMan.class, TrackedDataHandlerRegistry.BYTE);
    
    // 计时器和状态
    private int surprisedTicks = 0;
    private int explodingTicks = 0;
    private boolean hasSeenPlayer = false;
    private int idleAnimationChoice = 0; // 0 = idle_1, 1 = idle_2
    private int runningTicks = 0; // 追踪奔跑动画播放时间
    private int idleTicks = 0; // 追踪待机动画播放时间
    private int boomAnimationTicks = 0; // 追踪爆炸动画播放时间
    private static final int BOOM_ANIMATION_LENGTH = 18; // 爆炸动画长度 (0.9167秒 ≈ 18 ticks)
    
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
        this.dataTracker.startTracking(FLASH_INTENSITY, (byte)0);
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
        
        // 处理待机动画计时器
        if (!hasTarget || !hasSeenPlayer || this.dataTracker.get(IS_SURPRISED) || this.dataTracker.get(IS_EXPLODING)) {
            idleTicks++;
            // 根据当前播放的待机动画选择重置时间
            int maxIdleTicks = idleAnimationChoice == 0 ? 90 : 33; // idle1=4.5秒(90ticks), idle2=1.6667秒(33ticks)
            if (idleTicks >= maxIdleTicks) {
                idleTicks = 0;
                // 动画播放完成后随机选择下一个待机动画
                idleAnimationChoice = random.nextInt(2);
            }
        } else {
            idleTicks = 0; // 不在待机状态时重置计时器
        }
        
        // 处理奔跑计时器
        if (hasTarget && hasSeenPlayer && !this.dataTracker.get(IS_SURPRISED) && !this.dataTracker.get(IS_EXPLODING)) {
            runningTicks++;
            if (runningTicks > 15) { // 0.75秒 = 15 ticks
                runningTicks = 0; // 重置计时器以循环播放动画
            }
        } else {
            runningTicks = 0; // 不在奔跑状态时重置计时器
        }
        
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
            boomAnimationTicks++;
            // 设置实体静止
            this.setVelocity(0, this.getVelocity().y, 0);
            
            // 闪白效果 - 随时间变化闪白强度
            if (!this.getWorld().isClient()) {
                byte flashIntensity = 0;
                
                // 在爆炸过程中，使用更强烈的闪白效果模式
                if (boomAnimationTicks < BOOM_ANIMATION_LENGTH) { // 使用动画长度而不是固定时间
                    // 使用交替闪白模式，每隔几帧闪烁一次
                    if (explodingTicks % 2 == 0) {
                        // 直接设置为最大亮度 - 确保明显的闪白
                        flashIntensity = 15;
                    } else {
                        // 保持最小亮度
                        flashIntensity = 5;
                    }
                    
                    // 爆炸临近时加快闪烁速度
                    if (boomAnimationTicks > BOOM_ANIMATION_LENGTH * 2/3) {
                        flashIntensity = (byte)(explodingTicks % 2 == 0 ? 15 : 10);
                    }
                }
                
                this.dataTracker.set(FLASH_INTENSITY, flashIntensity);
                
                ServerWorld serverWorld = (ServerWorld)this.getWorld();
                
                // 粒子效果频率随着爆炸临近增加
                if (explodingTicks % Math.max(1, 3 - boomAnimationTicks / 6) == 0) {
                    // 烟雾粒子
                    serverWorld.spawnParticles(
                        ParticleTypes.SMOKE,
                        this.getX() + random.nextFloat() * 0.4f - 0.2f,
                        this.getY() + 0.7 + random.nextFloat() * 0.3f,
                        this.getZ() + random.nextFloat() * 0.4f - 0.2f,
                        3, 0.1, 0.1, 0.1, 0.01
                    );
                    
                    // 火焰粒子 - 提前出现并增加数量
                    if (boomAnimationTicks > BOOM_ANIMATION_LENGTH / 2) {
                        serverWorld.spawnParticles(
                            ParticleTypes.FLAME,
                            this.getX() + random.nextFloat() * 0.4f - 0.2f,
                            this.getY() + 0.5 + random.nextFloat() * 0.3f,
                            this.getZ() + random.nextFloat() * 0.4f - 0.2f,
                            3, 0.05, 0.05, 0.05, 0.01
                        );
                    }
                }
            }
            
            // 当爆炸动画播放完成时爆炸
            if (boomAnimationTicks >= BOOM_ANIMATION_LENGTH) {
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
            boomAnimationTicks = 0;
            this.dataTracker.set(FLASH_INTENSITY, (byte)0);
            
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
        // 如果实体移动速度超过0.01，并且不在惊讶或爆炸状态，播放奔跑动画
        if (state.isMoving() || (this.getVelocity().horizontalLengthSquared() > 0.0025 && 
            !this.dataTracker.get(IS_SURPRISED) && 
            !this.dataTracker.get(IS_EXPLODING))) {
            // 设置奔跑动画并调整动画速度为1.25倍，使其更好地匹配0.75秒周期
            state.getController().setAnimationSpeed(1.25);
            state.getController().setAnimation(RUN_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 爆炸状态 - 保持当前动画,通过渲染器实现闪白效果
        if (this.dataTracker.get(IS_EXPLODING)) {
            state.getController().setAnimationSpeed(1.0);
            state.getController().setAnimation(BOOM_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 惊讶状态
        if (this.dataTracker.get(IS_SURPRISED)) {
            state.getController().setAnimationSpeed(1.0);
            state.getController().setAnimation(SURPRISED_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 没有移动时播放待机动画
        if (idleAnimationChoice == 0) {
            // idle1动画4.5秒，使用正常速度
            state.getController().setAnimationSpeed(1.0);
            state.getController().setAnimation(IDLE_ANIM_1);
        } else {
            // idle2动画1.6667秒，调整为正常速度
            state.getController().setAnimationSpeed(1.0);
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

    // 获取当前闪白强度，给渲染器使用
    public float getFlashIntensity() {
        if (!this.dataTracker.get(IS_EXPLODING)) {
            return 0.0f;
        }
        
        // 使用爆炸动画计时来计算闪白强度
        float progress = boomAnimationTicks / (float)BOOM_ANIMATION_LENGTH;
        
        // 使用更快的闪烁频率
        float flash = (float) Math.sin(progress * Math.PI * 4) * 0.5f + 0.5f;
        
        // 随着时间推移增加基础亮度
        float baseIntensity = progress * 0.6f;
        
        // 合并闪烁和基础亮度
        return Math.min(1.0f, flash + baseIntensity);
    }

    public boolean isExploding() {
        return this.dataTracker.get(IS_EXPLODING);
    }
} 