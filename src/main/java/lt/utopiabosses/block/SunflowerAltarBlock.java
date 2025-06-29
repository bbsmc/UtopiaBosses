package lt.utopiabosses.block;

import lt.utopiabosses.block.entity.SunflowerAltarBlockEntity;
import lt.utopiabosses.registry.BlockEntityRegistry;
import lt.utopiabosses.registry.EntityRegistry;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

public class SunflowerAltarBlock extends HorizontalFacingBlock implements BlockEntityProvider {
    // 属性：是否已放置向日葵自然精华
    public static final BooleanProperty HAS_ESSENCE = BooleanProperty.of("has_essence");

    public SunflowerAltarBlock(Settings settings) {
        super(settings);
        // 设置默认状态
        setDefaultState(getDefaultState().with(FACING, net.minecraft.util.math.Direction.NORTH).with(HAS_ESSENCE, false));
    }


    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HAS_ESSENCE);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return super.getDefaultState()
                .with(FACING, ctx.getHorizontalPlayerFacing().getOpposite())
                .with(HAS_ESSENCE, false);
    }


    @Override
    public void precipitationTick(BlockState state, World world, BlockPos pos, Biome.Precipitation precipitation) {
        super.precipitationTick(state, world, pos, precipitation);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack heldItem = player.getStackInHand(hand);
        
        // 获取方块实体
        if (world.getBlockEntity(pos) instanceof SunflowerAltarBlockEntity blockEntity) {
            // 如果祭坛上没有精华，且玩家手持向日葵自然精华
            if (!state.get(HAS_ESSENCE) && heldItem.isOf(lt.utopiabosses.registry.ItemRegistry.SUNFLOWER_NATURAL_ESSENCE)) {
                // 更新方块状态
                world.setBlockState(pos, state.with(HAS_ESSENCE, true));
                blockEntity.setHasEssence(true);
                blockEntity.setEssenceType(1); // 向日葵精华
                
                // 消耗物品
                if (!player.isCreative()) {
                    heldItem.decrement(1);
                }
                
                return ActionResult.success(world.isClient);
            }
            
            // 如果祭坛上有精华，且玩家手持骨粉
            if (state.get(HAS_ESSENCE) && heldItem.isOf(Items.BONE_MEAL)) {
                // 只在服务器端执行
                if (!world.isClient) {
                    
                    if (!player.isCreative()) {
                        heldItem.decrement(1);
                    }
                    
                    // 根据精华类型生成对应的Boss
                    int essenceType = blockEntity.getEssenceType();
                    if (essenceType == 1) {
                        // 生成向日葵Boss
                        EntityRegistry.SUNFLOWER_BOSS.spawn(
                            (net.minecraft.server.world.ServerWorld)world, 
                            pos, 
                            net.minecraft.entity.SpawnReason.TRIGGERED
                        );
                    } else if (essenceType == 2) {
                        // 生成树灵之王Boss
                        EntityRegistry.TREE_BOSS.spawn(
                            (net.minecraft.server.world.ServerWorld)world, 
                            pos, 
                            net.minecraft.entity.SpawnReason.TRIGGERED
                        );
                    }
                    // 只消耗物品，重置祭坛状态
                    world.setBlockState(pos, state.with(HAS_ESSENCE, false));
                    blockEntity.setHasEssence(false);
                    blockEntity.setEssenceType(0);
                }
                
                return ActionResult.success(world.isClient);
            }
            
            // 如果祭坛上有精华，且玩家手持的不是骨粉（空手或其他物品），取回精华
            if (state.get(HAS_ESSENCE) && !heldItem.isOf(Items.BONE_MEAL)) {
                if (!world.isClient) {
                    // 根据精华类型给予对应物品
                    int essenceType = blockEntity.getEssenceType();
                    ItemStack essenceItem = ItemStack.EMPTY;
                    
                    if (essenceType == 1) {
                        essenceItem = new ItemStack(lt.utopiabosses.registry.ItemRegistry.SUNFLOWER_NATURAL_ESSENCE);
                    }
                    
                    if (!essenceItem.isEmpty()) {
                        // 尝试将物品放入玩家背包，如果背包满了就掉落在地上
                        if (!player.getInventory().insertStack(essenceItem)) {
                            player.dropItem(essenceItem, false);
                        }
                    }
                    
                    // 重置祭坛状态
                    world.setBlockState(pos, state.with(HAS_ESSENCE, false));
                    blockEntity.setHasEssence(false);
                    blockEntity.setEssenceType(0);
                }
                
                return ActionResult.success(world.isClient);
            }
        }
        
        return ActionResult.PASS;
    }
    
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new SunflowerAltarBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityRegistry.SUNFLOWER_ALTAR_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) ((world1, pos, state1, blockEntity) ->
                    SunflowerAltarBlockEntity.tick(world1, pos, state1, (SunflowerAltarBlockEntity) blockEntity));
        }
        return null;
    }
    
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            // 检查是否有精华需要掉落
            if (state.get(HAS_ESSENCE) && world.getBlockEntity(pos) instanceof SunflowerAltarBlockEntity blockEntity) {
                int essenceType = blockEntity.getEssenceType();
                ItemStack essenceItem = ItemStack.EMPTY;
                
                if (essenceType == 1) {
                    essenceItem = new ItemStack(lt.utopiabosses.registry.ItemRegistry.SUNFLOWER_NATURAL_ESSENCE);
                }
                
                if (!essenceItem.isEmpty()) {
                    // 在方块位置掉落精华
                    net.minecraft.util.ItemScatterer.spawn(world, pos, net.minecraft.util.collection.DefaultedList.ofSize(1, essenceItem));
                }
            }
            
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return true;
    }
} 