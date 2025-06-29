package lt.utopiabosses.item;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public class SunflowerToolMaterial implements ToolMaterial {
    public static final SunflowerToolMaterial INSTANCE = new SunflowerToolMaterial();

    @Override
    public int getDurability() {
        return 1561; // 钻石工具的耐久度
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 8.0f; // 钻石工具的挖掘速度
    }

    @Override
    public float getAttackDamage() {
        return 3.0f; // 基础攻击伤害（会根据具体工具调整）
    }

    @Override
    public int getMiningLevel() {
        return 3; // 钻石工具的挖掘等级
    }

    @Override
    public int getEnchantability() {
        return 10; // 钻石工具的附魔能力
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(lt.utopiabosses.registry.ItemRegistry.LIGHT_SEED_CRYSTAL);
    }
} 