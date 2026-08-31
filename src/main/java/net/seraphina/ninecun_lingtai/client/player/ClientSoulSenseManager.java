package net.seraphina.ninecun_lingtai.client.player;

import net.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import net.seraphina.ninecun_lingtai.client.screen.SoulSenseFilterScreen;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SetSoulSenseStatePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncSoulSensePacket;
import net.seraphina.ninecun_lingtai.common.soul.SoulSenseTargetCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Map;

public final class ClientSoulSenseManager {
    private static boolean active;
    private static int filterMask = SoulSenseTargetCategory.getFullMask();
    private static List<SyncSoulSensePacket.EntityEntry> entityEntries = List.of();
    private static Map<BlockPos, Byte> blockEntries = Map.of();

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        while (ModKeyMappings.SOUL_SENSE.consumeClick()) {
            if (minecraft.screen == null) {
                ModNetwork.sendToServer(new SetSoulSenseStatePacket(!active));
            }
        }
        while (ModKeyMappings.SOUL_SENSE_FILTER.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new SoulSenseFilterScreen(filterMask));
            }
        }
    }

    public static void applySync(
            boolean syncedActive,
            int syncedFilterMask,
            List<SyncSoulSensePacket.EntityEntry> entities,
            List<SyncSoulSensePacket.BlockEntry> blocks
    ) {
        active = syncedActive;
        filterMask = syncedFilterMask;
        if (!syncedActive) {
            entityEntries = List.of();
            blockEntries = Map.of();
            return;
        }
        entityEntries = List.copyOf(entities);
        blockEntries = blocks.stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        SyncSoulSensePacket.BlockEntry::pos,
                        SyncSoulSensePacket.BlockEntry::categoryOrdinal,
                        (first, second) -> first
                ));
    }

    public static boolean isActive() {
        return active;
    }

    public static int getFilterMask() {
        return filterMask;
    }

    public static List<SyncSoulSensePacket.EntityEntry> getEntityEntries() {
        return entityEntries;
    }

    public static Map<BlockPos, Byte> getBlockEntries() {
        return blockEntries;
    }

    public static void reset() {
        active = false;
        filterMask = SoulSenseTargetCategory.getFullMask();
        entityEntries = List.of();
        blockEntries = Map.of();
    }
}
