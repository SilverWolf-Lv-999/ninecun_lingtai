package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaOverlayManager;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaReleaseResult;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** 只发给当前施法者的佛国放生结果，不包含位置、维度或实体数据。 */
public record SyncPalmBuddhaReleaseResultPacket(
        UUID memberId,
        boolean successful,
        PalmBuddhaReleaseResult.FailureReason failureReason
) {
    public SyncPalmBuddhaReleaseResultPacket {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(failureReason, "failureReason");
        if (successful != (failureReason == PalmBuddhaReleaseResult.FailureReason.NONE)) {
            throw new IllegalArgumentException("Palm-buddha release result has inconsistent state");
        }
    }

    public static SyncPalmBuddhaReleaseResultPacket fromResult(
            UUID memberId,
            PalmBuddhaReleaseResult result
    ) {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(result, "result");
        return new SyncPalmBuddhaReleaseResultPacket(
                memberId,
                result.successful(),
                result.failureReason()
        );
    }

    public static void encode(
            SyncPalmBuddhaReleaseResultPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(packet.memberId());
        buffer.writeBoolean(packet.successful());
        buffer.writeVarInt(packet.failureReason().wireId());
    }

    public static SyncPalmBuddhaReleaseResultPacket decode(FriendlyByteBuf buffer) {
        UUID memberId = buffer.readUUID();
        boolean successful = buffer.readBoolean();
        PalmBuddhaReleaseResult.FailureReason failureReason =
                PalmBuddhaReleaseResult.FailureReason.fromWireId(buffer.readVarInt());
        return new SyncPalmBuddhaReleaseResultPacket(memberId, successful, failureReason);
    }

    public static void handle(
            SyncPalmBuddhaReleaseResultPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPalmBuddhaOverlayManager.acceptReleaseResult(
                        packet.memberId(),
                        packet.successful(),
                        packet.failureReason()
                )
        ));
        context.setPacketHandled(true);
    }
}
