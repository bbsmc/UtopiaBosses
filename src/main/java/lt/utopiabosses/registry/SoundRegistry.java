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
    public static final Identifier ENTITY_SUNFLOWER_RIGHT_ATTACK_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.sunflower_right_attack");
    public static final SoundEvent ENTITY_SUNFLOWER_RIGHT_ATTACK = SoundEvent.of(ENTITY_SUNFLOWER_RIGHT_ATTACK_ID);

    public static final Identifier ENTITY_SUNFLOWER_LEFT_ATTACK_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.sunflower_left_attack");
    public static final SoundEvent ENTITY_SUNFLOWER_LEFT_ATTACK = SoundEvent.of(ENTITY_SUNFLOWER_LEFT_ATTACK_ID);


    
    public static final Identifier ENTITY_SUNFLOWER_FLOWER_LADE_STORM_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.flower_lade_storm");
    public static final SoundEvent ENTITY_SUNFLOWER_FLOWER_LADE_STORM = SoundEvent.of(ENTITY_SUNFLOWER_FLOWER_LADE_STORM_ID);

    public static final Identifier ENTITY_SUNFLOWER_LASER_CANNON_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.laser_cannon");
    public static final SoundEvent ENTITY_SUNFLOWER_LASER_CANNON = SoundEvent.of(ENTITY_SUNFLOWER_LASER_CANNON_ID);
    
    public static final Identifier ENTITY_SUNFLOWER_SHOOT_ID = new Identifier(Utopiabosses.MOD_ID, "entity.sunflower.shoot");
    public static final SoundEvent ENTITY_SUNFLOWER_SHOOT = SoundEvent.of(ENTITY_SUNFLOWER_SHOOT_ID);






    // 树BOSS

    public static final Identifier ENTITY_TREEBOSS_STOMPING_FEET_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.skill_stomping_feet");
    public static final SoundEvent ENTITY_TREEBOSS_STOMPING_FEET = SoundEvent.of(ENTITY_TREEBOSS_STOMPING_FEET_ID);


    // skill_grab_with_the_left_hand
    public static final Identifier ENTITY_TREEBOSS_GRAB_WITH_THE_LEFT_HAND_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.skill_grab_with_the_left_hand");
    public static final SoundEvent ENTITY_TREEBOSS_GRAB_WITH_THE_LEFT_HAND = SoundEvent.of(ENTITY_TREEBOSS_GRAB_WITH_THE_LEFT_HAND_ID);


    // skill_insert_both_arms_into_the_ground_surface
    public static final Identifier ENTITY_TREEBOSS_INSERT_BOTH_ARMS_INTO_THE_GROUND_SURFACE_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.skill_insert_both_arms_into_the_ground_surface");
    public static final SoundEvent ENTITY_TREEBOSS_INSERT_BOTH_ARMS_INTO_THE_GROUND_SURFACE = SoundEvent.of(ENTITY_TREEBOSS_INSERT_BOTH_ARMS_INTO_THE_GROUND_SURFACE_ID);

    // skill_roar_towards_the_sky
    public static final Identifier ENTITY_TREEBOSS_ROAR_TOWARDS_THE_SKY_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.skill_roar_towards_the_sky");
    public static final SoundEvent ENTITY_TREEBOSS_ROAR_TOWARDS_THE_SKY = SoundEvent.of(ENTITY_TREEBOSS_ROAR_TOWARDS_THE_SKY_ID);

    // tree_run
    public static final Identifier ENTITY_TREEBOSS_TREE_RUN_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.tree_run");
    public static final SoundEvent ENTITY_TREEBOSS_TREE_RUN = SoundEvent.of(ENTITY_TREEBOSS_TREE_RUN_ID);
    // tree_run2
    public static final Identifier ENTITY_TREEBOSS_TREE_RUN2_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.tree_run2");
    public static final SoundEvent ENTITY_TREEBOSS_TREE_RUN2 = SoundEvent.of(ENTITY_TREEBOSS_TREE_RUN2_ID);

    // attack_left
    public static final Identifier ENTITY_TREEBOSS_ATTACK_LEFT_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.attack_left");
    public static final SoundEvent ENTITY_TREEBOSS_ATTACK_LEFT = SoundEvent.of(ENTITY_TREEBOSS_ATTACK_LEFT_ID);
    // attack_right
    public static final Identifier ENTITY_TREEBOSS_ATTACK_RIGHT_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.attack_right");
    public static final SoundEvent ENTITY_TREEBOSS_ATTACK_RIGHT = SoundEvent.of(ENTITY_TREEBOSS_ATTACK_RIGHT_ID);
    // die
    public static final Identifier ENTITY_TREEBOSS_DIE_ID = new Identifier(Utopiabosses.MOD_ID, "entity.treeboss.die");
    public static final SoundEvent ENTITY_TREEBOSS_DIE = SoundEvent.of(ENTITY_TREEBOSS_DIE_ID);

    /**
     * 注册所有音效
     */
    public static void registerSounds() {
        Utopiabosses.LOGGER.info("注册音效...");
        
        // 机枪音效
        Registry.register(Registries.SOUND_EVENT, ITEM_GATLING_GUN_FIRE_ID, ITEM_GATLING_GUN_FIRE);
        
        // 向日葵音效
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_RIGHT_ATTACK_ID, ENTITY_SUNFLOWER_RIGHT_ATTACK);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_LEFT_ATTACK_ID, ENTITY_SUNFLOWER_LEFT_ATTACK);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_FLOWER_LADE_STORM_ID, ENTITY_SUNFLOWER_FLOWER_LADE_STORM);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_LASER_CANNON_ID, ENTITY_SUNFLOWER_LASER_CANNON);
        Registry.register(Registries.SOUND_EVENT, ENTITY_SUNFLOWER_SHOOT_ID, ENTITY_SUNFLOWER_SHOOT);

        // 树BOSS音效
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_STOMPING_FEET_ID, ENTITY_TREEBOSS_STOMPING_FEET);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_GRAB_WITH_THE_LEFT_HAND_ID, ENTITY_TREEBOSS_GRAB_WITH_THE_LEFT_HAND);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_INSERT_BOTH_ARMS_INTO_THE_GROUND_SURFACE_ID, ENTITY_TREEBOSS_INSERT_BOTH_ARMS_INTO_THE_GROUND_SURFACE);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_ROAR_TOWARDS_THE_SKY_ID, ENTITY_TREEBOSS_ROAR_TOWARDS_THE_SKY);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_TREE_RUN_ID, ENTITY_TREEBOSS_TREE_RUN);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_TREE_RUN2_ID, ENTITY_TREEBOSS_TREE_RUN2);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_ATTACK_LEFT_ID, ENTITY_TREEBOSS_ATTACK_LEFT);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_ATTACK_RIGHT_ID, ENTITY_TREEBOSS_ATTACK_RIGHT);
        Registry.register(Registries.SOUND_EVENT, ENTITY_TREEBOSS_DIE_ID, ENTITY_TREEBOSS_DIE);
        Utopiabosses.LOGGER.info("音效注册完成！");
    }
} 