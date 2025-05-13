package lt.utopiabosses.util;

import lt.utopiabosses.entity.SummoningEntity;
import lt.utopiabosses.registry.EntityRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 特效辅助工具类，用于生成各种视觉特效
 */
public class EffectHelper {

    /**
     * 在指定位置生成召唤特效
     * 
     * @param world 世界实例
     * @param position 生成位置
     * @return 生成的特效实体
     */
    public static SummoningEntity spawnSummoningEffect(World world, Vec3d position) {
        if (world.isClient()) {
            return null; // 仅在服务端生成实体
        }
        
        SummoningEntity effect = new SummoningEntity(EntityRegistry.SUMMONING, world);
        effect.setPosition(position.x, position.y, position.z);
        world.spawnEntity(effect);
        return effect;
    }
    
    /**
     * 在指定实体位置生成召唤特效
     * 
     * @param entity 实体
     * @return 生成的特效实体
     */
    public static SummoningEntity spawnSummoningEffect(Entity entity) {
        SummoningEntity effect = spawnSummoningEffect(
            entity.getWorld(), 
            new Vec3d(entity.getX(), entity.getY(), entity.getZ())
        );
        
        // 设置特效的拥有者为该实体（重要：这使特效的朝向与实体同步）
        if (effect != null) {
            effect.setOwner(entity);
        }
        
        return effect;
    }
    
    /**
     * 在指定实体位置生成召唤特效，包含Y轴偏移
     * 
     * @param entity 实体
     * @param yOffset Y轴偏移量
     * @return 生成的特效实体
     */
    public static SummoningEntity spawnSummoningEffect(Entity entity, double yOffset) {
        SummoningEntity effect = spawnSummoningEffect(
            entity.getWorld(), 
            new Vec3d(entity.getX(), entity.getY() + yOffset, entity.getZ())
        );
        
        // 设置特效的拥有者为该实体（重要：这使特效的朝向与实体同步）
        if (effect != null) {
            effect.setOwner(entity);
        }
        
        return effect;
    }
} 