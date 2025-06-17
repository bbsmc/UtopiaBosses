package lt.utopiabosses.entity;

import lt.utopiabosses.registry.EntityRegistry;
import lt.utopiabosses.registry.SoundRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.particle.ParticleTypes;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.keyframe.event.CustomInstructionKeyframeEvent;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.minecraft.entity.ai.pathing.MobNavigation;

public class TreeBoss extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private final Random random = new Random();
    
    // 调试用-固定执行的技能类型
    public static SkillType DEBUG_FIXED_SKILL = null;


    
    // 动画定义
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().then("idle", Animation.LoopType.LOOP);
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().then("walk", Animation.LoopType.LOOP);
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().then("attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_LEFT_ANIM = RawAnimation.begin().then("attack_left", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ATTACK_RIGHT_ANIM = RawAnimation.begin().then("attack_right", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation DIE_ANIM = RawAnimation.begin().then("die", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation STOMP_ANIM = RawAnimation.begin().then("skill_stomping_feet", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation GRAB_ANIM = RawAnimation.begin().then("skill_grab_with_the_left_hand", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation VINES_ANIM = RawAnimation.begin().then("skill_insert_both_arms_into_the_ground_surface", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation ROAR_ANIM = RawAnimation.begin().then("skill_roar_towards_the_sky", Animation.LoopType.PLAY_ONCE);
    
    // 添加BOSS血条
    private final ServerBossBar bossBar;
    
    // 实体大小配置
    private static final float SIZE_SCALE = 2.0F; // 视觉模型比例保持不变
    private static final float COLLISION_SCALE = 0.8F; // 碰撞箱比例缩小
    
    // 定义数据追踪器
    private static final TrackedData<Integer> DATA_ANIMATION_ID = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> DATA_ANIMATION_PLAYING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> ATTACK_TYPE = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> IS_DYING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_STOMPING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_GRABBING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_SUMMONING_VINES = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_ROARING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 攻击相关属性
    private int attackCooldown = 0;
    private boolean isAttacking = false;
    private int animationTicks = 0;
    private int deathTicks = 0;
    private int stompTicks = 0;
    private int grabTicks = 0;
    private int vinesTicks = 0;
    private int roarTicks = 0;
    private int attackCounter = 0; // 攻击计数器，每4次普通攻击触发一次跺脚
    private PlayerEntity grabbedPlayer = null; // 被抓取的玩家
    private boolean hasThrown = false; // 是否已经投掷玩家
    
    // 藤曼相关属性
    private List<PlayerEntity> snaredPlayers = new ArrayList<>(); // 被禁锢的玩家
    private List<PlayerEntity> slowedPlayers = new ArrayList<>(); // 被减速的玩家
    private int vinesHealTicks = 0; // 回血计时器
    private int vinesDamageTicks = 0; // 伤害计时器
    private boolean vinesSpawned = false; // 藤曼是否已经生成
    
    // 攻击类型枚举
    private enum AttackType {
        NONE,
        LEFT,
        RIGHT
    }
    
    // 技能类型枚举
    public enum SkillType {
        STOMP,  // 跺脚技能
        GRAB,   // 抓取技能
        VINES,  // 藤曼技能
        ROAR    // 朝天怒吼技能
    }
    
    // 移动音效相关变量
    private int moveSoundCooldown = 0;
    private int walkAnimTicks = 0;
    private boolean isWalking = false;
    private boolean playedFirstStepSound = false;
    private boolean playedSecondStepSound = false;
    
    public TreeBoss(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
//         TreeBoss.setDebugFixedSkill(SkillType.ROAR);

        this.bossBar = new ServerBossBar(
            Text.literal("树木BOSS").formatted(Formatting.GREEN),
            BossBar.Color.GREEN, 
            BossBar.Style.PROGRESS
        );
    }



    EntityDimensions originalDimensions = new EntityDimensions(3F, 6F, true);

    EntityDimensions fakeDimensions = new EntityDimensions(1.1F, 6F, true);

    private boolean started = false;

    public EntityDimensions getDimensions(EntityPose pose) {
        return started ? fakeDimensions : originalDimensions;
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        // 删除自定义导航，使用原版MobNavigation
        return new MobNavigation(this, world);
    }
    
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 8.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28D) // 比正常生物(0.25)略快
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D) // 保持较大的追踪范围
                .add(EntityAttributes.GENERIC_ARMOR, 10.0D);
    }
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 40F));
        this.goalSelector.add(8, new LookAroundGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal(this, PlayerEntity.class, true));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1D, true));
//        // 基础生存AI
        this.goalSelector.add(0, new SwimGoal(this));

    }
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(DATA_ANIMATION_ID, 0);
        this.dataTracker.startTracking(DATA_ANIMATION_PLAYING, false);
        this.dataTracker.startTracking(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
        this.dataTracker.startTracking(IS_DYING, false);
        this.dataTracker.startTracking(IS_STOMPING, false);
        this.dataTracker.startTracking(IS_GRABBING, false);
        this.dataTracker.startTracking(IS_SUMMONING_VINES, false);
        this.dataTracker.startTracking(IS_ROARING, false);
    }

    @Override
    public void tick() {

        
        // 减少移动音效冷却
        if (moveSoundCooldown > 0) {
            moveSoundCooldown--;
        }
        
        // 如果正在播放死亡动画
        if (this.dataTracker.get(IS_DYING)) {
            // 增加死亡计时器
            deathTicks++;
            
            // 使实体静止不动
            this.setVelocity(0, 0, 0);
            
            // 完全禁用所有物理效果
            this.setNoGravity(true);
            this.setInvulnerable(true);
            this.fallDistance = 0; // 防止摔落效果
            
            // 如果死亡动画完成（1.83秒，约37帧）
            if (deathTicks >= 37) {
                // 清理血条
                if (bossBar != null) {
                    bossBar.clearPlayers();
                }
                
                // 实际移除实体
                this.remove(Entity.RemovalReason.KILLED);
                return;
            }

            
            return; // 跳过正常的tick逻辑
        }
        
        // 如果正在召唤藤曼
        if (this.dataTracker.get(IS_SUMMONING_VINES)) {
            // 增加技能计时器
            vinesTicks++;
            
            // 在1.5秒时生成藤曼
            if (vinesTicks == 30 && !this.getWorld().isClient()) {
                spawnVines();
                vinesSpawned = true;
            }
            
            // 在1.5-4.5秒期间回血
            if (vinesTicks >= 30 && vinesTicks <= 90 && !this.getWorld().isClient()) {
                vinesHealTicks++;
                
                // 每秒回复5颗心（10点）生命值
                if (vinesHealTicks >= 20) {
                    heal(10.0F);
                    vinesHealTicks = 0;
                    
                }
            }
            
            // 对藤曼范围内的玩家造成伤害和控制效果
            if (vinesSpawned && !this.getWorld().isClient()) {
                // 每10tick处理一次藤曼效果
                if (vinesTicks % 10 == 0) {
                    updateVinesEffects();
                }
                
                // 对被禁锢的玩家施加伤害
                vinesDamageTicks++;
                if (!snaredPlayers.isEmpty() && vinesDamageTicks >= 10) { // 每0.5秒一次伤害
                    for (PlayerEntity player : snaredPlayers) {
                        if (!player.isRemoved() && !player.isDead()) {
                            // 造成3颗心（6点）伤害
                            player.damage(this.getDamageSources().mobAttack(this), 6.0F);
                            

                        }
                    }
                    vinesDamageTicks = 0;
                }
            }
            
            // 在5秒后移除藤曼
            if (vinesTicks == 100 && !this.getWorld().isClient()) {
                removeVines();
            }
            
            // 检查动画是否结束（5.125秒，约102-103帧）
            if (vinesTicks >= 103) {
                // 重置技能状态
                this.dataTracker.set(IS_SUMMONING_VINES, false);
                vinesTicks = 0;
                vinesSpawned = false;
                vinesHealTicks = 0;
                vinesDamageTicks = 0;
                attackCooldown = 80; // 设置技能后的冷却时间
            }
            
//            super.tick();
            
//            // 固定视角方向
//            this.setYaw(savedYaw);
//            this.setPitch(savedPitch);
//            this.bodyYaw = savedBodyYaw;
//            this.headYaw = savedHeadYaw;
            
            return;
        }
        
        // 如果正在执行抓取技能
        if (this.dataTracker.get(IS_GRABBING)) {
            // 增加技能计时器
            grabTicks++;
            
            // 在0.25秒时尝试抓取玩家
            if (grabTicks == 5 && !this.getWorld().isClient()) {
                grabPlayer();
            }
            
            // 在1.25秒时投掷玩家
            if (grabTicks == 25 && !this.getWorld().isClient() && !hasThrown) {
                throwPlayer();
                hasThrown = true;
            }
            
            // 如果有抓住的玩家，更新其位置到左手
            if (grabbedPlayer != null && !hasThrown) {
                updateGrabbedPlayerPosition();
            }
            
            // 检查动画是否结束（1.75秒，约35帧）
            if (grabTicks >= 35) {
                // 重置技能状态
                this.dataTracker.set(IS_GRABBING, false);
                grabTicks = 0;
                hasThrown = false;
                grabbedPlayer = null;
                attackCooldown = 60; // 设置技能后的冷却时间
            }
            
//            super.tick();
            
            // 固定视角方向
//            this.setYaw(savedYaw);
//            this.setPitch(savedPitch);
//            this.bodyYaw = savedBodyYaw;
//            this.headYaw = savedHeadYaw;
            
            return;
        }
        
        // 如果正在执行跺脚技能
        if (this.dataTracker.get(IS_STOMPING)) {
            // 增加技能计时器
            stompTicks++;
            
            // 检查是否到达伤害帧（0.75秒，约15帧）
            if (stompTicks == 15) {
                executeStompAttack();
            }
            
            // 检查动画是否结束（1.75秒，约35帧）
            if (stompTicks >= 35) {
                // 重置技能状态
                this.dataTracker.set(IS_STOMPING, false);
                stompTicks = 0;
                attackCooldown = 40; // 设置技能后的冷却时间
            }

//            super.tick();

            // 固定视角方向
//            this.setYaw(savedYaw);
//            this.setPitch(savedPitch);
//            this.bodyYaw = savedBodyYaw;
//            this.headYaw = savedHeadYaw;

            return;
        }
        
        // 如果正在执行朝天怒吼技能
        if (this.dataTracker.get(IS_ROARING)) {
            // 增加技能计时器
            roarTicks++;
            
            // 设置实体静止
            this.setVelocity(0, this.getVelocity().y, 0);
            
            // 检查动画是否结束（1.5秒，约30帧）
            if (roarTicks >= 30) {
                // 动画结束，执行召唤小树人的效果
                if (!this.getWorld().isClient()) {
                    summonLittleTreeMen();
                }
                
                // 重置技能状态
                this.dataTracker.set(IS_ROARING, false);
                roarTicks = 0;
                attackCooldown = 60; // 设置技能后的冷却时间
            }

//            super.tick();

            // 固定视角方向
//            this.setYaw(savedYaw);
//            this.setPitch(savedPitch);
//            this.bodyYaw = savedBodyYaw;
//            this.headYaw = savedHeadYaw;

            return;
        }

        this.started = true;
        this.reinitDimensions();
        this.setBoundingBox(this.calculateBoundingBox());
        super.tick();
        this.started = false;
        this.reinitDimensions();
        this.setBoundingBox(this.calculateBoundingBox());
        
        // 每10ticks强制刷新一次动画状态
        if (this.getWorld().isClient() && this.getWorld().getTime() % 10 == 0) {
            forceAnimationRefresh();
            
//            // 如果开启了调试，每60ticks渲染一次伤害盒子
//            if (DEBUG_FIXED_SKILL != null && this.getWorld().getTime() % 60 == 0) {
//                renderHitboxes();
//            }
        }
        
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
                    // 确保cooldown设置
                    if (attackCooldown <= 0) {
                        attackCooldown = 20; // 确保攻击后有一个短暂的冷却
                    }
                }
                
                // 固定视角方向
//                this.setYaw(savedYaw);
//                this.setPitch(savedPitch);
//                this.bodyYaw = savedBodyYaw;
//                this.headYaw = savedHeadYaw;
            } else {
                animationTicks = 0;
            }
            
            // 添加调试日志
            if (this.getWorld().getTime() % 20 == 0) {
                System.out.println("TreeBoss状态: 攻击类型=" + attackTypeByte + ", 冷却=" + attackCooldown + ", 是否正在攻击=" + this.isAttacking);
            }
        }
    }
    
    /**
     * 强制刷新动画状态，确保动画控制器能够正确响应状态变化
     */
    private void forceAnimationRefresh() {
        if (this.getWorld().isClient()) {
            // 获取当前动画状态
            boolean isCurrentlyMoving = this.isMoving();
            boolean isAnyAnimationPlaying = this.dataTracker.get(IS_DYING) || 
                                         this.dataTracker.get(IS_SUMMONING_VINES) ||
                                         this.dataTracker.get(IS_GRABBING) ||
                                         this.dataTracker.get(IS_STOMPING) ||
                                         this.dataTracker.get(IS_ROARING) ||
                                         this.dataTracker.get(ATTACK_TYPE) != AttackType.NONE.ordinal();
            
            if (isCurrentlyMoving && !isAnyAnimationPlaying) {
                // 如果正在移动且没有播放其他动画，确保行走动画被激活
                // 这行不会实际改变动画，但会触发动画控制器重新评估状态
                this.addVelocity(0, 0, 0);
                
                if (this.getWorld().getTime() % 40 == 0) { // 每2秒输出一次日志
                    System.out.println("TreeBoss正在强制刷新移动动画状态");
                }
            }
        }
    }
    
    /**
     * 执行左手攻击效果
     */
    private void executeLeftAttack() {
        if (!this.getWorld().isClient() && this.getTarget() != null) {
            // 攻击范围 - 增加范围并添加垂直方向的范围
            Box attackBox = this.getBoundingBox().expand(5.0, 2.0, 5.0); // 恢复原始攻击范围
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
                        Vec3d knockbackVec = new Vec3d(dirToTarget.x, 0.3, dirToTarget.z).multiply(0.7); // 恢复原始击退力度
                        target.addVelocity(knockbackVec.x, knockbackVec.y, knockbackVec.z);
                        
                        hitAnyTarget = true;
                    }
                }
            }
            
            // 如果没有命中任何目标且存在目标，尝试直接攻击当前目标
            if (!hitAnyTarget && this.getTarget() != null && this.distanceTo(this.getTarget()) <= 6.0f) { // 恢复原始距离检测
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
            Box attackBox = this.getBoundingBox().expand(5.0, 2.0, 5.0); // 恢复原始攻击范围
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
                        Vec3d knockbackVec = new Vec3d(dirToTarget.x, 0.3, dirToTarget.z).multiply(0.7); // 恢复原始击退力度
                        target.addVelocity(knockbackVec.x, knockbackVec.y, knockbackVec.z);
                        
                        hitAnyTarget = true;
                    }
                }
            }
            
            // 如果没有命中任何目标且存在目标，尝试直接攻击当前目标
            if (!hitAnyTarget && this.getTarget() != null && this.distanceTo(this.getTarget()) <= 6.0f) { // 恢复原始距离检测
                this.getTarget().damage(this.getDamageSources().mobAttack(this), 10.0F);
            }
        }
    }
    
    /**
     * 执行跺脚践踏攻击
     */
    private void executeStompAttack() {
        if (!this.getWorld().isClient()) {
            // 大范围攻击区域
            Box attackBox = this.getBoundingBox().expand(8.0, 3.0, 8.0); // 恢复原始攻击范围
            List<LivingEntity> targets = this.getWorld().getNonSpectatingEntities(LivingEntity.class, attackBox);
            
            for (LivingEntity target : targets) {
                if (target != this && (!(target instanceof PlayerEntity) || !((PlayerEntity)target).isCreative())) {
                    // 计算目标相对于BOSS的方向向量
                    Vec3d dirToTarget = target.getPos().subtract(this.getPos()).normalize();
                    
                    // 造成8颗心（16点）伤害
                    target.damage(this.getDamageSources().mobAttack(this), 16.0F);
                    
                    // 向上和外侧击飞
                    float knockbackStrength = 1.5f - (this.distanceTo(target) / 8.0f); // 恢复原始最大距离
                    knockbackStrength = Math.max(0.3f, knockbackStrength); // 确保最小击退
                    
                    // 击飞效果
                    Vec3d knockbackVec = new Vec3d(dirToTarget.x, 0.8, dirToTarget.z).multiply(knockbackStrength); // 恢复原始击退力度
                    target.addVelocity(knockbackVec.x, knockbackVec.y, knockbackVec.z);
                    
                    // 确保客户端同步速度
                    if (target instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) target).networkHandler.sendPacket(
                            new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(target)
                        );
                    }
                }
            }
            
        }
    }
    
    /**
     * 尝试抓取附近的玩家
     */
    private void grabPlayer() {
        if (!this.getWorld().isClient()) {
            // 获取8格内的所有玩家
            Box searchBox = this.getBoundingBox().expand(8.0, 4.0, 8.0); // 恢复原始搜索范围
            List<PlayerEntity> players = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, searchBox);
            
            // 过滤掉创造模式的玩家
            players.removeIf(player -> player.isCreative() || player.isSpectator());
            
            // 如果附近有玩家
            if (!players.isEmpty()) {
                // 优先选择当前目标，如果目标不在列表中，则选择最近的玩家
                PlayerEntity targetPlayer = null;
                
                if (this.getTarget() instanceof PlayerEntity && players.contains(this.getTarget())) {
                    targetPlayer = (PlayerEntity) this.getTarget();
                } else {
                    double closestDistance = Double.MAX_VALUE;
                    for (PlayerEntity player : players) {
                        double distance = this.squaredDistanceTo(player);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            targetPlayer = player;
                        }
                    }
                }
                
                if (targetPlayer != null) {
                    // 抓取玩家
                    this.grabbedPlayer = targetPlayer;
                    
                    
                }
            }
        }
    }
    
    /**
     * 更新被抓取玩家的位置到BOSS的左手
     */
    private void updateGrabbedPlayerPosition() {
        if (grabbedPlayer != null && !grabbedPlayer.isRemoved() && !grabbedPlayer.isDead()) {
            // 根据动画帧计算偏移
            float animProgress = Math.min(1.0f, grabTicks / 15.0f); // 0-15帧的动画进度，最大为1.0
            
            // 获取基本方向向量
            Vec3d lookVec = Vec3d.fromPolar(0, this.getYaw());
            
            // 不同动画阶段的手臂位置变化
            Vec3d leftVec;
            Vec3d upVec;
            Vec3d frontVec;
            
            if (grabTicks < 10) { // 抓取初期(0-0.5秒)：手臂向前伸出
                // 手臂从体侧向前方伸出
                float phase1Progress = grabTicks / 10.0f;
                // 修正为真正的左手方向 (正确使用+90度旋转)
                leftVec = lookVec.rotateY((float)Math.toRadians(90 - phase1Progress * 30)).multiply(3.5 - phase1Progress * 1.0);
                upVec = new Vec3d(0, 3.0 - phase1Progress * 0.5, 0);
                frontVec = lookVec.multiply(0.5 + phase1Progress * 2.0);
            } else if (grabTicks < 15) { // 抓取中期(0.5-0.75秒)：手臂收回
                // 手臂抓住玩家后往回收
                float phase2Progress = (grabTicks - 10) / 5.0f;
                // 修正为真正的左手方向 (正确使用+60度旋转)
                leftVec = lookVec.rotateY((float)Math.toRadians(60 + phase2Progress * 10)).multiply(2.5 - phase2Progress * 0.5);
                upVec = new Vec3d(0, 2.5 + phase2Progress * 1.0, 0);
                frontVec = lookVec.multiply(2.5 - phase2Progress * 1.0);
            } else { // 抓取后期(0.75秒后)：手臂稳定抓住
                // 修正为真正的左手方向 (正确使用+70度旋转)
                leftVec = lookVec.rotateY((float)Math.toRadians(70)).multiply(2.0);
                upVec = new Vec3d(0, 3.5, 0);
                frontVec = lookVec.multiply(1.5);
            }
            
            // 计算左手的绝对位置
            Vec3d handPos = this.getPos().add(leftVec).add(upVec).add(frontVec);
            
            // 设置玩家位置
            grabbedPlayer.teleport(handPos.x, handPos.y, handPos.z);
            grabbedPlayer.setVelocity(0, 0, 0);
            grabbedPlayer.fallDistance = 0;
            
            // 设置玩家视角朝向BOSS
            if (grabTicks > 5) {
                // 计算从玩家到BOSS的方向
                float yaw = (float)Math.toDegrees(Math.atan2(
                    this.getZ() - grabbedPlayer.getZ(),
                    this.getX() - grabbedPlayer.getX()
                ));
                grabbedPlayer.setYaw(yaw);
                grabbedPlayer.setHeadYaw(yaw);
            }
            
            // 如果是服务器玩家，同步速度
            if (grabbedPlayer instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) grabbedPlayer).networkHandler.sendPacket(
                    new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(grabbedPlayer)
                );
            }
        }
    }
    
    /**
     * 投掷被抓取的玩家
     */
    private void throwPlayer() {
        if (grabbedPlayer != null && !grabbedPlayer.isRemoved() && !grabbedPlayer.isDead()) {
            // 计算投掷方向（向前上方）
            Vec3d lookVec = Vec3d.fromPolar(0, this.getYaw());
            Vec3d throwVec = lookVec.multiply(2.5).add(0, 0.8, 0);
            
            // 施加投掷速度
            grabbedPlayer.setVelocity(throwVec);
            grabbedPlayer.velocityModified = true;
            
            // 设置玩家为被投掷状态（用于检测落地伤害）
            markPlayerAsThrown(grabbedPlayer);
            

            
            // 如果是服务器玩家，同步速度
            if (grabbedPlayer instanceof ServerPlayerEntity) {
                ((ServerPlayerEntity) grabbedPlayer).networkHandler.sendPacket(
                    new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(grabbedPlayer)
                );
            }
        }
    }
    
    /**
     * 标记玩家为被投掷状态，用于检测落地伤害
     */
    private void markPlayerAsThrown(PlayerEntity player) {
        // 检测玩家着陆并造成伤害的逻辑
        // 这里不再使用DataTracker标记，改用其他方式
        
        // 在服务端添加一个临时监听器来检测玩家着陆
        if (!this.getWorld().isClient()) {
            // 在服务端为玩家设置一个标记
            ((ServerPlayerEntity) player).server.execute(new Runnable() {
                private int checkTicks = 0;
                private boolean hasHitGround = false;
                
                @Override
                public void run() {
                    checkTicks++;
                    
                    // 检查玩家是否已着陆或已死亡，否则继续检测
                    if (player.isRemoved() || player.isDead() || hasHitGround || checkTicks > 100) {
                        return;
                    }
                    
                    // 检测玩家是否撞到了方块
                    boolean onGround = player.isOnGround();
                    boolean inAir = !player.isOnGround() && player.getVelocity().y < -0.1;
                    
                    if (onGround && checkTicks > 5) {
                        // 玩家着陆，造成伤害
                        player.damage(player.getDamageSources().fall(), 8.0F);
                        hasHitGround = true;
                        
                        
                        
                        return;
                    }
                    
                    // 继续检测
                    ((ServerPlayerEntity) player).server.execute(this);
                }
            });
        }
    }
    
    /**
     * 使用抓取技能
     */
    private void useGrabAttack() {
        if (!this.getWorld().isClient()) {
            // 确保BOSS面向目标
            if (this.getTarget() != null) {
                faceEntity(this.getTarget());
            }
            
            // 设置抓取状态
            this.dataTracker.set(IS_GRABBING, true);
            this.grabTicks = 0;
            this.hasThrown = false;
            
            // 重置攻击状态
            this.dataTracker.set(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());

        }
    }
    
    /**
     * 使用跺脚技能
     */
    private void useStompAttack() {
        if (!this.getWorld().isClient()) {
            // 确保BOSS面向目标
            if (this.getTarget() != null) {
                faceEntity(this.getTarget());
            }
            
            // 设置跺脚状态
            this.dataTracker.set(IS_STOMPING, true);
            this.stompTicks = 0;
            
            // 重置攻击状态
            this.dataTracker.set(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
            

        }
    }
    
    /**
     * 使用藤曼技能
     */
    private void useVinesAttack() {
        if (!this.getWorld().isClient()) {
            // 确保BOSS面向目标
            if (this.getTarget() != null) {
                faceEntity(this.getTarget());
            }
            
            // 设置藤曼状态
            this.dataTracker.set(IS_SUMMONING_VINES, true);
            this.vinesTicks = 0;
            this.vinesSpawned = false;
            this.vinesHealTicks = 0;
            this.vinesDamageTicks = 0;
            
            // 在此处立即生成特效实体，确保与动画完全同步
            lt.utopiabosses.util.EffectHelper.spawnSummoningEffect(this);
            
            // 重置攻击状态
            this.dataTracker.set(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
            

        }
    }
    
    /**
     * 使用朝天怒吼技能
     */
    private void useRoarAttack() {
        if (!this.getWorld().isClient()) {
            // 确保BOSS面向目标
            if (this.getTarget() != null) {
                faceEntity(this.getTarget());
            }
            
            // 设置怒吼状态
            this.dataTracker.set(IS_ROARING, true);
            this.roarTicks = 0;
            
            // 重置攻击状态
            this.dataTracker.set(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
            

        }
    }
    
    /**
     * 让BOSS面向指定实体
     */
    private void faceEntity(Entity entity) {
//        // 计算BOSS和目标之间的向量
//        double dx = entity.getX() - this.getX();
//        double dz = entity.getZ() - this.getZ();
//        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
//
//        // 设置面向目标的视角
//        this.setYaw(yaw);
//        this.setHeadYaw(yaw);
//        this.setBodyYaw(yaw);
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
            } else if (keyframeData.equals("stomp_hit")) {
                // 跺脚践踏命中
                executeStompAttack();
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
        controllerRegistrar.add(
            // 主动画控制器
            new AnimationController<>(this, "main_controller", 2, event -> {
                // 获取速度
                double speed = Math.sqrt(
                    this.getVelocity().x * this.getVelocity().x + 
                    this.getVelocity().z * this.getVelocity().z
                );
                
                // 基于状态决定动画
                if (this.dataTracker.get(IS_DYING)) {
                    event.getController().setAnimation(DIE_ANIM);
                } else if (this.dataTracker.get(IS_SUMMONING_VINES)) {
                    event.getController().setAnimation(VINES_ANIM);
                } else if (this.dataTracker.get(IS_GRABBING)) {
                    event.getController().setAnimation(GRAB_ANIM);
                } else if (this.dataTracker.get(IS_STOMPING)) {
                    event.getController().setAnimation(STOMP_ANIM);
                } else if (this.dataTracker.get(IS_ROARING)) {
                    event.getController().setAnimation(ROAR_ANIM);
                } else if (this.dataTracker.get(ATTACK_TYPE) == AttackType.LEFT.ordinal()) {
                    event.getController().setAnimation(ATTACK_LEFT_ANIM);
                } else if (this.dataTracker.get(ATTACK_TYPE) == AttackType.RIGHT.ordinal()) {
                    event.getController().setAnimation(ATTACK_RIGHT_ANIM);
                } else if (this.isAttacking && attackCooldown <= 0) {
                    event.getController().setAnimation(ATTACK_ANIM);
                } else if (isMoving()) { // 使用isMoving方法代替直接检查速度
                    event.getController().setAnimation(WALK_ANIM);
                    if (this.getWorld().isClient() && this.getWorld().getTime() % 100 == 0) {
                        System.out.println("Animation Controller: TreeBoss正在移动");
                    }
                } else {
                    event.getController().setAnimation(IDLE_ANIM);
                    if (this.getWorld().isClient() && this.getWorld().getTime() % 100 == 0) {
                        System.out.println("Animation Controller: TreeBoss静止中");
                    }
                }
                
                return PlayState.CONTINUE;
            })
            .setCustomInstructionKeyframeHandler(event -> {
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
            })
            .setSoundKeyframeHandler(event -> {
                // 处理声音关键帧
                try {
                    if (getWorld().isClient()){
                        MinecraftClient.getInstance().getSoundManager().stopSounds(SoundRegistry.ENTITY_TREEBOSS_STOMPING_FEET_ID,null);
                        MinecraftClient.getInstance().getSoundManager().stopSounds(SoundRegistry.ENTITY_TREEBOSS_INSERT_BOTH_ARMS_INTO_THE_GROUND_SURFACE_ID,null);
                        MinecraftClient.getInstance().getSoundManager().stopSounds(SoundRegistry.ENTITY_TREEBOSS_ROAR_TOWARDS_THE_SKY_ID,null);
                        MinecraftClient.getInstance().getSoundManager().stopSounds(SoundRegistry.ENTITY_TREEBOSS_GRAB_WITH_THE_LEFT_HAND_ID,null);

                        if (event.getKeyframeData().getSound().equals("skill_stomping_feet")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_STOMPING_FEET,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("skill_grab_with_the_left_hand")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_GRAB_WITH_THE_LEFT_HAND,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("skill_insert_both_arms_into_the_ground_surface")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_INSERT_BOTH_ARMS_INTO_THE_GROUND_SURFACE,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("skill_roar_towards_the_sky")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_ROAR_TOWARDS_THE_SKY,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("tree_run")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_TREE_RUN,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("tree_run2")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_TREE_RUN2,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("attack_left")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_ATTACK_LEFT,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("attack_right")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_ATTACK_RIGHT,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                        else if (event.getKeyframeData().getSound().equals("die")) {
                            // 使用ClientPlayer播放声音
                            MinecraftClient.getInstance().getSoundManager().play(
                                    PositionedSoundInstance.master(
                                            SoundRegistry.ENTITY_TREEBOSS_DIE,
                                            1.0F,  // 音调
                                            1.0F   // 增大音量
                                    )
                            );
                        }
                    }
                } catch (Exception e) {
                    System.out.println("获取声音关键帧数据失败: " + e.getMessage());
                }
            })
        );
    }
    
    // 覆盖攻击方法，设置攻击状态和冷却时间
    @Override
    public boolean tryAttack(Entity target) {
        // 如果攻击冷却中，取消攻击
        if (attackCooldown > 0) {
            return false;
        }
        
        // 增加攻击计数
        attackCounter++;
        
        // 首先面向目标
        faceEntity(target);
        
        // 判断是否达到技能触发条件（第4次或第7次攻击）或 DEBUG 模式
        boolean shouldUseSkill = DEBUG_FIXED_SKILL != null || (attackCounter >= 4);
        
        // 如果达到技能释放条件
        if (shouldUseSkill) {
            // 重置攻击计数
            attackCounter = 0;
            
            // 根据调试设置决定使用哪个技能
            if (DEBUG_FIXED_SKILL != null) {
                // 使用固定技能
                if (DEBUG_FIXED_SKILL == SkillType.STOMP) {
                    useStompAttack();
                } else if (DEBUG_FIXED_SKILL == SkillType.GRAB) {
                    useGrabAttack();
                } else if (DEBUG_FIXED_SKILL == SkillType.VINES) {
                    useVinesAttack();
                } else if (DEBUG_FIXED_SKILL == SkillType.ROAR) {
                    useRoarAttack();
                }
            } else {
                // 使用随机技能逻辑
                int skillChoice = random.nextInt(4); // 0-3的随机数
                switch (skillChoice) {
                    case 0:
                        useStompAttack();
                        break;
                    case 1:
                        useGrabAttack();
                        break;
                    case 2:
                        useVinesAttack();
                        break;
                    case 3:
                        useRoarAttack();
                        break;
                }
            }
            return true;
        }
        
        // 随机选择左手或右手攻击
        AttackType attackToUse = random.nextBoolean() ? AttackType.LEFT : AttackType.RIGHT;
        this.dataTracker.set(ATTACK_TYPE, (byte)attackToUse.ordinal());
        
        this.isAttacking = true;
        this.attackCooldown = 40; // 2秒冷却时间
        this.animationTicks = 0;
        
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
        // 确保BOSS面向最后的攻击者或玩家
        if (source.getAttacker() != null) {
            faceEntity(source.getAttacker());
        } else if (this.getTarget() != null) {
            faceEntity(this.getTarget());
        } else {
            // 如果没有明确的目标，尝试找到最近的玩家
            PlayerEntity closestPlayer = this.getWorld().getClosestPlayer(this, 32.0);
            if (closestPlayer != null) {
                faceEntity(closestPlayer);
            }
        }
        
        // 不调用super.onDeath()来防止默认的死亡效果
        
        // 标记为正在死亡
        this.dataTracker.set(IS_DYING, true);
        this.deathTicks = 0;
        
        // 禁用AI
        this.setAiDisabled(true);
        
        // 设置为无敌状态，防止其他伤害打断死亡动画
        this.setInvulnerable(true);
        
        // 设置生命值为1而不是0，防止触发原版死亡效果
        this.setHealth(1.0F);
        
        // 掉落自定义物品
        this.dropCustomLoot(source);
        
        // 不要调用onRemoved()，它会立即清理血条
    }
    
    /**
     * 自定义掉落逻辑 - 使用战利品表
     */
    private void dropCustomLoot(net.minecraft.entity.damage.DamageSource source) {
        if (!this.getWorld().isClient()) {
            // 使用战利品表进行掉落
            this.dropLoot(source, true);
            
            // 掉落经验
            this.dropXp();
        }
    }
    
    @Override
    public net.minecraft.util.Identifier getLootTableId() {
        return new net.minecraft.util.Identifier("utopiabosses", "entities/tree_boss");
    }
    
    // 覆盖移除方法
    @Override
    public void remove(Entity.RemovalReason reason) {
        // 只有在不是因为死亡或者已经完成死亡动画时才执行移除
        if (reason != Entity.RemovalReason.KILLED || deathTicks >= 37) {
            // 如果是死亡动画结束，确保清理血条
            if (reason == Entity.RemovalReason.KILLED && deathTicks >= 37 && bossBar != null) {
                bossBar.clearPlayers();
            }
            super.remove(reason);
        }
    }
    
    // 覆盖默认的获取死亡声音方法，返回null以禁用死亡声音
    @Override
    protected net.minecraft.sound.SoundEvent getDeathSound() {
        return null;
    }
    
    // 覆盖受伤方法，在死亡动画播放时不显示受伤效果
    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        // 如果正在死亡，完全阻止所有伤害
        if (this.dataTracker.get(IS_DYING)) {
            return false;
        }
        
        // 是否命中上半身标志
        boolean hitUpperBody = false;
        
        // 如果是玩家造成的伤害，执行额外的碰撞检测
        if (source.getAttacker() instanceof PlayerEntity && !this.getWorld().isClient) {
            PlayerEntity player = (PlayerEntity)source.getAttacker();
            
            // 获取当前正常碰撞箱
            Box normalHitbox = this.getBoundingBox();
            float boxHeight = (float)(normalHitbox.maxY - normalHitbox.minY);
            
            // 创建扩展的上半身碰撞箱 - 只用于伤害检测
            float halfWidth = this.getWidth() / 2.0F;
            Box upperBodyHitbox = new Box(
                this.getX() - halfWidth, 
                normalHitbox.minY + boxHeight * 0.5F, // 从一半高度开始
                this.getZ() - halfWidth,
                this.getX() + halfWidth, 
                normalHitbox.minY + boxHeight * 1.2F, // 向上延伸20%
                this.getZ() + halfWidth
            );
            
            // 判断玩家攻击是否击中了上半身扩展碰撞箱
            double reachDistance = player.isCreative() ? 6.0D : 3.0D;
            Vec3d eyePos = player.getEyePos();
            Vec3d lookVec = player.getRotationVec(1.0F);
            Vec3d targetVec = eyePos.add(lookVec.x * reachDistance, lookVec.y * reachDistance, lookVec.z * reachDistance);
            
            // 如果击中了上半身扩展碰撞箱
            if (upperBodyHitbox.raycast(eyePos, targetVec).isPresent()) {
                System.out.println("TreeBoss上半身被击中!");
                hitUpperBody = true;
            }
        }
        
        // 处理伤害
        boolean damaged = super.damage(source, amount);
        
        // 如果成功造成伤害且生命值低于阈值，触发死亡程序
        if (damaged && this.getHealth() <= 0 && !this.dataTracker.get(IS_DYING)) {
            this.dataTracker.set(IS_DYING, true);
            
            // 设置无敌以防止在死亡动画中再次收到伤害
            this.setInvulnerable(true);
            
            // 停止所有动画状态
            stopAllAnimations();
            
            // 设置死亡计时器
            this.deathTicks = 0;
            
            return true;
        }
        
        return damaged;
    }
    
    // 停止所有动画状态的辅助方法
    private void stopAllAnimations() {
        this.dataTracker.set(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
        this.dataTracker.set(IS_STOMPING, false);
        this.dataTracker.set(IS_GRABBING, false);
        this.dataTracker.set(IS_SUMMONING_VINES, false);
        this.dataTracker.set(IS_ROARING, false);
        
        // 重置状态变量
        this.isAttacking = false;
        this.animationTicks = 0;
        this.stompTicks = 0;
        this.grabTicks = 0;
        this.vinesTicks = 0;
        this.roarTicks = 0;
        this.attackCounter = 0;
        
        // 如果抓住了玩家，释放他们
        if (this.grabbedPlayer != null) {
            this.throwPlayer();
        }
    }
    
    // 死亡时清理血条-这个方法不再直接从onDeath调用
    @Override
    public void onRemoved() {
        if (bossBar != null) {
            bossBar.clearPlayers();
        }
        super.onRemoved();
    }

    /**
     * 设置固定执行的技能类型(调试用)
     * @param skillType 要固定执行的技能类型，null表示随机执行
     */
    public static void setDebugFixedSkill(SkillType skillType) {
        DEBUG_FIXED_SKILL = skillType;
        System.out.println("TreeBoss调试模式: " + (skillType == null ? "随机技能" : skillType));
    }

    /**
     * 生成藤曼
     */
    private void spawnVines() {
        if (!this.getWorld().isClient()) {
            // 清空之前的玩家列表
            snaredPlayers.clear();
            slowedPlayers.clear();
            

            
            // 生成藤曼粒子效果
            spawnVinesParticles();
            
            // 应用初始减速效果
            applySlowEffectToNearbyPlayers();
        }
    }
    
    /**
     * 移除藤曼
     */
    private void removeVines() {
        if (!this.getWorld().isClient()) {
            // 移除所有玩家的控制效果
            for (PlayerEntity player : slowedPlayers) {
                if (!player.isRemoved() && !player.isDead()) {
                    player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
                }
            }
            
            for (PlayerEntity player : snaredPlayers) {
                if (!player.isRemoved() && !player.isDead()) {
                    // 移除禁锢效果
                    removeSnareEffect(player);
                }
            }
            
            // 清空列表
            snaredPlayers.clear();
            slowedPlayers.clear();
            

        }
    }
    
    /**
     * 更新藤曼效果
     */
    private void updateVinesEffects() {
        // 首先应用减速效果到范围内的玩家
        applySlowEffectToNearbyPlayers();
        
        // 然后检查哪些玩家需要被禁锢
        if (vinesTicks >= 60) { // 3秒后开始禁锢效果
            for (PlayerEntity player : slowedPlayers) {
                if (!player.isRemoved() && !player.isDead() && !snaredPlayers.contains(player)) {
                    // 应用禁锢效果
                    applySnareEffect(player);
                    snaredPlayers.add(player);
                    

                }
            }
        }
    }
    
    /**
     * 对附近玩家应用减速效果
     */
    private void applySlowEffectToNearbyPlayers() {
        // 获取8格内的所有玩家
        Box searchBox = this.getBoundingBox().expand(8.0, 3.0, 8.0); // 恢复原始搜索范围
        List<PlayerEntity> nearbyPlayers = this.getWorld().getNonSpectatingEntities(PlayerEntity.class, searchBox);
        
        // 过滤掉创造模式的玩家
        nearbyPlayers.removeIf(player -> player.isCreative() || player.isSpectator());
        
        // 应用减速效果
        for (PlayerEntity player : nearbyPlayers) {
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SLOWNESS, 
                30, // 持续1.5秒
                2, // 减速3级
                false, // 不显示粒子
                true, // 显示图标
                true // 可见
            ));
            
            // 添加到已减速列表
            if (!slowedPlayers.contains(player)) {
                slowedPlayers.add(player);
            }
            
            // 在玩家周围生成粒子
            spawnVineParticlesAround(player);
        }
    }
    
    /**
     * 应用禁锢效果
     */
    private void applySnareEffect(PlayerEntity player) {
        // 禁锢效果通过强力减速和挖掘疲劳实现
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SLOWNESS, 
            40, // 持续2秒
            6, // 减速7级 (几乎无法移动)
            false, 
            true, 
            true
        ));
        
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.MINING_FATIGUE, 
            40, // 持续2秒
            4, // 5级挖掘疲劳
            false, 
            true, 
            true
        ));
        
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.WEAKNESS, 
            40, // 持续2秒
            2, // 3级虚弱
            false, 
            true, 
            true
        ));
    }
    
    /**
     * 移除禁锢效果
     */
    private void removeSnareEffect(PlayerEntity player) {
        player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOWNESS);
        player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.MINING_FATIGUE);
        player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.WEAKNESS);
    }
    
    /**
     * 生成藤曼粒子效果
     */
    private void spawnVinesParticles() {
        // 在服务端计算粒子位置，然后发送到客户端
        if (!this.getWorld().isClient()) {
            // 计算12根藤曼的位置
            for (int i = 0; i < 12; i++) {
                // 计算角度
                double angle = Math.toRadians(i * 30); // 每30度一根藤曼
                
                // 藤曼向外延伸8格
                for (int distance = 1; distance <= 8; distance++) { // 恢复原始藤曼延伸距离
                    // 计算藤曼上的位置
                    double x = this.getX() + Math.cos(angle) * distance;
                    double z = this.getZ() + Math.sin(angle) * distance;
                    
                    // 发送粒子效果到客户端
                    ((ServerWorld)this.getWorld()).spawnParticles(
                        net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER, // 使用绿色粒子
                        x, 
                        this.getY() + 0.2, 
                        z, 
                        5, // 粒子数量
                        0.2, // X轴扩散
                        0.1, // Y轴扩散
                        0.2, // Z轴扩散
                        0.01 // 粒子速度
                    );
                    
                    // 添加一些额外的藤曼粒子
                    if (distance % 2 == 0) {
                        ((ServerWorld)this.getWorld()).spawnParticles(
                            net.minecraft.particle.ParticleTypes.COMPOSTER, 
                            x, 
                            this.getY() + 0.4, 
                            z, 
                            3,
                            0.2, 
                            0.1, 
                            0.2, 
                            0.01
                        );
                    }
                }
            }
        }
    }
    
    /**
     * 在玩家周围生成藤曼粒子
     */
    private void spawnVineParticlesAround(PlayerEntity player) {
        if (!this.getWorld().isClient()) {
            ((ServerWorld)this.getWorld()).spawnParticles(
                net.minecraft.particle.ParticleTypes.ITEM_SLIME, 
                player.getX(), 
                player.getY() + 1.0, 
                player.getZ(), 
                10, 
                0.3, 
                0.5, 
                0.3, 
                0.1
            );
        }
    }

    /**
     * 召唤小树人
     */
    private void summonLittleTreeMen() {
        if (!this.getWorld().isClient()) {
            // 生成4-5个小树人
            int count = 4 + this.random.nextInt(2); // 4或5个
            
            for (int i = 0; i < count; i++) {
                // 计算生成位置（在BOSS周围18格内的随机点）
                double angle = random.nextDouble() * 2 * Math.PI; // 随机角度 0-2π
                double distance = 5.0 + random.nextDouble() * 8.0; // 5-8格距离
                double x = this.getX() + Math.sin(angle) * distance;
                double z = this.getZ() + Math.cos(angle) * distance;
                
                // 找到适合的Y坐标（确保生成在地面上）
                double y = this.getY();
                
                // 创建小树人实体
                LittleTreeMan littleTreeMan = new LittleTreeMan(
                    EntityRegistry.LITTLE_TREE_MAN,
                    this.getWorld()
                );
                
                // 设置位置
                littleTreeMan.setPosition(x, y, z);
                
                // 将小树人的目标设置为BOSS的目标（只有当目标是玩家时）
                if (this.getTarget() != null && this.getTarget() instanceof PlayerEntity) {
                    littleTreeMan.setTarget(this.getTarget());
                } else {
                    // 如果BOSS没有玩家目标，尝试找到最近的玩家作为目标
                    PlayerEntity closestPlayer = this.getWorld().getClosestPlayer(this, 32.0);
                    if (closestPlayer != null) {
                        littleTreeMan.setTarget(closestPlayer);
                    }
                }
                
                // 添加到世界
                this.getWorld().spawnEntity(littleTreeMan);
                
                // 生成粒子效果
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.LARGE_SMOKE,
                    x, y + 0.5, z,
                    10, 0.3, 0.3, 0.3, 0.05
                );
            }
            

        }
    }

    // 返回实体碰撞箱的尺寸 - 使用较小的碰撞箱
//    @Override
//    public EntityDimensions getDimensions(EntityPose pose) {
//        // 增加高度以确保上半身能正确被击中
//        // 同时保持宽度适中，避免影响寻路
//        float width = 1.8F * SIZE_SCALE * COLLISION_SCALE;
//        float height = 3.5F * SIZE_SCALE; // 增加高度以覆盖整个模型
//
//        if (this.getWorld().isClient() && this.getWorld().getTime() % 200 == 0) {
//            System.out.println("TreeBoss碰撞箱尺寸 - 宽度: " + width + ", 高度: " + height);
//        }
//
//        return EntityDimensions.changing(width, height);
//    }
    
//    @Override
//    public float getPathfindingFavor(BlockPos pos, WorldView world) {
//        // 增加较为开阔区域的偏好度
//        if (world.isAir(pos.up()) && world.isAir(pos.up(2))) {
//            // 更倾向于有空间的区域
//            return 1.0F;
//        }
//
//        // 减少对树木和灌木丛区域的偏好
//        if (world.getBlockState(pos.up()).isIn(net.minecraft.registry.tag.BlockTags.LOGS) ||
//            world.getBlockState(pos.up()).isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
//            return -0.5F;
//        }
//
//        return super.getPathfindingFavor(pos, world);
//    }
//
//    // 设置眼睛高度
//    @Override
//    protected float getActiveEyeHeight(net.minecraft.entity.EntityPose pose, net.minecraft.entity.EntityDimensions dimensions) {
//        // 调整眼睛高度以匹配碰撞箱
//        return dimensions.height * 0.8F;
//    }



    /**
     * 判断实体是否在移动
     * @return 如果实体正在移动则返回true
     */
    private boolean isMoving() {
        // 简化逻辑，直接检查速度和路径
        double speed = Math.sqrt(
            this.getVelocity().x * this.getVelocity().x + 
            this.getVelocity().z * this.getVelocity().z
        );
        
        // 如果有速度或者正在寻路，则认为在移动
        return speed > 0.01 || !this.getNavigation().isIdle();
    }

    // 覆盖移动方法，添加平滑处理
    @Override
    public void travel(Vec3d movementInput) {
        // 如果Boss正在施放技能或死亡，不允许移动
        if (this.dataTracker.get(IS_DYING) || 
            this.dataTracker.get(IS_STOMPING) ||
            this.dataTracker.get(IS_GRABBING) ||
            this.dataTracker.get(IS_SUMMONING_VINES) || 
            this.dataTracker.get(IS_ROARING)) {
            super.travel(Vec3d.ZERO);
            return;
        }
        
        // 否则使用正常的移动处理
        super.travel(movementInput);

    }



    // 扩展伤害检测的碰撞箱范围，适用于melee攻击的情况
    @Override
    public boolean isCollidable() {
        return true; // 确保实体可碰撞
    }

    @Override
    public boolean collidesWith(Entity other) {
        // 对于玩家，使用更宽松的碰撞检测，其他实体保持正常
        return other instanceof PlayerEntity ? 
               this.isAlive() && !this.isRemoved() : 
               super.collidesWith(other);
    }

    // 渲染伤害盒子的调试方法
    private void renderHitboxes() {
        if (this.getWorld().isClient()) {
            Box normalHitbox = this.getBoundingBox();
            float boxHeight = (float)(normalHitbox.maxY - normalHitbox.minY);
            
            // 创建扩展的上半身碰撞箱
            float halfWidth = this.getWidth() / 2.0F;
            Box upperBodyHitbox = new Box(
                this.getX() - halfWidth, 
                normalHitbox.minY + boxHeight * 0.5F, // 从一半高度开始
                this.getZ() - halfWidth,
                this.getX() + halfWidth, 
                normalHitbox.minY + boxHeight * 1.2F, // 向上延伸20%
                this.getZ() + halfWidth
            );
            
            // 使用粒子效果可视化上半身碰撞箱的边缘
            double stepSize = 0.2D;
            
            // 绘制上半身碰撞箱的顶部和底部边缘
            for (double x = upperBodyHitbox.minX; x <= upperBodyHitbox.maxX; x += stepSize) {
                for (double z = upperBodyHitbox.minZ; z <= upperBodyHitbox.maxZ; z += stepSize) {
                    if (Math.abs(x - upperBodyHitbox.minX) < 0.1D || 
                        Math.abs(x - upperBodyHitbox.maxX) < 0.1D || 
                        Math.abs(z - upperBodyHitbox.minZ) < 0.1D || 
                        Math.abs(z - upperBodyHitbox.maxZ) < 0.1D) {
                        // 顶部边缘
                        this.getWorld().addParticle(
                            ParticleTypes.CRIT, 
                            x, upperBodyHitbox.maxY, z, 
                            0.0D, 0.0D, 0.0D
                        );
                        
                        // 底部边缘
                        this.getWorld().addParticle(
                            ParticleTypes.CRIT, 
                            x, upperBodyHitbox.minY, z, 
                            0.0D, 0.0D, 0.0D
                        );
                    }
                }
            }
            
            // 绘制上半身碰撞箱的四个边缘柱子
            for (double y = upperBodyHitbox.minY; y <= upperBodyHitbox.maxY; y += stepSize) {
                // 四个角落的垂直线
                this.getWorld().addParticle(
                    ParticleTypes.CRIT, 
                    upperBodyHitbox.minX, y, upperBodyHitbox.minZ, 
                    0.0D, 0.0D, 0.0D
                );
                this.getWorld().addParticle(
                    ParticleTypes.CRIT, 
                    upperBodyHitbox.maxX, y, upperBodyHitbox.minZ, 
                    0.0D, 0.0D, 0.0D
                );
                this.getWorld().addParticle(
                    ParticleTypes.CRIT, 
                    upperBodyHitbox.minX, y, upperBodyHitbox.maxZ, 
                    0.0D, 0.0D, 0.0D
                );
                this.getWorld().addParticle(
                    ParticleTypes.CRIT, 
                    upperBodyHitbox.maxX, y, upperBodyHitbox.maxZ, 
                    0.0D, 0.0D, 0.0D
                );
            }
        }
    }

}
