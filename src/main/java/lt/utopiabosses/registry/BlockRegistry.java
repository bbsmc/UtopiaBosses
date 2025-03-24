package lt.utopiabosses.registry;


import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.block.NatureAltarBlock;
import lt.utopiabosses.block.SunBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class BlockRegistry {


    // 自然祭坛方块
    public static final Block NATURE_ALTAR = registerBlock("nature_altar", 
            new NatureAltarBlock(Block.Settings.create().strength(3.0f).requiresTool().nonOpaque()));

    // 自然祭坛方块
    public static final Block SUN = registerBlock("sun",
            new SunBlock(Block.Settings.create().strength(3.0f).requiresTool().nonOpaque()));



    public static <B extends Block> B registerBlock(String name, B block) {
        return Registry.register(Registries.BLOCK, new Identifier(Utopiabosses.MOD_ID, name), block);
    }
    
    // 添加显式注册方法
    public static void registerBlocks() {
        Utopiabosses.LOGGER.info("注册方块...");
        // NATURE_ALTAR 已经在静态初始化中注册，但我们确保它已被正确引用
        Utopiabosses.LOGGER.info("已注册：" + NATURE_ALTAR.getClass().getSimpleName());
    }
}
