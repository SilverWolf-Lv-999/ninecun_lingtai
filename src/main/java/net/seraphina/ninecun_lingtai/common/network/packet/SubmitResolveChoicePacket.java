package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialManager;
import net.seraphina.ninecun_lingtai.common.inner_demon.ResolveChoice;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * C→S：决念三选一（心魔即己 §7）。服务端校验：仅在 RESOLVE 相位且发送者是
 * trial owner 时受理（{@link MindTrialManager#submitResolve} 内部保证）。
 *
 * <p><b>尚未注册</b>：ModNetwork 注册见开发日志"需集中接线"。注册前客户端经
 * {@code /mindtrial resolve <choice>} 命令提交（聊天可点按钮已下发）。</p>
 */
public record SubmitResolveChoicePacket(byte choiceOrdinal) {

    public static SubmitResolveChoicePacket of(ResolveChoice choice) {
        return new SubmitResolveChoicePacket((byte) choice.ordinal());
    }

    public static void encode(SubmitResolveChoicePacket packet, FriendlyByteBuf buffer) {
        buffer.writeByte(packet.choiceOrdinal);
    }

    public static SubmitResolveChoicePacket decode(FriendlyByteBuf buffer) {
        return new SubmitResolveChoicePacket(buffer.readByte());
    }

    public static void handle(SubmitResolveChoicePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            ResolveChoice[] choices = ResolveChoice.values();
            if (player != null
                    && packet.choiceOrdinal >= 0
                    && packet.choiceOrdinal < choices.length) {
                MindTrialManager.submitResolve(player, choices[packet.choiceOrdinal]);
            }
        });
        contextSupplier.get().setPacketHandled(true);
    }
}
