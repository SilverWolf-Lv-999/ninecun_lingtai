package net.seraphina.ninecun_lingtai.client.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.seraphina.ninecun_lingtai.common.registry.ModSounds;

public final class BreakthroughAnimationSoundUtility {
    private static SoundInstance waterDropSound;
    private static SoundInstance vortexSound;

    public static void playWaterDrop() {
        stopWaterDrop();
        waterDropSound = SimpleSoundInstance.forUI(
                ModSounds.BREAKTHROUGH_WATER_DROP.get(),
                1.0F,
                1.0F
        );
        Minecraft.getInstance().getSoundManager().play(waterDropSound);
    }

    public static void playVortex() {
        stopVortex();
        vortexSound = SimpleSoundInstance.forUI(
                ModSounds.BREAKTHROUGH_VORTEX.get(),
                1.0F,
                1.0F
        );
        Minecraft.getInstance().getSoundManager().play(vortexSound);
    }

    public static void stopAll() {
        stopWaterDrop();
        stopVortex();
    }

    private static void stopWaterDrop() {
        if (waterDropSound != null) {
            Minecraft.getInstance().getSoundManager().stop(waterDropSound);
            waterDropSound = null;
        }
    }

    private static void stopVortex() {
        if (vortexSound != null) {
            Minecraft.getInstance().getSoundManager().stop(vortexSound);
            vortexSound = null;
        }
    }
}
