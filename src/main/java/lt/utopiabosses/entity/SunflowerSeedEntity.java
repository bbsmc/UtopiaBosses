package lt.utopiabosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Vector3f;
import net.minecraft.world.World;
import lt.utopiabosses.registry.EntityRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public class SunflowerSeedEntity extends ThrownItemEntity {
    
    // 绿色粒子的颜色值
    private static final Vector3f GREEN_COLOR = new Vector3f(0.2f, 0.8f, 0.1f);
    
    // 添加追踪目标
    private LivingEntity homingTarget;
    private int homingStrength = 3; // 追踪强度
    
    // 添加一个标志，区分普通攻击和技能攻击
    private boolean isSkillAttack = false;

    public SunflowerSeedEntity(EntityType<? extends ThrownItemEntity> entityType, World world) {
        super(entityType, world);
    }

    public SunflowerSeedEntity(World world, LivingEntity owner) {
        super(EntityRegistry.SUNFLOWER_SEED, owner, world);
    }

    public SunflowerSeedEntity(World world, double x, double y, double z) {
        super(EntityRegistry.SUNFLOWER_SEED, x, y, z, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.WHEAT_SEEDS;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 追踪逻辑
        if (!this.getWorld().isClient && homingTarget != null && homingTarget.isAlive()) {
            // 计算当前速度
            Vec3d velocity = new Vec3d(this.getVelocity().x, this.getVelocity().y, this.getVelocity().z);
            double speed = velocity.length();
            
            // 计算到目标的方向
            Vec3d toTarget = homingTarget.getEyePos().subtract(this.getPos()).normalize();
            
            // 混合当前方向和目标方向
            Vec3d newDirection = velocity.normalize().multiply(5).add(toTarget.multiply(homingStrength)).normalize();
            
            // 应用新速度
            this.setVelocity(newDirection.x * speed, newDirection.y * speed, newDirection.z * speed);
        }
        
        // 在客户端生成粒子效果
        if (this.getWorld().isClient) {
            // 绿色尘埃粒子
            this.getWorld().addParticle(
                new DustParticleEffect(GREEN_COLOR, 1.0F),
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0
            );
            
            // 添加一些叶子粒子效果增强视觉效果
            if (this.random.nextInt(3) == 0) {
                this.getWorld().addParticle(
                    ParticleTypes.COMPOSTER,
                    this.getX(), this.getY(), this.getZ(),
                    this.random.nextGaussian() * 0.05,
                    this.random.nextGaussian() * 0.05,
                    this.random.nextGaussian() * 0.05
                );
            }
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        
        Entity entity = entityHitResult.getEntity();
        if (entity instanceof LivingEntity) {
            // 造成4颗心伤害
            entity.damage(this.getDamageSources().mobProjectile(this, (LivingEntity)this.getOwner()), 8.0f);
            
            // 如果是玩家，添加减速效果
            if (entity instanceof PlayerEntity) {
                ((LivingEntity)entity).addStatusEffect(
                    new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            }
            
            // 添加向后击退效果 - 让玩家向自己的后方击退
            float knockbackStrength = isSkillAttack ? 0.01f : 1f; // 降低技能攻击击退，提高普通攻击击退
            
            // 计算击退方向 - 使用玩家面向的反方向作为"后方"
            Vec3d knockbackDir;
            if (entity instanceof PlayerEntity) {
                // 获取玩家面向的方向并取反 - Minecraft中0度朝南(+Z)，90度朝西(-X)
                float yaw = entity.getYaw() * 0.017453292F; // 转换为弧度
                // 玩家面向的方向是(-sin(yaw), 0, cos(yaw))，所以后方是(sin(yaw), 0, -cos(yaw))
                knockbackDir = new Vec3d(Math.sin(yaw), 0, -Math.cos(yaw));
            } else {
                // 对于非玩家实体，使用种子飞行方向的反方向作为后备
                knockbackDir = this.getVelocity().normalize().negate();
            }
            
            // 应用击退
            entity.setVelocity(entity.getVelocity().add(
                knockbackDir.x * knockbackStrength,
                0, // 不影响y轴
                knockbackDir.z * knockbackStrength
            ));
            entity.velocityModified = true;
        }
        
        // 根据是否为技能攻击决定效果
        if (!this.getWorld().isClient()) {
            if (isSkillAttack) {
                // 技能攻击使用爆炸效果
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.EXPLOSION, 
                    this.getX(), this.getY(), this.getZ(),
                    8, 0.2, 0.2, 0.2, 0.1
                );
                
                // 播放爆炸音效
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.3f, 0.8f);
            } else {
                // 普通攻击使用更柔和的效果
                ((ServerWorld)this.getWorld()).spawnParticles(
                    ParticleTypes.COMPOSTER, 
                    this.getX(), this.getY(), this.getZ(),
                    12, 0.3, 0.3, 0.3, 0.1
                );
                
                // 播放柔和音效
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_SLIME_ATTACK, SoundCategory.BLOCKS, 0.5f, 1.0f);
            }
        }
        
        // 移除弹幕
        this.discard();
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);
        if (!this.getWorld().isClient) {
            // 碰撞时生成更多粒子效果
            this.getWorld().sendEntityStatus(this, (byte)3);
            this.discard();
        }
    }
    
    @Override
    public void handleStatus(byte status) {
        if (status == 3) {
            // 减少碰撞时的粒子效果数量和范围
            for (int i = 0; i < 6; ++i) { // 从12减少到6
                this.getWorld().addParticle(
                    new DustParticleEffect(GREEN_COLOR, 1.0F),
                    this.getX() + this.random.nextGaussian() * 0.1, // 减小范围
                    this.getY() + this.random.nextGaussian() * 0.1,
                    this.getZ() + this.random.nextGaussian() * 0.1,
                    this.random.nextGaussian() * 0.05, // 减小速度
                    this.random.nextGaussian() * 0.05,
                    this.random.nextGaussian() * 0.05
                );
                
                // 添加一些叶子粒子
                if (i % 2 == 0) {
                    this.getWorld().addParticle(
                        ParticleTypes.COMPOSTER,
                        this.getX() + this.random.nextGaussian() * 0.1,
                        this.getY() + this.random.nextGaussian() * 0.1,
                        this.getZ() + this.random.nextGaussian() * 0.1,
                        this.random.nextGaussian() * 0.05,
                        this.random.nextGaussian() * 0.05,
                        this.random.nextGaussian() * 0.05
                    );
                }
            }
        }
    }

    public void setHomingTarget(LivingEntity target) {
        this.homingTarget = target;
    }
    
    // 添加方法设置是否为技能攻击
    public void setSkillAttack(boolean isSkill) {
        this.isSkillAttack = isSkill;
    }
} 