package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.player.ClientShrinkEarthManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SmoothShrinkEarthTeleportPacket(Vec3 from, Vec3 to) {
    public static void encode(SmoothShrinkEarthTeleportPacket packet, FriendlyByteBuf buffer) {
        buffer.writeDouble(packet.from.x);
        buffer.writeDouble(packet.from.y);
        buffer.writeDouble(packet.from.z);
        buffer.writeDouble(packet.to.x);
        buffer.writeDouble(packet.to.y);
        buffer.writeDouble(packet.to.z);
    }

    public static SmoothShrinkEarthTeleportPacket decode(FriendlyByteBuf buffer) {
        return new SmoothShrinkEarthTeleportPacket(
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble())
        );
    }

    public static void handle(SmoothShrinkEarthTeleportPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientShrinkEarthManager.expectSmoothTeleport(packet.from, packet.to)
        );
    }
}
