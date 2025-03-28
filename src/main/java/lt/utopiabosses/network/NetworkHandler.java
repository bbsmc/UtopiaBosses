package lt.utopiabosses.network;

import lt.utopiabosses.Utopiabosses;
import lt.utopiabosses.item.SunflowerGatlingItem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

/**
 * 网络包处理类，用于客户端和服务端之间的通信
 */
public class NetworkHandler {
    // 向日葵加特林射击网络包ID
    public static final Identifier SHOOT_GATLING_PACKET_ID = 
        new Identifier(Utopiabosses.MOD_ID, "shoot_gatling");
    
    /**
     * 注册所有网络处理器
     */
    public static void registerNetworkHandlers() {
        // 注册向日葵加特林射击处理器
        ServerPlayNetworking.registerGlobalReceiver(SHOOT_GATLING_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            boolean shouldShoot = buf.readBoolean();
            if (shouldShoot) {
                // 在服务器线程上调度射击处理
                server.execute(() -> {
                    // 确保玩家仍然存在
                    if (player.isRemoved() || !player.isAlive()) return;
                    
                    // 调用射击方法
                    SunflowerGatlingItem.shoot(player);
                });
            }
        });
    }
} 