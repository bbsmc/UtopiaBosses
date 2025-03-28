package lt.utopiabosses.mixin;

import lt.utopiabosses.item.SunflowerGatlingItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow @Final private MinecraftClient client;
    @Shadow private float equipProgressMainHand;
    @Shadow private float equipProgressOffHand;
    @Shadow private float prevEquipProgressMainHand;
    @Shadow private float prevEquipProgressOffHand;

    // 修改后的方法，更彻底地处理向日葵加特林的手臂动画
    @Inject(method = "updateHeldItems", at = @At("TAIL"))
    private void fixGatlingAnimation(CallbackInfo ci) {
        if (this.client.player != null) {
            ItemStack mainStack = this.client.player.getMainHandStack();
            ItemStack offStack = this.client.player.getOffHandStack();

            // 主手持有加特林时
            if (mainStack.getItem() instanceof SunflowerGatlingItem) {
                // 设置当前和前一帧的进度值都为1.0，确保完全消除动画
                this.equipProgressMainHand = 1.0F;
                this.prevEquipProgressMainHand = 1.0F;
            }

            // 副手持有加特林时
            if (offStack.getItem() instanceof SunflowerGatlingItem) {
                // 设置当前和前一帧的进度值都为1.0，确保完全消除动画
                this.equipProgressOffHand = 1.0F;
                this.prevEquipProgressOffHand = 1.0F;
            }
        }
    }
}