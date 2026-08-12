package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.player.ClientDefenseSpellManager;
import net.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import net.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import net.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellShape;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;
import java.util.function.Supplier;

public record SyncDefenseSpellStatePacket(
        int entityId,
        Optional<DefenseSpellSnapshot> bodyShield,
        Optional<DefenseSpellSnapshot> guardian
) {
    public SyncDefenseSpellStatePacket {
        bodyShield = bodyShield == null ? Optional.empty() : bodyShield;
        guardian = guardian == null ? Optional.empty() : guardian;
    }

    public static SyncDefenseSpellStatePacket empty(int entityId) {
        return new SyncDefenseSpellStatePacket(entityId, Optional.empty(), Optional.empty());
    }

    public static void encode(SyncDefenseSpellStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        writeSnapshot(buffer, packet.bodyShield);
        writeSnapshot(buffer, packet.guardian);
    }

    public static SyncDefenseSpellStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncDefenseSpellStatePacket(
                buffer.readVarInt(),
                readSnapshot(buffer, DefenseSpellShape.BODY_SHIELD),
                readSnapshot(buffer, DefenseSpellShape.GUARDIAN)
        );
    }

    public static void handle(
            SyncDefenseSpellStatePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientDefenseSpellManager.accept(
                    packet.entityId,
                    packet.bodyShield.orElse(null),
                    packet.guardian.orElse(null)
            );
        });
    }

    private static void writeSnapshot(
            FriendlyByteBuf buffer,
            Optional<DefenseSpellSnapshot> snapshotOptional
    ) {
        buffer.writeBoolean(snapshotOptional.isPresent());
        snapshotOptional.ifPresent(snapshot -> {
            buffer.writeVarInt(snapshot.kind().ordinal());
            buffer.writeDouble(snapshot.current());
            buffer.writeDouble(snapshot.maximum());
            buffer.writeInt(snapshot.elementMask());
            buffer.writeVarInt(snapshot.remainingTicks());
        });
    }

    private static Optional<DefenseSpellSnapshot> readSnapshot(
            FriendlyByteBuf buffer,
            DefenseSpellShape expectedShape
    ) {
        if (!buffer.readBoolean()) {
            return Optional.empty();
        }
        int kindIndex = buffer.readVarInt();
        double current = buffer.readDouble();
        double maximum = buffer.readDouble();
        int elementMask = buffer.readInt();
        int remainingTicks = buffer.readVarInt();
        DefenseSpellKind[] kinds = DefenseSpellKind.values();
        if (kindIndex < 0
                || kindIndex >= kinds.length
                || kinds[kindIndex].getShape() != expectedShape
                || !Double.isFinite(current)
                || !Double.isFinite(maximum)
                || current <= 0.0D
                || maximum <= 0.0D
                || remainingTicks <= 0) {
            return Optional.empty();
        }
        return Optional.of(new DefenseSpellSnapshot(
                kinds[kindIndex],
                current,
                maximum,
                elementMask,
                remainingTicks
        ));
    }
}
