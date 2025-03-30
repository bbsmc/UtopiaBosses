package lt.utopiabosses.client.renderer;

import lt.utopiabosses.client.model.TreeBossModel;
import lt.utopiabosses.entity.TreeBoss;
import net.minecraft.client.render.entity.EntityRendererFactory;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TreeBossRenderer extends GeoEntityRenderer<TreeBoss> {
    public TreeBossRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new TreeBossModel());
        // 设置阴影半径
        this.shadowRadius = 1.5f;
    }
} 