package com.seraphina.ninecun_lingtai.client.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.seraphina.ninecun_lingtai.client.utility.FontCacheUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class CustomFontManager {
    private static FontSet shanBeiFontSet;
    private static Path cachedFontPath;

    private CustomFontManager() {
    }

    public static SimplePreparableReloadListener<Path> createReloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Path prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                try {
                    return FontCacheUtility.copyShanBeiFontToCache(resourceManager);
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to copy the Shan Bei font into the runtime cache", exception);
                }
            }

            @Override
            protected void apply(Path fontPath, ResourceManager resourceManager, ProfilerFiller profiler) {
                reloadFontSet(fontPath);
            }
        };
    }

    public static synchronized FontSet getFontSet() {
        RenderSystem.assertOnRenderThreadOrInit();
        if (shanBeiFontSet == null) {
            Path fontPath = cachedFontPath;
            if (fontPath == null) {
                try {
                    fontPath = FontCacheUtility.copyShanBeiFontToCache(Minecraft.getInstance().getResourceManager());
                } catch (IOException exception) {
                    throw new IllegalStateException("Unable to initialize the Shan Bei font cache", exception);
                }
            }
            reloadFontSet(fontPath);
        }
        return shanBeiFontSet;
    }

    private static synchronized void reloadFontSet(Path fontPath) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (shanBeiFontSet != null) {
            shanBeiFontSet.close();
            shanBeiFontSet = null;
        }

        try {
            CachedStbGlyphProvider glyphProvider = CachedStbGlyphProvider.load(
                    fontPath,
                    CustomFontConstants.SHAN_BEI_FONT_SIZE,
                    CustomFontConstants.SHAN_BEI_FONT_OVERSAMPLE
            );
            FontSet newFontSet = new FontSet(
                    Minecraft.getInstance().getTextureManager(),
                    CustomFontConstants.SHAN_BEI_FONT_ID
            );
            try {
                newFontSet.reload(List.of(glyphProvider));
            } catch (RuntimeException exception) {
                newFontSet.close();
                throw exception;
            }
            cachedFontPath = fontPath;
            shanBeiFontSet = newFontSet;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load the Shan Bei font from " + fontPath, exception);
        }
    }
}
