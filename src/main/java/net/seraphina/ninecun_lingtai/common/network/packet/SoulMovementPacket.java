package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SoulMovementPacket(
        float leftImpulse,
        float forwardImpulse,
        boolean jumping,
        boolean shiftKeyDown,
        float yRot,
        float xRot
) {
    public static void encode(SoulMovementPacket packet, FriendlyByteBuf buffer) {
        buffer.writeFloat(packet.leftImpulse);
        buffer.writeFloat(packet.forwardImpulse);
        buffer.writeBoolean(packet.jumping);
        buffer.writeBoolean(packet.shiftKeyDown);
        buffer.writeFloat(packet.yRot);
        buffer.writeFloat(packet.xRot);
    }

    public static SoulMovementPacket decode(FriendlyByteBuf buffer) {
        return new SoulMovementPacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    public static void handle(
            SoulMovementPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                PlayerSoulRoamingManager.moveSoul(
                        player,
                        packet.leftImpulse,
                        packet.forwardImpulse,
                        packet.jumping,
                        packet.shiftKeyDown,
                        packet.yRot,
                        packet.xRot
                );
            }
        });
        context.setPacketHandled(true);
    }
}
