package net.seraphina.ninecun_lingtai.client.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;

public final class CausalityBlurUtility {
    private static final ResourceLocation BLUR_EFFECT = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shaders/post/aptitude_blur.json"
    );
    private static final String BLUR_EFFECT_NAME = BLUR_EFFECT.toString();

    private static String previousPostEffectName;
    private static boolean effectInstalled;
    private static boolean blurLoadFailed;

    public static void install() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || effectInstalled || blurLoadFailed) {
            return;
        }

        PostChain currentEffect = minecraft.gameRenderer.currentEffect();
        if (currentEffect != null
                && !BLUR_EFFECT_NAME.equals(currentEffect.getName())
                && previousPostEffectName == null) {
            previousPostEffectName = currentEffect.getName();
        }
        loadBlurEffect(minecraft);
    }

    public static void ensureInstalled() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || blurLoadFailed) {
            return;
        }
        PostChain currentEffect = minecraft.gameRenderer.currentEffect();
        if (currentEffect == null || !BLUR_EFFECT_NAME.equals(currentEffect.getName())) {
            loadBlurEffect(minecraft);
        } else {
            effectInstalled = true;
        }
    }

    public static void restore() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return;
        }

        PostChain currentEffect = minecraft.gameRenderer.currentEffect();
        if (currentEffect != null && BLUR_EFFECT_NAME.equals(currentEffect.getName())) {
            minecraft.gameRenderer.shutdownEffect();
        }
        restorePreviousPostEffect(minecraft);
        effectInstalled = false;
        blurLoadFailed = false;
    }

    private static void loadBlurEffect(Minecraft minecraft) {
        try {
            minecraft.gameRenderer.loadEffect(BLUR_EFFECT);
            PostChain installedEffect = minecraft.gameRenderer.currentEffect();
            effectInstalled = installedEffect != null
                    && BLUR_EFFECT_NAME.equals(installedEffect.getName());
            if (!effectInstalled) {
                blurLoadFailed = true;
                ModMain.LOGGER.warn("Unable to install causality blur post effect");
                restorePreviousPostEffect(minecraft);
            }
        } catch (RuntimeException exception) {
            effectInstalled = false;
            blurLoadFailed = true;
            restorePreviousPostEffect(minecraft);
            ModMain.LOGGER.warn("Unable to load causality blur post effect", exception);
        }
    }

    private static void restorePreviousPostEffect(Minecraft minecraft) {
        PostChain currentEffect = minecraft.gameRenderer.currentEffect();
        if (currentEffect != null && BLUR_EFFECT_NAME.equals(currentEffect.getName())) {
            minecraft.gameRenderer.shutdownEffect();
            currentEffect = null;
        }

        if (previousPostEffectName == null) {
            return;
        }

        String currentEffectName = currentEffect == null ? null : currentEffect.getName();
        ResourceLocation previousEffect = ResourceLocation.tryParse(previousPostEffectName);
        String effectToRestore = previousPostEffectName;
        previousPostEffectName = null;
        if (previousEffect != null && !previousEffect.toString().equals(currentEffectName)) {
            try {
                minecraft.gameRenderer.loadEffect(previousEffect);
            } catch (RuntimeException exception) {
                ModMain.LOGGER.warn("Unable to restore previous post effect {}", effectToRestore, exception);
            }
        } else if (currentEffectName == null && previousEffect == null) {
            ModMain.LOGGER.warn("Unable to restore previous post effect {}", effectToRestore);
        }
    }
}
