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


//    向日葵

    // 近战
    public static final Identifier ENTITY_SUNFLOWER_JIN_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.jin");
    public static final SoundEvent ENTITY_SUNFLOWER_JIN = SoundEvent.of(ENTITY_SUNFLOWER_JIN_ID);


    // 远程
    public static final Identifier ENTITY_SUNFLOWER_YUAN_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.yuan");
    public static final SoundEvent ENTITY_SUNFLOWER_YUAN = SoundEvent.of(ENTITY_SUNFLOWER_YUAN_ID);


    // 技能 - 弹幕
    public static final Identifier ENTITY_SUNFLOWER_DANMU_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.dan_mu");
    public static final SoundEvent ENTITY_SUNFLOWER_DANMU = SoundEvent.of(ENTITY_SUNFLOWER_DANMU_ID);

    // 技能 - 激光-太阳生成
    public static final Identifier ENTITY_SUNFLOWER_SUN_SPAWN_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.sun_chuxian");
    public static final SoundEvent ENTITY_SUNFLOWER_SUN_SPAWN = SoundEvent.of(ENTITY_SUNFLOWER_SUN_SPAWN_ID);


    // 技能 - 激光-太阳回血
    public static final Identifier ENTITY_SUNFLOWER_SUN_HEAL_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.sun_xishou");
    public static final SoundEvent ENTITY_SUNFLOWER_SUN_HEAL = SoundEvent.of(ENTITY_SUNFLOWER_SUN_HEAL_ID);







    /**
     * 注册所有音效
     */
    public static void registerSounds() {
        Utopiabosses.LOGGER.info("注册音效...");
        
        // 机枪音效
        Registry.register(Registries.SOUND_EVENT, ITEM_GATLING_GUN_FIRE_ID, ITEM_GATLING_GUN_FIRE);
        
        // 向日葵音效
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_JIN_ID, ENTITY_SUNFLOWER_JIN);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_YUAN_ID, ENTITY_SUNFLOWER_YUAN);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_DANMU_ID, ENTITY_SUNFLOWER_DANMU);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_SUN_SPAWN_ID, ENTITY_SUNFLOWER_SUN_SPAWN);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_SUN_HEAL_ID, ENTITY_SUNFLOWER_SUN_HEAL);
        
        Utopiabosses.LOGGER.info("音效注册完成！");
    }
} 