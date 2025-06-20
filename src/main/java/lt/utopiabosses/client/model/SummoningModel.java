package lt.utopiabosses.client.model;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.SummoningEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class SummoningModel extends DefaultedEntityGeoModel<SummoningEntity> {

    public SummoningModel() {
        super(new Identifier(Utopiabosses.MOD_ID, "summoning"));
    }
    @Override
    public Identifier getModelResource(SummoningEntity object) {
        return new Identifier(Utopiabosses.MOD_ID, "geo/summoning.geo.json");
    }

    @Override
    public Identifier getTextureResource(SummoningEntity object) {
        return new Identifier(Utopiabosses.MOD_ID, "textures/entity/summoning.png");
    }

    @Override
    public Identifier getAnimationResource(SummoningEntity animatable) {
        return new Identifier(Utopiabosses.MOD_ID, "animations/summoning.animation.json");
    }
} 