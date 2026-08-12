package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.flight.LightFlightManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerValueControlData;
import net.seraphina.ninecun_lingtai.common.player.PlayerValueControlSettings;
import net.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;
import net.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import net.seraphina.ninecun_lingtai.utility.PlayerValueControlUnlockUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public record SetPlayerValueControlSettingsPacket(PlayerValueControlSettings settings) {
    public static void encode(SetPlayerValueControlSettingsPacket packet, FriendlyByteBuf buffer) {
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            buffer.writeDouble(packet.settings.getApplication(type));
        }
    }

    public static SetPlayerValueControlSettingsPacket decode(FriendlyByteBuf buffer) {
        Map<PlayerValueControlType, Double> applications = new EnumMap<>(PlayerValueControlType.class);
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            applications.put(type, buffer.readDouble());
        }
        return new SetPlayerValueControlSettingsPacket(new PlayerValueControlSettings(applications));
    }

    public static void handle(
            SetPlayerValueControlSettingsPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
            ModNetwork.sendPlayerValueControlSettings(player);
            return;
        }
        PlayerValueControlSettings settings = PlayerValueControlUnlockUtility.mergeUnlockedApplications(
                player,
                PlayerValueControlData.getSettings(player),
                packet.settings
        );
        PlayerValueControlData.setSettings(player, settings);
        SwordFlightManager.refreshFlightSpeed(player);
        LightFlightManager.refreshFlightSpeed(player);
        ModNetwork.sendPlayerValueControlSettings(player);
    }
}
