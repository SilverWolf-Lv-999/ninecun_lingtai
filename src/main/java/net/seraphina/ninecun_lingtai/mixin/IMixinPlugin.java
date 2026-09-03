package net.seraphina.ninecun_lingtai.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import net.seraphina.ninecun_lingtai.Static;
import net.seraphina.ninecun_lingtai.client.utility.SwingUtility;
import net.seraphina.ninecun_lingtai.mixin.api.MixinDisabled;
import net.seraphina.ninecun_lingtai.mixin.api.MixinEnabled;
import net.seraphina.ninecun_lingtai.mixin.api.Type;
import net.seraphina.ninecun_lingtai.utility.JDKUtility;
import net.seraphina.ninecun_lingtai.utility.jvm.AgentUtility;
import net.seraphina.ninecun_lingtai.utility.system.SysUtility;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Set;

public final class IMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_DISABLED_DESCRIPTOR =
            org.objectweb.asm.Type.getDescriptor(MixinDisabled.class);
    private static final String MIXIN_ENABLED_DESCRIPTOR =
            org.objectweb.asm.Type.getDescriptor(MixinEnabled.class);
    private static final String DEFAULT_DISABLED_MOD_ID = "ninecun_lingtai";
    private static final String DEFAULT_ENABLED_MOD_ID = "minecraft";

    private LoadingModList loadingModList;

    public static final Logger LOGGER = LoggerFactory.getLogger(IMixinPlugin.class);

    static {
        AgentUtility.patchAgentMain();
        if (Static.isClient() && Static.isOptifineLoaded() && !SwingUtility.isHeadless() && !SysUtility.isAndroid()) {
            SwingUtilities.invokeLater(() -> {
                JWindow window = SwingUtility.createWindow();
                window.setVisible(true);
            });
        }
        if (!JDKUtility.isJDK17() && Static.isClient() && !SwingUtility.isHeadless() && !SysUtility.isAndroid()) {
            SwingUtilities.invokeLater(() -> {
                JWindow window = SwingUtility.createJdkWarningWindow();
                window.setVisible(true);
            });
        }
        if (SysUtility.isAndroid()) {
            LOGGER.info("Why are you running my code on the completely wrong platform?");
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.loadingModList = FMLLoader.getLoadingModList();
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

        if (classNode == null) {
            return false;
        }

        AnnotationNode disabledAnnotation = this.findAnnotation(
                classNode,
                MIXIN_DISABLED_DESCRIPTOR
        );
        if (disabledAnnotation != null) {
            MixinCondition condition = this.readCondition(
                    disabledAnnotation,
                    DEFAULT_DISABLED_MOD_ID
            );
            if (this.isEnvironmentMatched(condition)) {
                return false;
            }
        }

        AnnotationNode enabledAnnotation = this.findAnnotation(
                classNode,
                MIXIN_ENABLED_DESCRIPTOR
        );
        if (enabledAnnotation != null) {
            MixinCondition condition = this.readCondition(
                    enabledAnnotation,
                    DEFAULT_ENABLED_MOD_ID
            );
            return this.isEnvironmentMatched(condition);
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
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

    private AnnotationNode findAnnotation(ClassNode classNode, String descriptor) {
        AnnotationNode annotationNode = this.findAnnotation(classNode.invisibleAnnotations, descriptor);
        if (annotationNode != null) {
            return annotationNode;
        }
        return this.findAnnotation(classNode.visibleAnnotations, descriptor);
    }

    private AnnotationNode findAnnotation(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return null;
        }

        for (AnnotationNode annotationNode : annotations) {
            if (descriptor.equals(annotationNode.desc)) {
                return annotationNode;
            }
        }
        return null;
    }

    private MixinCondition readCondition(AnnotationNode annotationNode, String defaultModId) {
        Type type = Type.FOREVER;
        String modId = defaultModId;

        if (annotationNode.values == null) {
            return new MixinCondition(type, modId);
        }

        for (int index = 0; index + 1 < annotationNode.values.size(); index += 2) {
            Object nameValue = annotationNode.values.get(index);
            Object value = annotationNode.values.get(index + 1);
            if (!(nameValue instanceof String name)) {
                continue;
            }

            if ("type".equals(name)) {
                type = this.readType(value);
            } else if ("modId".equals(name) && value instanceof String annotationModId
                    && !annotationModId.isBlank()) {
                modId = annotationModId;
            }
        }
        return new MixinCondition(type, modId);
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

    private boolean isEnvironmentMatched(MixinCondition condition) {
        return switch (condition.type()) {
            case MOD_LOAD -> this.isModLoaded(condition.modId());
            case UN_MOD_LOAD -> !this.isModLoaded(condition.modId());
            case FOREVER -> true;
        };
    }

    private boolean isModLoaded(String modId) {
        LoadingModList currentLoadingModList = this.loadingModList;
        if (currentLoadingModList == null) {
            currentLoadingModList = FMLLoader.getLoadingModList();
        }
        return currentLoadingModList != null
                && currentLoadingModList.getModFileById(modId) != null;
    }

    private record MixinCondition(Type type, String modId) {
    }
}
