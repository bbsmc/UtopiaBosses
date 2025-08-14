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


    public static final TreeArmorItem TREE_HELMET = registerItem("tree_helmet", new TreeArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, new FabricItemSettings()));
    public static final TreeArmorItem TREE_CHESTPLATE = registerItem("tree_chestplate", new TreeArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, new FabricItemSettings()));
    public static final TreeArmorItem TREE_LEGGINGS = registerItem("tree_leggings", new TreeArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.LEGGINGS, new FabricItemSettings()));
    public static final TreeArmorItem TREE_BOOTS = registerItem("tree_boots", new TreeArmorItem(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS, new FabricItemSettings()));



    // 铲子
    public static final SunflowerShovelItem SUNFLOWER_SHOVEL = registerItem("sunflower_shovel", new SunflowerShovelItem(new FabricItemSettings().fireproof()));
    // 斧子
    public static final SunflowerAxeItem SUNFLOWER_AXE = registerItem("sunflower_axe", new SunflowerAxeItem(new FabricItemSettings().fireproof()));
    // 加特林
    public static final SunflowerGatlingItem SUNFLOWER_GATLING = registerItem("sunflower_gatling", 
        new SunflowerGatlingItem(new FabricItemSettings()
            .maxDamage(1561)
            .fireproof()));
    // 锄子
    public static final SunflowerHoeItem SUNFLOWER_HOE = registerItem("sunflower_hoe", new SunflowerHoeItem(new FabricItemSettings().fireproof()));
    // 镐子
    public static final SunflowerPickaxeItem SUNFLOWER_PICKAXE = registerItem("sunflower_pickaxe", new SunflowerPickaxeItem(new FabricItemSettings().fireproof()));
    // 剑
    public static final SunflowerSwordItem SUNFLOWER_SWORD = registerItem("sunflower_sword", new SunflowerSwordItem(new FabricItemSettings().maxDamage(1600).fireproof()));
    // 盾牌
    public static final SunflowerShieldItem SUNFLOWER_SHIELD = registerItem("sunflower_shield", new SunflowerShieldItem(new FabricItemSettings().maxDamage(674).fireproof()));
    // 树灵之杖
    public static final TreeSpiritStaffItem TREE_SPIRIT_STAFF = registerItem("tree_spirit_staff", 
        new TreeSpiritStaffItem(new FabricItemSettings().maxDamage(2000)));

    // 自然祭坛物品
    public static final BlockItem NATURE_ALTAR = registerItem("nature_altar",
            new BlockItem(BlockRegistry.NATURE_ALTAR, new FabricItemSettings()));

    public static final BlockItem SUNFLOWER_ALTAR = registerItem("sunflower_altar",
            new BlockItem(BlockRegistry.SUNFLOWER_ALTAR, new FabricItemSettings()));

    // 太阳
    public static final BlockItem SUN_ITEM = registerItem("sun",
            new BlockItem(BlockRegistry.SUN, new FabricItemSettings()));

    // 花盆
    public static final BlockItem FLOWER_POT = registerItem("flowerpot",
            new BlockItem(BlockRegistry.FLOWER_POT, new FabricItemSettings()));

    // 生物蛋 - 使用自定义贴图
    public static final Item SUNFLOWER_BOSS_SPAWN_EGG = registerItem("sunflower_boss_spawn_egg",
            new CustomSpawnEggItem(EntityRegistry.SUNFLOWER_BOSS, new FabricItemSettings()));
    
    public static final Item TREE_BOSS_SPAWN_EGG = registerItem("tree_boss_spawn_egg",
            new CustomSpawnEggItem(EntityRegistry.TREE_BOSS, new FabricItemSettings()));
    
    public static final Item PLANT_SPIRIT_SPAWN_EGG = registerItem("plant_spirit_spawn_egg",
            new CustomSpawnEggItem(EntityRegistry.PLANT_SPIRIT, new FabricItemSettings()));
    
    public static final Item LITTLE_TREE_MAN_SPAWN_EGG = registerItem("little_tree_man_spawn_egg",
            new CustomSpawnEggItem(EntityRegistry.LITTLE_TREE_MAN, new FabricItemSettings()));

    public static <I extends Item> I registerItem(String name, I item) {
        return Registry.register(Registries.ITEM, new Identifier(Utopiabosses.MOD_ID, name), item);
    }

    // 添加新物品
    // 自然精华
    public static final Item NATURAL_ESSENCE = registerItem("natural_essence", 
            new Item(new FabricItemSettings()));
    // 向日葵自然精华
    public static final Item SUNFLOWER_NATURAL_ESSENCE = registerItem("sunflower_natural_essence",
            new Item(new FabricItemSettings()));
    // 光籽结晶  ->   击败向日葵掉落
    public static final Item LIGHT_SEED_CRYSTAL = registerItem("light_seed_crystal", 
            new Item(new FabricItemSettings()));

    // 树灵结晶  ->   击败树灵boss掉落，掉落数量为6-9个
    public static final Item TREE_SPIRIT_CRYSTAL = registerItem("tree_spirit_crystal",
            new Item(new FabricItemSettings()));


    // 树灵精华  ->   由8×任意树苗围绕一个自然精华合成
    public static final Item TREE_SPIRIT_ESSENCE = registerItem("tree_spirit_essence",
            new Item(new FabricItemSettings()));





    // 魔化葵花籽 - 食用后加少量饱食度，但会反胃5秒
    public static final Item ENCHANTED_SUNFLOWER_SEED = registerItem("enchanted_sunflower_seed", 
            new lt.utopiabosses.item.EnchantedSunflowerSeedItem(new FabricItemSettings()
                .food(new FoodComponent.Builder()
                    .hunger(1)                    // 饥饿值：1点（半格饥饿条）
                    .saturationModifier(0.2f)     // 饱食度：很低
                    .alwaysEdible()              // 可以在饱腹时食用
                    .snack()                     // 快速食用（像曲奇那样）
                    .build())));
    

    
    public static void registerItems() {
        // 初始化物品组(ItemGroup)
        Registry.register(Registries.ITEM_GROUP, UTOPIA_GROUP_ID, FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.utopiabosses.item_group"))
                .icon(() -> new ItemStack(LIGHT_SEED_CRYSTAL))
                .build());
        
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
            content.add(TREE_SPIRIT_STAFF);
            content.add(NATURE_ALTAR);
            content.add(SUNFLOWER_ALTAR);
            content.add(FLOWER_POT);
            content.add(NATURAL_ESSENCE);
            content.add(LIGHT_SEED_CRYSTAL);
            content.add(ENCHANTED_SUNFLOWER_SEED);
            content.add(SUNFLOWER_NATURAL_ESSENCE);


            // TREE_SPIRIT_CRYSTAL   树灵结晶
            content.add(TREE_SPIRIT_CRYSTAL);

            // TREE_SPIRIT_ESSENCE      树灵精华
            content.add(TREE_SPIRIT_ESSENCE);

            // 生物蛋
            content.add(SUNFLOWER_BOSS_SPAWN_EGG);
            content.add(TREE_BOSS_SPAWN_EGG);
            content.add(PLANT_SPIRIT_SPAWN_EGG);
            content.add(LITTLE_TREE_MAN_SPAWN_EGG);

            content.add(TREE_HELMET);
            content.add(TREE_CHESTPLATE);
            content.add(TREE_LEGGINGS);
            content.add(TREE_BOOTS);
        });
    }
}