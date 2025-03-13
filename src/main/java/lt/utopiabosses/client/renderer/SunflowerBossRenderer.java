package lt.utopiabosses.client.renderer;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.client.model.SunflowerBossModel;
import lt.utopiabosses.entity.SunflowerBossEntity;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SunflowerBossRenderer extends GeoEntityRenderer<SunflowerBossEntity> {
    public SunflowerBossRenderer(Context ctx) {
        super(ctx, new SunflowerBossModel());
        this.shadowRadius = 2.0F;
    }

    @Override
    public void render(SunflowerBossEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        try {
            if (entity == null || poseStack == null || bufferSource == null) {
                Utopiabosses.LOGGER.error("尝试渲染空的向日葵BOSS实体或渲染上下文");
                return;
            }
            
            poseStack.push();
            poseStack.scale(1.5F, 1.5F, 1.5F);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.pop();
        } catch (Exception e) {
            Utopiabosses.LOGGER.error("向日葵BOSS渲染过程中出现异常:", e);
        }
    }

    @Override
    public RenderLayer getRenderType(SunflowerBossEntity entity, Identifier texture, 
                                   VertexConsumerProvider bufferSource, float partialTick) {
        if (entity == null || texture == null || bufferSource == null) {
            return RenderLayer.getEntityCutout(new Identifier(Utopiabosses.MOD_ID, "textures/entity/sunflower_boss.png"));
        }
        return super.getRenderType(entity, texture, bufferSource, partialTick);
    }
} 