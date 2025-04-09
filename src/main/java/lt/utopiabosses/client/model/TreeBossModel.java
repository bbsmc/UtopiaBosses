package lt.utopiabosses.client.model;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.TreeBoss;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class TreeBossModel extends DefaultedEntityGeoModel<TreeBoss> {
    
    public TreeBossModel() {
        super(new Identifier(Utopiabosses.MOD_ID, "tree_boss"),true);
        
        // 输出资源路径用于调试
        System.out.println("TreeBoss 模型预期路径: assets/utopiabosses/geo/tree_boss.geo.json");
        System.out.println("TreeBoss 纹理预期路径: assets/utopiabosses/textures/entity/tree_boss.png");
        System.out.println("TreeBoss 动画预期路径: assets/utopiabosses/animations/tree_boss.animation.json");
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