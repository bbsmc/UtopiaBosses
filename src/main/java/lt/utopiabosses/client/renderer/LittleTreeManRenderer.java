package lt.utopiabosses.client.renderer;

import lt.utopiabosses.client.model.LittleTreeManModel;
import lt.utopiabosses.entity.LittleTreeMan;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LittleTreeManRenderer extends GeoEntityRenderer<LittleTreeMan> {
    public LittleTreeManRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new LittleTreeManModel());
        // 设置阴影半径
        this.shadowRadius = 0.4f;
    }
    
    @Override
    public void render(LittleTreeMan entity, float entityYaw, float partialTick, MatrixStack poseStack,
                     VertexConsumerProvider bufferSource, int packedLight) {
        // 调用父类的渲染方法
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public void preRender(MatrixStack poseStack, LittleTreeMan animatable, BakedGeoModel model, 
                         VertexConsumerProvider bufferSource, VertexConsumer buffer, 
                         boolean isReRender, float partialTick, int packedLight, int packedOverlay, 
                         float red, float green, float blue, float alpha) {
        
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, 
                      packedLight, packedOverlay, red, green, blue, alpha);
    }
    
    // 确保在移动时能够正确播放动画
    @Override
    public void actuallyRender(MatrixStack poseStack, LittleTreeMan animatable, BakedGeoModel model, 
                              RenderLayer renderType, VertexConsumerProvider bufferSource, 
                              VertexConsumer buffer, boolean isReRender, float partialTick, 
                              int packedLight, int packedOverlay, float red, float green, float blue, 
                              float alpha) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, 
                           isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
} 