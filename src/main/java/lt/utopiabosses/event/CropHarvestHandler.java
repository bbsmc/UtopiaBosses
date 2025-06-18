package lt.utopiabosses.event;

import lt.utopiabosses.item.SunflowerShieldItem;
import lt.utopiabosses.registry.ItemRegistry;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.block.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class CropHarvestHandler {
    private static final Random random = new Random();
    
    // 掉落自然精华的几率 (10%)
    private static final float DROP_CHANCE = 0.1f;
    
    public static void register() {
        // 注册方块破坏事件监听器
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            // 检查是否是服务器端
            if (world.isClient) return;
            
            // 检查破坏的是否是成熟的农作物
            if (isMatureCrop(state)) {
                // 随机决定是否掉落自然精华
                if (random.nextFloat() < DROP_CHANCE) {
                    // 创建自然精华物品实体
                    ItemEntity itemEntity = new ItemEntity(
                            world,
                            pos.getX() + 0.5,
                            pos.getY() + 0.5,
                            pos.getZ() + 0.5,
                            new ItemStack(ItemRegistry.NATURAL_ESSENCE)
                    );
                    
                    // 生成物品
                    world.spawnEntity(itemEntity);
                }
            }
        });
        
    }
    
    // 检查方块是否是成熟的农作物
    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        
        // 检查小麦
        if (block instanceof CropBlock) {
            CropBlock cropBlock = (CropBlock) block;
            return cropBlock.isMature(state);
        }
        
        // 检查胡萝卜、土豆等
        if (block instanceof CarrotsBlock || block instanceof PotatoesBlock || block instanceof BeetrootsBlock) {
            return ((CropBlock) block).isMature(state);
        }
        
        // 检查南瓜和西瓜
        if (block instanceof StemBlock) {
            return state.get(StemBlock.AGE) >= 7;
        }
        
        return false;
    }
    
    /**
     * 检查目标是否正在格挡来自攻击者的攻击
     */
    private static boolean isBlockingAttack(LivingEntity defender, PlayerEntity attacker) {
        if (!defender.isUsingItem() || !(defender.getActiveItem().getItem() instanceof SunflowerShieldItem)) {
            return false;
        }
        
        // 计算攻击者相对于格挡者的方向
        double dx = attacker.getX() - defender.getX();
        double dz = attacker.getZ() - defender.getZ();
        double attackAngle = Math.atan2(dz, dx) * 180.0 / Math.PI;
        
        // 规范化角度到0-360度
        attackAngle = (attackAngle + 360) % 360;
        
        // 获取格挡者的朝向角度
        double defenderYaw = ((defender.getYaw() % 360) + 360) % 360;
        
        // 计算角度差
        double angleDiff = Math.abs(attackAngle - defenderYaw);
        if (angleDiff > 180) {
            angleDiff = 360 - angleDiff;
        }
        
        // 如果角度差小于90度，说明正在面向攻击者格挡
        return angleDiff <= 90;
    }
} 