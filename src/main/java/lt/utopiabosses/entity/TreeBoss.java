package lt.utopiabosses.entity;

import lt.utopiabosses.registry.EntityRegistry;
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
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;

public class TreeBoss extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private final Random random = new Random();
    
    // 动画定义
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().then("idle", Animation.LoopType.LOOP);
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().then("walk", Animation.LoopType.LOOP);
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_LEFT_ANIM = RawAnimation.begin().then("attack_left", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_RIGHT_ANIM = RawAnimation.begin().then("attack_right", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DIE_ANIM = RawAnimation.begin().then("die", Animation.LoopType.PLAY_ONCE);
    
    // 添加BOSS血条
    private final ServerBossBar bossBar;
    
    // 定义数据追踪器
    private static final TrackedData<Integer> DATA_ANIMATION_ID = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> DATA_ANIMATION_PLAYING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> ATTACK_TYPE = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> IS_DYING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 攻击相关属性
    private int attackCooldown = 0;
    private boolean isAttacking = false;
    private int animationTicks = 0;
    private int deathTicks = 0;
    
    // 攻击类型枚举
    private enum AttackType {
        NONE,
        LEFT,
        RIGHT
    }
    
    public TreeBoss(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = new ServerBossBar(
            Text.literal("树木BOSS").formatted(Formatting.GREEN),
            BossBar.Color.GREEN, 
            BossBar.Style.PROGRESS
        );
    }
    
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 250.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D) // 提高移动速度
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D);
    }
    
    @Override
    protected void initGoals() {
        // 基础生存AI
        this.goalSelector.add(0, new SwimGoal(this)); // 游泳能力
        
        // 攻击AI
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0D, true)); // 近战攻击
        
        // 移动AI
        this.goalSelector.add(2, new WanderAroundFarGoal(this, 0.8D)); // 漫游移动
        
        // 基本观察AI
        this.goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 20.0F));
        this.goalSelector.add(4, new LookAroundGoal(this));
        
        // 目标选择AI
        this.targetSelector.add(1, new RevengeGoal(this)); // 受到伤害时反击
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DATA_ANIMATION_ID, 0);
        this.dataTracker.startTracking(DATA_ANIMATION_PLAYING, false);
        this.dataTracker.startTracking(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
        this.dataTracker.startTracking(IS_DYING, false);
    }

    @Override
    public void tick() {
        // 如果正在播放死亡动画
        if (this.dataTracker.get(IS_DYING)) {
            // 增加死亡计时器
            deathTicks++;
            
            // 使实体静止不动
            this.setVelocity(0, 0, 0);
            this.setYaw(this.prevYaw);
            this.setPitch(this.prevPitch);
            this.setBodyYaw(this.prevBodyYaw);
            this.setHeadYaw(this.prevHeadYaw);
            
            // 禁用AI和物理碰撞
            this.setNoGravity(true);
            
            // 如果死亡动画完成（1.83秒，约37帧）
            if (deathTicks >= 37) {
                // 实际移除实体
                this.remove(Entity.RemovalReason.KILLED);
                return;
            }
            
            return; // 跳过正常的tick逻辑
        }
        
        super.tick();
        
        if (!this.getWorld().isClient()) {
            // 更新血条
            this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
            
            // 攻击冷却时间处理
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            
            // 动画帧计数
            byte attackTypeByte = this.dataTracker.get(ATTACK_TYPE);
            boolean isPlayingAttackAnim = attackTypeByte != AttackType.NONE.ordinal();
            
            if (isPlayingAttackAnim) {
                animationTicks++;
                
                // 检查是否到达伤害帧（第20帧）
                if (animationTicks == 20) {
                    if (attackTypeByte == AttackType.LEFT.ordinal()) {
                        executeLeftAttack();
                    } else if (attackTypeByte == AttackType.RIGHT.ordinal()) {
                        executeRightAttack();
                    }
                }
                
                // 检查动画是否结束（总共30帧，1.5秒）
                if (animationTicks >= 30) {
                    // 重置攻击状态
                    this.dataTracker.set(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
                    animationTicks = 0;
                }
            } else {
                animationTicks = 0;
            }
        }
    }
    
    /**
     * 执行左手攻击效果
     */
    private void executeLeftAttack() {
        if (!this.getWorld().isClient() && this.getTarget() != null) {
            // 攻击范围 - 增加范围并添加垂直方向的范围
            Box attackBox = this.getBoundingBox().expand(5.0, 2.0, 5.0);
            List<LivingEntity> targets = this.getWorld().getNonSpectatingEntities(LivingEntity.class, attackBox);
            
            // 记录是否命中了任何目标
            boolean hitAnyTarget = false;
            
            // 获取BOSS的朝向向量
            Vec3d lookVec = Vec3d.fromPolar(0, this.getYaw());
            // 计算左侧向量（朝向左侧的单位向量）
            Vec3d leftVec = lookVec.rotateY((float)Math.toRadians(-90));
            // 计算左前方向量（处于左侧45度角的向量）
            Vec3d leftFrontVec = lookVec.add(leftVec).normalize();
            
            // 对左侧的目标造成伤害
            for (LivingEntity target : targets) {
                if (target != this && (!(target instanceof PlayerEntity) || !((PlayerEntity)target).isCreative())) {
                    // 计算目标相对于BOSS的方向向量
                    Vec3d dirToTarget = target.getPos().subtract(this.getPos()).normalize();
                    // 计算目标在水平面上的向量（忽略Y轴差异）
                    Vec3d horizontalDirToTarget = new Vec3d(dirToTarget.x, 0, dirToTarget.z).normalize();
                    
                    // 计算与左方向量的点积
                    double dotWithLeft = horizontalDirToTarget.dotProduct(leftVec);
                    // 计算与前方向量的点积
                    double dotWithForward = horizontalDirToTarget.dotProduct(lookVec);
                    // 计算与左前方向量的点积
                    double dotWithLeftFront = horizontalDirToTarget.dotProduct(leftFrontVec);
                    
                    // 如果目标在左侧（点积大于0）或在左前方范围内（与左前方向量点积大于0.5）
                    if (dotWithLeft > 0 || (dotWithLeftFront > 0.5 && dotWithForward > 0)) {
                        // 造成5颗心（10点）伤害
                        target.damage(this.getDamageSources().mobAttack(this), 10.0F);
                        
                        // 击退效果
                        Vec3d knockbackVec = new Vec3d(dirToTarget.x, 0.3, dirToTarget.z).multiply(0.7);
                        target.addVelocity(knockbackVec.x, knockbackVec.y, knockbackVec.z);
                        
                        hitAnyTarget = true;
                    }
                }
            }
            
            // 播放攻击音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.HOSTILE, 
                1.0F, 
                0.8F
            );
            
            // 如果没有命中任何目标且存在目标，尝试直接攻击当前目标
            if (!hitAnyTarget && this.getTarget() != null && this.distanceTo(this.getTarget()) <= 6.0f) {
                this.getTarget().damage(this.getDamageSources().mobAttack(this), 10.0F);
            }
        }
    }
    
    /**
     * 执行右手攻击效果
     */
    private void executeRightAttack() {
        if (!this.getWorld().isClient() && this.getTarget() != null) {
            // 攻击范围 - 增加范围并添加垂直方向的范围
            Box attackBox = this.getBoundingBox().expand(5.0, 2.0, 5.0);
            List<LivingEntity> targets = this.getWorld().getNonSpectatingEntities(LivingEntity.class, attackBox);
            
            // 记录是否命中了任何目标
            boolean hitAnyTarget = false;
            
            // 获取BOSS的朝向向量
            Vec3d lookVec = Vec3d.fromPolar(0, this.getYaw());
            // 计算右侧向量（朝向右侧的单位向量）
            Vec3d rightVec = lookVec.rotateY((float)Math.toRadians(90));
            // 计算右前方向量（处于右侧45度角的向量）
            Vec3d rightFrontVec = lookVec.add(rightVec).normalize();
            
            // 对右侧的目标造成伤害
            for (LivingEntity target : targets) {
                if (target != this && (!(target instanceof PlayerEntity) || !((PlayerEntity)target).isCreative())) {
                    // 计算目标相对于BOSS的方向向量
                    Vec3d dirToTarget = target.getPos().subtract(this.getPos()).normalize();
                    // 计算目标在水平面上的向量（忽略Y轴差异）
                    Vec3d horizontalDirToTarget = new Vec3d(dirToTarget.x, 0, dirToTarget.z).normalize();
                    
                    // 计算与右方向量的点积
                    double dotWithRight = horizontalDirToTarget.dotProduct(rightVec);
                    // 计算与前方向量的点积
                    double dotWithForward = horizontalDirToTarget.dotProduct(lookVec);
                    // 计算与右前方向量的点积
                    double dotWithRightFront = horizontalDirToTarget.dotProduct(rightFrontVec);
                    
                    // 如果目标在右侧（点积大于0）或在右前方范围内（与右前方向量点积大于0.5）
                    if (dotWithRight > 0 || (dotWithRightFront > 0.5 && dotWithForward > 0)) {
                        // 造成5颗心（10点）伤害
                        target.damage(this.getDamageSources().mobAttack(this), 10.0F);
                        
                        // 击退效果
                        Vec3d knockbackVec = new Vec3d(dirToTarget.x, 0.3, dirToTarget.z).multiply(0.7);
                        target.addVelocity(knockbackVec.x, knockbackVec.y, knockbackVec.z);
                        
                        hitAnyTarget = true;
                    }
                }
            }
            
            // 播放攻击音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                SoundCategory.HOSTILE, 
                1.0F, 
                1.2F
            );
            
            // 如果没有命中任何目标且存在目标，尝试直接攻击当前目标
            if (!hitAnyTarget && this.getTarget() != null && this.distanceTo(this.getTarget()) <= 6.0f) {
                this.getTarget().damage(this.getDamageSources().mobAttack(this), 10.0F);
            }
        }
    }
    
    /**
     * 处理关键帧事件
     * @param keyframeData 关键帧数据
     */
    private void handleKeyframeEvent(String keyframeData) {
        if (!this.getWorld().isClient()) {
            if (keyframeData.equals("attack_left_hit")) {
                // 左手攻击命中
                executeLeftAttack();
            } else if (keyframeData.equals("attack_right_hit")) {
                // 右手攻击命中
                executeRightAttack();
            } else if (keyframeData.equals("melee_attack_hit")) {
                // 普通攻击命中（普通攻击第20帧）
                if (this.getTarget() != null && this.distanceTo(this.getTarget()) <= 4.0f) {
                    this.getTarget().damage(this.getDamageSources().mobAttack(this), (float) this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                }
            }
        }
    }
    
    // BOSS血条相关方法
    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        // 添加自定义指令关键帧监听器
        AnimationController<TreeBoss> controller = new AnimationController<>(this, "controller", 0, this::predicate);
        
        // 使用匿名内部类处理关键帧事件
        controller.setCustomInstructionKeyframeHandler(event -> {
            // 获取关键帧指令并处理
            String instruction = event.getClass().getSimpleName(); // 临时方案：使用事件的类名
            // 尝试从事件中提取信息
            try {
                // 尝试反射获取字段值
                java.lang.reflect.Field field = event.getClass().getDeclaredField("instruction");
                field.setAccessible(true);
                instruction = (String) field.get(event);
            } catch (Exception e) {
                System.out.println("获取关键帧指令失败: " + e.getMessage());
                instruction = "melee_attack_hit"; // 默认为普通攻击命中
            }
            handleKeyframeEvent(instruction);
        });
        
        controllerRegistrar.add(controller);
    }
    
    /**
     * 处理关键帧事件
     */
    private void onKeyframeEvent(CustomInstructionKeyframeEvent<TreeBoss> event) {
        // 这个方法不再使用，因为我们现在使用匿名内部类处理事件
    }
    
    private PlayState predicate(AnimationState<TreeBoss> state) {
        // 检查是否正在死亡
        if (this.dataTracker.get(IS_DYING)) {
            // 播放死亡动画
            state.getController().setAnimation(DIE_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 获取实体当前状态
        byte attackType = this.dataTracker.get(ATTACK_TYPE);
        
        if (attackType == AttackType.LEFT.ordinal()) {
            // 播放左手攻击动画
            state.getController().setAnimation(ATTACK_LEFT_ANIM);
            return PlayState.CONTINUE;
        } else if (attackType == AttackType.RIGHT.ordinal()) {
            // 播放右手攻击动画
            state.getController().setAnimation(ATTACK_RIGHT_ANIM);
            return PlayState.CONTINUE;
        } else if (this.isAttacking && attackCooldown <= 0) {
            // 播放普通攻击动画
            state.getController().setAnimation(ATTACK_ANIM);
            return PlayState.CONTINUE;
        } else if (state.isMoving()) {
            // 移动时使用行走动画
            state.getController().setAnimation(WALK_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 默认使用闲置动画
        state.getController().setAnimation(IDLE_ANIM);
        return PlayState.CONTINUE;
    }

    // 覆盖攻击方法，设置攻击状态和冷却时间
    @Override
    public boolean tryAttack(Entity target) {
        // 如果攻击冷却中，取消攻击
        if (attackCooldown > 0) {
            return false;
        }
        
        // 随机选择左手或右手攻击
        AttackType attackToUse = random.nextBoolean() ? AttackType.LEFT : AttackType.RIGHT;
        this.dataTracker.set(ATTACK_TYPE, (byte)attackToUse.ordinal());
        
        this.isAttacking = true;
        this.attackCooldown = 40; // 2秒冷却时间
        this.animationTicks = 0;
        
        // 播放音效
        this.getWorld().playSound(
            null, 
            this.getX(), this.getY(), this.getZ(),
            SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
            SoundCategory.HOSTILE, 
            1.0F, 
            1.0F
        );
        
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }
    
    // 阻止被推动
    @Override
    public boolean isPushable() {
        return false;
    }
    
    // 覆盖原版的死亡方法
    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource source) {
        // 不调用super.onDeath()来防止默认的死亡效果
        
        // 标记为正在死亡
        this.dataTracker.set(IS_DYING, true);
        this.deathTicks = 0;
        
        // 禁用AI
        this.setAiDisabled(true);
        
        
        // 必须调用以确保游戏机制正确处理死亡
        this.setHealth(0.0F);
        this.onRemoved();
    }
    
    // 覆盖移除方法
    @Override
    public void remove(Entity.RemovalReason reason) {
        // 只有在不是因为死亡或者已经完成死亡动画时才执行移除
        if (reason != Entity.RemovalReason.KILLED || deathTicks >= 37) {
            super.remove(reason);
        }
    }
    
    // 死亡时清理血条
    @Override
    public void onRemoved() {
        if (bossBar != null) {
            bossBar.clearPlayers();
        }
        super.onRemoved();
    }
}
