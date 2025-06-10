package lt.utopiabosses.event;

import lt.utopiabosses.registry.ItemRegistry;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
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
        
//        // 检查甘蔗、仙人掌等
//        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) {
//            return true; // 这些作物没有成熟状态，所以总是返回true
//        }
        
        return false;
    }
} 