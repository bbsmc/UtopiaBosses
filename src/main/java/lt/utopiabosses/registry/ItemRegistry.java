package lt.utopiabosses.registry;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.*;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public class ItemRegistry {
    // 创建自定义物品组ID
    private static final Identifier UTOPIA_GROUP_ID = new Identifier(Utopiabosses.MOD_ID, "item_group");
    
    // 定义物品组
    private static final RegistryKey<ItemGroup> UTOPIA_GROUP = RegistryKey.of(
            Registries.ITEM_GROUP.getKey(), UTOPIA_GROUP_ID);

    public static final SunflowerArmorItem SUNFLOWER_HELMET = registerItem("sunflower_helmet", new SunflowerArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, new FabricItemSettings()));
    public static final SunflowerArmorItem SUNFLOWER_CHESTPLATE = registerItem("sunflower_chestplate", new SunflowerArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new FabricItemSettings()));
    public static final SunflowerArmorItem SUNFLOWER_LEGGINGS = registerItem("sunflower_leggings", new SunflowerArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS, new FabricItemSettings()));
    public static final SunflowerArmorItem SUNFLOWER_BOOTS = registerItem("sunflower_boots", new SunflowerArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS, new FabricItemSettings()));

    // 铲子
    public static final SunflowerShovelItem SUNFLOWER_SHOVEL = registerItem("sunflower_shovel", new SunflowerShovelItem(new FabricItemSettings().maxDamage(1561).fireproof()));
    // 斧子
    public static final SunflowerAxeItem SUNFLOWER_AXE = registerItem("sunflower_axe", new SunflowerAxeItem(new FabricItemSettings().maxDamage(1561).fireproof()));
    // 加特林
    public static final SunflowerGatlingItem SUNFLOWER_GATLING = registerItem("sunflower_gatling", new SunflowerGatlingItem(new FabricItemSettings().maxDamage(1561).fireproof()));
    // 锄子
    public static final SunflowerHoeItem SUNFLOWER_HOE = registerItem("sunflower_hoe", new SunflowerHoeItem(new FabricItemSettings().maxDamage(1561).fireproof()));
    // 镐子
    public static final SunflowerPickaxeItem SUNFLOWER_PICKAXE = registerItem("sunflower_pickaxe", new SunflowerPickaxeItem(new FabricItemSettings().maxDamage(1561).fireproof()));
    // 剑
    public static final SunflowerSwordItem SUNFLOWER_SWORD = registerItem("sunflower_sword", new SunflowerSwordItem(new FabricItemSettings().maxDamage(1600).fireproof()));
    // 盾牌
    public static final SunflowerShieldItem SUNFLOWER_SHIELD = registerItem("sunflower_shield", new SunflowerShieldItem(new FabricItemSettings().maxDamage(336).fireproof()));


    // 自然祭坛物品
    public static final BlockItem NATURE_ALTAR = registerItem("nature_altar",
            new BlockItem(BlockRegistry.NATURE_ALTAR, new FabricItemSettings()));

    public static <I extends Item> I registerItem(String name, I item) {
        return Registry.register(Registries.ITEM, new Identifier(Utopiabosses.MOD_ID, name), item);
    }

//    public static final Item SUNFLOWER_BOSS_SPAWN_EGG = new SpawnEggItem(
//            EntityRegistry.SUNFLOWER_BOSS, 0xF7DE3F, 0x198122, // 主体黄色，细节绿色
//            new FabricItemSettings()
//    );

    // 添加新物品
    // 自然精华
    public static final Item NATURAL_ESSENCE = registerItem("natural_essence", 
            new Item(new FabricItemSettings()));
    
    // 光籽结晶
    public static final Item LIGHT_SEED_CRYSTAL = registerItem("light_seed_crystal", 
            new Item(new FabricItemSettings()));
    
    // 葵花籽
    public static final Item SUNFLOWER_SEED = registerItem("sunflower_seed", 
            new Item(new FabricItemSettings().food(new FoodComponent.Builder().hunger(1).saturationModifier(0.1f).build())));
    
    // 熟葵花籽
    public static final Item COOKED_SUNFLOWER_SEED = registerItem("cooked_sunflower_seed", 
            new Item(new FabricItemSettings().food(new FoodComponent.Builder().hunger(4).saturationModifier(2.0f).build())));
    
    // 向日葵自然精华
    public static final Item SUNFLOWER_NATURAL_ESSENCE = registerItem("sunflower_natural_essence",
            new Item(new FabricItemSettings()));
    
    public static void registerItems() {
        // 初始化物品组(ItemGroup)
        Registry.register(Registries.ITEM_GROUP, UTOPIA_GROUP_ID, FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.utopiabosses.item_group"))
                .icon(() -> new ItemStack(LIGHT_SEED_CRYSTAL))
                .build());
        
//        // 注册向日葵BOSS生成蛋
//        Registry.register(Registries.ITEM, new Identifier(Utopiabosses.MOD_ID, "sunflower_boss_spawn_egg"),
//                SUNFLOWER_BOSS_SPAWN_EGG);
        
        // 将所有物品添加到物品组中
        ItemGroupEvents.modifyEntriesEvent(UTOPIA_GROUP).register(content -> {
            content.add(SUNFLOWER_HELMET);
            content.add(SUNFLOWER_CHESTPLATE);
            content.add(SUNFLOWER_LEGGINGS);
            content.add(SUNFLOWER_BOOTS);
            content.add(SUNFLOWER_SHOVEL);
            content.add(SUNFLOWER_AXE);
            content.add(SUNFLOWER_HOE);
            content.add(SUNFLOWER_PICKAXE);
            content.add(SUNFLOWER_GATLING);
            content.add(SUNFLOWER_SWORD);
            content.add(SUNFLOWER_SHIELD);
            content.add(NATURE_ALTAR);
            content.add(NATURAL_ESSENCE);
            content.add(LIGHT_SEED_CRYSTAL);
            content.add(SUNFLOWER_SEED);
            content.add(COOKED_SUNFLOWER_SEED);
            content.add(SUNFLOWER_NATURAL_ESSENCE);
//            content.add(SUNFLOWER_BOSS_SPAWN_EGG);
        });
    }
}