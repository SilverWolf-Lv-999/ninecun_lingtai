package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.seraphina.ninecun_lingtai.client.screen.TechniqueImprintScreen;
import net.seraphina.ninecun_lingtai.utility.TechniqueNetworkUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public record OpenTechniqueImprintScreenPacket(
        InteractionHand hand,
        List<TechniqueClientData> techniques
) {
    public OpenTechniqueImprintScreenPacket {
        techniques = List.copyOf(techniques);
    }

    public static void encode(OpenTechniqueImprintScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        TechniqueNetworkUtility.writeTechniques(buffer, packet.techniques);
    }

    public static OpenTechniqueImprintScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenTechniqueImprintScreenPacket(
                buffer.readEnum(InteractionHand.class),
                TechniqueNetworkUtility.readTechniques(buffer)
        );
    }

    public static void handle(
            OpenTechniqueImprintScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> Minecraft.getInstance().setScreen(
                        new TechniqueImprintScreen(packet.hand, packet.techniques)
                )
        );
    }
}
