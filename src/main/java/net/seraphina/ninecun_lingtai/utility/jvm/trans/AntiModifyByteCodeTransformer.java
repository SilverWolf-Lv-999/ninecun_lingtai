package net.seraphina.ninecun_lingtai.utility.jvm.trans;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public final class AntiModifyByteCodeTransformer implements ClassFileTransformer {
    public static final String PROTECT_PATH = "net.seraphina.ninecun_lingtai.";

    private static final String PROTECT_INTERNAL_PATH = PROTECT_PATH.replace('.', '/');
    private static final String CLASS_FILE_TRANSFORMER_INTERNAL_NAME =
            "java/lang/instrument/ClassFileTransformer";
    private static final String LAUNCH_PLUGIN_SERVICE_INTERNAL_NAME =
            "cpw/mods/modlauncher/serviceapi/ILaunchPluginService";
    private static final String TRANSFORM_METHOD_NAME = "transform";
    private static final String LEGACY_TRANSFORM_METHOD_DESCRIPTOR =
            "(Ljava/lang/ClassLoader;Ljava/lang/String;Ljava/lang/Class;"
                    + "Ljava/security/ProtectionDomain;[B)[B";
    private static final String MODULE_TRANSFORM_METHOD_DESCRIPTOR =
            "(Ljava/lang/Module;Ljava/lang/ClassLoader;Ljava/lang/String;"
                    + "Ljava/lang/Class;Ljava/security/ProtectionDomain;[B)[B";
    private static final String PROCESS_CLASS_METHOD_NAME = "processClass";
    private static final String PROCESS_CLASS_METHOD_DESCRIPTOR =
            "(Lcpw/mods/modlauncher/serviceapi/ILaunchPluginService$Phase;"
                    + "Lorg/objectweb/asm/tree/ClassNode;Lorg/objectweb/asm/Type;)Z";
    private static final String STRING_INTERNAL_NAME = "java/lang/String";
    private static final String STRING_STARTS_WITH_METHOD_NAME = "startsWith";
    private static final String STRING_STARTS_WITH_METHOD_DESCRIPTOR =
            "(Ljava/lang/String;)Z";
    private static final String ASM_TYPE_INTERNAL_NAME = "org/objectweb/asm/Type";
    private static final String ASM_TYPE_GET_CLASS_NAME_METHOD_NAME = "getClassName";
    private static final String ASM_TYPE_GET_CLASS_NAME_METHOD_DESCRIPTOR =
            "()Ljava/lang/String;";

    @Override
    public byte[] transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (classfileBuffer == null) {
            return null;
        }

        ClassReader classReader = new ClassReader(classfileBuffer);
        String[] interfaces = classReader.getInterfaces();

        boolean classFileTransformer = containsInterface(
                interfaces,
                CLASS_FILE_TRANSFORMER_INTERNAL_NAME
        ) || (classBeingRedefined != null
                && ClassFileTransformer.class.isAssignableFrom(classBeingRedefined));
        boolean launchPluginService = containsInterface(
                interfaces,
                LAUNCH_PLUGIN_SERVICE_INTERNAL_NAME
        ) || (classBeingRedefined != null
                && ILaunchPluginService.class.isAssignableFrom(classBeingRedefined));
        if (!classFileTransformer && !launchPluginService) {
            return null;
        }

        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        boolean transformed = false;
        if (classFileTransformer) {
            for (MethodNode method : classNode.methods) {
                if (!TRANSFORM_METHOD_NAME.equals(method.name)) {
                    continue;
                }

                int classNameLocal = switch (method.desc) {
                    case LEGACY_TRANSFORM_METHOD_DESCRIPTOR -> 2;
                    case MODULE_TRANSFORM_METHOD_DESCRIPTOR -> 3;
                    default -> -1;
                };
                if (classNameLocal >= 0) {
                    transformed |= insertClassFileTransformerGuard(method, classNameLocal);
                }
            }
        }
        if (launchPluginService) {
            for (MethodNode method : classNode.methods) {
                if (PROCESS_CLASS_METHOD_NAME.equals(method.name)
                        && PROCESS_CLASS_METHOD_DESCRIPTOR.equals(method.desc)) {
                    transformed |= insertLaunchPluginServiceGuard(method);
                }
            }
        }

        if (!transformed) {
            return null;
        }

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    private static boolean containsInterface(String[] interfaces, String targetInterface) {
        for (String interfaceName : interfaces) {
            if (targetInterface.equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean insertClassFileTransformerGuard(
            MethodNode method,
            int classNameLocal
    ) {
        if (method.instructions.getFirst() == null) {
            return false;
        }

        LabelNode continueLabel = new LabelNode();
        LabelNode returnLabel = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(ALOAD, classNameLocal));
        guard.add(new JumpInsnNode(IFNULL, continueLabel));
        addStartsWithCheck(guard, classNameLocal, PROTECT_INTERNAL_PATH, returnLabel);
        addStartsWithCheck(guard, classNameLocal, PROTECT_PATH, returnLabel);
        guard.add(new JumpInsnNode(GOTO, continueLabel));
        guard.add(returnLabel);
        guard.add(new InsnNode(ACONST_NULL));
        guard.add(new InsnNode(ARETURN));
        guard.add(continueLabel);
        method.instructions.insertBefore(method.instructions.getFirst(), guard);
        return true;
    }

    private static void addStartsWithCheck(
            InsnList instructions,
            int classNameLocal,
            String prefix,
            LabelNode returnLabel
    ) {
        instructions.add(new VarInsnNode(ALOAD, classNameLocal));
        instructions.add(new LdcInsnNode(prefix));
        instructions.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                STRING_INTERNAL_NAME,
                STRING_STARTS_WITH_METHOD_NAME,
                STRING_STARTS_WITH_METHOD_DESCRIPTOR,
                false
        ));
        instructions.add(new JumpInsnNode(IFNE, returnLabel));
    }

    private static boolean insertLaunchPluginServiceGuard(MethodNode method) {
        if (method.instructions.getFirst() == null) {
            return false;
        }

        LabelNode continueLabel = new LabelNode();
        InsnList guard = new InsnList();
        guard.add(new VarInsnNode(ALOAD, 3));
        guard.add(new JumpInsnNode(IFNULL, continueLabel));
        guard.add(new VarInsnNode(ALOAD, 3));
        guard.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                ASM_TYPE_INTERNAL_NAME,
                ASM_TYPE_GET_CLASS_NAME_METHOD_NAME,
                ASM_TYPE_GET_CLASS_NAME_METHOD_DESCRIPTOR,
                false
        ));
        guard.add(new LdcInsnNode(PROTECT_PATH));
        guard.add(new MethodInsnNode(
                INVOKEVIRTUAL,
                STRING_INTERNAL_NAME,
                STRING_STARTS_WITH_METHOD_NAME,
                STRING_STARTS_WITH_METHOD_DESCRIPTOR,
                false
        ));
        guard.add(new JumpInsnNode(IFEQ, continueLabel));
        guard.add(new InsnNode(ICONST_0));
        guard.add(new InsnNode(IRETURN));
        guard.add(continueLabel);
        method.instructions.insertBefore(method.instructions.getFirst(), guard);
        return true;
    }
}
