package lt.utopiabosses.mixin;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class WeaponRenderingMixin {

    @Inject(method = "getArmPose", at = @At(value = "TAIL"), cancellable = true)
    private static void tryItemPose(AbstractClientPlayerEntity player, Hand hand, CallbackInfoReturnable<BipedEntityModel.ArmPose> ci) {
        var itemstack = player.getStackInHand(hand);
        if (itemstack.getItem().getClass().getName().contains("SunflowerGatlingItem"))
            ci.setReturnValue(BipedEntityModel.ArmPose.CROSSBOW_HOLD);
    }
}
