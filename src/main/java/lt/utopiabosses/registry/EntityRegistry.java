package lt.utopiabosses.registry;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.entity.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;

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

    // 植物精灵生成的生物群系标签
    public static final TagKey<Biome> PLANT_SPIRIT_SPAWN_BIOMES = TagKey.of(
        RegistryKeys.BIOME,
        new Identifier(Utopiabosses.MOD_ID, "plant_spirit_spawn_biomes")
    );
    
    public static void registerEntities() {
        // 注册实体属性
        FabricDefaultAttributeRegistry.register(SUNFLOWER_BOSS, SunflowerBossEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(TREE_BOSS, TreeBoss.createAttributes());
        FabricDefaultAttributeRegistry.register(LITTLE_TREE_MAN, LittleTreeMan.createAttributes());
        FabricDefaultAttributeRegistry.register(PLANT_SPIRIT, PlantSpirit.createAttributes());

        // 注册植物精灵生成条件
        SpawnRestriction.register(
            PLANT_SPIRIT, 
            SpawnRestriction.Location.ON_GROUND, 
            Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, 
            PlantSpirit::canSpawn
        );

        // 添加植物精灵生成配置
        BiomeModifications.addSpawn(
            BiomeSelectors.tag(PLANT_SPIRIT_SPAWN_BIOMES),
            SpawnGroup.CREATURE,
            PLANT_SPIRIT,
            15, // 权重降低为15
            1,  // 最小群组大小
            2   // 最大群组大小减少到2
        );
    }
    
    public static void registerEntityAttributes() {
        // FabricDefaultAttributeRegistry.register(SUNFLOWER_BOSS, SunflowerBossEntity.createAttributes());
    }
} 