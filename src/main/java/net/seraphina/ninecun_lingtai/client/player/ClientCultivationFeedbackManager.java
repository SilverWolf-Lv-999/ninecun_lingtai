package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class ClientCultivationFeedbackManager {
    private static int revision;
    private static boolean success;
    private static Component message;

    private ClientCultivationFeedbackManager() {
    }

    public static void accept(boolean updatedSuccess, Component updatedMessage) {
        success = updatedSuccess;
        message = updatedMessage;
        revision++;
    }

    public static int getRevision() {
        return revision;
    }

    public static boolean isSuccess() {
        return success;
    }

    public static Optional<Component> getMessage() {
        return Optional.ofNullable(message);
    }

    public static void reset() {
        success = false;
        message = null;
        revision++;
    }
}
