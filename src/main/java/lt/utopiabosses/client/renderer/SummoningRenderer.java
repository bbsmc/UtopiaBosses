package lt.utopiabosses.client.renderer;

import lt.utopiabosses.client.model.SummoningModel;
import lt.utopiabosses.entity.SummoningEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Quaternionf;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SummoningRenderer extends GeoEntityRenderer<SummoningEntity> {
    public SummoningRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SummoningModel());
        // 设置阴影大小为0（特效实体不需要阴影）
        this.shadowRadius = 0.0f;
    }
    
    @Override
    public void render(SummoningEntity entity, float entityYaw, float partialTick, 
                      MatrixStack poseStack, 
                      VertexConsumerProvider bufferSource, 
                      int packedLight) {
        
        // 应用缩放和旋转
        poseStack.push();
        
        // 计算正确的旋转角度（需要以度为单位的角度）
        float rotationYaw = entityYaw;
        
        // 平移到正确位置
        float scale = entity.getRenderScale();
        poseStack.scale(scale, scale, scale);
        
        // 在矩阵上直接应用旋转效果（需在缩放之后）
        // 在特效的位置创建Y轴上的旋转
        poseStack.translate(0, 0, 0);
        // 使用JOML Quaternion旋转Y轴 (JOML是Minecraft使用的数学库)
        poseStack.multiply(new Quaternionf().rotateY((float)Math.toRadians(-rotationYaw)));

        
        // 调用原始渲染方法，但传入0作为entityYaw，因为我们已经手动应用了旋转
        super.render(entity, 0, partialTick, poseStack, bufferSource, packedLight);
        
        // 恢复矩阵状态
        poseStack.pop();
    }
    
    // 使特效实体在黑暗中也能发光显示
    @Override
    protected int getBlockLight(SummoningEntity entity, net.minecraft.util.math.BlockPos pos) {
        return 15; // 最大亮度
    }
} 