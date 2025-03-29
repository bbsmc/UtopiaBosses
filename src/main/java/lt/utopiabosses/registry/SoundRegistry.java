package lt.utopiabosses.registry;

import lt.utopiabosses.Utopiabosses;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundRegistry {
    // 机枪音效
    public static final Identifier ITEM_GATLING_GUN_FIRE_ID = new Identifier(Utopiabosses.MOD_ID, "item.gatling.fire");
    public static final SoundEvent ITEM_GATLING_GUN_FIRE = SoundEvent.of(ITEM_GATLING_GUN_FIRE_ID);
    
    /**
     * 注册所有音效
     */
    public static void registerSounds() {
        Utopiabosses.LOGGER.info("注册音效...");
        
        // 机枪音效
        Registry.register(Registries.SOUND_EVENT, ITEM_GATLING_GUN_FIRE_ID, ITEM_GATLING_GUN_FIRE);
        
        Utopiabosses.LOGGER.info("音效注册完成！");
    }
} 