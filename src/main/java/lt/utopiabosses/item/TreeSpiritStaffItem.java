package lt.utopiabosses.item;

import lt.utopiabosses.client.renderer.item.TreeSpiritStaffRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TreeSpiritStaffItem extends Item implements GeoItem {
    private static final String CONTROLLER_NAME = "rotateController";
    private static final RawAnimation ROTATE_ANIM = RawAnimation.begin().thenLoop("rotate");
    private static final int LIFE_DRAIN_RANGE = 10;
    private static final int LIFE_DRAIN_DAMAGE = 2; // 一颗心 = 2点生命值
    private static final int LIFE_DRAIN_COOLDOWN = 20; // 1秒
    private final Map<UUID, LifeDrainData> drainTargets = new HashMap<>();
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    // 生命吸取数据
    private static class LifeDrainData {
        private int cooldown = 0;
        
        public void decrementCooldown() {
            if (cooldown > 0) {
                cooldown--;
            }
        }
        
        public void resetCooldown() {
            cooldown = LIFE_DRAIN_COOLDOWN;
        }
        
        public int getCooldown() {
            return cooldown;
        }
    }

    public TreeSpiritStaffItem(Settings settings) {
        super(settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }
    
    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private TreeSpiritStaffRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null)
                    this.renderer = new TreeSpiritStaffRenderer();

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
        controllerRegistrar.add(new AnimationController<>(this, CONTROLLER_NAME, 0, animationState -> {
            // 始终播放旋转动画
            animationState.getController().setAnimation(ROTATE_ANIM);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.utopiabosses.tree_spirit_staff.tooltip1")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("item.utopiabosses.tree_spirit_staff.tooltip2")
                .formatted(Formatting.GREEN));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        BlockState blockState = world.getBlockState(blockPos);
        
        if (blockState.getBlock() instanceof Fertilizable fertilizable) {
            // 如果是可以施肥的方块
            PlayerEntity player = context.getPlayer();
            if (player == null) return ActionResult.PASS;
            
            if (fertilizable.isFertilizable(world, blockPos, blockState, world.isClient)) {
                if (world.isClient) {
                    return ActionResult.SUCCESS;
                }
                
                if (fertilizable.canGrow(world, world.random, blockPos, blockState)) {
                    // 催熟植物
                    fertilizable.grow((ServerWorld) world, world.random, blockPos, blockState);
                    
                    // 播放音效和粒子
                    world.playSound(null, blockPos, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                    
                    // 显示骨粉粒子
                    ServerWorld serverWorld = (ServerWorld) world;
                    serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER, 
                            blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5, 
                            10, 0.5, 0.5, 0.5, 0.0);
                    
                    // 减少耐久度
                    ItemStack stack = context.getStack();
                    if (player != null && !player.getAbilities().creativeMode) {
                        stack.damage(1, player, p -> p.sendToolBreakStatus(context.getHand()));
                    }
                    
                    return ActionResult.SUCCESS;
                }
            }
        }
        
        return ActionResult.PASS;
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof PlayerEntity player && !attacker.getWorld().isClient) {
            // 开始生命吸取
            drainTargets.put(target.getUuid(), new LifeDrainData());
            
            // 播放特效
            player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_WITCH_AMBIENT, SoundCategory.PLAYERS, 0.5f, 1.0f);
            
            // 如果物品有耐久度，点击减少耐久
            if (!player.getAbilities().creativeMode) {
                stack.damage(1, player, p -> p.sendToolBreakStatus(Hand.MAIN_HAND));
            }
        }
        return true;
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient || !(entity instanceof PlayerEntity player)) {
            return;
        }

        // 处理生命吸取效果
        if (!drainTargets.isEmpty()) {
            Iterator<Map.Entry<UUID, LifeDrainData>> iterator = drainTargets.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, LifeDrainData> entry = iterator.next();
                LifeDrainData data = entry.getValue();
                
                // 更新计时器
                data.decrementCooldown();
                
                if (data.getCooldown() <= 0) {
                    Entity targetEntity = ((ServerWorld) world).getEntity(entry.getKey());
                    if (targetEntity instanceof LivingEntity target && !target.isDead() && 
                            player.squaredDistanceTo(target) <= LIFE_DRAIN_RANGE * LIFE_DRAIN_RANGE) {
                        
                        // 造成伤害并恢复生命
                        target.damage(world.getDamageSources().playerAttack(player), LIFE_DRAIN_DAMAGE);
                        player.heal(LIFE_DRAIN_DAMAGE);
                        
                        // 播放特效
                        Vec3d playerPos = player.getEyePos();
                        Vec3d targetPos = target.getEyePos();
                        
                        // 在目标和玩家之间创建粒子线
                        double distance = playerPos.distanceTo(targetPos);
                        Vec3d direction = targetPos.subtract(playerPos).normalize();
                        for (double d = 0.5; d < distance; d += 0.5) {
                            Vec3d particlePos = playerPos.add(direction.multiply(d));
                            ((ServerWorld) world).spawnParticles(
                                    ParticleTypes.WITCH,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    1, 0.0, 0.0, 0.0, 0.0);
                        }
                        
                        // 播放音效
                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENTITY_WITCH_DRINK, SoundCategory.PLAYERS, 0.5f, 1.0f);
                        
                        // 重置冷却时间
                        data.resetCooldown();
                    } else {
                        // 目标无效，移除
                        iterator.remove();
                    }
                }
            }
            
            // 如果物品有耐久度，每次吸取生命减少耐久
            if (!player.getAbilities().creativeMode && player.getRandom().nextFloat() < 0.01f) {
                stack.damage(1, player, p -> p.sendToolBreakStatus(Hand.MAIN_HAND));
            }
        }
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // 右键点击空气不做任何事情
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
    
    // 允许使用任何树木相关的物品修复
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.isIn(ItemTags.LOGS) || ingredient.isIn(ItemTags.LEAVES);
    }
} 