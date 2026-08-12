package com.seraphina.ninecun_lingtai.client.ring;

import com.seraphina.ninecun_lingtai.client.screen.StorageRingScreen;
import com.seraphina.ninecun_lingtai.client.utility.StorageRingProjectionRenderUtility;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.StorageRingInteractionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.UUID;

public final class ClientStorageRingImmersionManager {
    private static final long TRANSITION_DURATION_NANOS = 300_000_000L;
    private static final float MOUSE_ROTATION_SENSITIVITY = 0.15F;

    private static State thisState = State.INACTIVE;
    private static StorageRingScreen thisStorageRingScreen;
    private static UUID thisRingId;
    private static long thisTransitionStartNanos;

    private ClientStorageRingImmersionManager() {
    }

    public static boolean beginEntering(StorageRingScreen screen) {
        if (thisState != State.INACTIVE
                || !ClientStorageRingProjectionManager.isActiveFor(screen.getRingId())) {
            return false;
        }

        thisState = State.ENTERING;
        thisStorageRingScreen = screen;
        thisRingId = screen.getRingId();
        thisTransitionStartNanos = System.nanoTime();
        return true;
    }

    public static boolean beginExiting() {
        if (thisState != State.IMMERSIVE || thisStorageRingScreen == null || thisRingId == null) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (!ClientStorageRingProjectionManager.isActiveFor(thisRingId)
                || minecraft.player == null) {
            reset();
            return false;
        }
        if (minecraft.screen != null) {
            return false;
        }

        ClientStorageRingProjectionManager.sendCameraInput(thisRingId, 0.0F, 0.0F, false, false);
        thisStorageRingScreen.clearProjectionMovementInput();
        thisState = State.EXITING;
        thisTransitionStartNanos = System.nanoTime();
        minecraft.setScreen(thisStorageRingScreen);
        return true;
    }

    public static void tick() {
        if (thisState == State.INACTIVE) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (thisRingId == null
                || !ClientStorageRingProjectionManager.isActiveFor(thisRingId)
                || minecraft.player == null) {
            reset();
            return;
        }

        if (thisState == State.ENTERING) {
            if (minecraft.screen != thisStorageRingScreen) {
                reset();
                return;
            }
            if (isTransitionFinished()) {
                thisStorageRingScreen.clearProjectionMovementInput();
                ClientStorageRingProjectionManager.sendCameraInput(thisRingId, 0.0F, 0.0F, false, false);
                thisState = State.IMMERSIVE;
                thisTransitionStartNanos = 0L;
                minecraft.setScreen(null);
            }
            return;
        }

        if (thisState == State.IMMERSIVE) {
            return;
        }

        if (minecraft.screen != thisStorageRingScreen) {
            reset();
            return;
        }
        if (isTransitionFinished()) {
            reset();
        }
    }

    public static boolean renderScreenTransition(
            GuiGraphics guiGraphics,
            StorageRingScreen screen,
            float partialTick
    ) {
        if (screen != thisStorageRingScreen
                || (thisState != State.ENTERING && thisState != State.EXITING)
                || !ClientStorageRingProjectionManager.isActiveFor(screen.getRingId())) {
            return false;
        }

        StorageRingProjectionRenderUtility.Viewport initialViewport =
                StorageRingProjectionRenderUtility.getGuiViewport(screen.getLeftPos(), screen.getTopPos());
        StorageRingProjectionRenderUtility.Viewport fullscreenViewport =
                StorageRingProjectionRenderUtility.getCoveringViewport(initialViewport);
        float progress = getSmoothedProgress();
        StorageRingProjectionRenderUtility.Viewport viewport = thisState == State.ENTERING
                ? StorageRingProjectionRenderUtility.interpolate(initialViewport, fullscreenViewport, progress)
                : StorageRingProjectionRenderUtility.interpolate(fullscreenViewport, initialViewport, progress);
        StorageRingProjectionRenderUtility.render(guiGraphics, viewport, false, partialTick);
        return true;
    }

    public static void renderImmersive(RenderLevelStageEvent event) {
        if (!isImmersiveProjectionActive()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES
                || thisRingId == null) {
            return;
        }
        StorageRingProjectionRenderUtility.render(
                StorageRingProjectionRenderUtility.getFullscreenViewport(),
                true,
                event.getPartialTick()
        );
    }

    public static boolean handleMovementInput(Input input) {
        if (!isImmersive()) {
            return false;
        }

        ClientStorageRingProjectionManager.sendCameraInput(
                thisRingId,
                Mth.clamp(input.leftImpulse, -1.0F, 1.0F),
                Mth.clamp(input.forwardImpulse, -1.0F, 1.0F),
                input.jumping,
                input.shiftKeyDown
        );
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
        return true;
    }

    public static boolean rotateCamera(LocalPlayer player, double yRotDelta, double xRotDelta) {
        if (!isImmersive() || Minecraft.getInstance().player != player) {
            return false;
        }

        ClientStorageRingProjectionManager.rotate(
                thisRingId,
                yRotDelta,
                xRotDelta,
                MOUSE_ROTATION_SENSITIVITY
        );
        return true;
    }

    public static boolean handleInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isImmersive()) {
            return false;
        }

        if (event.isAttack() || event.isUseItem()) {
            boolean use = event.isUseItem();
            Minecraft minecraft = Minecraft.getInstance();
            ClientStorageRingProjectionManager.ProjectionTarget target = ClientStorageRingProjectionManager.pick(
                    thisRingId,
                    0,
                    0,
                    minecraft.getWindow().getWidth(),
                    minecraft.getWindow().getHeight(),
                    minecraft.getWindow().getWidth() * 0.5D,
                    minecraft.getWindow().getHeight() * 0.5D
            );
            if (target != null) {
                if (!use) {
                    ClientStorageRingProjectionManager.removeBlock(thisRingId, target.localPosition());
                }
                ModNetwork.sendToServer(new StorageRingInteractionPacket(
                        target.localPosition(),
                        target.direction(),
                        target.localHitLocation(),
                        use
                ));
            }
        }
        event.setSwingHand(false);
        event.setCanceled(true);
        return true;
    }

    public static boolean isTransitioningFor(StorageRingScreen screen) {
        return screen == thisStorageRingScreen
                && (thisState == State.ENTERING || thisState == State.EXITING);
    }

    public static boolean isImmersive() {
        return isImmersiveProjectionActive()
                && Minecraft.getInstance().screen == null;
    }

    public static boolean isImmersiveProjectionActive() {
        return thisState == State.IMMERSIVE
                && thisRingId != null
                && ClientStorageRingProjectionManager.isActiveFor(thisRingId);
    }

    public static void cancelForScreen(StorageRingScreen screen) {
        if (screen == thisStorageRingScreen) {
            reset();
        }
    }

    public static void reset() {
        thisState = State.INACTIVE;
        thisStorageRingScreen = null;
        thisRingId = null;
        thisTransitionStartNanos = 0L;
    }

    private static boolean isTransitionFinished() {
        return thisTransitionStartNanos != 0L
                && System.nanoTime() - thisTransitionStartNanos >= TRANSITION_DURATION_NANOS;
    }

    private static float getSmoothedProgress() {
        if (thisTransitionStartNanos == 0L) {
            return 1.0F;
        }
        float progress = Mth.clamp(
                (System.nanoTime() - thisTransitionStartNanos) / (float) TRANSITION_DURATION_NANOS,
                0.0F,
                1.0F
        );
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private enum State {
        INACTIVE,
        ENTERING,
        IMMERSIVE,
        EXITING
    }
}
