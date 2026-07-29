package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.Objects;

public final class NotificationUtility {
    public static final int DEFAULT_DURATION_TICKS = 20 * 4;
    public static final int MINIMUM_DURATION_TICKS = 20;
    public static final int MAXIMUM_DURATION_TICKS = 20 * 60;

    private NotificationUtility() {
        throw new IllegalStateException("哎呀我去你干什么呢");
    }

    public static void send(ServerPlayer player, Component title, Component content) {
        send(player, title, content, DEFAULT_DURATION_TICKS);
    }

    public static void send(
            ServerPlayer player,
            Component title,
            Component content,
            int durationTicks
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        ModNetwork.sendNotification(
                player,
                title,
                content,
                clampDurationTicks(durationTicks)
        );
    }

    public static int clampDurationTicks(int durationTicks) {
        return Mth.clamp(durationTicks, MINIMUM_DURATION_TICKS, MAXIMUM_DURATION_TICKS);
    }
}
