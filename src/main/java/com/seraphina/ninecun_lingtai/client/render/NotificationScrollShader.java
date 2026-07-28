package com.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public final class NotificationScrollShader {
    @Nullable
    private static ShaderInstance notificationScrollShader;

    private NotificationScrollShader() {
    }

    @Nullable
    public static ShaderInstance getNotificationScrollShader() {
        return notificationScrollShader;
    }

    public static void setNotificationScrollShader(ShaderInstance shaderInstance) {
        notificationScrollShader = shaderInstance;
    }
}
