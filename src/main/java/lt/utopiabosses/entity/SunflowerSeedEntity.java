package lt.utopiabosses.entity;

import lt.utopiabosses.registry.EntityRegistry;
import lt.utopiabosses.registry.ItemRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class SunflowerSeedEntity extends ThrownItemEntity {

    float damage = 8f;
    
    // 绿色粒子的颜色值
    private static final Vector3f GREEN_COLOR = new Vector3f(0.2f, 0.8f, 0.1f);
    
    // 添加追踪目标
    private LivingEntity homingTarget;
    private int homingStrength = 3; // 追踪强度
    
    // 添加一个标志，区分普通攻击和技能攻击
    private boolean isSkillAttack = false;
    
    // 添加一个标志，标记是否来自加特林
    private boolean isFromGatling = false;

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
        return ItemRegistry.ENCHANTED_SUNFLOWER_SEED;
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
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        super.onEntityHit(entityHitResult);
        
        Entity entity = entityHitResult.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            // 使用标志判断是否来自加特林
            if (this.isFromGatling) {
                // 加特林的子弹绕过无敌时间
                // 保存原本的无敌时间
                int originalHurtTime = livingEntity.hurtTime;
                int originalRegenTime = livingEntity.timeUntilRegen;
                
                // 临时设置无敌时间为0，允许立即受到伤害
                livingEntity.hurtTime = 0;
                livingEntity.timeUntilRegen = 0;
                
                // 造成伤害
                boolean damaged = entity.damage(this.getDamageSources().mobProjectile(this, (LivingEntity)this.getOwner()), damage);
                
                // 如果伤害成功，设置一个极短的无敌时间（1 tick），仅防止同一发子弹多次伤害
                if (damaged) {
                    livingEntity.hurtTime = 1;
                    livingEntity.timeUntilRegen = 1;
                } else {
                    // 如果伤害失败，恢复原本的无敌时间
                    livingEntity.hurtTime = originalHurtTime;
                    livingEntity.timeUntilRegen = originalRegenTime;
                }
            } else {
                // 非加特林的子弹使用正常伤害机制
                entity.damage(this.getDamageSources().mobProjectile(this, (LivingEntity)this.getOwner()), damage);
            }
            
            // 如果是玩家，添加减速效果
            if (entity instanceof PlayerEntity) {
                livingEntity.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            }
            
            // 添加击退效果 - 让实体朝向攻击者面向的方向击退
            float knockbackStrength = 0.2f; // 统一为轻微击退（约一格距离）
            
            // 计算击退方向 - 使用攻击者面向的方向
            Vec3d knockbackDir;
            Entity owner = this.getOwner();
            
            if (owner instanceof LivingEntity) {
                // 获取攻击者面向的方向 - Minecraft中0度朝南(+Z)，90度朝西(-X)
                float yaw = owner.getYaw() * 0.017453292F; // 转换为弧度
                // 攻击者面向的方向向量
                knockbackDir = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw));
            } else {
                // 如果没有所有者，使用种子飞行方向
                knockbackDir = this.getVelocity().normalize();
            }
            
            // 应用击退
            entity.setVelocity(entity.getVelocity().add(
                knockbackDir.x * knockbackStrength,
                0, // 不影响y轴
                knockbackDir.z * knockbackStrength
            ));
            entity.velocityModified = true;
        }
        
        // 删除粒子效果代码，但保留音效
        if (!this.getWorld().isClient()) {
            if (isSkillAttack) {
                // 播放爆炸音效
                this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.3f, 0.8f);
            } else {
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
            // 删除粒子效果代码
            this.discard();
        }
    }
    
    @Override
    public void handleStatus(byte status) {
        // 删除所有粒子效果代码
    }

    public void setHomingTarget(LivingEntity target) {
        this.homingTarget = target;
    }
    
    // 添加方法设置是否为技能攻击
    public void setSkillAttack(boolean isSkill) {
        this.isSkillAttack = isSkill;
    }

    public float getDamage() {
        return damage;
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }
    
    // 添加设置加特林标志的方法
    public void setFromGatling(boolean fromGatling) {
        this.isFromGatling = fromGatling;
    }
}