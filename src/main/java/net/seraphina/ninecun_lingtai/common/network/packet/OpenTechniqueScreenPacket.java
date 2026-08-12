package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import net.seraphina.ninecun_lingtai.utility.TechniqueNetworkUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenTechniqueScreenPacket(TechniqueClientData technique, boolean imprinted) {
    public static void encode(OpenTechniqueScreenPacket packet, FriendlyByteBuf buffer) {
        TechniqueNetworkUtility.writeTechnique(buffer, packet.technique);
        buffer.writeBoolean(packet.imprinted);
    }

    public static OpenTechniqueScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenTechniqueScreenPacket(
                TechniqueNetworkUtility.readTechnique(buffer),
                buffer.readBoolean()
        );
    }

    public static void handle(
            OpenTechniqueScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientTechniqueManager.openTechnique(packet.technique, packet.imprinted)
        );
    }
}
