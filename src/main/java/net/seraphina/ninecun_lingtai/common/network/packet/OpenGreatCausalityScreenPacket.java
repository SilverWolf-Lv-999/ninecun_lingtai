package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.causality.ClientCausalityManager;
import net.seraphina.ninecun_lingtai.common.causality.CausalityPlayerSnapshot;
import net.seraphina.ninecun_lingtai.common.causality.CausalityRecord;
import net.seraphina.ninecun_lingtai.common.causality.CausalitySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public record OpenGreatCausalityScreenPacket(CausalitySnapshot snapshot) {
    private static final int MAXIMUM_PLAYERS = 64;
    private static final int MAXIMUM_RECORDS = 4096;
    private static final int MAXIMUM_TEXT_LENGTH = 4096;

    public static void encode(OpenGreatCausalityScreenPacket packet, FriendlyByteBuf buffer) {
        CausalitySnapshot snapshot = packet.snapshot();
        buffer.writeBoolean(snapshot.playerListMode());
        buffer.writeVarInt(Math.min(MAXIMUM_PLAYERS, snapshot.players().size()));
        snapshot.players().stream().limit(MAXIMUM_PLAYERS).forEach(player -> writePlayer(buffer, player));
        buffer.writeBoolean(snapshot.target() != null);
        if (snapshot.target() != null) {
            writePlayer(buffer, snapshot.target());
        }
        buffer.writeVarInt(Math.min(MAXIMUM_RECORDS, snapshot.records().size()));
        snapshot.records().stream().skip(Math.max(0, snapshot.records().size() - MAXIMUM_RECORDS))
                .forEach(record -> writeRecord(buffer, record));
        buffer.writeBoolean(snapshot.fullHistory());
        buffer.writeUtf(snapshot.message(), MAXIMUM_TEXT_LENGTH);
    }

    public static OpenGreatCausalityScreenPacket decode(FriendlyByteBuf buffer) {
        boolean playerListMode = buffer.readBoolean();
        int playerCount = readCount(buffer, MAXIMUM_PLAYERS);
        List<CausalityPlayerSnapshot> players = new ArrayList<>(playerCount);
        for (int index = 0; index < playerCount; index++) {
            players.add(readPlayer(buffer));
        }
        CausalityPlayerSnapshot target = buffer.readBoolean() ? readPlayer(buffer) : null;
        int recordCount = readCount(buffer, MAXIMUM_RECORDS);
        List<CausalityRecord> records = new ArrayList<>(recordCount);
        for (int index = 0; index < recordCount; index++) {
            records.add(readRecord(buffer));
        }
        return new OpenGreatCausalityScreenPacket(new CausalitySnapshot(
                playerListMode,
                players,
                target,
                records,
                buffer.readBoolean(),
                buffer.readUtf(MAXIMUM_TEXT_LENGTH)
        ));
    }

    public static void handle(
            OpenGreatCausalityScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientCausalityManager.open(packet.snapshot())
        );
    }

    private static void writePlayer(FriendlyByteBuf buffer, CausalityPlayerSnapshot player) {
        buffer.writeUUID(player.playerId());
        buffer.writeUtf(player.playerName(), 128);
        buffer.writeUtf(player.dimension(), 256);
        buffer.writeUtf(player.cultivation(), 128);
    }

    private static CausalityPlayerSnapshot readPlayer(FriendlyByteBuf buffer) {
        return new CausalityPlayerSnapshot(
                buffer.readUUID(),
                buffer.readUtf(128),
                buffer.readUtf(256),
                buffer.readUtf(128)
        );
    }

    private static void writeRecord(FriendlyByteBuf buffer, CausalityRecord record) {
        buffer.writeUtf(record.timestamp(), 128);
        buffer.writeVarLong(record.gameTime());
        buffer.writeUtf(record.dimension(), 256);
        buffer.writeInt(record.x());
        buffer.writeInt(record.y());
        buffer.writeInt(record.z());
        buffer.writeUtf(record.category(), 64);
        buffer.writeUtf(record.description(), MAXIMUM_TEXT_LENGTH);
    }

    private static CausalityRecord readRecord(FriendlyByteBuf buffer) {
        return new CausalityRecord(
                buffer.readUtf(128),
                buffer.readVarLong(),
                buffer.readUtf(256),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(64),
                buffer.readUtf(MAXIMUM_TEXT_LENGTH)
        );
    }

    private static int readCount(FriendlyByteBuf buffer, int maximum) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maximum) {
            throw new IllegalArgumentException("Invalid causality packet count: " + count);
        }
        return count;
    }
}
