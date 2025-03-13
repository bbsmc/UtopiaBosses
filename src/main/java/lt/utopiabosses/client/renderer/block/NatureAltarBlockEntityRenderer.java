package lt.utopiabosses.client.renderer.block;

import lt.utopiabosses.block.entity.NatureAltarBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

public class NatureAltarBlockEntityRenderer implements BlockEntityRenderer<NatureAltarBlockEntity> {
    
    public NatureAltarBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }
    
    @Override
    public void render(NatureAltarBlockEntity entity, float tickDelta, MatrixStack matrices, 
                      VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // 如果没有精华，不渲染任何东西
        if (!entity.hasEssence()) {
            return;
        }
        
        ItemStack itemStack = entity.getDisplayItem();
        if (itemStack.isEmpty()) {
            return;
        }
        
        matrices.push();
        
        // 将物品放置在方块中心上方（稍微提高位置）
        matrices.translate(0.5, 1.35 + entity.getFloatOffset(), 0.5);
        
        // 物品旋转
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getItemRotation()));
        
        // 缩放物品大小（增大）
        matrices.scale(0.75f, 0.75f, 0.75f);
        
        // 渲染物品
        MinecraftClient.getInstance().getItemRenderer().renderItem(
                itemStack,
                ModelTransformationMode.GROUND,
                light,
                overlay,
                matrices,
                vertexConsumers,
                entity.getWorld(),
                0);
        
        matrices.pop();
    }
} 