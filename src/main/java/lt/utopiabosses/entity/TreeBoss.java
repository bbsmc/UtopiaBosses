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
    
    // 添加BOSS血条
    private final ServerBossBar bossBar;
    
    // 定义数据追踪器
    private static final TrackedData<Integer> DATA_ANIMATION_ID = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> DATA_ANIMATION_PLAYING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Byte> ATTACK_TYPE = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Boolean> IS_DYING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_STOMPING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> IS_GRABBING = DataTracker.registerData(TreeBoss.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 攻击相关属性
    private int attackCooldown = 0;
    private boolean isAttacking = false;
    private int animationTicks = 0;
    private int deathTicks = 0;
    private int stompTicks = 0;
    private int grabTicks = 0;
    private int attackCounter = 0; // 攻击计数器，每4次普通攻击触发一次跺脚
    private PlayerEntity grabbedPlayer = null; // 被抓取的玩家
    private boolean hasThrown = false; // 是否已经投掷玩家
    
    // 攻击类型枚举
    private enum AttackType {
        NONE,
        LEFT,
        RIGHT
    }
    
    // 技能类型枚举
    public enum SkillType {
        STOMP,  // 跺脚技能
        GRAB    // 抓取技能
    }
    
    public TreeBoss(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        TreeBoss.setDebugFixedSkill(TreeBoss.SkillType.GRAB);

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
        this.dataTracker.startTracking(IS_STOMPING, false);
        this.dataTracker.startTracking(IS_GRABBING, false);
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
            
            // 不需要执行后续的逻辑
            super.tick();
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
            
            // 不需要执行后续的逻辑
            super.tick();
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
                }
            } else {
                // 使用随机技能逻辑
                // 第7次攻击触发抓取, 第4次攻击触发跺脚
                if (attackCounter >= 7) {
                    useGrabAttack();
                } else if (attackCounter >= 4) {
                    useStompAttack();
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

    /**
     * 设置固定执行的技能类型(调试用)
     * @param skillType 要固定执行的技能类型，null表示随机执行
     */
    public static void setDebugFixedSkill(SkillType skillType) {
        DEBUG_FIXED_SKILL = skillType;
        System.out.println("TreeBoss调试模式: " + (skillType == null ? "随机技能" : skillType));
    }
}
