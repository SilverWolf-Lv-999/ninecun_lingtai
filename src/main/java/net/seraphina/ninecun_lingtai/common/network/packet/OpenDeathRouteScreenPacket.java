package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * S→C：死亡路线选择界面（倒计时 + 可选路线掩码 + 灰显原因键，按 DeathRoute 序）。
 */
public record OpenDeathRouteScreenPacket(
        long sessionNonce,
        int remainingSeconds,
        int availableMask,
        List<String> lockedReasonKeys
) {
    public static void encode(OpenDeathRouteScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.sessionNonce);
        buffer.writeVarInt(packet.remainingSeconds);
        buffer.writeVarInt(packet.availableMask);
        buffer.writeVarInt(packet.lockedReasonKeys.size());
        for (String reasonKey : packet.lockedReasonKeys) {
            buffer.writeUtf(reasonKey);
        }
    }

    public static OpenDeathRouteScreenPacket decode(FriendlyByteBuf buffer) {
        long sessionNonce = buffer.readLong();
        int remainingSeconds = buffer.readVarInt();
        int availableMask = buffer.readVarInt();
        int count = buffer.readVarInt();
        List<String> lockedReasonKeys = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            lockedReasonKeys.add(buffer.readUtf());
        }
        return new OpenDeathRouteScreenPacket(
                sessionNonce, remainingSeconds, availableMask, lockedReasonKeys);
    }

    public static void handle(
            OpenDeathRouteScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> net.seraphina.ninecun_lingtai.client.screen.DeathRouteScreen.open(packet)
        ));
        contextSupplier.get().setPacketHandled(true);
    }
}
