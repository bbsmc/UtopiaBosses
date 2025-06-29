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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class SunflowerAltarBlockEntity extends BlockEntity {
    private boolean hasEssence = false;
    private int essenceType = 0; // 0=无精华, 1=向日葵精华, 2=树灵精华
    private float itemRotation = 0.0F;
    private static final float ROTATION_SPEED = 2.0F;
    private static final float FLOAT_AMPLITUDE = 0.15F;
    private static final float FLOAT_SPEED = 0.05F;
    private float floatOffset = 0.0F;

    static Random random =  new Random();

    public SunflowerAltarBlockEntity(BlockPos pos, BlockState state) {
        this(null, pos, state);
    }

    // 主构造函数
    public SunflowerAltarBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type != null ? type : BlockEntityRegistry.SUNFLOWER_ALTAR_BLOCK_ENTITY, pos, state);
    }

    
    public boolean hasEssence() {
        return hasEssence;
    }
    
    public void setHasEssence(boolean hasEssence) {
        this.hasEssence = hasEssence;
        markDirty();
    }
    
    public int getEssenceType() {
        return essenceType;
    }
    
    public void setEssenceType(int essenceType) {
        this.essenceType = essenceType;
        markDirty();
    }
    
    public ItemStack getDisplayItem() {
        if (!hasEssence) return ItemStack.EMPTY;
        
        return switch (essenceType) {
            case 1 -> new ItemStack(ItemRegistry.SUNFLOWER_NATURAL_ESSENCE);
            case 2 -> new ItemStack(ItemRegistry.TREE_SPIRIT_ESSENCE);
            default -> ItemStack.EMPTY;
        };
    }
    
    // 用于浮动和旋转动画的静态方法
    public static void tick(World world, BlockPos pos, BlockState state, SunflowerAltarBlockEntity entity) {
        if (entity.hasEssence && world.isClient) {
            // 更新旋转
            entity.itemRotation = (entity.itemRotation + ROTATION_SPEED) % 360.0F;
            
            // 更新浮动位置
            entity.floatOffset = (float) Math.sin(world.getTime() * FLOAT_SPEED) * FLOAT_AMPLITUDE;
        }



        if (world.isClient){
            // 粒子的数量
            int count = 40;

            // 粒子生成的立方体区域的一半边长
            double range = 40.0;

            // 粒子的最大速度
            double speed = 0.2;

            // 生成粒子
            for (int i = 0; i < count; i++) {
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * range * 2;
                double y = pos.getY() + 0.5 + (random.nextDouble() - 0.5) * range * 2;
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * range * 2;

                double deltaX = (random.nextDouble() - 0.5) * speed;
                double deltaY = (random.nextDouble() - 0.5) * speed;
                double deltaZ = (random.nextDouble() - 0.5) * speed;

                world.addParticle(ParticleTypes.WAX_ON, x, y, z, deltaX, deltaY, deltaZ);
            }
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
        nbt.putInt("EssenceType", essenceType);
    }
    
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        hasEssence = nbt.getBoolean("HasEssence");
        essenceType = nbt.getInt("EssenceType");
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