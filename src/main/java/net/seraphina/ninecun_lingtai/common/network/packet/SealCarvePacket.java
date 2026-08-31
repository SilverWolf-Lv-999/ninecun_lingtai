package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.seal.SealKind;
import net.seraphina.ninecun_lingtai.common.seal.SealManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：发起刻印仪式（kindOrdinal ≥ 0）或废除最外层（kindOrdinal = -1）。
 * detonateThresholdPct 仅自毁禁有效（50-90）；legacyMessage 仅遗念禁有效
 * （M4，服务端裁剪长度、只落 SavedData 不进物品 NBT）。全部校验在服务端。
 */
public record SealCarvePacket(
        boolean mainHand,
        int kindOrdinal,
        int detonateThresholdPct,
        String legacyMessage
) {

    public static void encode(SealCarvePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.mainHand);
        buffer.writeVarInt(packet.kindOrdinal);
        buffer.writeVarInt(packet.detonateThresholdPct);
        buffer.writeUtf(packet.legacyMessage, SealManager.LEGACY_MESSAGE_MAX_LENGTH * 4);
    }

    public static SealCarvePacket decode(FriendlyByteBuf buffer) {
        return new SealCarvePacket(
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readUtf(SealManager.LEGACY_MESSAGE_MAX_LENGTH * 4)
        );
    }

    public static void handle(SealCarvePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            if (player == null) {
                return;
            }
            InteractionHand hand = packet.mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            if (packet.kindOrdinal < 0) {
                SealManager.startRevoke(player, hand);
                return;
            }
            SealKind[] kinds = SealKind.values();
            if (packet.kindOrdinal >= kinds.length) {
                return;
            }
            SealManager.startCarve(
                    player,
                    hand,
                    kinds[packet.kindOrdinal],
                    packet.detonateThresholdPct / 100.0D,
                    packet.legacyMessage
            );
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
