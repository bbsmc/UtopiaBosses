package lt.utopiabosses.client.renderer;

import lt.utopiabosses.client.model.SummoningModel;
import lt.utopiabosses.entity.SummoningEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SummoningRenderer extends GeoEntityRenderer<SummoningEntity> {
    public SummoningRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new SummoningModel());
        // 设置阴影大小为0（特效实体不需要阴影）
        this.shadowRadius = 0.0f;
    }
   
    // 使特效实体在黑暗中也能发光显示
    @Override
    protected int getBlockLight(SummoningEntity entity, net.minecraft.util.math.BlockPos pos) {
        return 15; // 最大亮度
    }
} 