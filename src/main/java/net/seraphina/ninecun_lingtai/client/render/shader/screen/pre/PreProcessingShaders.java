package net.seraphina.ninecun_lingtai.client.render.shader.screen.pre;

import com.google.gson.JsonSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.seraphina.ninecun_lingtai.ModMain;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;

public final class PreProcessingShaders implements ResourceManagerReloadListener {
    public static final PreProcessingShaders INSTANCE = new PreProcessingShaders(Minecraft.getInstance());

    private final Minecraft minecraft;
    private final Logger logger = ModMain.LOGGER;
    private final Map<PreScreenEffect, PostChain> preChains = new IdentityHashMap<>();
    private int previousWidth = -1;
    private int previousHeight = -1;

    public PreProcessingShaders(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    public PostChain getPreChain(PreScreenEffect effect) {
        return this.preChains.get(effect);
    }

    public void renderShaders(float partialTicks) {
        if (this.minecraft.level == null || this.minecraft.player == null) {
            return;
        }

        this.minecraft.getProfiler().push("ninecun_lingtai_pre_effects");
        try {
            this.resizeIfNeeded();
            for (PreScreenEffect effect : PreEffectHandler.getActiveEffects()) {
                PostChain postChain = this.preChains.get(effect);
                if (postChain == null) {
                    continue;
                }
                effect.onRenderTick(partialTicks);
                postChain.process(partialTicks);
                this.minecraft.getMainRenderTarget().bindWrite(false);
            }
        } finally {
            this.minecraft.getProfiler().pop();
        }
    }

    public void resize(int width, int height) {
        for (PostChain postChain : this.preChains.values()) {
            postChain.resize(width, height);
        }
        this.previousWidth = width;
        this.previousHeight = height;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.closeChains();
        for (PreScreenEffect effect : PreEffectHandler.getEffects()) {
            try {
                PostChain postChain = new PostChain(
                        this.minecraft.getTextureManager(),
                        resourceManager,
                        this.minecraft.getMainRenderTarget(),
                        effect.getShaderLocation()
                );
                postChain.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
                this.preChains.put(effect, postChain);
            } catch (JsonSyntaxException exception) {
                this.logger.warn("Failed to parse pre shader {}", effect.getShaderLocation(), exception);
            } catch (IOException exception) {
                this.logger.warn("Failed to load pre shader {}", effect.getShaderLocation(), exception);
            }
        }
        this.previousWidth = this.minecraft.getWindow().getWidth();
        this.previousHeight = this.minecraft.getWindow().getHeight();
    }

    private void resizeIfNeeded() {
        int width = this.minecraft.getWindow().getWidth();
        int height = this.minecraft.getWindow().getHeight();
        if (width != this.previousWidth || height != this.previousHeight) {
            this.resize(width, height);
        }
    }

    private void closeChains() {
        for (PostChain postChain : this.preChains.values()) {
            postChain.close();
        }
        this.preChains.clear();
    }
}
