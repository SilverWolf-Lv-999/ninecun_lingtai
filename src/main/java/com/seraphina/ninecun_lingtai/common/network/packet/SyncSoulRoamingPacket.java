package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.client.player.ClientSoulRoamingManager;
import com.seraphina.ninecun_lingtai.common.player.SoulRoamingSyncData;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSoulRoamingPacket(SoulRoamingSyncData syncData) {
    public static void encode(SyncSoulRoamingPacket packet, FriendlyByteBuf buffer) {
        SoulRoamingSyncData syncData = packet.syncData;
        buffer.writeBoolean(syncData.active());
        buffer.writeResourceLocation(syncData.bodyDimension().location());
        buffer.writeDouble(syncData.bodyPosition().x);
        buffer.writeDouble(syncData.bodyPosition().y);
        buffer.writeDouble(syncData.bodyPosition().z);
        buffer.writeVarInt(syncData.bodyEntityId());
        buffer.writeBoolean(syncData.bodyExists());
    }

    public static SyncSoulRoamingPacket decode(FriendlyByteBuf buffer) {
        boolean active = buffer.readBoolean();
        ResourceKey<Level> bodyDimension = ResourceKey.create(
                Registries.DIMENSION,
                buffer.readResourceLocation()
        );
        Vec3 bodyPosition = new Vec3(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
        int bodyEntityId = buffer.readVarInt();
        boolean bodyExists = buffer.readBoolean();
        if (!active) {
            return new SyncSoulRoamingPacket(SoulRoamingSyncData.inactive());
        }
        return new SyncSoulRoamingPacket(new SoulRoamingSyncData(
                true,
                bodyDimension,
                bodyPosition,
                bodyEntityId,
                bodyExists
        ));
    }

    public static void handle(
            SyncSoulRoamingPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientSoulRoamingManager.accept(packet.syncData)
        );
    }
}
