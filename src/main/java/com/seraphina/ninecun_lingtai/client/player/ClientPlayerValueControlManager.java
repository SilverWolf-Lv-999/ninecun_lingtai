package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.RequestPlayerValueControlSettingsPacket;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlData;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlSettings;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.Minecraft;

public final class ClientPlayerValueControlManager {
    private static PlayerValueControlSettings settings = PlayerValueControlSettings.defaultSettings();
    private static Set<PlayerValueControlType> unlockedTypes = Set.of();
    private static boolean initialized;
    private static int revision;

    private ClientPlayerValueControlManager() {
    }

    public static void beginOpen() {
        initialized = false;
        requestRefresh();
    }

    public static void requestRefresh() {
        ModNetwork.sendToServer(RequestPlayerValueControlSettingsPacket.INSTANCE);
    }

    public static void accept(Set<PlayerValueControlType> updatedUnlockedTypes) {
        settings = readLocalSettings();
        unlockedTypes = copyUnlockedTypes(updatedUnlockedTypes);
        initialized = true;
        revision++;
    }

    public static PlayerValueControlSettings getSettings() {
        settings = readLocalSettings();
        return settings;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isUnlocked(PlayerValueControlType type) {
        return unlockedTypes.contains(type);
    }

    public static boolean hasUnlockedType() {
        return !unlockedTypes.isEmpty();
    }

    public static int getRevision() {
        return revision;
    }

    public static void reset() {
        settings = PlayerValueControlSettings.defaultSettings();
        unlockedTypes = Set.of();
        initialized = false;
        revision++;
    }

    private static PlayerValueControlSettings readLocalSettings() {
        if (Minecraft.getInstance().player == null) {
            return settings;
        }
        return PlayerValueControlData.getSettings(Minecraft.getInstance().player);
    }

    private static Set<PlayerValueControlType> copyUnlockedTypes(Set<PlayerValueControlType> types) {
        if (types == null || types.isEmpty()) {
            return Set.of();
        }
        EnumSet<PlayerValueControlType> copiedTypes = EnumSet.noneOf(PlayerValueControlType.class);
        copiedTypes.addAll(types);
        return Set.copyOf(copiedTypes);
    }
}
