package lt.utopiabosses.client.renderer.item;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.SunflowerArmorItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class SunflowerArmorRenderer extends GeoArmorRenderer<SunflowerArmorItem> {
    
    private static final Identifier MODEL_ID = new Identifier(Utopiabosses.MOD_ID, "armor/sunflower_armor");
    
    public SunflowerArmorRenderer() {
        super(new DefaultedItemGeoModel<>(MODEL_ID));
        // addRenderLayer(new AutoGlowingGeoLayer<>(this));
        Utopiabosses.LOGGER.info("ArmorRenderer初始化成功，使用模型: {}", MODEL_ID);
    }

}
