package lt.utopiabosses.client.renderer.item;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.TreeArmorItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class TreeArmorRenderer extends GeoArmorRenderer<TreeArmorItem> {

    private static final Identifier MODEL_ID = new Identifier(Utopiabosses.MOD_ID, "armor/tree_armor");

    public TreeArmorRenderer() {
        super(new DefaultedItemGeoModel<>(MODEL_ID));
        // addRenderLayer(new AutoGlowingGeoLayer<>(this));
        Utopiabosses.LOGGER.info("ArmorRenderer初始化成功，使用模型: {}", MODEL_ID);
    }

}
