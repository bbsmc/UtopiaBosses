package lt.utopiabosses.client.renderer;

import lt.utopiabosses.client.model.TreeBossModel;
import lt.utopiabosses.entity.TreeBoss;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

public class TreeBossRenderer extends GeoEntityRenderer<TreeBoss> {
    public TreeBossRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new TreeBossModel());
        // 设置阴影半径
        this.shadowRadius = 3.0f;
    }
    
    @Override
    public void render(TreeBoss entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.push();
        poseStack.scale(2.0F, 2.0F, 2.0F);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }
} 