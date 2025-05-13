package lt.utopiabosses.client.model;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.PlantSpirit;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class PlantSpiritModel extends DefaultedEntityGeoModel<PlantSpirit> {
    
    public PlantSpiritModel() {
        super(new Identifier(Utopiabosses.MOD_ID, "plant_spirit"));
    }
    
    @Override
    public Identifier getModelResource(PlantSpirit animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "geo/plant_spirit.geo.json");
    }
    
    @Override
    public Identifier getTextureResource(PlantSpirit animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "textures/entity/plant_spirit.png");
    }

    @Override
    public Identifier getAnimationResource(PlantSpirit animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "animations/plant_spirit.animation.json");
    }
} 