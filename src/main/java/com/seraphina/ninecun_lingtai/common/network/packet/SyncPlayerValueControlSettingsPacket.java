package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientPlayerValueControlManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;

public record SyncPlayerValueControlSettingsPacket(Set<PlayerValueControlType> unlockedTypes) {
    public SyncPlayerValueControlSettingsPacket {
        unlockedTypes = unlockedTypes == null ? Set.of() : Set.copyOf(unlockedTypes);
    }

    public static void encode(SyncPlayerValueControlSettingsPacket packet, FriendlyByteBuf buffer) {
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            buffer.writeBoolean(packet.unlockedTypes.contains(type));
        }
    }

    public static SyncPlayerValueControlSettingsPacket decode(FriendlyByteBuf buffer) {
        Set<PlayerValueControlType> unlockedTypes = EnumSet.noneOf(PlayerValueControlType.class);
        for (PlayerValueControlType type : PlayerValueControlType.values()) {
            if (buffer.readBoolean()) {
                unlockedTypes.add(type);
            }
        }
        return new SyncPlayerValueControlSettingsPacket(unlockedTypes);
    }

    public static void handle(
            SyncPlayerValueControlSettingsPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPlayerValueControlManager.accept(packet.unlockedTypes)
        );
    }
}
