package lt.utopiabosses.client.renderer.item;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.SunflowerShieldItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SunflowerShieldRenderer extends GeoItemRenderer<SunflowerShieldItem> {
    public SunflowerShieldRenderer() {
        super(new DefaultedItemGeoModel<>(new Identifier(Utopiabosses.MOD_ID, "sunflower_shield")));
    }

    @Override
    public void render(ItemStack stack, ModelTransformationMode transformType, MatrixStack poseStack, 
                      VertexConsumerProvider bufferSource, int packedLight, int packedOverlay) {
        
        // 检查是否是第一人称视角
        if (transformType == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND || 
            transformType == ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
            
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.player.isUsingItem() && 
                client.player.getActiveItem() == stack) {
                
                // 格挡状态：移动到胸前
                poseStack.push();
                
                if (transformType == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
                    // 右手格挡：向屏幕中间移动
                    poseStack.translate(-0.3f, 0.1f, 0.1f);
                    // poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10f));
                } else {
                    // 左手格挡：向屏幕中间移动
                    poseStack.translate(0.3f, 0.1f, 0.1f);
                    // poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10f));
                }
                
                super.render(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
                poseStack.pop();
                return;
            }
        }
        
        // 非格挡状态：使用默认渲染
        super.render(stack, transformType, poseStack, bufferSource, packedLight, packedOverlay);
    }
}
