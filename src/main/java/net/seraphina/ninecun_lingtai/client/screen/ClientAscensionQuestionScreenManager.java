package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.Minecraft;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenAscensionQuestionScreenPacket;

public final class ClientAscensionQuestionScreenManager {
    private ClientAscensionQuestionScreenManager() {
    }

    public static void open(OpenAscensionQuestionScreenPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AscensionQuestionScreen screen
                && screen.representsQuestion(
                packet.questionIndex(),
                packet.questionTotal(),
                packet.title(),
                packet.prompt()
        )) {
            return;
        }
        minecraft.setScreen(new AscensionQuestionScreen(
                packet.questionIndex(),
                packet.questionTotal(),
                packet.title(),
                packet.prompt()
        ));
    }

    public static void close() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AscensionQuestionScreen) {
            minecraft.setScreen(null);
        }
    }

    public static void reset() {
    }
}
