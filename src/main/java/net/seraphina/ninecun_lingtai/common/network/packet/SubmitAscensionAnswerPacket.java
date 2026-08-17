package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.common.ascension.AscensionManager;

import java.util.function.Supplier;

public record SubmitAscensionAnswerPacket(int questionIndex, String answer) {
    public static final int MAXIMUM_ANSWER_LENGTH = 2_000;

    public SubmitAscensionAnswerPacket {
        answer = answer == null ? "" : answer;
    }

    public static void encode(SubmitAscensionAnswerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.questionIndex);
        buffer.writeUtf(packet.answer, MAXIMUM_ANSWER_LENGTH);
    }

    public static SubmitAscensionAnswerPacket decode(FriendlyByteBuf buffer) {
        return new SubmitAscensionAnswerPacket(
                buffer.readInt(),
                buffer.readUtf(MAXIMUM_ANSWER_LENGTH)
        );
    }

    public static void handle(
            SubmitAscensionAnswerPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player != null) {
            AscensionManager.submitAnswer(player, packet.questionIndex, packet.answer);
        }
    }
}
