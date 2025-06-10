package lt.utopiabosses.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.SpawnEggItem;

public class CustomSpawnEggItem extends SpawnEggItem {
    public CustomSpawnEggItem(EntityType<? extends MobEntity> type, Settings settings) {
        super(type, 0, 0, settings); // 使用透明色让自定义贴图显示
    }
    
    // 重写颜色方法返回透明色，确保使用材质文件
    @Override
    public int getColor(int tintIndex) {
        return 0x00FFFFFF; // 透明白色
    }
} 