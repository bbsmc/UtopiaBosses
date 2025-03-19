package lt.utopiabosses.client.renderer.item;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.SunflowerGatlingItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SunflowerGatlingRenderer extends GeoItemRenderer<SunflowerGatlingItem> {
    public SunflowerGatlingRenderer() {
        super(new DefaultedItemGeoModel<>(new Identifier(Utopiabosses.MOD_ID, "sunflower_gatling")));
    }
}
