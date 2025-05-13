package lt.utopiabosses.registry;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.*;
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
    
    public static final EntityType<TreeBoss> TREE_BOSS = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(Utopiabosses.MOD_ID, "tree_boss"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, TreeBoss::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.2f))
            .build()
    );
    
    public static final EntityType<LittleTreeMan> LITTLE_TREE_MAN = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(Utopiabosses.MOD_ID, "little_tree_man"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, LittleTreeMan::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.2f))
            .build()
    );

    public static final EntityType<PlantSpirit> PLANT_SPIRIT = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(Utopiabosses.MOD_ID, "plant_spirit"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, PlantSpirit::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.8f))
            .build()
    );

    public static final EntityType<SummoningEntity> SUMMONING = Registry.register(
        Registries.ENTITY_TYPE,
        new Identifier(Utopiabosses.MOD_ID, "summoning"),
        FabricEntityTypeBuilder.<SummoningEntity>create(SpawnGroup.MISC, SummoningEntity::new)
            .dimensions(EntityDimensions.fixed(6.0F, 6.0F))
            .trackRangeBlocks(64).trackedUpdateRate(1)
            .build()
    );
    
    public static void registerEntities() {
        // 只保留属性注册
        FabricDefaultAttributeRegistry.register(SUNFLOWER_BOSS, SunflowerBossEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TREE_BOSS, TreeBoss.createAttributes());
        FabricDefaultAttributeRegistry.register(LITTLE_TREE_MAN, LittleTreeMan.createAttributes());
        FabricDefaultAttributeRegistry.register(PLANT_SPIRIT, PlantSpirit.createAttributes());
    }
    
    public static void registerEntityAttributes() {
        // FabricDefaultAttributeRegistry.register(SUNFLOWER_BOSS, SunflowerBossEntity.createAttributes());
    }
} 