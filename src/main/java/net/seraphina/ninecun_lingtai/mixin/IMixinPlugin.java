package net.seraphina.ninecun_lingtai.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class IMixinPlugin implements IMixinConfigPlugin {
    private static final String EMBEDDIUM_MOD_ID = "embeddium";
    private static final String BAD_OPTIMIZATIONS_MOD_ID = "badoptimizations";
    private static final String JEI_MOD_ID = "jei";
    private static final String LEVEL_RENDERER_MIXIN =
            "net.seraphina.ninecun_lingtai.mixin.client.LevelRendererMixin";
    private static final String BAD_OPTIMIZATIONS_ENTITY_DATA_MIXIN =
            "net.seraphina.ninecun_lingtai.mixin.client.compat.badoptimizations.EntityDataCacheMixin";
    private static final String JEI_MIXIN_PREFIX =
            "net.seraphina.ninecun_lingtai.mixin.client.compat.jei.";

    private boolean embeddiumLoaded;
    private boolean badOptimizationsLoaded;
    private boolean jeiLoaded;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList loadingModList = FMLLoader.getLoadingModList();
        this.embeddiumLoaded = loadingModList != null
                && loadingModList.getModFileById(EMBEDDIUM_MOD_ID) != null;
        this.badOptimizationsLoaded = loadingModList != null
                && loadingModList.getModFileById(BAD_OPTIMIZATIONS_MOD_ID) != null;
        this.jeiLoaded = loadingModList != null
                && loadingModList.getModFileById(JEI_MOD_ID) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (BAD_OPTIMIZATIONS_ENTITY_DATA_MIXIN.equals(mixinClassName)) {
            return this.badOptimizationsLoaded;
        }
        if (mixinClassName.startsWith(JEI_MIXIN_PREFIX)) {
            return this.jeiLoaded;
        }
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
