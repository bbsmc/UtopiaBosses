package lt.utopiabosses.client.model;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.LittleTreeMan;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class LittleTreeManModel extends DefaultedEntityGeoModel<LittleTreeMan> {
    
    public LittleTreeManModel() {
        super(new Identifier(Utopiabosses.MOD_ID, "little_tree_man"));
        
        // 输出资源路径用于调试
        System.out.println("LittleTreeMan 模型预期路径: assets/utopiabosses/geo/little_tree_man.geo.json");
        System.out.println("LittleTreeMan 纹理预期路径: assets/utopiabosses/textures/entity/little_tree_man.png");
        System.out.println("LittleTreeMan 动画预期路径: assets/utopiabosses/animations/little_tree_man.animation.json");
    }
    
    @Override
    public Identifier getModelResource(LittleTreeMan animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "geo/little_tree_man.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(LittleTreeMan animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "textures/entity/little_tree_man.png");
    }

    @Override
    public Identifier getAnimationResource(LittleTreeMan animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "animations/little_tree_man.animation.json");
    }
} 