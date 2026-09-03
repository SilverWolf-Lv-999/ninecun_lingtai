package net.seraphina.ninecun_lingtai.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.seraphina.ninecun_lingtai.mixin.api.MixinDisabled;
import net.seraphina.ninecun_lingtai.mixin.api.Type;
import net.seraphina.ninecun_lingtai.utility.jvm.AgentUtility;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.List;
import java.util.Set;

public final class IMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_DISABLED_DESCRIPTOR =
            org.objectweb.asm.Type.getDescriptor(MixinDisabled.class);
    private static final String DEFAULT_MOD_ID = "ninecun_lingtai";

    static {
        AgentUtility.patchAgentMain();
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return "";
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        ClassNode classNode;
        try {
            classNode = MixinService.getService()
                    .getBytecodeProvider()
                    .getClassNode(mixinClassName);
        } catch (Exception exception) {
            return false;
        }

        AnnotationNode annotationNode = this.findMixinDisabledAnnotation(classNode);
        if (annotationNode == null) {
            return true;
        }

        MixinDisabledData disabledData = this.readMixinDisabledData(annotationNode);
        return switch (disabledData.type()) {
            case MOD_LOAD -> !this.isModLoaded(disabledData.modId());
            case UN_MOD_LOAD -> this.isModLoaded(disabledData.modId());
            case FOREVER -> false;
        };
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

    private AnnotationNode findMixinDisabledAnnotation(ClassNode classNode) {
        if (classNode.invisibleAnnotations == null) {
            return null;
        }

        for (AnnotationNode annotationNode : classNode.invisibleAnnotations) {
            if (MIXIN_DISABLED_DESCRIPTOR.equals(annotationNode.desc)) {
                return annotationNode;
            }
        }
        return null;
    }

    private MixinDisabledData readMixinDisabledData(AnnotationNode annotationNode) {
        Type type = Type.FOREVER;
        String modId = DEFAULT_MOD_ID;

        if (annotationNode.values == null) {
            return new MixinDisabledData(type, modId);
        }

        for (int index = 0; index < annotationNode.values.size(); index += 2) {
            String name = (String) annotationNode.values.get(index);
            Object value = annotationNode.values.get(index + 1);
            if ("type".equals(name)) {
                type = this.readType(value);
            } else if ("modId".equals(name) && value instanceof String annotationModId
                    && !annotationModId.isBlank()) {
                modId = annotationModId;
            }
        }
        return new MixinDisabledData(type, modId);
    }

    private Type readType(Object value) {
        if (value instanceof String[] enumValue && enumValue.length == 2) {
            try {
                return Type.valueOf(enumValue[1]);
            } catch (IllegalArgumentException ignored) {
                return Type.FOREVER;
            }
        }
        return Type.FOREVER;
    }

    private boolean isModLoaded(String modId) {
        LoadingModList loadingModList = FMLLoader.getLoadingModList();
        return loadingModList != null && loadingModList.getModFileById(modId) != null;
    }

    private record MixinDisabledData(Type type, String modId) {
    }
}
