package lt.utopiabosses.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 魔化葵花籽物品
 * 食用后提供少量饱食度，但会造成5秒反胃效果
 */
public class EnchantedSunflowerSeedItem extends Item {
    
    public EnchantedSunflowerSeedItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        
        // 添加反胃效果（5秒 = 100 ticks）
        if (!world.isClient && user instanceof PlayerEntity player) {
            player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.NAUSEA,  // 反胃效果
                100,                    // 持续时间：5秒（100 ticks）
                0,                      // 等级：0（等级1）
                false,                  // 不显示粒子
                true,                   // 显示图标
                true                    // 显示在物品栏
            ));
        }
        
        return result;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("食用效果：")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  反胃 (0:05)")
                .formatted(Formatting.RED));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("加特林弹药")
                .formatted(Formatting.GOLD));
    }
}