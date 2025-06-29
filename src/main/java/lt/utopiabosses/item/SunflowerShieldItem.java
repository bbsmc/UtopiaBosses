package lt.utopiabosses.item;

import lt.utopiabosses.client.renderer.item.SunflowerShieldRenderer;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SunflowerShieldItem extends ShieldItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public SunflowerShieldItem(Settings settings) {
        super(settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private SunflowerShieldRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new SunflowerShieldRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    @Override
    public boolean isPerspectiveAware() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 0, animationState -> {
            // 简化动画逻辑，修复光影兼容性问题
            // 检查渲染视角，但使用更宽松的条件
            Object renderPerspective = animationState.getData(DataTickets.ITEM_RENDER_PERSPECTIVE);
            
            if (renderPerspective != null) {
                String perspectiveStr = renderPerspective.toString();
                
                // 在手持状态下播放旋转动画（包括第一人称和第三人称）
                // 排除GUI和物品展示框等情况
                if (perspectiveStr.contains("FIRST_PERSON") || 
                    perspectiveStr.contains("THIRD_PERSON") ||
                    perspectiveStr.contains("FIXED") ||  // 添加FIXED支持，提高兼容性
                    perspectiveStr.contains("HAND")) {   // 添加HAND支持，提高兼容性
                    
                    // 播放循环旋转动画
                    animationState.getController().setAnimation(RawAnimation.begin().thenLoop("xuanzhuan"));
                    return PlayState.CONTINUE;
                }
                
                // 只在明确的GUI环境下停止动画
                if (perspectiveStr.contains("GUI") || 
                    perspectiveStr.contains("GROUND") ||
                    perspectiveStr.contains("ITEM_FRAME")) {
                    return PlayState.STOP;
                }
            }
            
            // 默认情况下播放动画，确保光影环境下的兼容性
            // 这可以解决某些光影包改变渲染视角检测的问题
            animationState.getController().setAnimation(RawAnimation.begin().thenLoop("xuanzhuan"));
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        // 可以使用光籽结晶进行修复
        return ingredient.isOf(lt.utopiabosses.registry.ItemRegistry.LIGHT_SEED_CRYSTAL);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK; // 确保使用动作是格挡
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000; // 允许长时间格挡
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        return TypedActionResult.consume(itemStack);
    }

    @Override
    public boolean isDamageable() {
        return true; // 确保盾牌可以损坏，会消耗耐久度
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 当盾牌被用来攻击时，消耗耐久度
        if (!attacker.getWorld().isClient) {
            stack.damage(2, attacker, (entity) -> entity.sendEquipmentBreakStatus(entity.getMainHandStack().equals(stack) ? 
                net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND));
        }
        return true;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        // 确保使用时的行为与原版一致
        if (!world.isClient && user instanceof PlayerEntity) {
            // 格挡时的耐久损失通过SunflowerShieldMixin处理
            // 这里可以添加其他格挡时的特殊效果
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
