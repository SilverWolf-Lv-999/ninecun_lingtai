package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientDefenseSpellManager;
import com.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncDefenseSpellStatePacket(
        int entityId,
        int kindIndex,
        double current,
        double maximum,
        int elementMask
) {
    public static SyncDefenseSpellStatePacket empty(int entityId) {
        return new SyncDefenseSpellStatePacket(entityId, -1, 0.0D, 0.0D, 0);
    }

    public static SyncDefenseSpellStatePacket of(int entityId, DefenseSpellSnapshot snapshot) {
        return new SyncDefenseSpellStatePacket(
                entityId,
                snapshot.kind().ordinal(),
                snapshot.current(),
                snapshot.maximum(),
                snapshot.elementMask()
        );
    }

    public static void encode(SyncDefenseSpellStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeVarInt(packet.kindIndex + 1);
        buffer.writeDouble(packet.current);
        buffer.writeDouble(packet.maximum);
        buffer.writeInt(packet.elementMask);
    }

    public static SyncDefenseSpellStatePacket decode(FriendlyByteBuf buffer) {
        return new SyncDefenseSpellStatePacket(
                buffer.readVarInt(),
                buffer.readVarInt() - 1,
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readInt()
        );
    }

    public static void handle(
            SyncDefenseSpellStatePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            DefenseSpellKind[] kinds = DefenseSpellKind.values();
            if (packet.kindIndex < 0
                    || packet.kindIndex >= kinds.length
                    || packet.maximum <= 0.0D
                    || packet.current <= 0.0D) {
                ClientDefenseSpellManager.accept(packet.entityId, null);
                return;
            }
            ClientDefenseSpellManager.accept(
                    packet.entityId,
                    new DefenseSpellSnapshot(
                            kinds[packet.kindIndex],
                            packet.current,
                            packet.maximum,
                            packet.elementMask
                    )
            );
        });
    }
}
