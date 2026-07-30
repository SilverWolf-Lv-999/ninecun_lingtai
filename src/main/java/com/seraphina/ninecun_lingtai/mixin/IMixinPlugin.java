package com.seraphina.ninecun_lingtai.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class IMixinPlugin implements IMixinConfigPlugin {
    private static final String EMBEDDIUM_MOD_ID = "embeddium";
    private static final String LEVEL_RENDERER_MIXIN =
            "com.seraphina.ninecun_lingtai.mixin.client.LevelRendererMixin";

    private boolean embeddiumLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList loadingModList = FMLLoader.getLoadingModList();
        this.embeddiumLoaded = loadingModList != null
                && loadingModList.getModFileById(EMBEDDIUM_MOD_ID) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !this.embeddiumLoaded || !LEVEL_RENDERER_MIXIN.equals(mixinClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return List.of();
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo
    ) {
    }
}
