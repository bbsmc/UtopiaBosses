package lt.utopiabosses.client.renderer;

import lt.utopiabosses.client.model.PlantSpiritModel;
import lt.utopiabosses.entity.PlantSpirit;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PlantSpiritRenderer extends GeoEntityRenderer<PlantSpirit> {
    public PlantSpiritRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new PlantSpiritModel());
        // 设置阴影半径
        this.shadowRadius = 0.4f;
    }
    
    @Override
    public void render(PlantSpirit entity, float entityYaw, float partialTick, MatrixStack poseStack,
                     VertexConsumerProvider bufferSource, int packedLight) {
        // 调用父类的渲染方法
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
} 