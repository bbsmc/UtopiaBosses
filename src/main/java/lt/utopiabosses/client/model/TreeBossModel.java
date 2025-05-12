package lt.utopiabosses.client.model;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.TreeBoss;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class TreeBossModel extends DefaultedEntityGeoModel<TreeBoss> {
    
    public TreeBossModel() {
        super(new Identifier(Utopiabosses.MOD_ID, "tree_boss"),true);

    }
    
    @Override
    public Identifier getModelResource(TreeBoss animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "geo/tree_boss.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(TreeBoss animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "textures/entity/tree_boss.png");
    }

    @Override
    public Identifier getAnimationResource(TreeBoss animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "animations/tree_boss.animation.json");
    }
} 