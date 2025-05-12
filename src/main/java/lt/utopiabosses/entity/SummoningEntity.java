package lt.utopiabosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class SummoningEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SUMMONING_ANIM = RawAnimation.begin().then("skill_insert_both_arms_into_the_ground_surface", Animation.LoopType.PLAY_ONCE);
    
    // 跟踪动画播放状态
    private static final TrackedData<Boolean> ANIMATION_FINISHED = DataTracker.registerData(SummoningEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 动画计时器
    private int animationTicks = 0;
    // 动画总时长（6.25秒，对应125ticks）
    private static final int ANIMATION_DURATION = 125;
    
    public SummoningEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true; // 允许穿过方块
        this.setInvisible(false); // 实体可见
    }
    
    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(ANIMATION_FINISHED, false);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!this.getWorld().isClient()) {
            // 增加动画计时器
            animationTicks++;
            
            // 检查动画是否已完成
            if (animationTicks >= ANIMATION_DURATION) {
                this.dataTracker.set(ANIMATION_FINISHED, true);
                this.remove(RemovalReason.DISCARDED);
            }
        }
    }
    
    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        animationTicks = nbt.getInt("AnimationTicks");
    }
    
    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("AnimationTicks", animationTicks);
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(
            this, 
            "controller", 
            0, 
            event -> {
                // 始终播放召唤动画
                event.getController().setAnimation(SUMMONING_ANIM);
                return PlayState.CONTINUE;
            }
        ));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.factory;
    }
    
    // 不受重力影响
    @Override
    public boolean hasNoGravity() {
        return true;
    }
    
    // 不能被推动
    @Override
    public boolean isPushable() {
        return false;
    }
} 