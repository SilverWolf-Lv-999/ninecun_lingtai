package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaCandidateManager;
import net.seraphina.ninecun_lingtai.common.network.PalmBuddhaMemberPacketUtility;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaCandidateSet;

import java.util.function.Supplier;

public record SyncPalmBuddhaCandidatesPacket(PalmBuddhaCandidateSet candidateSet) {
    public static void encode(
            SyncPalmBuddhaCandidatesPacket packet,
            FriendlyByteBuf buffer
    ) {
        PalmBuddhaMemberPacketUtility.writeCandidates(buffer, packet.candidateSet());
    }

    public static SyncPalmBuddhaCandidatesPacket decode(FriendlyByteBuf buffer) {
        return new SyncPalmBuddhaCandidatesPacket(
                PalmBuddhaMemberPacketUtility.readCandidates(buffer)
        );
    }

    public static void handle(
            SyncPalmBuddhaCandidatesPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPalmBuddhaCandidateManager.accept(packet.candidateSet())
        ));
        context.setPacketHandled(true);
    }
}
