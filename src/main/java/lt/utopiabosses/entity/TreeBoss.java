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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;

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
    
    public TreeBoss(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        TreeBoss.setDebugFixedSkill(TreeBoss.SkillType.ROAR);

        this.bossBar = new ServerBossBar(
            Text.literal("树木BOSS").formatted(Formatting.GREEN),
            BossBar.Color.GREEN, 
            BossBar.Style.PROGRESS
        );
    }
    
    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 500.0D)
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
        this.dataTracker.startTracking(IS_STOMPING, false);
        this.dataTracker.startTracking(IS_GRABBING, false);
        this.dataTracker.startTracking(IS_SUMMONING_VINES, false);
        this.dataTracker.startTracking(IS_ROARING, false);
    }

    @Override
    public void tick() {
        // 保存当前的视角方向
        float savedYaw = this.getYaw();
        float savedPitch = this.getPitch();
        float savedBodyYaw = this.bodyYaw;
        float savedHeadYaw = this.headYaw;
        
        // 判断是否正在播放任何动画
        boolean isPlayingAnimation = this.dataTracker.get(IS_DYING) || 
                                    this.dataTracker.get(IS_SUMMONING_VINES) ||
                                    this.dataTracker.get(IS_GRABBING) ||
                                    this.dataTracker.get(IS_STOMPING) ||
                                    this.dataTracker.get(IS_ROARING) ||
                                    this.dataTracker.get(ATTACK_TYPE) != AttackType.NONE.ordinal();
        
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
            
            // 必须在子类中调用super.tick()，但我们确保不运行原版死亡代码
            super.tick();
            
            // 固定视角方向
            this.setYaw(savedYaw);
            this.setPitch(savedPitch);
            this.bodyYaw = savedBodyYaw;
            this.headYaw = savedHeadYaw;
            
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
                    
                    // 播放回血音效和粒子效果
                    this.getWorld().playSound(
                        null, 
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_PLAYER_LEVELUP,
                        SoundCategory.HOSTILE, 
                        0.5F, 
                        1.5F
                    );
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
                            
                            // 播放伤害音效
                            this.getWorld().playSound(
                                null, 
                                player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES,
                                SoundCategory.HOSTILE, 
                                1.0F, 
                                0.8F
                            );
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
            
            super.tick();
            
            // 固定视角方向
            this.setYaw(savedYaw);
            this.setPitch(savedPitch);
            this.bodyYaw = savedBodyYaw;
            this.headYaw = savedHeadYaw;
            
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
            
            super.tick();
            
            // 固定视角方向
            this.setYaw(savedYaw);
            this.setPitch(savedPitch);
            this.bodyYaw = savedBodyYaw;
            this.headYaw = savedHeadYaw;
            
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
            
            super.tick();
            
            // 固定视角方向
            this.setYaw(savedYaw);
            this.setPitch(savedPitch);
            this.bodyYaw = savedBodyYaw;
            this.headYaw = savedHeadYaw;
            
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
            
            super.tick();
            
            // 固定视角方向
            this.setYaw(savedYaw);
            this.setPitch(savedPitch);
            this.bodyYaw = savedBodyYaw;
            this.headYaw = savedHeadYaw;
            
            return;
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
                
                // 固定视角方向
                this.setYaw(savedYaw);
                this.setPitch(savedPitch);
                this.bodyYaw = savedBodyYaw;
                this.headYaw = savedHeadYaw;
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
     * 执行跺脚践踏攻击
     */
    private void executeStompAttack() {
        if (!this.getWorld().isClient()) {
            // 大范围攻击区域
            Box attackBox = this.getBoundingBox().expand(8.0, 3.0, 8.0);
            List<LivingEntity> targets = this.getWorld().getNonSpectatingEntities(LivingEntity.class, attackBox);
            
            for (LivingEntity target : targets) {
                if (target != this && (!(target instanceof PlayerEntity) || !((PlayerEntity)target).isCreative())) {
                    // 计算目标相对于BOSS的方向向量
                    Vec3d dirToTarget = target.getPos().subtract(this.getPos()).normalize();
                    
                    // 造成8颗心（16点）伤害
                    target.damage(this.getDamageSources().mobAttack(this), 16.0F);
                    
                    // 向上和外侧击飞
                    float knockbackStrength = 1.5f - (this.distanceTo(target) / 8.0f);
                    knockbackStrength = Math.max(0.3f, knockbackStrength); // 确保最小击退
                    
                    // 击飞效果
                    Vec3d knockbackVec = new Vec3d(dirToTarget.x, 0.8, dirToTarget.z).multiply(knockbackStrength);
                    target.addVelocity(knockbackVec.x, knockbackVec.y, knockbackVec.z);
                    
                    // 确保客户端同步速度
                    if (target instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) target).networkHandler.sendPacket(
                            new net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket(target)
                        );
                    }
                }
            }
            
            // 播放地面震动音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.HOSTILE, 
                1.5F, 
                0.6F
            );
            
            // 播放震动音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_STONE_BREAK,
                SoundCategory.BLOCKS, 
                2.0F, 
                0.5F
            );
        }
    }
    
    /**
     * 尝试抓取附近的玩家
     */
    private void grabPlayer() {
        if (!this.getWorld().isClient()) {
            // 获取8格内的所有玩家
            Box searchBox = this.getBoundingBox().expand(8.0, 4.0, 8.0);
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
                    
                    // 播放抓取音效
                    this.getWorld().playSound(
                        null, 
                        this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ENTITY_IRON_GOLEM_ATTACK,
                        SoundCategory.HOSTILE, 
                        1.0F, 
                        0.8F
                    );
                }
            }
        }
    }
    
    /**
     * 更新被抓取玩家的位置到BOSS的左手
     */
    private void updateGrabbedPlayerPosition() {
        if (grabbedPlayer != null && !grabbedPlayer.isRemoved() && !grabbedPlayer.isDead()) {
            // 计算左手的相对位置
            Vec3d lookVec = Vec3d.fromPolar(0, this.getYaw());
            Vec3d leftVec = lookVec.rotateY((float)Math.toRadians(-90)).multiply(2.0); // 左侧2.0格
            Vec3d upVec = new Vec3d(0, 1.5, 0); // 向上1.5格而不是3格
            Vec3d frontVec = lookVec.multiply(0.5); // 向前0.5格
            
            // 计算左手的绝对位置
            Vec3d handPos = this.getPos().add(leftVec).add(upVec).add(frontVec);
            
            // 设置玩家位置
            grabbedPlayer.teleport(handPos.x, handPos.y, handPos.z);
            grabbedPlayer.setVelocity(0, 0, 0);
            grabbedPlayer.fallDistance = 0;
            
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
            
            // 播放投掷音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_WITCH_THROW,
                SoundCategory.HOSTILE, 
                1.5F, 
                0.8F
            );
            
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
                        
                        // 播放撞击音效
                        player.getWorld().playSound(
                            null, 
                            player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_GENERIC_BIG_FALL,
                            SoundCategory.PLAYERS, 
                            1.0F, 
                            1.0F
                        );
                        
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
            
            // 播放准备音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK,
                SoundCategory.HOSTILE, 
                1.0F, 
                0.6F
            );
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
            
            // 播放准备音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_RAVAGER_ROAR,
                SoundCategory.HOSTILE, 
                1.0F, 
                0.8F
            );
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
            
            // 重置攻击状态
            this.dataTracker.set(ATTACK_TYPE, (byte)AttackType.NONE.ordinal());
            
            // 播放准备音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON,
                SoundCategory.HOSTILE, 
                1.0F, 
                0.6F
            );
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
            
            // 播放准备音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_RAVAGER_ROAR,
                SoundCategory.HOSTILE, 
                1.2F, 
                0.5F
            );
        }
    }
    
    /**
     * 让BOSS面向指定实体
     */
    private void faceEntity(Entity entity) {
        // 计算BOSS和目标之间的向量
        double dx = entity.getX() - this.getX();
        double dz = entity.getZ() - this.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        
        // 设置面向目标的视角
        this.setYaw(yaw);
        this.setHeadYaw(yaw);
        this.setBodyYaw(yaw);
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
        
        // 检查是否正在召唤藤曼
        if (this.dataTracker.get(IS_SUMMONING_VINES)) {
            // 播放藤曼动画
            state.getController().setAnimation(VINES_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 检查是否正在执行抓取技能
        if (this.dataTracker.get(IS_GRABBING)) {
            // 播放抓取动画
            state.getController().setAnimation(GRAB_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 检查是否正在执行跺脚技能
        if (this.dataTracker.get(IS_STOMPING)) {
            // 播放跺脚动画
            state.getController().setAnimation(STOMP_ANIM);
            return PlayState.CONTINUE;
        }
        
        // 检查是否正在执行怒吼技能
        if (this.dataTracker.get(IS_ROARING)) {
            // 播放怒吼动画
            state.getController().setAnimation(ROAR_ANIM);
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
        
        // 增加攻击计数
        attackCounter++;
        
        // 首先面向目标
        faceEntity(target);
        
        // 判断是否达到技能触发条件（第4次或第7次攻击）
        boolean shouldUseSkill = (attackCounter >= 4);
        
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
        
        // 不要调用onRemoved()，它会立即清理血条
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
        return super.damage(source, amount);
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
            
            // 播放藤曼生成音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_GRASS_BREAK,
                SoundCategory.HOSTILE, 
                2.0F, 
                0.6F
            );
            
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
            
            // 播放藤曼消失音效
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.BLOCK_GRASS_PLACE,
                SoundCategory.HOSTILE, 
                1.5F, 
                0.7F
            );
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
                    
                    // 播放禁锢音效
                    this.getWorld().playSound(
                        null, 
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_VINE_BREAK,
                        SoundCategory.HOSTILE, 
                        1.0F, 
                        0.5F
                    );
                }
            }
        }
    }
    
    /**
     * 对附近玩家应用减速效果
     */
    private void applySlowEffectToNearbyPlayers() {
        // 获取8格内的所有玩家
        Box searchBox = this.getBoundingBox().expand(8.0, 3.0, 8.0);
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
                for (int distance = 1; distance <= 8; distance++) {
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
            
            // 播放召唤音效
            this.getWorld().playSound(
                null,
                this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.HOSTILE,
                1.5F,
                0.8F
            );
        }
    }
}
