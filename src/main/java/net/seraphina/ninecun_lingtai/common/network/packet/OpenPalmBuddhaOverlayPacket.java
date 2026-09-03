package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaOverlayManager;

import java.util.function.Supplier;

/** Opens the owner's shader-backed Palm Buddha member overlay. */
public final class OpenPalmBuddhaOverlayPacket {
    public static final OpenPalmBuddhaOverlayPacket INSTANCE = new OpenPalmBuddhaOverlayPacket();

    private OpenPalmBuddhaOverlayPacket() {
    }

    public static void encode(OpenPalmBuddhaOverlayPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenPalmBuddhaOverlayPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            OpenPalmBuddhaOverlayPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> ClientPalmBuddhaOverlayManager::open
        ));
        context.setPacketHandled(true);
    }
}
