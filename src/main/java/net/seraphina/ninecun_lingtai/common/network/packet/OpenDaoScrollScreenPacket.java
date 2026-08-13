package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.screen.ClientDaoScrollScreenManager;
import net.seraphina.ninecun_lingtai.common.debug.DaoScrollSnapshot;
import net.seraphina.ninecun_lingtai.common.network.DaoScrollPacketUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenDaoScrollScreenPacket(DaoScrollSnapshot snapshot, boolean editingSelf) {
    public static void encode(OpenDaoScrollScreenPacket packet, FriendlyByteBuf buffer) {
        DaoScrollPacketUtility.writeSnapshot(buffer, packet.snapshot());
        buffer.writeBoolean(packet.editingSelf());
    }

    public static OpenDaoScrollScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenDaoScrollScreenPacket(
                DaoScrollPacketUtility.readSnapshot(buffer),
                buffer.readBoolean()
        );
    }

    public static void handle(
            OpenDaoScrollScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientDaoScrollScreenManager.open(packet.snapshot(), packet.editingSelf())
        );
    }
}
