package lt.utopiabosses.mixin;

import lt.utopiabosses.item.SunflowerShieldItem;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class SunflowerShieldMixin {

    @Inject(method = "damage", at = @At("HEAD"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        // 只在服务端处理
        if (entity.getWorld().isClient()) {
            return;
        }

        // 检查实体是否正在使用物品（格挡状态）
        if (!entity.isUsingItem()) {
            return;
        }

        ItemStack activeItem = entity.getActiveItem();
        
        // 检查是否是向日葵盾牌
        if (!(activeItem.getItem() instanceof SunflowerShieldItem)) {
            return;
        }

        // 检查伤害是否被盾牌格挡
        LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
        if (!accessor.utopiabosses$invokeBlockedByShield(source)) {
            return;
        }

        // 对盾牌造成耐久损失
        int damageAmount = 1 + (int) Math.floor(amount);
        activeItem.damage(damageAmount, entity, (livingEntity) -> {
            livingEntity.sendEquipmentBreakStatus(entity.getActiveHand() == net.minecraft.util.Hand.MAIN_HAND ? 
                net.minecraft.entity.EquipmentSlot.MAINHAND : net.minecraft.entity.EquipmentSlot.OFFHAND);
        });

        // 如果是玩家，增加统计数据和播放音效
        if (entity instanceof PlayerEntity player) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.incrementStat(Stats.USED.getOrCreateStat(activeItem.getItem()));
            }
            
            // 播放盾牌格挡音效
            entity.getWorld().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 0.8F + entity.getWorld().random.nextFloat() * 0.4F);
        }
    }
} 