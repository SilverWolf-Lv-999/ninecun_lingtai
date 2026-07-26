package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientSwordFlightManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** 向追踪者与本人同步某玩家的御剑状态；剑的快照用于在脚下渲染。 */
public record SyncSwordFlightPacket(
        UUID playerId,
        boolean flying,
        ItemStack sword
) {
    public static void encode(SyncSwordFlightPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeBoolean(packet.flying);
        buffer.writeItem(packet.sword);
    }

    public static SyncSwordFlightPacket decode(FriendlyByteBuf buffer) {
        return new SyncSwordFlightPacket(
                buffer.readUUID(),
                buffer.readBoolean(),
                buffer.readItem()
        );
    }

    public static void handle(SyncSwordFlightPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientSwordFlightManager.setFlying(
                        packet.playerId,
                        packet.flying,
                        packet.sword
                )
        );
    }
}
