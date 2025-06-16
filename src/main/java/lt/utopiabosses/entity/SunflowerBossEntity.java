package lt.utopiabosses.entity;

import lt.utopiabosses.client.renderer.SunflowerBossRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationController.State;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.AnimationState;
import net.minecraft.util.Identifier;
import lt.utopiabosses.Utopiabosses;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import lt.utopiabosses.registry.SoundRegistry;
public class SunflowerBossEntity extends HostileEntity implements GeoEntity {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);

    // 1. 修复动画定义 - 确保每个动画使用正确的资源
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().then("daiji", Animation.LoopType.LOOP);
    private static final RawAnimation LEFT_ATTACK_ANIM = RawAnimation.begin().then("sunflower_left_attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RIGHT_MELEE_ATTACK_ANIM = RawAnimation.begin().then("sunflower_right_attack", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation RANGED_ATTACK_ANIM = RawAnimation.begin().then("yuancheng", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SEED_BARRAGE_ANIM = RawAnimation.begin().then("yuancheng", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SUNBEAM_ANIM = RawAnimation.begin().then("laser_cannon", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation PETAL_STORM_ANIM = RawAnimation.begin().then("flower_lade_storm", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation KUWEI_STORM_ANIM = RawAnimation.begin().then("sunflower_dies", Animation.LoopType.PLAY_ONCE);

    // 技能冷却时间
    private int seedBarrageCooldown = 0;
    private int sunbeamCooldown = 0;
    private int petalStormCooldown = 0;
    
    
    // 当前播放的动画类型
    private AnimationType currentAnimation = AnimationType.IDLE;
    
    // 随机数生成器
    private final Random random = new Random();
    

    // 更新动画类型枚举，区分左右攻击
    private enum AnimationType {
        IDLE,
        LEFT_MELEE_ATTACK,
        RIGHT_MELEE_ATTACK,
        RANGED_ATTACK,
        SEED_BARRAGE,
        SUNBEAM,
        PETAL_STORM,
        KUWEI_STORM // 添加死亡动画类型
    }

    // 在meleeAttack方法中添加计时器
    private int meleeAnimationTimer = 0;

    // 添加BOSS血条
    private final ServerBossBar bossBar;

    // 定义数据追踪器ID
    private static final TrackedData<Integer> DATA_ANIMATION_ID = DataTracker.registerData(SunflowerBossEntity.class, TrackedDataHandlerRegistry.INTEGER);

    // 使用专用的标记来表示是否处于动画状态
    private static final TrackedData<Boolean> DATA_ANIMATION_PLAYING = 
        DataTracker.registerData(SunflowerBossEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // 添加攻击计数器和技能序列控制
    private int normalAttackCounter = 0;   // 普通攻击计数
    private boolean isHalfHealthTriggered = false;  // 半血触发标记
    private int currentSkillIndex = 0;     // 当前技能序列索引
    
    // 添加变量保存上一次的动画类型，用于正确计数
    private AnimationType lastAnimation = AnimationType.IDLE;
    
    // 在类中添加动画持续时间控制变量
    private int animationTicks = 0;
    private static final int LEFT_ATTACK_DURATION = 40; // 左手攻击动画持续40帧
    private static final int RIGHT_ATTACK_DURATION = 60; // 右手攻击动画持续60帧，确保能触发第40帧的伤害
    private static final int RANGED_ATTACK_DURATION = 50; // 例如：远程攻击动画为2.5秒
    private static final int SEED_BARRAGE_DURATION = 50; // 例如：种子弹幕动画为2.5秒
    private static final int SUNBEAM_DURATION = 160; // 例如：阳光射线动画为8秒
    private static final int PETAL_STORM_DURATION = 80; // 例如：花瓣风暴动画为4秒
    private static final int KUWEI_STORM_DURATION = 147; // 例如：死亡动画为4秒

    // 1. 添加数据追踪器声明 - 放在类顶部变量声明区域
    private static final TrackedData<Byte> ANIMATION_STATE = DataTracker.registerData(SunflowerBossEntity.class, TrackedDataHandlerRegistry.BYTE);

    // 添加一个严格的锁定机制
    private boolean isAnimationLocked = false;
    private int animationLockTimer = 0;

    // 添加死亡动画标志和遗留实体处理
    private boolean isPlayingDeathAnimation = false;
    private boolean shouldBeRemoved = false;
    private RemovalReason removalReason = null;
    private DamageSource deathCause = null; // 存储致命伤害的来源
    private float lethalDamage = 0; // 存储致命伤害的数值

    // 添加一个标志变量来控制BOSS是否应该旋转
    private boolean shouldRotate = false;
    private float rotationYaw = 0.0f;
    private boolean aiDisabled = false; // 新增：标记AI是否被禁用

    // 调整攻击冷却时间
    private int attackCooldown = 0;
    private static final int ATTACK_COOLDOWN_TIME = 20; // 3.5秒 (70 ticks)

    // 添加太阳实体列表
    private List<SunEntity> suns = new ArrayList<>();
    private int absorbedSunCount = 0;

    // 添加插值移动相关的字段
    private static class SunPosition {
        double x, y, z;
        double targetX, targetY, targetZ;
        
        SunPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = x;
            this.targetY = y;
            this.targetZ = z;
        }
        
        void updateTarget(double newX, double newY, double newZ) {
            this.targetX = newX;
            this.targetY = newY;
            this.targetZ = newZ;
        }
        
        void interpolate() {
            // 每tick移动固定距离
            double dx = targetX - x;
            double dy = targetY - y;
            double dz = targetZ - z;
            
            // 计算到目标的距离
            double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);
            
            if (distance > 0.1) { // 如果距离大于0.1格
                // 每tick移动0.1格距离
                double moveSpeed = 0.1;
                double factor = moveSpeed / distance;
                
                x += dx * factor;
                y += dy * factor;
                z += dz * factor;
            } else {
                // 如果非常接近目标，直接设置到目标位置
                x = targetX;
                y = targetY;
                z = targetZ;
            }
        }
    }

    // 添加太阳位置缓存
    private List<SunPosition> sunPositions = new ArrayList<>();

    // 在类的开头添加调试开关
    private static final boolean DEBUG_SUNBEAM_ONLY = false; // 调试开关：设为true时只使用阳光射线技能

    // 添加新字段用于防止重复触发IDLE状态
    private long lastIdleSetTime = 0;
    // 添加计数器，用于跟踪右手攻击重置次数
    private int rightAttackResetCount = 0;

    // 添加一个标志，指示技能是否应该在下一个tick重新启动
    private boolean shouldRestartSunbeam = false;

    // 添加一个调试模式下的攻击计数器
    private int debugAttackCounter = 0;

    public SunflowerBossEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = new ServerBossBar(
            Text.literal("向日葵BOSS").formatted(Formatting.YELLOW),
            BossBar.Color.YELLOW, 
            BossBar.Style.PROGRESS
        );
        System.out.println("向日葵BOSS已创建，尝试加载动画...");
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.8D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0D);
    }

    @Override
    protected void initGoals() {
        // 只保留最基本的AI，移除所有攻击AI
        this.goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 20.0F));
        this.goalSelector.add(2, new LookAroundGoal(this));
        
        // 保留目标选择AI
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        try {
            // 在tick开始时检查并清理可能的遗留太阳
            if (!this.getWorld().isClient() && this.age % 20 == 0) { // 每秒检查一次
                if (currentAnimation != AnimationType.SUNBEAM && !suns.isEmpty()) {
                    System.out.println("检测到非阳光射线状态下的残留太阳，执行清理");
                    cleanupSuns();
                    cleanupAllSunsInWorld();
                }
                
                // 在调试模式下，如果当前状态不是SUNBEAM，确保攻击计数器和冷却正常工作
                if (DEBUG_SUNBEAM_ONLY && currentAnimation != AnimationType.SUNBEAM) {
                    // 如果attackCooldown为0但没有目标，重置debugAttackCounter，避免卡在技能前夕
                    if (attackCooldown <= 0 && this.getTarget() == null && debugAttackCounter > 0) {
                        System.out.println("调试模式：无目标，重置攻击计数器");
                        debugAttackCounter = 0;
                    }
                }
            }
            
            super.tick();

            // 如果正在播放死亡动画，只处理死亡动画相关逻辑
            if (isPlayingDeathAnimation) {
                // 确保不旋转和移动
                this.setVelocity(0, this.getVelocity().y, 0);
                
                // 更新死亡动画
                if (currentAnimation == AnimationType.KUWEI_STORM) {
                    animationTicks++;
                    
                    // 检查死亡动画是否已经完成
                    if (animationTicks >= KUWEI_STORM_DURATION) {
                        // 动画完成后真正处理死亡
                        System.out.println("死亡动画播放完毕，现在真正处理死亡");
                        isPlayingDeathAnimation = false;
                        
                        // 确保实体真正死亡
                        if (deathCause != null && !this.getWorld().isClient()) {
                            // 不要设置血量为0，直接调用死亡处理逻辑
                            // this.setHealth(0); - 这行可能导致触发原版死亡动画
                            actuallyDie(deathCause);
                            
                            // 标记为应该移除，直接处理移除逻辑
                            shouldBeRemoved = true;
                            super.remove(RemovalReason.KILLED);
                        }
                    }
                } else {
                    // 如果当前动画不是死亡动画，但标志是死亡状态，则强制设置为死亡动画
                    System.out.println("检测到死亡状态但动画不正确，强制设置为死亡动画");
                    this.currentAnimation = AnimationType.KUWEI_STORM;
                    this.dataTracker.set(ANIMATION_STATE, (byte)AnimationType.KUWEI_STORM.ordinal());
                    this.animationTicks = 0;
                    forceTriggerAnimation("controller", "kuwei");
                }
                
                // 死亡动画播放时跳过其他逻辑
                return;
            }

            // 更新攻击冷却时间
            if (attackCooldown > 0) {
                attackCooldown--;
            }

            // 如果应该旋转，则在AI处理前强制设置朝向
            if (shouldRotate) {
                // 强制设置朝向
                this.setYaw(rotationYaw);
                this.bodyYaw = rotationYaw;
                this.headYaw = rotationYaw;
                
                // 如果AI未被禁用，则禁用AI
                if (!aiDisabled) {
                    // 保存当前目标
                    this.setAiDisabled(true);
                    aiDisabled = true;
                    System.out.println("BOSS旋转：AI已禁用，当前角度: " + rotationYaw);
                }
            } else if (aiDisabled) {
                // 如果不应该旋转但AI被禁用，则恢复AI
                this.setAiDisabled(false);
                aiDisabled = false;
                System.out.println("BOSS旋转：AI已恢复");
            }
            
            // 技能冷却时间
            if (seedBarrageCooldown > 0) seedBarrageCooldown--;
            if (sunbeamCooldown > 0) sunbeamCooldown--;
            if (petalStormCooldown > 0) petalStormCooldown--;
            if (meleeAnimationTimer > 0) meleeAnimationTimer--;
            
            // 动画锁定计时器
            if (animationLockTimer > 0) {
                animationLockTimer--;
                if (animationLockTimer <= 0) {
                    isAnimationLocked = false;
                    System.out.println("动画锁定已解除");
                }
            }
            
            // 检查是否到达半血且尚未触发
            // 在tick方法中只检查，不触发，因为已在damage方法中处理
            if (!isHalfHealthTriggered && this.getHealth() <= this.getMaxHealth() / 2) {
                isHalfHealthTriggered = true;
                System.out.println("【半血触发】在tick方法中检测到半血状态");
                // 不在这里尝试触发花瓣风暴，而是通过damage方法处理
            }
            
            // 检查当前动画是否在播放中
            if (currentAnimation != AnimationType.IDLE) {
                // 确保currentAnimation与tracker保持同步
                byte trackedAnim = this.dataTracker.get(ANIMATION_STATE);
                if (trackedAnim != this.currentAnimation.ordinal()) {
                    System.out.println("【同步警告】当前动画(" + this.currentAnimation + ")与追踪器(" + 
                                     AnimationType.values()[trackedAnim] + ")不一致，正在同步");
                    this.currentAnimation = AnimationType.values()[trackedAnim];
                }
                
                animationTicks++;
                int animationDuration = getAnimationDuration(currentAnimation);
                
                // 更新DATA_ANIMATION_ID数据追踪器，用于同步动画帧数
                this.dataTracker.set(DATA_ANIMATION_ID, animationTicks);
                
                // 简化动画同步，只在右手攻击的关键帧前同步一次
                if (!this.getWorld().isClient() && 
                    currentAnimation == AnimationType.RIGHT_MELEE_ATTACK && 
                    animationTicks == 39) { // 在伤害触发前一帧同步
                    // 发送动画帧同步请求，使用不会触发类型转换的方式
                    try {
                        // 使用自定义数据包或其他安全的方式来同步
                        System.out.println("发送关键帧同步请求：即将触发右手攻击伤害");
                        
                        // 我们暂时禁用这个同步功能，直到找到更安全的方式
                        // this.getWorld().sendEntityStatus(this, (byte)21);
                        
                        // 仅使用dataTracker同步
                        // 确保dataTracker是最新的
                        this.dataTracker.set(DATA_ANIMATION_ID, animationTicks);
                    } catch (Exception e) {
                        System.out.println("发送动画帧同步请求时出错: " + e.getMessage());
                    }
                }
                
                // 处理动画帧效果
                // 技能效果触发通常在动画的1/3处
                int effectFrame = animationDuration / 3;
                effectFrame = Math.max(10, effectFrame); // 确保至少在第10帧触发
                
                // 根据当前动画类型执行对应效果
                switch (this.currentAnimation) {
                    case SEED_BARRAGE:
                        // 葵花籽弹幕 - 每5帧发射一次，连续发射多次
                        if (animationTicks % 5 == 0 && animationTicks <= effectFrame + 20) {
                            LivingEntity seedTarget = this.getTarget();
                            if (seedTarget != null) {
                                System.out.println("【葵花籽弹幕】发射种子 - 第" + animationTicks + "帧");
                                
                                // 每次发射5颗种子，形成密集弹幕
                                for (int i = 0; i < 5; i++) {
                                    fireSeedBarrage(seedTarget);
                                    this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                                        SoundRegistry.ENTITY_SUNFLOWER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
                                }
                                
                            }
                        }
                        break;
                        
                    case SUNBEAM:
                        // 阳光灼烧射线 - 在每一帧都调用，内部处理具体逻辑
                        if (this.getTarget() != null) {
                            createSunbeamEffect(this.getTarget());
                        }
                        break;
                        
                    case PETAL_STORM:
                        // 花瓣风暴 - 在特定帧触发效果
                        if (animationTicks == effectFrame) {
                            System.out.println("【花瓣风暴】触发效果 - 第" + animationTicks + "帧");
                            createPetalStormEffect();
                            
                        }
                        break;
                        
                    case RANGED_ATTACK:
                        // 处理远程普通攻击效果
                        handleRangedAttackEffect(animationTicks);
                        break;
                        
                    case LEFT_MELEE_ATTACK:
                        // 左手攻击 - 在第21帧触发伤害
                        // 每10帧记录一次日志，方便调试
                        if (animationTicks % 10 == 0) {
                            System.out.println("【左手近战攻击】当前进度 - 第" + animationTicks + "帧 / " + LEFT_ATTACK_DURATION + "帧");
                        }
                        
                        if (animationTicks == 21) {
                            LivingEntity target = this.getTarget();
                            if (target != null && this.distanceTo(target) < 6.0) {
                                System.out.println("【左手近战攻击】触发伤害效果 - 第" + animationTicks + "帧");
                                
                                // 在动画关键帧应用伤害
                                if (!this.getWorld().isClient()) {
                                    // 服务器端处理伤害
                                    target.damage(this.getDamageSources().mobAttack(this), 8.0f); // 近战伤害为8点
                                    
                                    // 添加击退效果
                                    double knockbackStrength = 1.2;
                                    double xRatio = Math.sin(this.getYaw() * 0.017453292F);
                                    double zRatio = -Math.cos(this.getYaw() * 0.017453292F);
                                    target.takeKnockback(knockbackStrength, xRatio, zRatio);
                                    
                                    // 给予玩家短暂的缓慢效果
                                    if (target instanceof PlayerEntity) {
                                        ((PlayerEntity)target).addStatusEffect(
                                            new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
                                    }
                                }
                            }
                        }
                        break;
                    
                    case RIGHT_MELEE_ATTACK:
                        // 右手攻击 - 在第40帧触发伤害（与视觉效果一致）
                        // 每10帧记录一次日志，方便调试
                        if (animationTicks % 10 == 0) {
                            System.out.println("【右手近战攻击】当前进度 - 第" + animationTicks + "帧 / " + RIGHT_ATTACK_DURATION + "帧");
                        }
                        
                        if (animationTicks == 21) {
                            LivingEntity target = this.getTarget();
                            if (target != null && this.distanceTo(target) < 6.0) {
                                System.out.println("【右手近战攻击】触发伤害效果 - 第" + animationTicks + "帧");
                                
                                // 在动画关键帧应用伤害
                                if (!this.getWorld().isClient()) {
                                    // 服务器端处理伤害
                                    target.damage(this.getDamageSources().mobAttack(this), 8.0f); // 近战伤害为8点
                                    
                                    // 添加击退效果
                                    double knockbackStrength = 1.2;
                                    double xRatio = Math.sin(this.getYaw() * 0.017453292F);
                                    double zRatio = -Math.cos(this.getYaw() * 0.017453292F);
                                    target.takeKnockback(knockbackStrength, xRatio, zRatio);
                                    
                                    // 给予玩家短暂的缓慢效果
                                    if (target instanceof PlayerEntity) {
                                        ((PlayerEntity)target).addStatusEffect(
                                            new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1), this);
                                    }
                                }
                                
                            }
                        }
                        break;
                        
                    case KUWEI_STORM:
                        // 死亡动画处理
                        break;
                        
                    default:
                        break;
                }
                
                // 检查动画是否已经完成
                if (animationTicks >= animationDuration) {
                    // 记录上一个动画类型
                    AnimationType completedAnimation = currentAnimation;
                    
                    // 重置为IDLE状态
                    currentAnimation = AnimationType.IDLE;
                    animationTicks = 0;
                    this.dataTracker.set(DATA_ANIMATION_ID, 0); // 重置动画帧追踪器
                    isAnimationLocked = false;
                    
                    System.out.println("动画已完成，重置为IDLE状态");
                    
                    // 根据完成的动画类型增加普通攻击计数
                    if (completedAnimation == AnimationType.LEFT_MELEE_ATTACK || 
                        completedAnimation == AnimationType.RIGHT_MELEE_ATTACK || 
                        completedAnimation == AnimationType.RANGED_ATTACK) {
                        normalAttackCounter++;
                        System.out.println("普通攻击计数增加: " + normalAttackCounter + " (完成的动画: " + completedAnimation + ")");
                        
                        // 在普通攻击动画完成后开始攻击冷却
                        attackCooldown = ATTACK_COOLDOWN_TIME;
                        System.out.println("攻击冷却开始: " + ATTACK_COOLDOWN_TIME + " ticks (3.5秒)");
                    }
                    
                    // 保存上一次的动画类型
                    lastAnimation = completedAnimation;
                    
                    // 数据追踪器更新 - 确保IDLE状态被正确设置
                    this.dataTracker.set(ANIMATION_STATE, (byte)AnimationType.IDLE.ordinal());
                    
                    // 触发IDLE动画
                    forceTriggerAnimation("controller", "idle");
                    
                    // 如果在服务器端，向客户端发送状态更新
                    if (!this.getWorld().isClient()) {
                        try {
                            this.getWorld().sendEntityStatus(this, (byte)10);
                            System.out.println("向客户端发送动画重置通知");
                        } catch (Exception e) {
                            System.out.println("发送动画重置通知时出错: " + e.getMessage());
                        }
                    }
                }
            }
            
            // AI决策 - 只有在IDLE状态且没有锁定时才执行
            if (this.currentAnimation == AnimationType.IDLE && !isAnimationLocked) {
                // 获取当前目标
                LivingEntity target = this.getTarget();
                
                if (target != null) {
                    double distanceToTarget = this.distanceTo(target);
                    
                    // 执行新的攻击模式
                    executeAttackPattern(target, distanceToTarget);
                }
            }
            
            // 更新BOSS血条
            if (!this.getWorld().isClient()) {
                this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
            }

            // 添加这段代码来确保BOSS始终面向目标
            // 但在播放死亡动画时不旋转
            LivingEntity target = this.getTarget();
            if (target != null && !isPlayingDeathAnimation && currentAnimation != AnimationType.KUWEI_STORM
                    && currentAnimation != AnimationType.SUNBEAM) { // 添加判断，阳光射线技能期间不旋转
                // 增加对KUWEI_STORM动画的判断，确保死亡动画期间不旋转
                // 计算朝向目标的角度
                double deltaX = target.getX() - this.getX();
                double deltaZ = target.getZ() - this.getZ();
                float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
                
                // 平滑旋转
                float currentYaw = this.getYaw();
                float yawDifference = MathHelper.wrapDegrees(yaw - currentYaw);
                
                // 限制旋转速度
                float maxRotation = 10.0F; // 每tick最大旋转角度
                if (Math.abs(yawDifference) > maxRotation) {
                    yawDifference = Math.signum(yawDifference) * maxRotation;
                }
                
                // 应用新的朝向
                this.setYaw(currentYaw + yawDifference);
                this.bodyYaw = this.getYaw();
                this.headYaw = this.getYaw();
            }
            
            // 检查并清理任何可能残留的太阳实体
            if (!this.getWorld().isClient() && this.currentAnimation != AnimationType.SUNBEAM) {
                for (SunEntity sun : new ArrayList<>(suns)) {
                    if (sun != null && (!sun.isAlive() || sun.isRemoved())) {
                        suns.remove(sun);
                        System.out.println("移除了一个无效的太阳实体");
                    }
                }
            }
            
            // 在tick开始时检查当前动画状态
            if (currentAnimation != AnimationType.SUNBEAM && !suns.isEmpty()) {
                System.out.println("检测到非阳光射线状态下的残留太阳，执行清理");
                cleanupSuns();
            }
        } catch (Exception e) {
            // 捕获所有异常，防止实体崩溃
            System.out.println("实体处理tick时出现异常: " + e.getMessage());
            e.printStackTrace();
            
            // 尝试恢复到安全状态
            try {
                if (isAnimationLocked) {
                    // 解除锁定状态
                    isAnimationLocked = false;
                    animationLockTimer = 0;
                    System.out.println("异常处理：强制解除动画锁定");
                }
                
                // 仅尝试恢复到IDLE状态，不尝试任何其他复杂处理
                if (currentAnimation != AnimationType.IDLE && !isPlayingDeathAnimation) {
                    currentAnimation = AnimationType.IDLE;
                    dataTracker.set(ANIMATION_STATE, (byte)AnimationType.IDLE.ordinal());
                    animationTicks = 0;
                    System.out.println("异常处理：重置为IDLE状态");
                    
                    // 尝试发送IDLE状态到客户端
                    if (!this.getWorld().isClient()) {
                        try {
                            this.getWorld().sendEntityStatus(this, (byte)10);
                        } catch (Exception ex) {
                            // 忽略发送状态时的错误
                        }
                    }
                }
            } catch (Exception ex) {
                // 忽略异常恢复过程中的错误
                System.out.println("尝试恢复状态时出错: " + ex.getMessage());
            }
        }
    }
    
    // 获取动画持续时间的辅助方法
    private int getAnimationDuration(AnimationType animType) {
        switch (animType) {
            case LEFT_MELEE_ATTACK:
                return LEFT_ATTACK_DURATION;
            case RIGHT_MELEE_ATTACK:
                return RIGHT_ATTACK_DURATION;
            case SEED_BARRAGE:
                return SEED_BARRAGE_DURATION;
            case SUNBEAM:
                return SUNBEAM_DURATION;
            case PETAL_STORM:
                return PETAL_STORM_DURATION;
            case RANGED_ATTACK:
                return RANGED_ATTACK_DURATION;
            case KUWEI_STORM: // 添加死亡动画持续时间
                return KUWEI_STORM_DURATION;
            case IDLE:
            default:
                return 20; // 默认为1秒
        }
    }

    // 添加一个新的技能选择方法
    private void selectAndExecuteSkill(LivingEntity target) {
        // 如果不是IDLE状态，不执行任何技能
        if (this.currentAnimation != AnimationType.IDLE) {
            System.out.println("无法执行技能：当前不是IDLE状态");
            return;
        }
        
        // 近战判断
        if (this.squaredDistanceTo(target) < 16.0 && random.nextFloat() < 0.4) {
            System.out.println("选择执行近战攻击");
            if (random.nextBoolean()) {
                setAnimation(AnimationType.LEFT_MELEE_ATTACK);
            } else {
                setAnimation(AnimationType.RIGHT_MELEE_ATTACK);
            }
            return;
        }
        
        // 远程技能选择
        List<AnimationType> availableSkills = new ArrayList<>();
        
        if (seedBarrageCooldown <= 0) availableSkills.add(AnimationType.SEED_BARRAGE);
        if (sunbeamCooldown <= 0) availableSkills.add(AnimationType.SUNBEAM);
        if (petalStormCooldown <= 0) availableSkills.add(AnimationType.PETAL_STORM);
        
        if (!availableSkills.isEmpty()) {
            AnimationType selectedSkill = availableSkills.get(random.nextInt(availableSkills.size()));
            System.out.println("选择执行技能: " + selectedSkill);
            
            switch (selectedSkill) {
                case SEED_BARRAGE:
                    seedBarrageCooldown = 200;
                    setAnimation(AnimationType.SEED_BARRAGE);
                    sendMessageToNearbyPlayers("§e向日葵BOSS使用了§6葵花籽弹幕§e技能！");
                    break;
                case SUNBEAM:
                    sunbeamCooldown = 300;
                    setAnimation(AnimationType.SUNBEAM);
                    sendMessageToNearbyPlayers("§e向日葵BOSS使用了§c阳光灼烧射线§e技能！");
                    break;
                case PETAL_STORM:
                    petalStormCooldown = 400;
                    setAnimation(AnimationType.PETAL_STORM);
                    sendMessageToNearbyPlayers("§e向日葵BOSS使用了§d花瓣风暴§e技能！§c小心！");
                    break;
            }
        }
    }

    // 重写技能启动方法以确保正确动画设置
    private void startSeedBarrageSkill() {
        // 检查当前动画状态
        if (this.currentAnimation != AnimationType.IDLE) {
            System.out.println("无法开始葵花籽弹幕：当前已有其他动画: " + this.currentAnimation);
            return;
        }
        
        System.out.println("开始葵花籽弹幕技能");
        setAnimation(AnimationType.SEED_BARRAGE);
        seedBarrageCooldown = 10; // 短冷却，确保技能序列
        
        sendMessageToNearbyPlayers("§e向日葵BOSS使用了§6葵花籽弹幕§e技能！");
    }

    private void startSunbeamSkill() {
        if (this.currentAnimation != AnimationType.IDLE) {
            System.out.println("无法开始阳光灼烧射线：当前已有其他动画: " + this.currentAnimation);
            return;
        }
        
        System.out.println("开始阳光灼烧射线技能");
        
        // 在设置动画前先清理所有可能残留的太阳实体
        cleanupAllSunsInWorld();
        cleanupSuns();
        
        // 确保状态重置
        suns.clear();
        sunPositions.clear();
        absorbedSunCount = 0;
        
        // 尝试获取并立即朝向目标
        LivingEntity target = this.getTarget();
        if (target != null) {
            // 计算朝向目标的角度
            double deltaX = target.getX() - this.getX();
            double deltaZ = target.getZ() - this.getZ();
            float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
            
            // 立即设置为朝向目标
            this.setYaw(yaw);
            this.bodyYaw = yaw;
            this.headYaw = yaw;
            
            System.out.println("阳光灼烧射线技能：BOSS已对准目标，角度: " + yaw);
        }
        
        setAnimation(AnimationType.SUNBEAM);
        
        // 在调试模式下将冷却时间设为0
        sunbeamCooldown = DEBUG_SUNBEAM_ONLY ? 0 : 10;
        
        // 生成太阳
        spawnSuns();
        
        
        // 发送消息
        if (!DEBUG_SUNBEAM_ONLY) { // 在调试模式下不发送消息
            sendMessageToNearbyPlayers("§e向日葵BOSS使用了§c阳光灼烧射线§e技能！");
        }
        
        System.out.println("阳光灼烧射线技能初始化完成，开始生成太阳");
    }

    private void startPetalStormSkill() {
        // 移除对当前动画状态的检查，允许从任何状态切换到花瓣风暴
        // 这对于半血触发特别重要
        
        System.out.println("开始花瓣风暴技能");
        
        // 强制清理所有资源，确保没有冲突
        if (this.currentAnimation == AnimationType.SUNBEAM) {
            cleanupSuns();
        }
        
        // 强制解除动画锁定
        this.isAnimationLocked = false;
        this.animationLockTimer = 0;
        
        // 设置新动画
        setAnimation(AnimationType.PETAL_STORM);
        petalStormCooldown = 10; // 短冷却，确保技能序列
        
        
        // 发送消息
        sendMessageToNearbyPlayers("§e向日葵BOSS使用了§d花瓣风暴§e技能！§c小心！");
    }

    // 修改葵花籽弹幕方法，调整伤害设置
    private void fireSeedBarrage(LivingEntity target) {
        if (target == null || this.getWorld().isClient()) return;
        
        // 调整发射位置计算，使其与视觉模型匹配
        double modelScale = 1.5; 
        double headHeight = this.getHeight() * 0.65;
        
        // 考虑实体朝向，确保从花盘正面发射
        float yaw = this.getYaw() * 0.017453292F;
        double offsetX = -Math.sin(yaw) * 0.5 * modelScale;
        double offsetZ = Math.cos(yaw) * 0.5 * modelScale;
        
        Vec3d position = this.getPos().add(offsetX, headHeight, offsetZ);
        
        // 计算基础方向 - 朝向目标
        Vec3d baseDirection = target.getPos().add(0, target.getHeight() / 2, 0)
            .subtract(position).normalize();
        
        // 扇形发射 - 每次发射多个种子，形成扇形
        int seedCount = 5; // 每次发射的种子数量
        double spreadAngle = Math.PI / 6; // 30度扇形
        
        for (int i = 0; i < seedCount; i++) {
            // 计算扇形角度
            double angle = spreadAngle * ((double)i / (seedCount - 1) - 0.5);
            
            // 旋转基础方向
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            double newX = baseDirection.x * cos - baseDirection.z * sin;
            double newZ = baseDirection.x * sin + baseDirection.z * cos;
            
            Vec3d direction = new Vec3d(newX, baseDirection.y, newZ).normalize();
            
            // 创建种子实体
            SunflowerSeedEntity seed = new SunflowerSeedEntity(this.getWorld(), this);
            seed.setPosition(position.x, position.y, position.z);
            // 增加发射速度从1.5f到2.0f，因为没有追踪功能了
            seed.setVelocity(direction.x, direction.y, direction.z, 2.0f, 0f);
            seed.setOwner(this);
            
            // 设置为技能攻击种子并设置伤害值
            seed.setSkillAttack(true);
            seed.setDamage(12.0f); // 设置技能种子的伤害为6颗心
            
            // 添加到世界
            this.getWorld().spawnEntity(seed);
        }
        
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 使用更简单的控制器配置
        controllers.add(
            new AnimationController<>(this, "controller", 0, event -> {
                try {
                    // 获取当前动画状态
                    AnimationType currentAnimationType = getAnimation();
                    
                    // 限制DEBUG日志输出频率，避免大量无用日志
                    if (this.getWorld().isClient() && this.age % 5 == 0) {
                        System.out.println("动画控制器 - 当前动画: " + currentAnimationType);
                    }
                    
                    // 检查当前是否播放完了非循环动画
                    if (event.getController().hasAnimationFinished() && currentAnimationType != AnimationType.IDLE) {
                        // 非循环动画已经结束，强制切换回IDLE
                        System.out.println("检测到非循环动画已结束，强制切换回IDLE");
                        
                        // 强制重置动画状态
                        this.currentAnimation = AnimationType.IDLE;
                        this.animationTicks = 0;
                        
                        // 发送状态更新给服务器/其他客户端
                        if (!this.getWorld().isClient()) {
                            // 向客户端发送动画重置通知
                            this.getWorld().sendEntityStatus(this, (byte)10);
                            // 更新数据追踪器
                            this.dataTracker.set(ANIMATION_STATE, (byte)AnimationType.IDLE.ordinal());
                        }
                        
                        // 直接返回IDLE动画
                        return event.setAndContinue(IDLE_ANIM);
                    }
                    
                    // RIGHT_MELEE_ATTACK特殊处理：如果是右手攻击且已运行超过50帧，检查是否应该强制结束
                    if (currentAnimationType == AnimationType.RIGHT_MELEE_ATTACK && 
                            animationTicks >= 50 && 
                            event.getController().getAnimationState() != AnimationController.State.STOPPED) {
                        
                        System.out.println("右手攻击已运行50帧以上，检查是否应强制结束");
                        
                        // 检查是否已经执行了伤害逻辑 (第40帧)
                        if (animationTicks >= 45) {
                            // 已经触发了伤害，直接强制结束动画
                            System.out.println("右手攻击已完成伤害阶段，强制切换到IDLE");
                            
                            // 强制重置动画状态
                            event.getController().forceAnimationReset();
                            
                            // 更新实体状态
                            this.currentAnimation = AnimationType.IDLE;
                            this.animationTicks = 0;
                            
                            // 强制切换动画
                            return event.setAndContinue(IDLE_ANIM);
                        }
                    }
                    
                    // 选择要播放的动画
                    RawAnimation animation;
                    switch (currentAnimationType) {
                        case LEFT_MELEE_ATTACK:
                            animation = LEFT_ATTACK_ANIM;
                            break;
                        case RIGHT_MELEE_ATTACK:
                            animation = RIGHT_MELEE_ATTACK_ANIM;
                            break;
                        case RANGED_ATTACK:
                            animation = RANGED_ATTACK_ANIM;
                            break;
                        case SEED_BARRAGE:
                            animation = SEED_BARRAGE_ANIM;
                            break;
                        case SUNBEAM:
                            animation = SUNBEAM_ANIM;
                            break;
                        case PETAL_STORM:
                            animation = PETAL_STORM_ANIM;
                            break;
                        case KUWEI_STORM:
                            animation = KUWEI_STORM_ANIM;
                            break;
                        default:
                            animation = IDLE_ANIM;
                            break;
                    }
                    
                    // 直接设置动画，使用setAndContinue而不是直接修改状态
                    return event.setAndContinue(animation);
                } catch (Exception e) {
                    System.out.println("动画控制器出错: " + e.getMessage());
                    e.printStackTrace();
                    return PlayState.STOP;
                }
            })
            .triggerableAnim("idle", IDLE_ANIM)
            .triggerableAnim("left_attack", LEFT_ATTACK_ANIM)
            .triggerableAnim("right_attack", RIGHT_MELEE_ATTACK_ANIM)
            .triggerableAnim("ranged", RANGED_ATTACK_ANIM)
            .triggerableAnim("seed_barrage", SEED_BARRAGE_ANIM)
            .triggerableAnim("sunbeam", SUNBEAM_ANIM)
            .triggerableAnim("petal_storm", PETAL_STORM_ANIM)
            .triggerableAnim("kuwei", KUWEI_STORM_ANIM)
            .setParticleKeyframeHandler(particleKeyframeEvent -> {
                // 确保只在客户端执行粒子生成
                if (getWorld().isClient()) {
                    // 获取粒子应该在的位置
                    Vec3d loc = SunflowerBossRenderer.getBonePosition(particleKeyframeEvent.getAnimatable().getId(), "liz");

                    // 检查位置是否有效
                    if (loc != null) {
                        // 创建一次性爆发效果
                        int particleCount = 60; // 一次性生成60个粒子
                        float radius = 3.0f;    // 球形半径3格
                        float initialSpeed = 0.6f; // 初始速度

                        // 循环生成多个粒子形成爆发效果
                        for (int i = 0; i < particleCount; i++) {
                            // 球形分布的随机方向
                            double phi = Math.random() * Math.PI * 2; // 水平角度
                            double theta = Math.random() * Math.PI;   // 垂直角度
                            
                            // 计算方向向量 (球面坐标转笛卡尔坐标)
                            double dirX = Math.sin(theta) * Math.cos(phi);
                            double dirY = Math.cos(theta);
                            double dirZ = Math.sin(theta) * Math.sin(phi);
                            
                            // 随机化初始位置（稍微偏离中心）
                            double offsetX = (Math.random() - 0.5) * 0.2;
                            double offsetY = (Math.random() - 0.5) * 0.2;
                            double offsetZ = (Math.random() - 0.5) * 0.2;
                            
                            // 设置粒子初速度（向外发散）
                            double velX = dirX * initialSpeed;
                            double velY = dirY * initialSpeed;
                            double velZ = dirZ * initialSpeed;
                            
                            // 选择不同的粒子类型增加变化
                            DefaultParticleType particleType;
                            int choice = i % 5;
                            switch(choice) {
                                case 0:
                                    particleType = ParticleTypes.END_ROD; // 末地烛粒子，亮白色
                                    break;
                                case 1:
                                    particleType = ParticleTypes.FIREWORK; // 烟花粒子，多彩
                                    break;
                                case 2:
                                    particleType = ParticleTypes.SOUL_FIRE_FLAME; // 灵魂火焰，蓝色
                                    break;
                                case 3:
                                    particleType = ParticleTypes.FLAME; // 普通火焰，橙色
                                    break;
                                default:
                                    particleType = ParticleTypes.ELECTRIC_SPARK; // 电火花，白色
                                    break;
                            }
                            
                            // 添加粒子与初速度
                            getWorld().addParticle(
                                particleType,
                                loc.getX() + offsetX, 
                                loc.getY() + offsetY,
                                loc.getZ() + offsetZ,
                                velX, velY, velZ
                            );
                        }
                        
                        // 额外添加中心爆发效果
                        getWorld().addParticle(
                            ParticleTypes.EXPLOSION,
                            loc.getX(), loc.getY(), loc.getZ(),
                            0, 0, 0
                        );
                    }
                }
            }).setSoundKeyframeHandler(event -> {
                // 处理声音关键帧
                if (event.getKeyframeData().getSound().equals("sunflower_right_attack")) {
                    // 使用ClientPlayer播放声音
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(
                            SoundRegistry.ENTITY_SUNFLOWER_RIGHT_ATTACK,
                            1.0F,  // 音调
                            1.0F   // 增大音量
                        )
                    );
                }else if (event.getKeyframeData().getSound().equals("sunflower_left_attack")) {
                    // 使用ClientPlayer播放声音
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(
                            SoundRegistry.ENTITY_SUNFLOWER_LEFT_ATTACK,
                            1.0F,  // 音调
                            1.0F   // 增大音量
                        )
                    );
                }else if (event.getKeyframeData().getSound().equals("laser_cannon")) {
                    // 使用ClientPlayer播放声音
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(
                            SoundRegistry.ENTITY_SUNFLOWER_LASER_CANNON,
                            1.0F,  // 音调
                            1.0F   // 增大音量
                        )
                    );
                }else if (event.getKeyframeData().getSound().equals("flower_lade_storm")) {
                    // 使用ClientPlayer播放声音
                    MinecraftClient.getInstance().getSoundManager().play(
                        PositionedSoundInstance.master(
                            SoundRegistry.ENTITY_SUNFLOWER_FLOWER_LADE_STORM,
                            1.0F,  // 音调
                            1.0F   // 增大音量
                        )
                    );
                }
            })
        );
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }
    
    @Override
    public boolean isPushable() {
        return false; // 不能被推动
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        // 如果正在播放死亡动画，不接受任何伤害
        if (isPlayingDeathAnimation) {
            return false;
        }
        
        // 检查这次伤害是否会致命
        if (this.getHealth() <= amount) {
            // 即将致命，播放死亡动画
            System.out.println("收到致命伤害，开始播放死亡动画");
            startDeathAnimation(source, amount);
            
            // 发送死亡消息
            if (source.getAttacker() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) source.getAttacker();
                sendMessageToNearbyPlayers("§e向日葵BOSS被 §b" + player.getName().getString() + " §e击败了！");
            } else {
                sendMessageToNearbyPlayers("§e向日葵BOSS被击败了！");
            }
            
            return true; // 伤害已处理
        }
        
        // 正常伤害处理
        boolean damaged = super.damage(source, amount);
        
        // 检查是否到达半血
        if (damaged && !isHalfHealthTriggered && this.getHealth() <= this.getMaxHealth() / 2) {
            // 半血首次触发 - 立即处理，不再等待下一个tick
            isHalfHealthTriggered = true;
            System.out.println("BOSS血量降至半血，立即触发花瓣风暴");
            
            // 如果当前正在执行阳光射线技能，先中断它
            if (this.currentAnimation == AnimationType.SUNBEAM) {
                System.out.println("中断阳光射线技能，准备执行花瓣风暴");
                cleanupSuns();
                // 强制结束当前动画
                this.animationTicks = getAnimationDuration(AnimationType.SUNBEAM);
                // 解除动画锁定，允许切换到花瓣风暴
                this.isAnimationLocked = false;
                this.animationLockTimer = 0;
            }
            
            // 直接开始花瓣风暴技能，不等待IDLE状态
            try {
                // 短暂延迟一帧，确保资源清理完成
                if (!this.getWorld().isClient()) {
                    startPetalStormSkill();
                    normalAttackCounter = 0; // 重置攻击计数
                    currentSkillIndex = 0;   // 重置技能序列
                }
            } catch (Exception e) {
                System.out.println("触发花瓣风暴时出错: " + e.getMessage());
            }
        }
        
        // 只在受到大量伤害时才中断阳光射线技能
        if (damaged && this.currentAnimation == AnimationType.SUNBEAM && amount >= 10.0f && !isHalfHealthTriggered) {
            System.out.println("受到大量伤害(" + amount + ")，中断阳光射线技能");
            cleanupSuns();
            // 重置为IDLE状态
            setAnimation(AnimationType.IDLE);
        }
        
        return damaged;
    }
    
    // 新增：开始死亡动画
    private void startDeathAnimation(DamageSource source, float amount) {
        // 检查是否已经在播放死亡动画
        if (isPlayingDeathAnimation) {
            System.out.println("已经在播放死亡动画，不再重复触发");
            return;
        }
        
        System.out.println("开始死亡动画");
        
        // 设置标记
        isPlayingDeathAnimation = true;
        this.deathCause = source;
        this.lethalDamage = amount;
        
        // 禁用AI，确保BOSS不会继续执行攻击逻辑
        this.setAiDisabled(true);
        
        // 记录当前视角方向，保持死亡动画期间不变
        float currentYaw = this.getYaw();
        float currentBodyYaw = this.bodyYaw;
        float currentHeadYaw = this.headYaw;
        
        // 切换到死亡动画
        currentAnimation = AnimationType.KUWEI_STORM;
        animationTicks = 0;
        this.dataTracker.set(ANIMATION_STATE, (byte)AnimationType.KUWEI_STORM.ordinal());
        
        // 设置死亡动画后保持原方向
        this.setYaw(currentYaw);
        this.bodyYaw = currentBodyYaw;
        this.headYaw = currentHeadYaw;
        
        // 立即强制设置死亡动画，不通过setAnimation方法，直接设置所有相关状态
        this.currentAnimation = AnimationType.KUWEI_STORM;
        this.dataTracker.set(ANIMATION_STATE, (byte)AnimationType.KUWEI_STORM.ordinal());
        this.animationTicks = 0;
        
        // 锁定动画状态，防止被打断
        isAnimationLocked = true;
        animationLockTimer = getAnimationDuration(AnimationType.KUWEI_STORM);
        
        // 直接触发死亡动画
        forceTriggerAnimation("controller", "kuwei");
        
        // 向附近的玩家广播消息
        sendMessageToNearbyPlayers("向日葵BOSS开始播放死亡动画");
        
        // 确保实体不会立即被移除，等待动画播放完毕
        this.shouldBeRemoved = false;
        
        // 向客户端发送特殊状态码，确保死亡动画被正确同步
        this.getWorld().sendEntityStatus(this, (byte)20); // 使用特殊状态码20表示死亡动画
    }
    
    // 新增：强制设置死亡动画的方法
    private void forceDeathAnimation() {
        System.out.println("强制切换到死亡动画");
        
        // 强制清理所有正在进行的技能
        cleanupSuns();
        
        // 记录当前视角方向，保持死亡动画期间不变
        float currentYaw = this.getYaw();
        float currentBodyYaw = this.bodyYaw;
        float currentHeadYaw = this.headYaw;
        
        // 设置死亡动画
        this.currentAnimation = AnimationType.KUWEI_STORM;
        this.animationTicks = 0;
        this.isPlayingDeathAnimation = true;
        this.dataTracker.set(ANIMATION_STATE, (byte)AnimationType.KUWEI_STORM.ordinal());
        
        // 设置死亡动画后保持原方向
        this.setYaw(currentYaw);
        this.bodyYaw = currentBodyYaw;
        this.headYaw = currentHeadYaw;
    }
    
    // 新增：实际处理死亡的方法
    private void actuallyDie(DamageSource source) {
        // 如果已经在客户端，跳过处理
        if (this.getWorld().isClient()) {
            System.out.println("已经在播放死亡动画，跳过onDeath处理");
            return;
        }
        
        // 清理BOSS相关资源
        System.out.println("向日葵BOSS已死亡，血条已清理");
        if (this.bossBar != null) {
            this.bossBar.clearPlayers();
        }
        
        // 设置应该被移除的标志
        this.shouldBeRemoved = true;
        
        // 设置移除原因
        this.removalReason = RemovalReason.KILLED;
        
        // 生成掉落物和经验，但不调用任何可能触发原版死亡动画的方法
        this.dropCustomLoot(source);
        // 不要调用super.onDeath或类似可能触发原版死亡动画的方法
    }

    /**
     * 自定义掉落逻辑 - 使用战利品表
     */
    private void dropCustomLoot(DamageSource source) {
        if (!this.getWorld().isClient()) {
            // 使用战利品表进行掉落
            this.dropLoot(source, true);
            
            // 掉落经验
            this.dropXp();
        }
    }
    
    @Override
    public Identifier getLootTableId() {
        return new Identifier("utopiabosses", "entities/sunflower_boss");
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        System.out.println("BOSS死亡，清理所有太阳实体");
        cleanupSuns();
        super.onDeath(damageSource);
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        System.out.println("BOSS被移除，清理所有太阳实体");
        cleanupSuns();
        // 如果正在播放死亡动画且不是明确要求删除，推迟删除
        if (isPlayingDeathAnimation && reason != RemovalReason.KILLED && !shouldBeRemoved) {
            System.out.println("正在播放死亡动画，推迟实体移除: " + reason);
            // 保存移除原因，但不立即移除
            this.removalReason = reason;
            return;
        }
        
        // 如果自己设置了shouldBeRemoved标志，则允许正常移除
        if (shouldBeRemoved || !isPlayingDeathAnimation) {
            // 清理血条
            if (this.bossBar != null) {
                this.bossBar.clearPlayers();
                System.out.println("向日葵BOSS已移除，血条已清理：" + reason);
            }
            
            // 正常移除
            super.remove(reason);
        }
    }

    // 添加一个方法，向附近的玩家发送消息
    private void sendMessageToNearbyPlayers(String message) {
        if (this.getWorld() instanceof ServerWorld) {
            // 获取64格范围内的所有玩家
            List<PlayerEntity> nearbyPlayers = this.getWorld().getEntitiesByClass(
                PlayerEntity.class, 
                this.getBoundingBox().expand(64.0), 
                player -> !player.isSpectator()
            );
            
            // 向每个玩家发送消息
            for (PlayerEntity player : nearbyPlayers) {
                player.sendMessage(Text.of(message), false);
            }
        }
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("AnimationState", currentAnimation.ordinal());
        
        // 保存太阳技能相关状态
        nbt.putBoolean("HasActiveSuns", !suns.isEmpty());
        nbt.putInt("AbsorbedSunCount", absorbedSunCount);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        
        // 读取动画状态
        if (nbt.contains("AnimationState")) {
            int animState = nbt.getInt("AnimationState");
            if (animState >= 0 && animState < AnimationType.values().length) {
                currentAnimation = AnimationType.values()[animState];
            }
        }
        
        // 检查是否有未完成的太阳技能
        if (nbt.contains("HasActiveSuns") && nbt.getBoolean("HasActiveSuns")) {
            System.out.println("检测到未完成的太阳技能，执行清理...");
            // 强制清理所有可能存在的太阳实体
            cleanupAllSunsInWorld();
            // 重置相关状态
            suns.clear();
            sunPositions.clear();
            absorbedSunCount = 0;
            // 重置技能状态
            if (currentAnimation == AnimationType.SUNBEAM) {
                currentAnimation = AnimationType.IDLE;
                animationTicks = 0;
            }
        }
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        // 使用一致的动画状态追踪器
        this.dataTracker.startTracking(ANIMATION_STATE, (byte)AnimationType.IDLE.ordinal());
        
        // 添加动画锁定状态追踪
        this.dataTracker.startTracking(DataTracker.registerData(SunflowerBossEntity.class, 
                              TrackedDataHandlerRegistry.BOOLEAN), false);
        
        // 添加当前动画帧追踪
        this.dataTracker.startTracking(DATA_ANIMATION_ID, 0);
    }

    // 修改setAnimation方法以同步到客户端
    public void setAnimation(AnimationType animation) {
        try {
            // 特殊处理死亡动画 - 可以从任何状态切换
            if (animation == AnimationType.KUWEI_STORM) {
                isPlayingDeathAnimation = true;
                currentAnimation = animation;
                dataTracker.set(ANIMATION_STATE, (byte)animation.ordinal());
                animationTicks = 0;
                System.out.println("开始播放死亡动画");
                
                // 直接触发动画
                forceTriggerAnimation("controller", "kuwei");
                return;
            }
            
            // 只有非锁定状态下才能切换动画
            if (isAnimationLocked && animation != AnimationType.IDLE) {
                System.out.println("动画已锁定，无法切换到:" + animation);
                return;
            }
            
            // 更新当前动画和数据追踪器
            System.out.println("设置动画: " + animation);
            currentAnimation = animation;
            dataTracker.set(ANIMATION_STATE, (byte)animation.ordinal());
            animationTicks = 0;
            dataTracker.set(DATA_ANIMATION_ID, 0); // 重置动画帧计数器
            
            // 非IDLE状态锁定动画
            if (animation != AnimationType.IDLE) {
                isAnimationLocked = true;
                animationLockTimer = getAnimationDuration(animation);
            }
            
            // 使用triggerAnim直接触发动画
            switch(animation) {
                case LEFT_MELEE_ATTACK:
                    System.out.println("开始左手攻击动画 - 持续" + LEFT_ATTACK_DURATION + "帧，将在第21帧触发伤害");
                    forceTriggerAnimation("controller", "left_attack");
                    break;
                case RIGHT_MELEE_ATTACK:
                    System.out.println("开始右手攻击动画 - 持续" + RIGHT_ATTACK_DURATION + "帧，将在第40帧触发伤害");
                    forceTriggerAnimation("controller", "right_attack");
                    break;
                case RANGED_ATTACK:
                    forceTriggerAnimation("controller", "ranged");
                    break;
                case SEED_BARRAGE:
                    forceTriggerAnimation("controller", "seed_barrage");
                    break;
                case SUNBEAM:
                    forceTriggerAnimation("controller", "sunbeam");
                    break;
                case PETAL_STORM:
                    forceTriggerAnimation("controller", "petal_storm");
                    break;
                case IDLE:
                default:
                    forceTriggerAnimation("controller", "idle");
                    break;
            }
            
            // 发送状态更新到客户端
            if (!this.getWorld().isClient()) {
                try {
                    // 设置好追踪数据
                    dataTracker.set(ANIMATION_STATE, (byte)animation.ordinal());
                    
                    // 向客户端发送状态更新，使用状态码10，但客户端会从追踪器读取正确的动画类型
                    this.getWorld().sendEntityStatus(this, (byte)10);
                    System.out.println("向客户端发送动画状态更新: " + animation + " (状态码: " + animation.ordinal() + ")");
                } catch (Exception e) {
                    System.out.println("发送动画状态更新时出错: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.out.println("设置动画时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 修改getAnimation方法从追踪器读取动画状态
    public AnimationType getAnimation() {
        try {
            // 直接使用已保存的动画状态
            // 这在客户端特别重要，因为这里我们不希望从追踪器读取（可能有延迟）
            if (this.getWorld() != null && this.getWorld().isClient()) {
                // 在客户端，优先使用当前保存的状态
                // 但也检查追踪器是否有更新
                byte animationId = this.dataTracker.get(ANIMATION_STATE);
                AnimationType trackedAnimType = AnimationType.IDLE;
                
                if (animationId >= 0 && animationId < AnimationType.values().length) {
                    trackedAnimType = AnimationType.values()[animationId];
                    
                    // 如果追踪器状态与当前不同，更新日志但不一定更新状态
                    if (trackedAnimType != this.currentAnimation) {
                        System.out.println("客户端动画状态差异: 当前=" + this.currentAnimation + 
                                        ", 追踪器=" + trackedAnimType);
                    }
                }
                
                return this.currentAnimation;
            } else {
                // 服务端，直接从追踪器获取状态
                byte animationId = this.dataTracker.get(ANIMATION_STATE);
                if (animationId >= 0 && animationId < AnimationType.values().length) {
                    AnimationType animType = AnimationType.values()[animationId];
                    return animType;
                }
            }
            
            return currentAnimation; // 默认返回当前缓存的状态
        } catch (Exception e) {
            System.out.println("获取动画状态时出错: " + e.getMessage());
            e.printStackTrace();
            return AnimationType.IDLE;
        }
    }

    // 修改方法签名，使用完全限定的类名
    public EntitySpawnS2CPacket createSpawnPacket() {
        // 使用EntitySpawnS2CPacket
        return new EntitySpawnS2CPacket(this);
    }

    @Override
    public void travel(Vec3d movementInput) {
        // 不调用super.travel()，实体将不会移动
        // 但仍然允许转向
    }

    // 确保即使被击退也不会移动
    @Override
    public void takeKnockback(double strength, double x, double z) {
        // 不调用super，禁止击退效果
    }


    // 修改createSunbeamEffect方法
    private void createSunbeamEffect(LivingEntity target) {
        if (target == null) return;
        
        int currentFrame = this.animationTicks;
        
        // 添加更多详细的日志信息
        if (currentFrame % 10 == 0 || currentFrame == 100) { // 改回100帧
            System.out.println("阳光灼烧射线技能执行中，当前帧: " + currentFrame + 
                              "，目标: " + target.getName().getString() + 
                              "，距离: " + this.distanceTo(target));
        }
        
        // 在整个技能期间，始终让BOSS面向玩家
        // 计算朝向目标的角度
        double deltaX = target.getX() - this.getX();
        double deltaZ = target.getZ() - this.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        
        // 直接设置朝向
        this.setYaw(yaw);
        this.bodyYaw = yaw;
        this.headYaw = yaw;
        
        // 只在开始时和每30帧记录一次朝向日志，避免日志过多
        if (currentFrame == 1 || currentFrame % 30 == 0) {
            System.out.println("阳光灼烧射线技能：BOSS已对准目标，角度: " + yaw);
        }
        
        // 前100帧处理太阳相关的逻辑
        if (currentFrame < 100) { // 改回100帧
            // 更新太阳的位置和动画
            updateSuns(currentFrame);
        } else if (currentFrame < SUNBEAM_DURATION) {
            // 记录光束发射阶段开始
            if (currentFrame == 100) { // 改回100帧
                System.out.println("阳光灼烧射线开始发射光束阶段，太阳数量: " + suns.size() + 
                                  "，目标位置: " + target.getPos());
            }
            
            // 在100-160帧间，创建阳光射线
            // 获取BOSS花朵中心位置作为光束起点
            double headHeight = this.getHeight() * 0.65 + 1;
            Vec3d beamStartPos = new Vec3d(this.getX(), this.getY() + headHeight, this.getZ());
            
            // 使用当前角度创建光束（始终朝向玩家）
            double angleRadians = Math.toRadians(yaw + 90); // 转换成弧度
            double beamLength = 25.0; // 光束长度
            
            // 创建光束效果
            createBeam(beamStartPos, angleRadians, beamLength);
        }
        
        // 技能结束时确保清理
        if (currentFrame >= SUNBEAM_DURATION) {
            System.out.println("阳光灼烧射线技能结束，执行清理");
            cleanupSuns();
            
            // 在调试模式下，重置状态准备下一轮攻击
            if (DEBUG_SUNBEAM_ONLY) {
                System.out.println("调试模式：技能结束，重置状态");
                // 重置状态
                this.animationTicks = 0;
                this.currentAnimation = AnimationType.IDLE;
                this.isAnimationLocked = false;
                this.sunbeamCooldown = 0; // 确保没有冷却时间
                this.debugAttackCounter = 0; // 重置攻击计数器
                this.attackCooldown = 0; // 清除攻击冷却
                
                // 确保状态更新被传递到客户端
                if (!this.getWorld().isClient()) {
                    this.dataTracker.set(ANIMATION_STATE, (byte)AnimationType.IDLE.ordinal());
                    this.getWorld().sendEntityStatus(this, (byte)10);
                }
            }
        }
    }

    // 修改spawnSuns方法，恢复原来的生成半径
    private void spawnSuns() {
        if (this.getWorld().isClient()) return;
        
        System.out.println("开始生成太阳实体");
        suns.clear();
        sunPositions.clear();
        absorbedSunCount = 0;
        
        // 生成5个太阳围绕BOSS头部
        for (int i = 0; i < 5; i++) {
            double angle = (i * 2 * Math.PI) / 5;
            double radius = 10.0; // 将半径恢复到10.0
            double x = this.getX() + Math.cos(angle) * radius;
            double z = this.getZ() + Math.sin(angle) * radius;
            double y = this.getY() + this.getHeight() * 0.65 + 4; // 高度恢复为原来的4
            
            try {
                SunEntity sun = new SunEntity(EntityType.ITEM, this.getWorld());
                sun.setPosition(x, y, z);
                sun.setOwner(this);
                sun.setNoGravity(true);
                
                // 创建并存储位置信息
                sunPositions.add(new SunPosition(x, y, z));
                
                this.getWorld().spawnEntity(sun);
                suns.add(sun);
                
                System.out.println("成功生成第 " + (i + 1) + " 个太阳实体");
                
            } catch (Exception e) {
                System.out.println("生成太阳实体失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 修改updateSuns方法，恢复原来的太阳回收时间
    private void updateSuns(int currentFrame) {
        if (this.getWorld().isClient()) return;
        
        // 如果已经超过100帧，强制清理所有太阳并返回
        if (currentFrame >= 100) {
            cleanupSuns();
            return;
        }
        
        // 移除无效的太阳实体
        suns.removeIf(sun -> sun == null || sun.isRemoved() || !sun.isAlive());
        
        // 修改时间分配：50帧旋转，45帧吸收
        int rotationEndFrame = 50;
        int absorbInterval = 7;
        
        // 更新每个太阳的位置
        for (int i = 0; i < suns.size(); i++) {
            SunEntity sun = suns.get(i);
            if (sun != null && !sun.isAbsorbed()) {
                // 计算每个太阳的基础角度
                double baseAngle = (i * 2 * Math.PI) / 5;
                
                if (currentFrame < rotationEndFrame) {
                    // 旋转阶段（0-50帧）
                    double rotationProgress = currentFrame / (double)rotationEndFrame;
                    double currentRotation = rotationProgress * (2 * Math.PI);
                    double angle = baseAngle - currentRotation;
                    double radius = 10.0;
                    
                    // 计算目标位置
                    double targetX = this.getX() + Math.cos(angle) * radius;
                    double targetZ = this.getZ() + Math.sin(angle) * radius;
                    double targetY = this.getY() + this.getHeight() * 0.65 + 4;
                    
                    // 设置目标位置
                    sun.setTargetPos(new Vec3d(targetX, targetY, targetZ));
                    
                    // 计算旋转速度
                    double circumference = 2 * Math.PI * radius;
                    double speedPerFrame = (circumference / rotationEndFrame) / 20;
                    sun.setMoveSpeed(speedPerFrame);
                    
                } else if (currentFrame < 95) { // 确保在95帧前完成所有吸收
                    // 吸收阶段（50-95帧）
                    int absorptionFrame = rotationEndFrame + (i * absorbInterval);
                    
                    if (currentFrame >= absorptionFrame && !sun.isAbsorbed()) {
                        // 计算到BOSS的位置
                        double bossX = this.getX();
                        double bossY = this.getY() + this.getHeight() * 0.65 + 1;
                        double bossZ = this.getZ();
                        Vec3d bossPos = new Vec3d(bossX, bossY, bossZ);
                        
                        // 设置目标位置为BOSS位置
                        sun.setTargetPos(bossPos);
                        
                        // 增加最后一个太阳的移动速度
                        double moveSpeed = (i == 4) ? 0.8 : 0.5; // 最后一个太阳移动更快
                        sun.setMoveSpeed(moveSpeed);
                        
                        // 增加吸收距离，特别是对最后一个太阳
                        double absorbDistance = (i == 4) ? 3.0 : 2.0;
                        double distanceToBoss = sun.getPos().distanceTo(bossPos);
                        if (distanceToBoss < absorbDistance) {
                            absorbSun();
                            continue;
                        }
                    }
                }
                
                // 添加视觉效果
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    // 普通粒子效果
                    serverWorld.spawnParticles(
                        ParticleTypes.END_ROD,
                        sun.getX(), sun.getY(), sun.getZ(),
                        1, 0.1, 0.1, 0.1, 0.02
                    );
                    
                    // 如果在吸收阶段，添加连接线效果
                    if (currentFrame >= rotationEndFrame) {
                        Vec3d sunPos = sun.getPos();
                        Vec3d bossPos = this.getPos().add(0, this.getHeight() * 0.65, 0);
                        
                        // 在太阳和BOSS之间生成粒子线
                        for (int j = 0; j < 5; j++) {
                            double progress = j / 5.0;
                            Vec3d particlePos = sunPos.lerp(bossPos, progress);
                            serverWorld.spawnParticles(
                                ParticleTypes.FLAME,
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.1, 0.1, 0.1, 0.01
                            );
                        }
                    }
                }
            }
        }
        
        // 在95帧后强制清理所有剩余的太阳
        if (currentFrame >= 95) {
            cleanupSuns();
        }
    }

    // 添加平滑插值函数
    private double smoothstep(double x) {
        // 使用三次平滑插值函数
        return x * x * (3 - 2 * x);
    }

    // 在技能结束时清理
    private void cleanupSuns() {
        try {
            System.out.println("开始清理所有太阳实体，当前数量: " + suns.size());
            
            // 复制列表，避免并发修改异常
            List<SunEntity> sunsCopy = new ArrayList<>(suns);
            
            // 移除所有太阳
            for (SunEntity sun : sunsCopy) {
                if (sun != null && sun.isAlive()) {
                    // 设置为已吸收，这会触发删除逻辑
                    sun.setAbsorbed(true);
                    sun.discard();
                    
                    try {
                        // 强制移除
                        sun.remove(Entity.RemovalReason.DISCARDED);
                        System.out.println("成功移除一个太阳实体");
                    } catch (Exception e) {
                        System.out.println("移除太阳实体时出错: " + e.getMessage());
                    }
                }
            }
            
            // 清空太阳列表
            suns.clear();
            sunPositions.clear();
            
            // 重置吸收计数
            absorbedSunCount = 0;
            
            // 为了以防万一，还删除世界中所有的太阳实体
            cleanupAllSunsInWorld();
            
            System.out.println("太阳实体清理完成");
        } catch (Exception e) {
            System.out.println("清理太阳实体时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 修改absorbSun方法，确保快速移除
    private void absorbSun() {
        if (suns.isEmpty() || absorbedSunCount >= 5) {
            return;
        }
        
        // 获取要吸收的太阳
        SunEntity sunToAbsorb = null;
        for (SunEntity sun : new ArrayList<>(suns)) {
            if (sun != null && !sun.isAbsorbed()) {
                sunToAbsorb = sun;
                break;
            }
        }
        
        if (sunToAbsorb != null) {
            try {
                // 立即移除太阳实体
                sunToAbsorb.setAbsorbed(true);
                sunToAbsorb.discard(); // 立即移除实体
                suns.remove(sunToAbsorb);
                absorbedSunCount++;
                
                // 回复生命值
                this.heal(20.0f);
                
                // 播放吸收效果
                if (this.getWorld() instanceof ServerWorld serverWorld) {
                    // 缩短粒子效果的持续时间
                    serverWorld.spawnParticles(
                        ParticleTypes.EXPLOSION,
                        sunToAbsorb.getX(),
                        sunToAbsorb.getY(),
                        sunToAbsorb.getZ(),
                        3, 0.1, 0.1, 0.1, 0.05 // 减少粒子扩散范围
                    );
                    
                }
                
                System.out.println("太阳实体已被立即吸收和移除，剩余数量: " + suns.size());
            } catch (Exception e) {
                System.out.println("移除太阳实体时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 添加一个辅助方法来创建光束，避免代码重复
    private void createBeam(Vec3d start, double angleRadians, double beamLength) {
        try {
            // 水平方向向量
            double horizontalX = Math.cos(angleRadians);
            double horizontalZ = Math.sin(angleRadians);
            
            // 25度角朝下的方向向量
            double verticalComponent = -Math.tan(Math.toRadians(25));
            Vec3d direction = new Vec3d(horizontalX, verticalComponent, horizontalZ).normalize();
            
            // 光束终点
            Vec3d end = start.add(direction.multiply(beamLength));

            
            // 添加更详细的调试信息
//            System.out.println("创建光束 - 起点: " + start + ", 角度: " + Math.toDegrees(angleRadians) + "°, 长度: " + beamLength);
            
            // 伤害检测逻辑
            Box beamBox = new Box(
                Math.min(start.x, end.x) - 1.5, Math.min(start.y, end.y) - 1.5, Math.min(start.z, end.z) - 1.5,
                Math.max(start.x, end.x) + 1.5, Math.max(start.y, end.y) + 1.5, Math.max(start.z, end.z) + 1.5
            );
            
//            System.out.println("光束碰撞箱: " + beamBox);
            
            List<PlayerEntity> players = this.getWorld().getEntitiesByClass(
                PlayerEntity.class, beamBox, player -> !player.isSpectator() && player.isAlive()
            );
            
//            System.out.println("检测到玩家数量: " + players.size());
            
            for (PlayerEntity player : players) {
                System.out.println("对玩家 " + player.getName().getString() + " 造成阳光射线伤害");
                player.damage(this.getDamageSources().mobAttack(this), 16.0f);
                player.setOnFireFor(10);
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 3));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 1));
                
                Vec3d knockback = player.getPos().subtract(this.getPos()).normalize();
                player.setVelocity(knockback.x * 1.5, 0.5, knockback.z * 1.5);
                player.velocityModified = true;
            }
        } catch (Exception e) {
            System.out.println("创建光束时出错: " + e.getMessage());
        }
    }

    // 花瓣风暴效果方法
    private void createPetalStormEffect() {
        // 不使用isClient判断
        double radius = 8.0;
        
        // 在BOSS周围生成花瓣粒子效果
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            for (int i = 0; i < 100; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = random.nextDouble() * radius;
                double x = this.getX() + Math.cos(angle) * distance;
                double z = this.getZ() + Math.sin(angle) * distance;
                double y = this.getY() + random.nextDouble() * 3;
                
                serverWorld.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    x, y, z,
                    1, 0, 0, 0, 0.1
                );
            }
        }
        
        // 对范围内的玩家造成伤害
        List<PlayerEntity> players = this.getWorld().getEntitiesByClass(
            PlayerEntity.class,
            this.getBoundingBox().expand(radius, 3.0, radius),
            player -> !player.isSpectator() && player.isAlive()
        );
        
        for (PlayerEntity player : players) {
            // 造成10颗心伤害
            player.damage(this.getDamageSources().mobAttack(this), 20.0f);
            
            // 大幅增强击退效果，至少15格
            Vec3d knockback = player.getPos().subtract(this.getPos()).normalize();
            player.setVelocity(knockback.x * 3.0, 1.0, knockback.z * 3.0); // 水平方向3.0，垂直方向1.0
            player.velocityModified = true;
        }
        
        System.out.println("花瓣风暴造成伤害");
    }

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
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        if (!this.getWorld().isClient()) {
            // 确保血条被创建
            if (this.bossBar == null) {
                System.out.println("错误: BOSS血条未初始化!");
            } else {
                System.out.println("BOSS血条初始化成功");
            }
        }
    }

    // 添加一个方法来主动触发阳光灼烧射线技能
    public void triggerSunbeamSkill() {
        if (sunbeamCooldown <= 0 && !isAnimationLocked) {
            System.out.println("手动触发阳光灼烧射线技能");
            setAnimation(AnimationType.SUNBEAM);
            sunbeamCooldown = 200; // 10秒冷却
            
        }
    }

    // 新方法：按照指定模式执行攻击
    private void executeAttackPattern(LivingEntity target, double distance) {
        // 如果开启了调试模式，在执行两次普通攻击后才使用阳光射线技能
        if (DEBUG_SUNBEAM_ONLY) {
            // 检查当前是否可以执行攻击
            if (attackCooldown > 0 || isAnimationLocked) {
                return;
            }
            
            // 如果当前是SUNBEAM状态，不执行其他攻击
            if (currentAnimation == AnimationType.SUNBEAM) {
                return;
            }
            
            // 计算是否应该执行阳光射线技能
            if (debugAttackCounter >= 2) {
                // 重置计数器
                debugAttackCounter = 0;
                
                System.out.println("调试模式：已完成两次普通攻击，触发阳光射线技能");
                
                // 如果当前仍然锁定，强行解除锁定
                if (isAnimationLocked) {
                    System.out.println("调试模式：强制解除动画锁定");
                    isAnimationLocked = false;
                    animationLockTimer = 0;
                }
                
                // 立即启动太阳光束技能
                startSunbeamSkill();
                return;
            }
            
            // 如果未达到计数要求，执行普通攻击
            System.out.println("调试模式：执行普通攻击 " + (debugAttackCounter + 1) + "/2");
            
            // 根据距离选择攻击方式
            if (distance <= 6.0) {
                // 在6格以内执行近战攻击
                System.out.println("执行近战攻击，距离: " + distance);
                
                // 随机选择左手或右手攻击
                if (this.random.nextBoolean()) {
                    System.out.println("选择执行左手攻击，将在第21帧触发伤害");
                    setAnimation(AnimationType.LEFT_MELEE_ATTACK);
                } else {
                    System.out.println("选择执行右手攻击，将在第40帧触发伤害");
                    setAnimation(AnimationType.RIGHT_MELEE_ATTACK);
                }
            } else {
                // 在6格以外执行远程攻击
                System.out.println("执行远程普通攻击");
                executeSimpleRangedAttack(target);
            }
            
            // 增加计数器
            debugAttackCounter++;
            
            // 设置攻击冷却
            attackCooldown = ATTACK_COOLDOWN_TIME;
            
            return;
        }

        // 原有的攻击模式逻辑
        if (attackCooldown > 0) {
            return;
        }

        System.out.println("执行攻击模式 - 当前普通攻击计数: " + normalAttackCounter + ", 技能序列索引: " + currentSkillIndex + ", 半血状态: " + isHalfHealthTriggered);

        // 检查是否需要释放特定技能
        if (normalAttackCounter >= 4) {
            // 重置普通攻击计数
            normalAttackCounter = 0;
            
            // 技能序列处理
            if (this.getHealth() <= this.getMaxHealth() / 2) {
                // 半血以下的技能序列: 种子弹幕 -> 阳光灼烧 -> 花瓣风暴 -> 循环
                if (!isHalfHealthTriggered) {
                    isHalfHealthTriggered = true;
                    startPetalStormSkill();
                    currentSkillIndex = 0;
                } else {
                    switch(currentSkillIndex) {
                        case 0:
                            startSeedBarrageSkill();
                            currentSkillIndex = 1;
                            break;
                        case 1:
                            startSunbeamSkill();
                            currentSkillIndex = 2;
                            break;
                        case 2:
                            startPetalStormSkill();
                            currentSkillIndex = 0;
                            break;
                    }
                }
            } else {
                // 正常血量的技能序列: 种子弹幕 -> 阳光灼烧 -> 循环
                switch(currentSkillIndex) {
                    case 0:
                        startSeedBarrageSkill();
                        currentSkillIndex = 1;
                        break;
                    case 1:
                        startSunbeamSkill();
                        currentSkillIndex = 0;
                        break;
                }
            }
            return;
        }

        // 根据距离选择攻击方式
        if (distance <= 6.0) {
            // 在6格以内执行近战攻击
            System.out.println("执行近战攻击，距离: " + distance);
            
            // 随机选择左手或右手攻击
            if (this.random.nextBoolean()) {
                System.out.println("选择执行左手攻击，将在第21帧触发伤害");
                setAnimation(AnimationType.LEFT_MELEE_ATTACK);
            } else {
                System.out.println("选择执行右手攻击，将在第40帧触发伤害");
                setAnimation(AnimationType.RIGHT_MELEE_ATTACK);
            }
        } else {
            // 在6格以外执行远程攻击
            System.out.println("执行远程普通攻击");
            executeSimpleRangedAttack(target);
        }
    }

    // 修改远程攻击方法，确保正确触发动画
    private void executeSimpleRangedAttack(LivingEntity target) {
        // 简化远程攻击执行逻辑，确保更可靠地设置动画状态
        System.out.println("执行远程普通攻击，设置RANGED_ATTACK动画");
        
        try {
            // 使用新的安全动画设置方法
            setAnimation(AnimationType.RANGED_ATTACK);
            
            // 强制同步到客户端
            if (!this.getWorld().isClient()) {
                // 同步状态到所有客户端
                this.getWorld().sendEntityStatus(this, (byte)10);
                System.out.println("成功设置远程攻击动画 - 将在第" + (RANGED_ATTACK_DURATION / 3) + "帧发射种子");
            }
        } catch (Exception e) {
            System.out.println("设置远程攻击动画时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 在动画处理中添加远程攻击的效果处理
    private void handleRangedAttackEffect(int currentFrame) {
        int effectFrame = RANGED_ATTACK_DURATION / 3;
        
        // 在特定帧发射种子
        if (currentFrame == effectFrame) {
            LivingEntity target = this.getTarget();
            if (target != null) {
                System.out.println("【远程普通攻击】发射种子 - 第" + currentFrame + "帧");
                
                // 发射单个种子
                fireSimpleSeed(target);
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundRegistry.ENTITY_SUNFLOWER_SHOOT, SoundCategory.HOSTILE, 2.0f, 1.0f);
                
            }
        }
    }
    
    // 新方法：发射单个种子（普通远程攻击用）
    private void fireSimpleSeed(LivingEntity target) {
        if (target == null || this.getWorld().isClient()) return;
        
        // 调整发射位置计算，使其与视觉模型匹配
        double modelScale = 1.5; // 与渲染器中的缩放因子匹配
        double headHeight = this.getHeight() * 0.65;
        
        // 考虑实体朝向，确保从花盘正面发射
        float yaw = this.getYaw() * 0.017453292F;
        double offsetX = -Math.sin(yaw) * 0.5 * modelScale;
        double offsetZ = Math.cos(yaw) * 0.5 * modelScale;
        
        Vec3d position = this.getPos().add(offsetX, headHeight, offsetZ);
        
        // 计算到目标的方向
        Vec3d direction = target.getPos().add(0, target.getHeight() / 2, 0)
            .subtract(position).normalize();
            
        // 创建种子实体
        SunflowerSeedEntity seed = new SunflowerSeedEntity(this.getWorld(), this);
        seed.setPosition(position.x, position.y, position.z);
        seed.setVelocity(direction.x, direction.y, direction.z, 1.5f, 0f);
        seed.setOwner(this);
        
        // 设置追踪目标
        seed.setHomingTarget(target);
        
        // 设置为普通攻击种子(非技能攻击)
        seed.setSkillAttack(false);
        
        // 添加到世界
        this.getWorld().spawnEntity(seed);
    }

    // 添加实体状态处理方法
    @Override
    public void handleStatus(byte status) {
        try {
            // 处理自定义状态码10 - 用于动画状态同步
            if (status == 10) {
                if (this.getWorld().isClient()) {
                    try {
                        // 获取当前播放的动画类型
                        AnimationType previousAnimation = this.currentAnimation;
                        
                        // 从追踪器获取最新状态，而不是强制设置为IDLE
                        byte animationId = this.dataTracker.get(ANIMATION_STATE);
                        AnimationType serverAnimType = AnimationType.IDLE;
                        if (animationId >= 0 && animationId < AnimationType.values().length) {
                            serverAnimType = AnimationType.values()[animationId];
                        }
                        
                        System.out.println("客户端接收到动画状态更新: " + serverAnimType);
                        
                        // 更新动画状态
                        this.currentAnimation = serverAnimType;
                        this.animationTicks = 0;
                        
                        // 根据新的动画类型触发相应动画
                        switch(serverAnimType) {
                            case LEFT_MELEE_ATTACK:
                                forceTriggerAnimation("controller", "left_attack");
                                break;
                            case RIGHT_MELEE_ATTACK:
                                forceTriggerAnimation("controller", "right_attack");
                                break;
                            case RANGED_ATTACK:
                                forceTriggerAnimation("controller", "ranged");
                                break;
                            case SEED_BARRAGE:
                                forceTriggerAnimation("controller", "seed_barrage");
                                break;
                            case SUNBEAM:
                                // 多次触发阳光射线动画，确保播放
                                forceTriggerAnimation("controller", "sunbeam");
                                System.out.println("客户端触发阳光射线动画(数据变更)");
                                
                                // 强制使用线程在短时间后再次触发动画，确保播放
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(100);
                                        // 再次检查状态，如果仍然是SUNBEAM，则再次触发
                                        if (currentAnimation == AnimationType.SUNBEAM) {
                                            System.out.println("客户端延迟再次触发阳光射线动画");
                                            triggerAnim("controller", "sunbeam");
                                        }
                                    } catch (Exception e) {
                                        // 忽略异常
                                    }
                                }).start();
                                break;
                            case PETAL_STORM:
                                forceTriggerAnimation("controller", "petal_storm");
                                break;
                            case IDLE:
                            default:
                                System.out.println("客户端触发待机动画");
                                forceTriggerAnimation("controller", "idle");
                                break;
                        }
                        
                        // 记录动画触发时间
                        lastIdleSetTime = System.currentTimeMillis();
                    } catch (Exception e) {
                        System.out.println("处理动画同步请求时出错: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                return;
            }
            // 处理自定义状态码20 - 用于死亡动画同步，具有最高优先级
            else if (status == 20) {
                if (this.getWorld().isClient()) {
                    try {
                        System.out.println("客户端收到死亡动画同步请求");
                        // 强制设置死亡动画状态
                        this.isPlayingDeathAnimation = true;
                        this.currentAnimation = AnimationType.KUWEI_STORM;
                        this.animationTicks = 0;
                        // 锁定动画状态
                        this.isAnimationLocked = true;
                        this.animationLockTimer = getAnimationDuration(AnimationType.KUWEI_STORM);
                        // 触发死亡动画
                        forceTriggerAnimation("controller", "kuwei");
                        // 禁用AI
                        this.aiDisabled = true;
                        // 固定朝向
                        this.setYaw(this.getYaw());
                        this.bodyYaw = this.getYaw();
                        this.headYaw = this.getYaw();
                    } catch (Exception e) {
                        System.out.println("处理死亡动画同步请求时出错: " + e.getMessage());
                    }
                }
                return;
            }
            // 处理自定义状态码21 - 用于动画进度同步
            else if (status == 21) {
                if (this.getWorld().isClient()) {
                    try {
                        // 获取服务器当前的动画帧数
                        int serverFrame = this.dataTracker.get(DATA_ANIMATION_ID);
                        System.out.println("客户端收到服务端动画帧同步请求: 帧数=" + serverFrame);
                        
                        // 仅当帧数差异较大时才同步
                        if (Math.abs(serverFrame - this.animationTicks) > 20) {
                            System.out.println("同步动画帧: 本地=" + this.animationTicks + ", 服务器=" + serverFrame);
                            this.animationTicks = serverFrame;
                        }
                    } catch (Exception e) {
                        System.out.println("同步动画帧数据时出错: " + e.getMessage());
                    }
                }
                return;
            }
            
            // 使用try-catch包裹super调用，避免类型转换异常
            try {
                super.handleStatus(status);
            } catch (ClassCastException e) {
                // 安全处理类型转换异常
                System.out.println("处理实体状态时发生类型转换异常: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("处理实体状态时出错: " + e.getMessage());
            }
        } catch (Exception e) {
            // 捕获所有可能的异常，确保不会导致客户端崩溃
            System.out.println("处理实体状态发生严重错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 添加新方法：清理世界中所有相关的太阳实体
    private void cleanupAllSunsInWorld() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            // 获取BOSS周围较大范围内的所有实体
            Box searchBox = this.getBoundingBox().expand(32.0);
            List<Entity> nearbyEntities = serverWorld.getEntitiesByClass(
                Entity.class,
                searchBox,
                entity -> entity instanceof SunEntity // 只处理SunEntity
            );
            
            // 移除所有找到的太阳实体
            for (Entity entity : nearbyEntities) {
                if (entity instanceof SunEntity sunEntity) {
                    System.out.println("清理到一个遗留的太阳实体");
                    sunEntity.setAbsorbed(true);
                    sunEntity.discard();
                    sunEntity.remove(Entity.RemovalReason.DISCARDED);
                }
            }
        }
    }

    // 修改onRemoved方法，确保在实体被移除时清理
    @Override
    public void onRemoved() {
        System.out.println("BOSS被移除，执行最终清理");
        cleanupAllSunsInWorld(); // 添加这行
        cleanupSuns();
        super.onRemoved();
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        
        // 当动画状态被更新时，触发动画
        if (data.equals(ANIMATION_STATE) && this.getWorld().isClient()) {
            try {
                byte animationId = this.dataTracker.get(ANIMATION_STATE);
                if (animationId >= 0 && animationId < AnimationType.values().length) {
                    AnimationType animType = AnimationType.values()[animationId];
                    System.out.println("数据追踪器检测到动画状态变更: " + animType);
                    
                    // 如果动画状态和当前不一致，进行更新
                    if (this.currentAnimation != animType) {
                        // 保存之前的动画状态，用于防止重复触发
                        AnimationType previousAnimation = this.currentAnimation;
                        
                        // 如果是从非IDLE状态切换到IDLE，并且上一次的动画刚被重置
                        // 这可能是由于服务器发送的状态码10触发的，就不需要再次触发
                        if (animType == AnimationType.IDLE && 
                            previousAnimation != AnimationType.IDLE && 
                            System.currentTimeMillis() - lastIdleSetTime < 500) {
                            System.out.println("防止重复触发IDLE状态，跳过此次触发");
                            return;
                        }
                        
                        // 更新当前动画状态
                        this.currentAnimation = animType;
                        this.animationTicks = 0;
                        
                        // 如果上一个动画是RIGHT_MELEE_ATTACK，强制设置这个动画进度为结束
                        // 这是为了解决右手攻击重复播放的问题
                        if (previousAnimation == AnimationType.RIGHT_MELEE_ATTACK) {
                            System.out.println("强制结束右手攻击动画");
                            // 立即强制中断动画控制器，防止动画重复播放
                            try {
                                // 向客户端发送特殊状态码，强制重置动画
                                if (animType == AnimationType.IDLE) {
                                    // 记录强制重置次数
                                    rightAttackResetCount++;
                                    // 第二次才执行强制重置
                                    if (rightAttackResetCount >= 2) {
                                        triggerAnim("controller", "idle");
                                        rightAttackResetCount = 0;
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("强制结束动画时出错: " + e.getMessage());
                            }
                        }
                        
                        // 记录IDLE触发时间
                        if (animType == AnimationType.IDLE) {
                            lastIdleSetTime = System.currentTimeMillis();
                        } else {
                            // 非IDLE状态重置右手攻击计数
                            rightAttackResetCount = 0;
                        }
                        
                        // 根据动画类型触发相应动画
                        switch(animType) {
                            case LEFT_MELEE_ATTACK:
                                forceTriggerAnimation("controller", "left_attack");
                                System.out.println("客户端触发左手攻击动画(数据变更)");
                                break;
                            case RIGHT_MELEE_ATTACK:
                                forceTriggerAnimation("controller", "right_attack");
                                System.out.println("客户端触发右手攻击动画(数据变更)");
                                break;
                            case RANGED_ATTACK:
                                forceTriggerAnimation("controller", "ranged");
                                System.out.println("客户端触发远程攻击动画(数据变更)");
                                break;
                            case SEED_BARRAGE:
                                forceTriggerAnimation("controller", "seed_barrage");
                                System.out.println("客户端触发葵花籽弹幕动画(数据变更)");
                                break;
                            case SUNBEAM:
                                // 多次触发阳光射线动画，确保播放
                                forceTriggerAnimation("controller", "sunbeam");
                                System.out.println("客户端触发阳光射线动画(数据变更)");
                                
                                // 强制使用线程在短时间后再次触发动画，确保播放
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(100);
                                        // 再次检查状态，如果仍然是SUNBEAM，则再次触发
                                        if (currentAnimation == AnimationType.SUNBEAM) {
                                            System.out.println("客户端延迟再次触发阳光射线动画");
                                            triggerAnim("controller", "sunbeam");
                                        }
                                    } catch (Exception e) {
                                        // 忽略异常
                                    }
                                }).start();
                                break;
                            case PETAL_STORM:
                                forceTriggerAnimation("controller", "petal_storm");
                                System.out.println("客户端触发花瓣风暴动画(数据变更)");
                                break;
                            case KUWEI_STORM:
                                forceTriggerAnimation("controller", "kuwei");
                                System.out.println("客户端触发死亡动画(数据变更)");
                                break;
                            case IDLE:
                            default:
                                forceTriggerAnimation("controller", "idle");
                                System.out.println("客户端触发待机动画(数据变更)");
                                break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("处理动画数据变更时出错: " + e.getMessage());
            }
        }
    }
    
    /**
     * 强制触发动画 - 替代triggerAnim()
     * 不仅设置动画，还确保动画状态被正确同步
     */
    private void forceTriggerAnimation(String controllerName, String animName) {
        try {
            AnimationType animationType = getAnimation();
            System.out.println("强制触发动画: " + animName + " (动画状态: " + animationType + ")");
            
            // 如果尝试触发的是idle动画，而当前不是idle状态，先强制停止其他动画
            if ("idle".equals(animName) && animationType != AnimationType.IDLE) {
                try {
                    // 先停止当前动画
                    this.triggerAnim(controllerName, "idle");
                    // 短暂延迟确保动画系统有时间响应
                    Thread.sleep(5);
                } catch (Exception e) {
                    // 忽略中断异常
                }
            }
            
            // 调用原始的triggerAnim方法
            this.triggerAnim(controllerName, animName);
            
            // 如果是设置idle动画，确保完全停止其他动画
            if ("idle".equals(animName) && this.getWorld().isClient()) {
                // 关键：在客户端，RIGHT_MELEE_ATTACK结束后需要特殊处理
                AnimationType prevAnim = this.currentAnimation;
                // 设置当前动画状态为IDLE
                this.currentAnimation = AnimationType.IDLE;
                this.animationTicks = 0;
                
                // 如果之前是右手攻击，使用额外的手段确保动画停止
                if (prevAnim == AnimationType.RIGHT_MELEE_ATTACK) {
                    // 再次触发idle动画，增加成功几率
                    new Thread(() -> {
                        try {
                            Thread.sleep(50);
                            triggerAnim(controllerName, "idle");
                        } catch (Exception e) {
                            // 忽略
                        }
                    }).start();
                }
            }
        } catch (Exception e) {
            System.out.println("触发动画时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 