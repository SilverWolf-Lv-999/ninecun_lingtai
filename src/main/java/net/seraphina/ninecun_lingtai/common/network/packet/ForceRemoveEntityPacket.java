package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;

import java.util.function.Supplier;

public record ForceRemoveEntityPacket(int entityId) {
    public static void encode(ForceRemoveEntityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
    }

    public static ForceRemoveEntityPacket decode(FriendlyByteBuf buffer) {
        return new ForceRemoveEntityPacket(buffer.readVarInt());
    }

    public static void handle(
            ForceRemoveEntityPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    ClientLevel level = minecraft.level;
                    if (level == null) {
                        return;
                    }
                    Entity entity = level.getEntity(packet.entityId);
                    if (entity != null) {
                        EntityUtility.removedFromClient(level, entity);
                    }
                }
        );
    }
}
