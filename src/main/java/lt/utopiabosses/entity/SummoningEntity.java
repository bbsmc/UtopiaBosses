package lt.utopiabosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class SummoningEntity extends Entity implements GeoEntity {
    private final AnimatableInstanceCache factory = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation SUMMONING_ANIM = RawAnimation.begin().then("skill_insert_both_arms_into_the_ground_surface", Animation.LoopType.PLAY_ONCE);
    
    // 跟踪动画播放状态
    private static final TrackedData<Boolean> ANIMATION_FINISHED = DataTracker.registerData(SummoningEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // 添加一个跟踪关联实体UUID的数据
    private static final TrackedData<String> OWNER_UUID = DataTracker.registerData(SummoningEntity.class, TrackedDataHandlerRegistry.STRING);
    
    // 动画计时器
    private int animationTicks = 0;
    // 动画总时长（6.25秒，对应125ticks）
    private static final int ANIMATION_DURATION = 125;
    
    // 默认放大尺寸
    private static final float SCALE = 2.0f;
    
    // 关联的实体引用
    private Entity owner;


    public SummoningEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true; // 允许穿过方块
        this.setInvisible(false); // 实体可见
        
        // 特效实体无碰撞箱，允许玩家穿过
        this.setBoundingBox(new net.minecraft.util.math.Box(0, 0, 0, 0, 0, 0));
    }
    
    /**
     * 设置特效的拥有者实体，用于同步方向
     * @param entity 拥有者实体
     */
    public void setOwner(Entity entity) {
        this.owner = entity;
        if (entity != null) {
            this.dataTracker.set(OWNER_UUID, entity.getUuidAsString());
            // 初始设置方向
            this.setYaw(entity.getYaw());
        }
    }
    
    /**
     * 获取特效的拥有者实体
     * @return 拥有者实体
     */
    public Entity getOwner() {
        if (this.owner == null && !this.getWorld().isClient() && 
            this.dataTracker.get(OWNER_UUID) != null && 
            !this.dataTracker.get(OWNER_UUID).isEmpty()) {
            try {
                UUID ownerUuid = UUID.fromString(this.dataTracker.get(OWNER_UUID));
                // 使用正确的方法获取实体
                if (this.getWorld() instanceof ServerWorld) {
                    Entity entity = ((ServerWorld)this.getWorld()).getEntity(ownerUuid);
                    if (entity != null) {
                        this.owner = entity;
                        return entity;
                    }
                }
            } catch (Exception e) {
                // 处理UUID解析错误
            }
        }
        return this.owner;
    }
    
    // 覆盖碰撞检测相关方法
    @Override
    public boolean isCollidable() {
        return false; // 禁止碰撞
    }
    
    @Override
    public boolean isPushable() {
        return false; // 不能被推动
    }


    // 对于GeckoLib动画模型的缩放，保留原始尺寸计算但增加模型比例
    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        EntityDimensions originalDimensions = EntityDimensions.fixed(2.0f, 2.0f);
        return EntityDimensions.changing(
            originalDimensions.width, 
            originalDimensions.height
        );
    }
    
    // 通过自定义渲染比例来控制放大效果的另一种方式
    public float getRenderScale() {
        return SCALE;
    }
    
    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(ANIMATION_FINISHED, false);
        this.dataTracker.startTracking(OWNER_UUID, "");
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 确保实体始终无碰撞
        this.noClip = true;
        
        // 同步方向与拥有者保持一致
        if (this.owner != null && !this.owner.isRemoved()) {
            // 设置该实体的朝向与拥有者相同
            this.setYaw(this.owner.getYaw());
            
        }
        
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
        if (nbt.contains("OwnerUUID")) {
            this.dataTracker.set(OWNER_UUID, nbt.getString("OwnerUUID"));
        }
    }
    
    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("AnimationTicks", animationTicks);
        String ownerUUID = this.dataTracker.get(OWNER_UUID);
        if (ownerUUID != null && !ownerUUID.isEmpty()) {
            nbt.putString("OwnerUUID", ownerUUID);
        }
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
} 