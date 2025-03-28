package lt.utopiabosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import lt.utopiabosses.registry.ItemRegistry;
import net.minecraft.util.math.Vec3d;

public class SunEntity extends ItemEntity {
    private boolean absorbed = false;
    private Entity owner = null;
    private Vec3d targetPos = null;
    private double moveSpeed = 0.05; // 移动速度，可以调整
    
    public SunEntity(EntityType<? extends ItemEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.setPickupDelay(32767);
        // 设置物品堆栈为SUN_ITEM
        this.setStack(new ItemStack(ItemRegistry.SUN_ITEM));
        // 防止实体消失
        this.age = -32768;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 确保实体不会被拾取
        this.setPickupDelay(32767);
        this.age = -32768; // 在tick中持续保持年龄值
        
        // 如果已被吸收，立即移除
        if (absorbed) {
            this.discard();
            return;
        }
        
        // 如果有目标位置，平滑移动
        if (targetPos != null) {
            Vec3d currentPos = this.getPos();
            Vec3d direction = targetPos.subtract(currentPos);
            double distance = direction.length();
            
            if (distance > 0.01) {
                // 标准化方向向量并应用速度
                direction = direction.normalize().multiply(moveSpeed);
                
                // 使用原生运动系统设置速度
                this.setVelocity(direction.x, direction.y, direction.z);
                this.velocityModified = true;
            } else {
                // 到达目标位置，停止移动
                this.setVelocity(0, 0, 0);
                this.setPosition(targetPos);
            }
        }
    }
    
    public void setTargetPos(Vec3d target) {
        this.targetPos = target;
    }
    
    public void setMoveSpeed(double speed) {
        this.moveSpeed = speed;
    }
    
    @Override
    public boolean cannotPickup() {
        return true;
    }
    
    @Override
    public boolean isAttackable() {
        return false;
    }
    
    public void setAbsorbed(boolean absorbed) {
        this.absorbed = absorbed;
        if (absorbed) {
            this.discard();
        }
    }
    
    public boolean isAbsorbed() {
        return this.absorbed;
    }
    
    public void setOwner(Entity owner) {
        this.owner = owner;
    }

    public Entity getOwner() {
        return this.owner;
    }
    
    @Override
    public boolean shouldRender(double distance) {
        return !this.absorbed && super.shouldRender(distance);
    }
} 