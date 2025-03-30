package lt.utopiabosses.client.renderer;

import lt.utopiabosses.client.model.LittleTreeManModel;
import lt.utopiabosses.entity.LittleTreeMan;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LittleTreeManRenderer extends GeoEntityRenderer<LittleTreeMan> {
    public LittleTreeManRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new LittleTreeManModel());
        // 设置阴影半径
        this.shadowRadius = 0.4f;
    }
} 