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
    private static final RawAnimation ROTATE_ANIM = RawAnimation.begin().thenLoop("rotation");
    // 树灵之杖现在只用于催熟农作物，不再有吸血功能
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);
    
    // 树灵之杖现在只有催熟功能，不再需要生命吸取数据

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
        tooltip.add(Text.literal("无限耐久")
                .formatted(Formatting.GOLD));
        tooltip.add(Text.literal("右键方块：催熟3x3区域的农作物")
                .formatted(Formatting.GREEN));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();

        if (world.isClient || player == null) {
            return ActionResult.SUCCESS;
        }

        // 催熟3x3区域的农作物
        boolean didSomething = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos targetPos = blockPos.add(x, 0, z);
                BlockState state = world.getBlockState(targetPos);
                
                // 检查是否是可催熟的方块
                if (state.getBlock() instanceof Fertilizable fertilizable) {
                    if (fertilizable.isFertilizable(world, targetPos, state, false)) {
                        if (fertilizable.canGrow(world, world.random, targetPos, state)) {
                            // 催熟植物
                            fertilizable.grow((ServerWorld) world, world.random, targetPos, state);
                            didSomething = true;
                            
                            // 生成粒子效果
                            ((ServerWorld) world).spawnParticles(ParticleTypes.HAPPY_VILLAGER, 
                                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 
                                5, 0.3, 0.3, 0.3, 0.1);
                        }
                    }
                }
            }
        }
        
        if (didSomething) {
            // 播放音效
            world.playSound(null, blockPos, SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
    
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 树灵之杖现在无限耐久，不再有吸血效果
        return true;
    }
    
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        // 树灵之杖现在无限耐久，不再有吸血效果，无需处理任何特殊逻辑
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        // 右键点击空气不做任何事情
        return TypedActionResult.pass(user.getStackInHand(hand));
    }
    
    // 树灵之杖无限耐久，不需要修复
    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return false;
    }

    @Override
    public boolean isDamageable() {
        // 树灵之杖无限耐久
        return false;
    }
} 