package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.ScreenShaderEffects;

/**
 * Drives the local player's real-dimension transition for Palm Buddha.
 *
 * <p>This manager never creates a second camera or a substitute world. The
 * server's respawn packet remains the source of truth; the shader only veils
 * the frames rendered before and after that real dimension change.</p>
 */
public final class ClientPalmBuddhaTransitionManager {
    private static final int TRANSITION_TOTAL_TICKS = 24;
    private static final int TRANSITION_TIMEOUT_TICKS = 40;
    private static final String PALM_BUDDHA_REALM_PATH_PREFIX = "palm_buddha_";

    private static int thisTransitionTick = -1;
    private static ResourceLocation thisExpectedDimension;

    private ClientPalmBuddhaTransitionManager() {
    }

    /**
     * Starts a veil only when the actual server transition touches a Palm
     * Buddha realm. Ordinary Nether, End, and portal respawns are untouched.
     */
    public static void begin(ResourceLocation sourceDimension, ResourceLocation destinationDimension) {
        if (!isPalmBuddhaRealm(sourceDimension) && !isPalmBuddhaRealm(destinationDimension)) {
            return;
        }
        thisExpectedDimension = destinationDimension;
        thisTransitionTick = 0;
        ScreenShaderEffects.setPalmBuddhaTransitionActive(true);
    }

    public static void tick() {
        if (thisTransitionTick < 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (minecraft.player == null || clientLevel == null) {
            reset();
            return;
        }

        thisTransitionTick++;
        boolean reachedExpectedDimension = thisExpectedDimension != null
                && thisExpectedDimension.equals(clientLevel.dimension().location());
        if ((reachedExpectedDimension && thisTransitionTick >= TRANSITION_TOTAL_TICKS)
                || thisTransitionTick >= TRANSITION_TIMEOUT_TICKS) {
            reset();
        }
    }

    public static void reset() {
        thisTransitionTick = -1;
        thisExpectedDimension = null;
        ScreenShaderEffects.setPalmBuddhaTransitionActive(false);
    }

    public static boolean isActive() {
        return thisTransitionTick >= 0;
    }

    public static float getProgress(float partialTicks) {
        if (!isActive()) {
            return 0.0F;
        }
        float progress = (thisTransitionTick + Math.max(0.0F, partialTicks))
                / (float) TRANSITION_TOTAL_TICKS;
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    public static float getTransitionTime(float partialTicks) {
        if (!isActive()) {
            return 0.0F;
        }
        return thisTransitionTick + Math.max(0.0F, partialTicks);
    }

    public static float getSoundVolumeFactor() {
        if (!isActive()) {
            return 1.0F;
        }
        float progress = getProgress(0.0F);
        float fadeIn = smoothStep(0.0F, 0.44F, progress);
        float fadeOut = 1.0F - smoothStep(0.56F, 1.0F, progress);
        return Math.max(0.04F, Math.min(1.0F, Math.min(fadeIn, fadeOut)));
    }

    private static float smoothStep(float edge0, float edge1, float value) {
        float normalized = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return normalized * normalized * (3.0F - 2.0F * normalized);
    }

    public static boolean isPalmBuddhaRealm(ResourceLocation dimension) {
        return dimension != null
                && ModMain.MOD_ID.equals(dimension.getNamespace())
                && dimension.getPath().startsWith(PALM_BUDDHA_REALM_PATH_PREFIX);
    }
}
