package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.animation.BreakthroughAnimationManager;
import net.seraphina.ninecun_lingtai.common.animation.BreakthroughAnimationType;

import java.util.Optional;
import java.util.function.Supplier;

public record BreakthroughAnimationPacket(BreakthroughAnimationType animationType, int elementMask) {
    public static void encode(BreakthroughAnimationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.animationType.ordinal());
        buffer.writeVarInt(packet.elementMask);
    }

    public static BreakthroughAnimationPacket decode(FriendlyByteBuf buffer) {
        Optional<BreakthroughAnimationType> animationType = BreakthroughAnimationType.fromOrdinal(
                buffer.readVarInt()
        );
        int elementMask = buffer.readVarInt();
        return new BreakthroughAnimationPacket(
                animationType.orElse(BreakthroughAnimationType.MORTAL_TO_QI),
                elementMask
        );
    }

    public static void handle(
            BreakthroughAnimationPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> BreakthroughAnimationManager.start(
                        packet.animationType,
                        packet.elementMask
                )
        );
    }
}
