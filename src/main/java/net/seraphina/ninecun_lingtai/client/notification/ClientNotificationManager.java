package net.seraphina.ninecun_lingtai.client.notification;

import net.seraphina.ninecun_lingtai.client.utility.NotificationAnimationUtility;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class ClientNotificationManager {
    private static final int MAXIMUM_QUEUED_NOTIFICATIONS = 8;
    private static final Deque<Notification> NOTIFICATIONS = new ArrayDeque<>();

    private static Notification activeNotification;
    private static int elapsedTicks;

    public static void accept(Component title, Component content, int durationTicks) {
        Notification notification = new Notification(
                title.copy(),
                content.copy(),
                NotificationAnimationUtility.clampHoldTicks(durationTicks)
        );
        if (activeNotification == null) {
            activeNotification = notification;
            elapsedTicks = 0;
            return;
        }
        if (NOTIFICATIONS.size() >= MAXIMUM_QUEUED_NOTIFICATIONS) {
            NOTIFICATIONS.removeFirst();
        }
        NOTIFICATIONS.addLast(notification);
    }

    public static void tick() {
        if (activeNotification == null) {
            showNextNotification();
            return;
        }
        elapsedTicks++;
        if (elapsedTicks >= NotificationAnimationUtility.getTotalTicks(activeNotification.durationTicks())) {
            activeNotification = null;
            showNextNotification();
        }
    }

    public static Optional<NotificationView> getCurrent(float partialTick) {
        if (activeNotification == null) {
            return Optional.empty();
        }
        return Optional.of(new NotificationView(
                activeNotification.title(),
                activeNotification.content(),
                activeNotification.durationTicks(),
                elapsedTicks + Math.max(0.0F, partialTick)
        ));
    }

    public static void reset() {
        NOTIFICATIONS.clear();
        activeNotification = null;
        elapsedTicks = 0;
    }

    private static void showNextNotification() {
        activeNotification = NOTIFICATIONS.pollFirst();
        elapsedTicks = 0;
    }

    private record Notification(Component title, Component content, int durationTicks) {
    }

    public record NotificationView(
            Component title,
            Component content,
            int durationTicks,
            float ageTicks
    ) {
    }
}
