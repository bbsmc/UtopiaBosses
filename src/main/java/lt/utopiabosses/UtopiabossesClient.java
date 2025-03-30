package lt.utopiabosses;

import lt.utopiabosses.client.renderer.SunflowerBossRenderer;
import lt.utopiabosses.client.renderer.SunflowerSeedRenderer;
import lt.utopiabosses.client.renderer.TreeBossRenderer;
import lt.utopiabosses.client.renderer.LittleTreeManRenderer;
import lt.utopiabosses.client.renderer.block.NatureAltarBlockEntityRenderer;
import lt.utopiabosses.registry.EntityRegistry;
import lt.utopiabosses.registry.BlockEntityRegistry;
import lt.utopiabosses.registry.BlockRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

/**
 * 客户端初始化类
 */
@Environment(EnvType.CLIENT)
public class UtopiabossesClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("utopiabosses-client");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("初始化Utopiabosses客户端...");
        try {
            // 初始化GeckoLib（仅用于生物模型和动画）
            GeckoLib.initialize();
            
            // 设置方块渲染层
            setupRenderLayers();
            
            // 注册实体渲染器
            registerEntityRenderers();
            
            // 注册方块实体渲染器
            registerBlockEntityRenderers();
            
            LOGGER.info("Utopiabosses客户端初始化完成！");
        } catch (Exception e) {
            LOGGER.error("初始化Utopiabosses客户端时出错:", e);
        }
    }
    
    /**
     * 设置方块渲染层
     */
    private void setupRenderLayers() {
        try {
            LOGGER.info("设置方块渲染层...");
            // 设置NatureAltarBlock为透明渲染层
            BlockRenderLayerMap.INSTANCE.putBlock(BlockRegistry.NATURE_ALTAR, RenderLayer.getCutout());
            LOGGER.info("已设置方块渲染层");
        } catch (Exception e) {
            LOGGER.error("设置方块渲染层时出错:", e);
        }
    }
    
    /**
     * 注册方块实体渲染器
     */
    private void registerBlockEntityRenderers() {
        try {
            LOGGER.info("注册方块实体渲染器...");
            // 使用延迟初始化，确保注册表同步已完成
            if (BlockEntityRegistry.NATURE_ALTAR_BLOCK_ENTITY != null) {
                BlockEntityRendererRegistry.register(
                        BlockEntityRegistry.NATURE_ALTAR_BLOCK_ENTITY, 
                        NatureAltarBlockEntityRenderer::new
                );
                LOGGER.info("已注册自然祭坛渲染器");
            } else {
                LOGGER.warn("NATURE_ALTAR_BLOCK_ENTITY为null，跳过渲染器注册");
            }
        } catch (Exception e) {
            LOGGER.error("注册方块实体渲染器时出错:", e);
        }
    }
    
    /**
     * 注册实体渲染器
     */
    private void registerEntityRenderers() {
        try {
            // 注册向日葵BOSS渲染器
            LOGGER.info("注册向日葵BOSS渲染器...");
            EntityRendererRegistry.register(EntityRegistry.SUNFLOWER_BOSS, SunflowerBossRenderer::new);
            
            // 注册向日葵种子渲染器
            LOGGER.info("注册向日葵种子渲染器...");
            EntityRendererRegistry.register(EntityRegistry.SUNFLOWER_SEED, SunflowerSeedRenderer::new);
            
            // 注册树木BOSS渲染器
            LOGGER.info("注册树木BOSS渲染器...");
            EntityRendererRegistry.register(EntityRegistry.TREE_BOSS, TreeBossRenderer::new);
            
            // 注册小树人渲染器
            LOGGER.info("注册小树人渲染器...");
            EntityRendererRegistry.register(EntityRegistry.LITTLE_TREE_MAN, LittleTreeManRenderer::new);
        } catch (Exception e) {
            LOGGER.error("注册实体渲染器时出错:", e);
        }
    }
} 