package lt.utopiabosses.registry;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.block.entity.NatureAltarBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Collections;

public class BlockEntityRegistry {
    // 使用volatile确保线程可见性
    public static volatile BlockEntityType<NatureAltarBlockEntity> NATURE_ALTAR_BLOCK_ENTITY;
    
    public static void registerBlockEntities() {
        Utopiabosses.LOGGER.info("注册方块实体...");
        
        try {
            // 确保BlockRegistry.NATURE_ALTAR已经被正确初始化
            if (BlockRegistry.NATURE_ALTAR != null) {
                NATURE_ALTAR_BLOCK_ENTITY = Registry.register(
                        Registries.BLOCK_ENTITY_TYPE,
                        new Identifier(Utopiabosses.MOD_ID, "nature_altar"),
                        BlockEntityType.Builder.create(
                                NatureAltarBlockEntity::new, 
                                BlockRegistry.NATURE_ALTAR).build(null)
                );
                Utopiabosses.LOGGER.info("已注册方块实体：NATURE_ALTAR_BLOCK_ENTITY");
            } else {
                Utopiabosses.LOGGER.error("NATURE_ALTAR为null，无法注册方块实体！");
            }
        } catch (Exception e) {
            Utopiabosses.LOGGER.error("注册方块实体时出错", e);
        }
    }
}
