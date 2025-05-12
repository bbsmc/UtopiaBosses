package lt.utopiabosses.client.renderer;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.client.model.SunflowerBossModel;
import lt.utopiabosses.entity.SunflowerBossEntity;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.EntityRendererFactory.Context;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SunflowerBossRenderer extends GeoEntityRenderer<SunflowerBossEntity> {
    public static final Map<Integer, Map<String, Vec3d>> BONE_POSITIONS = new HashMap<>();
    private static final List<String> BONES = List.of("liz");


    public SunflowerBossRenderer(Context ctx) {
        super(ctx, new SunflowerBossModel());
        this.shadowRadius = 2.0F;
    }

    public static Vec3d getBonePosition(final Integer id, final String name) {

        Map<String, Vec3d> positions = BONE_POSITIONS.get(id);

        if (positions == null) {
            return Vec3d.ZERO;
        }

        return positions.getOrDefault(name, Vec3d.ZERO);
    }


    @Override
    public void postRender(MatrixStack poseStack, SunflowerBossEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.postRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);


        // Need to store the positions per entity ourselves
        // Since the model is a singleton, and it stores the bones
        BONES.forEach(name -> model.getBone(name).ifPresent(bone -> {
            Vector3d worldPosition = bone.getWorldPosition();
            Vec3d position = new Vec3d(worldPosition.x(), worldPosition.y(), worldPosition.z());
            BONE_POSITIONS.computeIfAbsent(animatable.getId(), key -> new HashMap<>()).put(bone.getName(), position);
        }));

    }

    @Override
    public void render(SunflowerBossEntity entity, float entityYaw, float partialTick, MatrixStack poseStack,
                      VertexConsumerProvider bufferSource, int packedLight) {
        try {
            if (entity == null || poseStack == null || bufferSource == null) {
                Utopiabosses.LOGGER.error("尝试渲染空的向日葵BOSS实体或渲染上下文");
                return;
            }

            
            poseStack.push();
            poseStack.scale(1.5F, 1.5F, 1.5F);
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
            poseStack.pop();
        } catch (Exception e) {
            Utopiabosses.LOGGER.error("向日葵BOSS渲染过程中出现异常:", e);
        }
    }


    @Override
    public RenderLayer getRenderType(SunflowerBossEntity entity, Identifier texture, 
                                   VertexConsumerProvider bufferSource, float partialTick) {
        if (entity == null || texture == null || bufferSource == null) {
            return RenderLayer.getEntityCutout(new Identifier(Utopiabosses.MOD_ID, "textures/entity/sunflower_boss.png"));
        }
        return super.getRenderType(entity, texture, bufferSource, partialTick);
    }
} 