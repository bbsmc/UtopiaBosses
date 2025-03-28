package lt.utopiabosses;

import lt.utopiabosses.event.CropHarvestHandler;
import lt.utopiabosses.network.NetworkHandler;
import lt.utopiabosses.registry.BlockEntityRegistry;
import lt.utopiabosses.registry.BlockRegistry;
import lt.utopiabosses.registry.EntityRegistry;
import lt.utopiabosses.registry.ItemRegistry;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

public class Utopiabosses implements ModInitializer {
    public static final String MOD_ID = "utopiabosses";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("初始化向日葵BOSS模组...");
        
        // 初始化GeckoLib（用于生物模型和动画）
        GeckoLib.initialize();
        
        // 调整注册顺序
        // 1. 先注册方块
        BlockRegistry.registerBlocks();
        
        // 2. 然后注册方块实体
        BlockEntityRegistry.registerBlockEntities();
        
        // 3. 再注册实体
        EntityRegistry.registerEntities();
        EntityRegistry.registerEntityAttributes();
        
        // 4. 最后注册物品
        ItemRegistry.registerItems();
        
        // 注册网络处理器
        NetworkHandler.registerNetworkHandlers();
        
        // 注册事件处理器
        CropHarvestHandler.register();
        
        LOGGER.info("UtopiaBosses初始化完成");
    }
}
