package lt.utopiabosses.block.entity;

import lt.utopiabosses.registry.BlockEntityRegistry;
import lt.utopiabosses.registry.ItemRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class NatureAltarBlockEntity extends BlockEntity {
    private boolean hasEssence = false;
    private float itemRotation = 0.0F;
    private static final float ROTATION_SPEED = 2.0F;
    private static final float FLOAT_AMPLITUDE = 0.15F;
    private static final float FLOAT_SPEED = 0.05F;
    private float floatOffset = 0.0F;
    
    // 避免直接引用BlockEntityRegistry.NATURE_ALTAR_BLOCK_ENTITY
    public NatureAltarBlockEntity(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }
    
    // 主构造函数
    public NatureAltarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type != null ? type : BlockEntityRegistry.NATURE_ALTAR_BLOCK_ENTITY, pos, state);
    }
    
    public boolean hasEssence() {
        return hasEssence;
    }
    
    public void setHasEssence(boolean hasEssence) {
        this.hasEssence = hasEssence;
        markDirty();
    }
    
    public ItemStack getDisplayItem() {
        return hasEssence ? new ItemStack(ItemRegistry.SUNFLOWER_NATURAL_ESSENCE) : ItemStack.EMPTY;
    }
    
    // 用于浮动和旋转动画的静态方法
    public static void tick(World world, BlockPos pos, BlockState state, NatureAltarBlockEntity entity) {
        if (entity.hasEssence && world.isClient) {
            // 更新旋转
            entity.itemRotation = (entity.itemRotation + ROTATION_SPEED) % 360.0F;
            
            // 更新浮动位置
            entity.floatOffset = (float) Math.sin(world.getTime() * FLOAT_SPEED) * FLOAT_AMPLITUDE;
        }
    }
    
    public float getItemRotation() {
        return itemRotation;
    }
    
    public float getFloatOffset() {
        return floatOffset;
    }
    
    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean("HasEssence", hasEssence);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hasEssence = nbt.getBoolean("HasEssence");
    }
    
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
    
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
} 