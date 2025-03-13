package lt.utopiabosses.client.renderer.item;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.SunflowerShovelItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SunflowerSwordRenderer extends GeoItemRenderer<SunflowerShovelItem> {
    public SunflowerSwordRenderer() {

        super(new DefaultedItemGeoModel<>(new Identifier(Utopiabosses.MOD_ID, "sunflower_sword")));
    }
}
