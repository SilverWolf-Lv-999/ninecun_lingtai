package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaRealmManager;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 由施法者请求放出自己佛国中的一名在线玩家。
 *
 * <p>请求只携带目标 UUID；归属、在线状态、原位置和实际迁移均由服务端
 * {@link PalmBuddhaRealmManager} 权威处理。</p>
 */
public record ReleasePalmBuddhaMemberPacket(UUID memberId) {
    public ReleasePalmBuddhaMemberPacket {
        Objects.requireNonNull(memberId, "memberId");
    }

    public static void encode(
            ReleasePalmBuddhaMemberPacket packet,
            FriendlyByteBuf buffer
    ) {
        buffer.writeUUID(packet.memberId());
    }

    public static ReleasePalmBuddhaMemberPacket decode(FriendlyByteBuf buffer) {
        return new ReleasePalmBuddhaMemberPacket(buffer.readUUID());
    }

    public static void handle(
            ReleasePalmBuddhaMemberPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer owner = context.getSender();
            if (owner != null) {
                PalmBuddhaRealmManager.get(owner.server).releasePlayer(owner, packet.memberId());
            }
        });
        context.setPacketHandled(true);
    }
}
