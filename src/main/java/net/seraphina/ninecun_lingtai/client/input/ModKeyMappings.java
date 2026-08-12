package net.seraphina.ninecun_lingtai.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public final class ModKeyMappings {
    public static final KeyMapping MEDITATION = new KeyMapping(
            "key." + ModMain.MOD_ID + ".meditation",
            KeyConflictContext.IN_GAME,
            KeyModifier.SHIFT,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories." + ModMain.MOD_ID
    );

    public static final KeyMapping PRESSURE = new KeyMapping(
            "key." + ModMain.MOD_ID + ".pressure",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            "key.categories." + ModMain.MOD_ID
    );

    public static final KeyMapping SPELL_SELECTION = new KeyMapping(
            "key." + ModMain.MOD_ID + ".spell_selection",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.categories." + ModMain.MOD_ID
    );

    public static final KeyMapping CAST_SPELL = new KeyMapping(
            "key." + ModMain.MOD_ID + ".cast_spell",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories." + ModMain.MOD_ID
    );

    public static final KeyMapping PUSHING_MOUNTAINS = new KeyMapping(
            "key." + ModMain.MOD_ID + ".pushing_mountains",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_GRAVE_ACCENT,
            "key.categories." + ModMain.MOD_ID
    );

    public static final KeyMapping STORAGE_RING_IMMERSION = new KeyMapping(
            "key." + ModMain.MOD_ID + ".storage_ring_immersion",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories." + ModMain.MOD_ID
    );
}
