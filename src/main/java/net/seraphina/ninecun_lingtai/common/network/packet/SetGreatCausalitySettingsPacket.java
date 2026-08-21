package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalityTargetMode;
import net.seraphina.ninecun_lingtai.common.player.PlayerGreatCausalityData;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record SetGreatCausalitySettingsPacket(GreatCausalitySettings settings) {
    public static void encode(SetGreatCausalitySettingsPacket packet, FriendlyByteBuf buffer) {
        GreatCausalitySettings settings = packet.settings();
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

    public static SetGreatCausalitySettingsPacket decode(FriendlyByteBuf buffer) {
        return new SetGreatCausalitySettingsPacket(new GreatCausalitySettings(
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                readTargetMode(buffer),
                readTargetId(buffer),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                readTargetMode(buffer),
                readTargetId(buffer)
        ));
    }

    public static void handle(
            SetGreatCausalitySettingsPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !GreatCausalityUtility.isPerfection(player)) {
                return;
            }
            PlayerGreatCausalityData.setSettings(
                    player,
                    GreatCausalityUtility.sanitizeSettings(player, packet.settings())
            );
        });
        context.setPacketHandled(true);
    }

    private static void writeTarget(
            FriendlyByteBuf buffer,
            GreatCausalityTargetMode mode,
            UUID targetId
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

    private static UUID readTargetId(FriendlyByteBuf buffer) {
        return buffer.readBoolean() ? buffer.readUUID() : null;
    }
}
