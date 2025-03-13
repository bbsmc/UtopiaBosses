package lt.utopiabosses.client.renderer;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.SunflowerSeedEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class SunflowerSeedRenderer extends FlyingItemEntityRenderer<SunflowerSeedEntity> {
    public SunflowerSeedRenderer(EntityRendererFactory.Context context) {
        super(context, 0.75f, true); // 缩放因子和是否添加随机偏移
    }
    
    @Override
    public void render(SunflowerSeedEntity entity, float yaw, float tickDelta, MatrixStack matrices, 
                      VertexConsumerProvider vertexConsumers, int light) {
        try {
            if (entity == null || matrices == null || vertexConsumers == null) {
                Utopiabosses.LOGGER.error("尝试渲染空的向日葵种子实体或渲染上下文");
                return;
            }
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
        } catch (Exception e) {
            Utopiabosses.LOGGER.error("向日葵种子渲染过程中出现异常:", e);
        }
    }
} 