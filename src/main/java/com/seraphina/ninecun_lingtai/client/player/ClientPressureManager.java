package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SetPressureStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class ClientPressureManager {
    private static final ResourceLocation PRESSURE_EFFECT = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shaders/post/pressure_blur.json"
    );
    private static final String PRESSURE_EFFECT_NAME = PRESSURE_EFFECT.toString();

    private static boolean requestedActive;
    private static boolean affected;
    private static boolean immobilized;
    private static float severity;
    private static UUID controlledPlayerId;
    private static float lockedYRot;
    private static float lockedXRot;
    private static float lockedYHeadRot;
    private static float lockedYBodyRot;
    private static String previousPostEffectName;
    private static boolean pressureEffectInstalled;
    private static PressureSoundInstance pressureSound;

    private ClientPressureManager() {
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        boolean shouldBeActive = player != null
                && minecraft.screen == null
                && ModKeyMappings.PRESSURE.isDown();
        if (shouldBeActive != requestedActive) {
            requestedActive = shouldBeActive;
            ModNetwork.sendToServer(new SetPressureStatePacket(shouldBeActive));
        }

        if (!affected || player == null || !immobilized) {
            return;
        }
        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
        lockPlayerRotation(player);
        ensurePressureEffect();
    }

    public static void setPressure(boolean affected, float severity, boolean immobilized) {
        boolean wasAffected = ClientPressureManager.affected;
        ClientPressureManager.affected = affected;
        ClientPressureManager.severity = Mth.clamp(severity, 0.0F, 1.0F);
        ClientPressureManager.immobilized = immobilized;

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (affected && !wasAffected) {
            if (player != null) {
                startControl(player);
            }
            playPressureSound();
            installPressureEffect();
        } else if (!affected && wasAffected) {
            if (player != null) {
                stopControl(player);
            }
            stopPressureSound();
            restorePreviousPostEffect();
        }
    }

    public static boolean isLocalPlayerAffected() {
        return affected;
    }

    public static boolean isLocalPlayerImmobilized() {
        return affected && immobilized;
    }

    public static boolean rotateCamera(LocalPlayer player, double yRotDelta, double xRotDelta) {
        if (!isControlling(player)) {
            return false;
        }
        return true;
    }

    public static void reset() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null) {
            stopControl(player);
        }
        stopPressureSound();
        restorePreviousPostEffect();
        requestedActive = false;
        affected = false;
        immobilized = false;
        severity = 0.0F;
        controlledPlayerId = null;
    }

    private static void startControl(LocalPlayer player) {
        controlledPlayerId = player.getUUID();
        lockedYRot = player.getYRot();
        lockedXRot = player.getXRot();
        lockedYHeadRot = player.yHeadRot;
        lockedYBodyRot = player.yBodyRot;
        player.stopUsingItem();
        lockPlayerRotation(player);
    }

    private static void stopControl(LocalPlayer player) {
        if (!isControlling(player)) {
            return;
        }
        applyPlayerRotation(player, lockedYRot, lockedXRot, lockedYHeadRot, lockedYBodyRot);
        controlledPlayerId = null;
    }

    private static void lockPlayerRotation(LocalPlayer player) {
        if (isControlling(player)) {
            applyPlayerRotation(player, lockedYRot, lockedXRot, lockedYHeadRot, lockedYBodyRot);
        }
    }

    private static boolean isControlling(Entity entity) {
        return controlledPlayerId != null
                && entity instanceof LocalPlayer
                && controlledPlayerId.equals(entity.getUUID());
    }

    private static void applyPlayerRotation(
            LocalPlayer player,
            float yRot,
            float xRot,
            float yHeadRot,
            float yBodyRot
    ) {
        player.setYRot(yRot);
        player.setXRot(xRot);
        player.setYHeadRot(yHeadRot);
        player.setYBodyRot(yBodyRot);
        player.yRotO = yRot;
        player.xRotO = xRot;
        player.yHeadRotO = yHeadRot;
        player.yBodyRotO = yBodyRot;
    }

    private static void playPressureSound() {
        if (pressureSound != null) {
            return;
        }
        pressureSound = new PressureSoundInstance();
        Minecraft.getInstance().getSoundManager().play(pressureSound);
    }

    private static void stopPressureSound() {
        if (pressureSound != null) {
            pressureSound.finish();
            pressureSound = null;
        }
    }

    private static void installPressureEffect() {
        Minecraft minecraft = Minecraft.getInstance();
        if (pressureEffectInstalled) {
            return;
        }
        PostChain currentEffect = minecraft.gameRenderer.currentEffect();
        if (currentEffect != null && !PRESSURE_EFFECT_NAME.equals(currentEffect.getName())) {
            previousPostEffectName = currentEffect.getName();
        }
        minecraft.gameRenderer.loadEffect(PRESSURE_EFFECT);
        PostChain installedEffect = minecraft.gameRenderer.currentEffect();
        pressureEffectInstalled = installedEffect != null
                && PRESSURE_EFFECT_NAME.equals(installedEffect.getName());
        if (!pressureEffectInstalled) {
            restorePreviousPostEffect();
        }
    }

    private static void ensurePressureEffect() {
        if (!pressureEffectInstalled) {
            installPressureEffect();
        }
    }

    private static void restorePreviousPostEffect() {
        Minecraft minecraft = Minecraft.getInstance();
        PostChain currentEffect = minecraft.gameRenderer.currentEffect();
        if (currentEffect != null && PRESSURE_EFFECT_NAME.equals(currentEffect.getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }
        pressureEffectInstalled = false;
        if (previousPostEffectName != null) {
            ResourceLocation previousEffect = ResourceLocation.tryParse(previousPostEffectName);
            previousPostEffectName = null;
            if (previousEffect != null) {
                minecraft.gameRenderer.loadEffect(previousEffect);
            }
        }
    }
}
