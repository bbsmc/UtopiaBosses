package lt.utopiabosses.registry;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.SunflowerBossEntity;
import lt.utopiabosses.entity.SunflowerSeedEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class EntityRegistry {
    public static final EntityType<SunflowerBossEntity> SUNFLOWER_BOSS = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(Utopiabosses.MOD_ID, "sunflower_boss"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SunflowerBossEntity::new)
            .dimensions(EntityDimensions.fixed(6.0f, 15.0f))
            .build()
    );
    
    public static final EntityType<SunflowerSeedEntity> SUNFLOWER_SEED = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(Utopiabosses.MOD_ID, "sunflower_seed"),
        FabricEntityTypeBuilder.<SunflowerSeedEntity>create(SpawnGroup.MISC, SunflowerSeedEntity::new)
            .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
            .trackRangeBlocks(64).trackedUpdateRate(10)
            .build()
    );
    
    public static void registerEntities() {
        // 只保留属性注册
        FabricDefaultAttributeRegistry.register(SUNFLOWER_BOSS, SunflowerBossEntity.createAttributes());
    }
    
    public static void registerEntityAttributes() {
        // FabricDefaultAttributeRegistry.register(SUNFLOWER_BOSS, SunflowerBossEntity.createAttributes());
    }
} 