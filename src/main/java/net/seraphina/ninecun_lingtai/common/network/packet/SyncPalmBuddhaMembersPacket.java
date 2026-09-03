package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.player.ClientPalmBuddhaMemberManager;
import net.seraphina.ninecun_lingtai.common.network.PalmBuddhaMemberPacketUtility;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaMemberPageSnapshot;

import java.util.function.Supplier;

/** 服务端返回的一页佛国成员摘要；只发给发起请求的施法者。 */
public record SyncPalmBuddhaMembersPacket(PalmBuddhaMemberPageSnapshot snapshot) {
    public static void encode(
            SyncPalmBuddhaMembersPacket packet,
            FriendlyByteBuf buffer
    ) {
        PalmBuddhaMemberPacketUtility.writePage(buffer, packet.snapshot());
    }

    public static SyncPalmBuddhaMembersPacket decode(FriendlyByteBuf buffer) {
        return new SyncPalmBuddhaMembersPacket(
                PalmBuddhaMemberPacketUtility.readPage(buffer)
        );
    }

    public static void handle(
            SyncPalmBuddhaMembersPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPalmBuddhaMemberManager.accept(packet.snapshot())
        ));
        context.setPacketHandled(true);
    }
}
