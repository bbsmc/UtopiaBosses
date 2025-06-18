package lt.utopiabosses.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("blockedByShield")
    boolean utopiabosses$invokeBlockedByShield(DamageSource damageSource);

    @Invoker("damageShield")
    void utopiabosses$invokeDamageShield(float amount);

    @Invoker("takeShieldHit")
    void utopiabosses$invokeTakeShieldHit(LivingEntity attacker);
} 