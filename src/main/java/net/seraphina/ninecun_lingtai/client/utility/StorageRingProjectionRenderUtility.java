package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Window;
import net.seraphina.ninecun_lingtai.client.render.StorageRingProjectionRenderMeshManager;
import net.seraphina.ninecun_lingtai.client.ring.ClientStorageRingProjectionManager;
import net.seraphina.ninecun_lingtai.common.menu.StorageRingMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.opengl.GL11;

public final class StorageRingProjectionRenderUtility {
    public static Viewport getGuiViewport(int guiLeft, int guiTop) {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        int viewportLeft = guiLeft + StorageRingMenu.VIEWPORT_LEFT;
        int viewportTop = guiTop + StorageRingMenu.VIEWPORT_TOP;
        int viewportRight = viewportLeft + StorageRingMenu.VIEWPORT_WIDTH;
        int viewportBottom = viewportTop + StorageRingMenu.VIEWPORT_HEIGHT;
        int viewportX = toFramebufferCoordinate(
                viewportLeft,
                window.getGuiScaledWidth(),
                window.getWidth()
        );
        int viewportY = window.getHeight() - toFramebufferCoordinate(
                viewportBottom,
                window.getGuiScaledHeight(),
                window.getHeight()
        );
        int viewportWidth = Math.max(
                1,
                toFramebufferCoordinate(viewportRight, window.getGuiScaledWidth(), window.getWidth()) - viewportX
        );
        int viewportHeight = Math.max(
                1,
                toFramebufferCoordinate(viewportBottom, window.getGuiScaledHeight(), window.getHeight())
                        - toFramebufferCoordinate(viewportTop, window.getGuiScaledHeight(), window.getHeight())
        );
        return new Viewport(viewportX, viewportY, viewportWidth, viewportHeight);
    }

    public static Viewport getFullscreenViewport() {
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        return getCoveringViewport(new Viewport(
                0,
                0,
                Math.max(1, StorageRingMenu.VIEWPORT_WIDTH),
                Math.max(1, StorageRingMenu.VIEWPORT_HEIGHT)
        ), window.getWidth(), window.getHeight());
    }

    public static Viewport getCoveringViewport(Viewport viewport) {
        Window window = Minecraft.getInstance().getWindow();
        return getCoveringViewport(viewport, window.getWidth(), window.getHeight());
    }

    public static Viewport interpolate(Viewport start, Viewport end, float progress) {
        float clampedProgress = Math.max(0.0F, Math.min(1.0F, progress));
        return new Viewport(
                Math.round(start.x + (end.x - start.x) * clampedProgress),
                Math.round(start.y + (end.y - start.y) * clampedProgress),
                Math.max(1, Math.round(start.width + (end.width - start.width) * clampedProgress)),
                Math.max(1, Math.round(start.height + (end.height - start.height) * clampedProgress))
        );
    }

    public static void renderGuiProjection(
            GuiGraphics guiGraphics,
            int guiLeft,
            int guiTop,
            float partialTick
    ) {
        int viewportLeft = guiLeft + StorageRingMenu.VIEWPORT_LEFT;
        int viewportTop = guiTop + StorageRingMenu.VIEWPORT_TOP;
        int viewportRight = viewportLeft + StorageRingMenu.VIEWPORT_WIDTH;
        int viewportBottom = viewportTop + StorageRingMenu.VIEWPORT_HEIGHT;
        guiGraphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
        guiGraphics.flush();
        try {
            render(getGuiViewport(guiLeft, guiTop), false, partialTick, false);
        } finally {
            guiGraphics.disableScissor();
        }
    }

    public static void render(
            GuiGraphics guiGraphics,
            Viewport viewport,
            boolean clearColor,
            float partialTick
    ) {
        guiGraphics.flush();
        render(viewport, clearColor, partialTick, false);
    }

    public static void render(Viewport viewport, boolean clearColor, float partialTick) {
        render(viewport, clearColor, partialTick, true);
    }

    private static void render(
            Viewport viewport,
            boolean clearColor,
            float partialTick,
            boolean interpolateCameraPosition
    ) {
        if (viewport.width <= 0 || viewport.height <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        RenderSystem.viewport(viewport.x, viewport.y, viewport.width, viewport.height);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        try {
            RenderSystem.clear(
                    clearColor
                            ? GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT
                            : GL11.GL_DEPTH_BUFFER_BIT,
                    Minecraft.ON_OSX
            );
            StorageRingProjectionRenderMeshManager.render(
                    minecraft.getBlockRenderer(),
                    ClientStorageRingProjectionManager.getBlockStates(),
                    ClientStorageRingProjectionManager.getSnapshotVersion(),
                    interpolateCameraPosition
                            ? ClientStorageRingProjectionManager.getInterpolatedCameraPosition(partialTick)
                            : ClientStorageRingProjectionManager.getCameraPosition(),
                    ClientStorageRingProjectionManager.getCameraYRot(),
                    ClientStorageRingProjectionManager.getCameraXRot(),
                    viewport.width,
                    viewport.height
            );
        } finally {
            RenderSystem.disableDepthTest();
            RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
        }
    }

    private static Viewport getCoveringViewport(Viewport viewport, int framebufferWidth, int framebufferHeight) {
        float scale = Math.max(
                (float) framebufferWidth / viewport.width,
                (float) framebufferHeight / viewport.height
        );
        int width = Math.max(framebufferWidth, (int) Math.ceil(viewport.width * scale));
        int height = Math.max(framebufferHeight, (int) Math.ceil(viewport.height * scale));
        return new Viewport(
                (framebufferWidth - width) / 2,
                (framebufferHeight - height) / 2,
                width,
                height
        );
    }

    private static int toFramebufferCoordinate(int coordinate, int guiSize, int framebufferSize) {
        return (int) Math.round((double) coordinate * framebufferSize / guiSize);
    }

    public record Viewport(int x, int y, int width, int height) {
    }
}
