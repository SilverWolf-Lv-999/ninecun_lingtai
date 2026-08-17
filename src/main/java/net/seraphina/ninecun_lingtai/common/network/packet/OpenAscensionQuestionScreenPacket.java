package net.seraphina.ninecun_lingtai.common.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.seraphina.ninecun_lingtai.client.screen.ClientAscensionQuestionScreenManager;
import net.seraphina.ninecun_lingtai.utility.ChineseNumeralUtility;

import java.util.Objects;
import java.util.function.Supplier;

public record OpenAscensionQuestionScreenPacket(
        int questionIndex,
        int questionTotal,
        String title,
        String prompt
) {
    public static final int MAXIMUM_TITLE_LENGTH = 128;
    public static final int MAXIMUM_PROMPT_LENGTH = 4_096;

    public OpenAscensionQuestionScreenPacket {
        questionIndex = Math.max(0, questionIndex);
        questionTotal = Math.max(1, questionTotal);
        title = ChineseNumeralUtility.replaceArabicNumerals(Objects.requireNonNullElse(title, ""));
        prompt = ChineseNumeralUtility.replaceArabicNumerals(Objects.requireNonNullElse(prompt, ""));
    }

    public static void encode(OpenAscensionQuestionScreenPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.questionIndex);
        buffer.writeInt(packet.questionTotal);
        buffer.writeUtf(packet.title, MAXIMUM_TITLE_LENGTH);
        buffer.writeUtf(packet.prompt, MAXIMUM_PROMPT_LENGTH);
    }

    public static OpenAscensionQuestionScreenPacket decode(FriendlyByteBuf buffer) {
        return new OpenAscensionQuestionScreenPacket(
                buffer.readInt(),
                buffer.readInt(),
                buffer.readUtf(MAXIMUM_TITLE_LENGTH),
                buffer.readUtf(MAXIMUM_PROMPT_LENGTH)
        );
    }

    public static void handle(
            OpenAscensionQuestionScreenPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientAscensionQuestionScreenManager.open(packet)
        );
    }
}
