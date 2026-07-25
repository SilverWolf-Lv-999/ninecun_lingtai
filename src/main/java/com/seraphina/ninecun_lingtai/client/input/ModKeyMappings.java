package com.seraphina.ninecun_lingtai.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.seraphina.ninecun_lingtai.ModMain;
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

    private ModKeyMappings() {
    }
}
