package lt.utopiabosses.client.model;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.SunflowerBossEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

public class SunflowerBossModel extends GeoModel<SunflowerBossEntity> {
    // 定义可能的资源路径常量，便于排错
    private static final Identifier MODEL_RESOURCE = new Identifier(Utopiabosses.MOD_ID, "geo/sunflower_boss.geo.json");
    private static final Identifier TEXTURE_RESOURCE = new Identifier(Utopiabosses.MOD_ID, "textures/entity/sunflower_boss.png");
    private static final Identifier ANIMATION_RESOURCE = new Identifier(Utopiabosses.MOD_ID, "animations/sunflower_boss.json");

    public SunflowerBossModel() {
        // 构造函数中输出资源路径，便于排错
        Utopiabosses.LOGGER.info("向日葵BOSS模型路径: {}", MODEL_RESOURCE);
        Utopiabosses.LOGGER.info("向日葵BOSS纹理路径: {}", TEXTURE_RESOURCE);
        Utopiabosses.LOGGER.info("向日葵BOSS动画路径: {}", ANIMATION_RESOURCE);
    }

    @Override
    public Identifier getModelResource(SunflowerBossEntity entity) {
        return MODEL_RESOURCE;
    }

    @Override
    public Identifier getTextureResource(SunflowerBossEntity entity) {
        return TEXTURE_RESOURCE;
    }

    @Override
    public Identifier getAnimationResource(SunflowerBossEntity entity) {
        return ANIMATION_RESOURCE;
    }
} 