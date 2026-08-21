package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.causality.ClientCausalityManager;
import net.seraphina.ninecun_lingtai.common.causality.CausalityPlayerSnapshot;
import net.seraphina.ninecun_lingtai.common.causality.CausalityRecord;
import net.seraphina.ninecun_lingtai.common.causality.CausalitySnapshot;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalityTargetMode;

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
        buffer.writeVarInt(Math.min(MAXIMUM_PLAYERS, snapshot.settingsTargets().size()));
        snapshot.settingsTargets().stream().limit(MAXIMUM_PLAYERS).forEach(player -> writePlayer(buffer, player));
        buffer.writeBoolean(snapshot.target() != null);
        if (snapshot.target() != null) {
            writePlayer(buffer, snapshot.target());
        }
        buffer.writeVarInt(Math.min(MAXIMUM_RECORDS, snapshot.records().size()));
        snapshot.records().stream().skip(Math.max(0, snapshot.records().size() - MAXIMUM_RECORDS))
                .forEach(record -> writeRecord(buffer, record));
        buffer.writeBoolean(snapshot.fullHistory());
        buffer.writeUtf(snapshot.message(), MAXIMUM_TEXT_LENGTH);
        writeSettings(buffer, snapshot.settings());
        buffer.writeBoolean(snapshot.settingsAvailable());
    }

    public static OpenGreatCausalityScreenPacket decode(FriendlyByteBuf buffer) {
        boolean playerListMode = buffer.readBoolean();
        int playerCount = readCount(buffer, MAXIMUM_PLAYERS);
        List<CausalityPlayerSnapshot> players = new ArrayList<>(playerCount);
        for (int index = 0; index < playerCount; index++) {
            players.add(readPlayer(buffer));
        }
        int settingsTargetCount = readCount(buffer, MAXIMUM_PLAYERS);
        List<CausalityPlayerSnapshot> settingsTargets = new ArrayList<>(settingsTargetCount);
        for (int index = 0; index < settingsTargetCount; index++) {
            settingsTargets.add(readPlayer(buffer));
        }
        CausalityPlayerSnapshot target = buffer.readBoolean() ? readPlayer(buffer) : null;
        int recordCount = readCount(buffer, MAXIMUM_RECORDS);
        List<CausalityRecord> records = new ArrayList<>(recordCount);
        for (int index = 0; index < recordCount; index++) {
            records.add(readRecord(buffer));
        }
        boolean fullHistory = buffer.readBoolean();
        String message = buffer.readUtf(MAXIMUM_TEXT_LENGTH);
        GreatCausalitySettings settings = readSettings(buffer);
        boolean settingsAvailable = buffer.readBoolean();
        return new OpenGreatCausalityScreenPacket(new CausalitySnapshot(
                playerListMode,
                players,
                settingsTargets,
                target,
                records,
                fullHistory,
                message,
                settings,
                settingsAvailable
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

    private static void writeSettings(FriendlyByteBuf buffer, GreatCausalitySettings settings) {
        buffer.writeBoolean(settings.transferAttack());
        buffer.writeBoolean(settings.transferHate());
        buffer.writeBoolean(settings.transferTribulation());
        buffer.writeBoolean(settings.transferNegativeEffects());
        buffer.writeBoolean(settings.transferVillagerPrice());
        buffer.writeBoolean(settings.transferDamage());
        writeTarget(buffer, settings.transferTargetMode(), settings.transferTargetId());
        buffer.writeBoolean(settings.randomItem());
        buffer.writeBoolean(settings.corruptBeneficialEffects());
        buffer.writeBoolean(settings.transferItems());
        buffer.writeBoolean(settings.transferAdvancements());
        buffer.writeBoolean(settings.voidCrafting());
        writeTarget(buffer, settings.tamperTargetMode(), settings.tamperTargetId());
    }

    private static GreatCausalitySettings readSettings(FriendlyByteBuf buffer) {
        boolean transferAttack = buffer.readBoolean();
        boolean transferHate = buffer.readBoolean();
        boolean transferTribulation = buffer.readBoolean();
        boolean transferNegativeEffects = buffer.readBoolean();
        boolean transferVillagerPrice = buffer.readBoolean();
        boolean transferDamage = buffer.readBoolean();
        GreatCausalityTargetMode transferTargetMode = readTargetMode(buffer);
        java.util.UUID transferTargetId = readTargetId(buffer);
        boolean randomItem = buffer.readBoolean();
        boolean corruptBeneficialEffects = buffer.readBoolean();
        boolean transferItems = buffer.readBoolean();
        boolean transferAdvancements = buffer.readBoolean();
        boolean voidCrafting = buffer.readBoolean();
        GreatCausalityTargetMode tamperTargetMode = readTargetMode(buffer);
        java.util.UUID tamperTargetId = readTargetId(buffer);
        return new GreatCausalitySettings(
                transferAttack,
                transferHate,
                transferTribulation,
                transferNegativeEffects,
                transferVillagerPrice,
                transferDamage,
                transferTargetMode,
                transferTargetId,
                randomItem,
                corruptBeneficialEffects,
                transferItems,
                transferAdvancements,
                voidCrafting,
                tamperTargetMode,
                tamperTargetId
        );
    }

    private static void writeTarget(
            FriendlyByteBuf buffer,
            GreatCausalityTargetMode mode,
            java.util.UUID targetId
    ) {
        buffer.writeVarInt(mode.ordinal());
        buffer.writeBoolean(targetId != null);
        if (targetId != null) {
            buffer.writeUUID(targetId);
        }
    }

    private static GreatCausalityTargetMode readTargetMode(FriendlyByteBuf buffer) {
        int ordinal = buffer.readVarInt();
        return ordinal >= 0 && ordinal < GreatCausalityTargetMode.values().length
                ? GreatCausalityTargetMode.values()[ordinal]
                : GreatCausalityTargetMode.RANDOM;
    }

    private static java.util.UUID readTargetId(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUUID() : null;
    }
}
