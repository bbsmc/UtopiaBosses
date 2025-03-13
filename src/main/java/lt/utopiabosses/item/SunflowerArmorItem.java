package lt.utopiabosses.item;

import lt.utopiabosses.client.renderer.item.SunflowerArmorRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SunflowerArmorItem extends ArmorItem implements GeoItem{

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    public SunflowerArmorItem(ArmorMaterial material, Type type, Settings settings) {
        super(material, type, settings);
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private GeoArmorRenderer<?> renderer;
            @Environment(EnvType.CLIENT)
            @Override
            public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
                if(this.renderer == null) // 重要的是我们这样做。如果我们直接在字段中实例化它，可能会导致与某些模组的不兼容。
                    this.renderer = new SunflowerArmorRenderer();

                // 这为当前渲染帧准备我们的GeoArmorRenderer。
                // 这些参数可能为null，因此我们不会进一步处理它们
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);

                return this.renderer;
            }
        });
    }

    @Environment(EnvType.CLIENT)
    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
//        controllers.add(new AnimationController<>(this, 20, state -> {
//            // 应用我们的通用闲置动画。
//            // 是否播放取决于下面的决定。
//            // 这是当前穿戴/持有该物品的实体
//            state.getController().setAnimation(DefaultAnimations.IDLE);
//
//            // 我们只希望盔甲架始终动画，因此我们可以在这里返回
//            Entity entity = state.getData(DataTickets.ENTITY);
//
//            if (entity instanceof ArmorStandEntity)
//                return PlayState.CONTINUE;
//
//            // 对于这个例子，我们只希望在实体穿戴所有盔甲件时播放动画
//            // 收集实体当前穿戴的盔甲件
//            Set<Item> wornArmor = new ObjectOpenHashSet<>();
//
//            for (ItemStack stack : entity.getArmorItems()) {
//                // 如果任何插槽为空，我们可以立即停止
//                if (stack.isEmpty())
//                    return PlayState.STOP;
//
//                wornArmor.add(stack.getItem());
//            }
//
//            // 检查每个件是否与我们的集合匹配
//            boolean isFullSet = wornArmor.containsAll(ObjectArrayList.of(
//                    ItemRegistry.ARMOR_BOOTS,
//                    ItemRegistry.ARMOR_LEGGINGS,
//                    ItemRegistry.ARMOR_CHESTPLATE,
//                    ItemRegistry.ARMOR_HELMET));
//
//            // 如果穿戴了完整套装，则播放动画，否则停止
//            return isFullSet ? PlayState.CONTINUE : PlayState.STOP;
//        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
