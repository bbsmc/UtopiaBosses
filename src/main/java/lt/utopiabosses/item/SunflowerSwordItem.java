package lt.utopiabosses.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import lt.utopiabosses.client.renderer.item.SunflowerSwordRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.util.math.BlockPos;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SunflowerSwordItem extends SwordItem implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    private final Random random = new Random();

    public SunflowerSwordItem(Settings settings) {
        // 使用钻石材料作为基础，攻击力3+3=6，总计7点伤害
        super(ToolMaterials.DIAMOND, 3, -2.4F, settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private SunflowerSwordRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new SunflowerSwordRenderer();

                return this.renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }


    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 调用父类方法处理基本攻击逻辑
        boolean result = super.postHit(stack, target, attacker);

        // 20%几率触发阳光冲击波
        if (random.nextFloat() < 0.2f) {
            World world = attacker.getWorld();

            // 播放阳光冲击波音效
            world.playSound(
                    null,
                    attacker.getX(),
                    attacker.getY(),
                    attacker.getZ(),
                    SoundEvents.ENTITY_BLAZE_SHOOT,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );

            // 生成阳光冲击波粒子效果和伤害
            if (!world.isClient) {
                // 创建阳光冲击波效果
                createSunbeamWave((ServerWorld)world, attacker.getPos(), 10.0);

                // 对范围内的敌人造成伤害
                Box damageBox = attacker.getBoundingBox().expand(5.0); // 5格范围
                List<LivingEntity> targets = world.getNonSpectatingEntities(LivingEntity.class, damageBox);

                for (LivingEntity entity : targets) {
                    // 排除攻击者自己和已死亡的实体
                    if (entity != attacker && entity.isAlive()) {
                        // 计算距离，用于确定伤害衰减
                        double distance = entity.squaredDistanceTo(attacker);
                        double maxDistance = 25.0; // 5*5，最大距离的平方

                        if (distance <= maxDistance) {
                            // 伤害随距离衰减，最大10点伤害
                            float damage = 10.0f * (1.0f - (float)(distance / maxDistance));
                            entity.damage(entity.getDamageSources().indirectMagic(attacker, attacker), damage);

                            // 施加短暂的发光效果，让敌人更容易被看见
                            entity.setGlowing(true);

                            // 点燃目标，造成阳光灼烧效果
                            entity.setOnFireFor(3); // 3秒着火时间
                        }
                    }
                }
            }
        }

        return result;
    }

    // 创建阳光冲击波粒子效果
    private void createSunbeamWave(ServerWorld world, Vec3d center, double radius) {
        // 创建圆形阳光波粒子
        for (int i = 0; i < 360; i += 5) { // 每5度一个粒子
            double angle = Math.toRadians(i);
            for (double r = 0; r < radius; r += 0.5) {
                double x = center.x + Math.cos(angle) * r;
                double z = center.z + Math.sin(angle) * r;

                // 生成不同高度的粒子，创造3D效果
                for (double y = 0; y < 2; y += 0.5) {
                    world.spawnParticles(
                            ParticleTypes.END_ROD,
                            x, center.y + y, z,
                            1, // 数量
                            0.02, 0.02, 0.02, // 随机扩散
                            0.01 // 速度
                    );
                }

                // 添加一些火焰粒子增强视觉效果
                if (random.nextFloat() < 0.2) {
                    world.spawnParticles(
                            ParticleTypes.FLAME,
                            x, center.y + random.nextFloat() * 2, z,
                            1, // 数量
                            0.05, 0.05, 0.05, // 随机扩散
                            0.02 // 速度
                    );
                }
            }
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        
        // 添加空行分隔
        tooltip.add(Text.empty());
        
        // 添加武器信息标题
        tooltip.add(Text.literal("§6═══ 向日葵剑 ═══").formatted(Formatting.GOLD));
        
        // 添加攻击属性描述
        tooltip.add(Text.literal("§7攻击力: §c7 §7(+6)").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7攻击速度: §e1.6").formatted(Formatting.GRAY));
        
        // 添加空行分隔
        tooltip.add(Text.empty());
        
        // 添加特殊技能标题
        tooltip.add(Text.literal("§e⚡ 阳光冲击波").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("§7攻击时有 §620% §7几率触发").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("§7效果:").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  §8• §7对 §c5格 §7范围内敌人造成 §c最多10点 §7伤害").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  §8• §7施加 §6发光 §7效果").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  §8• §7施加 §c阳光灼烧 §7效果 (3秒)").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("  §8• §7伤害随距离衰减").formatted(Formatting.GRAY));
        
        // 添加空行分隔
        tooltip.add(Text.empty());
        
        // 添加修复信息
        tooltip.add(Text.literal("§9🔧 修复材料: §b光籽结晶").formatted(Formatting.BLUE));
        
        // 添加装饰性分隔线
        tooltip.add(Text.literal("§6═══════════════").formatted(Formatting.GOLD));
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        // 可以使用光籽结晶进行修复
        return ingredient.isOf(lt.utopiabosses.registry.ItemRegistry.LIGHT_SEED_CRYSTAL) || super.canRepair(stack, ingredient);
    }
    
    @Override
    public boolean canMine(BlockState state, net.minecraft.world.World world, BlockPos pos, PlayerEntity miner) {
        // 剑不应该用来挖掘方块（除了蜘蛛网等特殊方块）
        return !miner.isCreative();
    }
    
    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        // 剑的挖掘速度很慢（除了某些特定方块）
        if (state.isOf(net.minecraft.block.Blocks.COBWEB)) {
            return 15.0F;
        }
        // 对植物类方块稍快
        if (state.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) {
            return 1.5F;
        }
        return super.getMiningSpeedMultiplier(stack, state);
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
