package lt.utopiabosses.item;

import lt.utopiabosses.client.renderer.item.SunflowerArmorRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SunflowerArmorItem extends ArmorItem implements GeoItem {

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
                if(this.renderer == null)
                    this.renderer = new SunflowerArmorRenderer();

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
        // 头盔动画控制器
        controllers.add(new AnimationController<>(this, "head_controller", 20, state -> {
            // 获取当前穿戴盔甲的实体和装备槽
            Entity entity = state.getData(DataTickets.ENTITY);
            EquipmentSlot slot = state.getData(DataTickets.EQUIPMENT_SLOT);
            
            // 只有在头盔槽位才播放头部动画
            if (slot == EquipmentSlot.HEAD) {
                // 设置头部动画
                RawAnimation animation = RawAnimation.begin()
                        .then("toudonghua", Animation.LoopType.LOOP);
                state.getController().setAnimation(animation);
                
                // 如果是盔甲架或玩家，播放动画
                if (entity instanceof ArmorStandEntity || entity instanceof LivingEntity)
                    return PlayState.CONTINUE;
            }
            
            // 其他情况停止动画
            return PlayState.STOP;
        }));
        
        // 胸甲动画控制器
        controllers.add(new AnimationController<>(this, "chest_controller", 20, state -> {
            // 获取当前穿戴盔甲的实体和装备槽
            Entity entity = state.getData(DataTickets.ENTITY);
            EquipmentSlot slot = state.getData(DataTickets.EQUIPMENT_SLOT);
            
            // 只有在胸甲槽位才播放胸部动画
            if (slot == EquipmentSlot.CHEST) {
                // 设置胸部动画
                RawAnimation animation = RawAnimation.begin()
                        .then("xiongdonghua", Animation.LoopType.LOOP);
                state.getController().setAnimation(animation);
                
                // 如果是盔甲架或玩家，播放动画
                if (entity instanceof ArmorStandEntity || entity instanceof LivingEntity)
                    return PlayState.CONTINUE;
            }
            
            // 其他情况停止动画
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
