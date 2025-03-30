package lt.utopiabosses.client.renderer.item;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.TreeSpiritStaffItem;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class TreeSpiritStaffRenderer extends GeoItemRenderer<TreeSpiritStaffItem> {
    public TreeSpiritStaffRenderer() {
        super(new DefaultedItemGeoModel<>(new Identifier(Utopiabosses.MOD_ID, "tree_spirit_staff")));
    }
} 