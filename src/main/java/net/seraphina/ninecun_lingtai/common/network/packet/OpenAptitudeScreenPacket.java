package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.client.screen.ClientAptitudeScreenManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class OpenAptitudeScreenPacket {
    public static final OpenAptitudeScreenPacket INSTANCE = new OpenAptitudeScreenPacket();

    private OpenAptitudeScreenPacket() {
    }

    public static void encode(OpenAptitudeScreenPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenAptitudeScreenPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(OpenAptitudeScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientAptitudeScreenManager::requestOpen);
    }
}
