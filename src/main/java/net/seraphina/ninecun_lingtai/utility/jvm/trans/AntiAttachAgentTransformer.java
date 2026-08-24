package net.seraphina.ninecun_lingtai.utility.jvm.trans;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class AntiAttachAgentTransformer implements ClassFileTransformer {
    private static final String VIRTUAL_MACHINE_INTERNAL_NAME =
            "com/sun/tools/attach/VirtualMachine";
    private static final String HOTSPOT_VIRTUAL_MACHINE_INTERNAL_NAME =
            "sun/tools/attach/HotSpotVirtualMachine";

    private static final String[] AGENT_METHOD_NAMES = {
            "loadAgentLibrary",
            "loadAgentPath",
            "loadAgent"
    };

    @Override
    public byte[] transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (className == null || classfileBuffer == null) {
            return null;
        }

        String internalClassName = className.replace('.', '/');
        if (!VIRTUAL_MACHINE_INTERNAL_NAME.equals(internalClassName)
                && !HOTSPOT_VIRTUAL_MACHINE_INTERNAL_NAME.equals(internalClassName)) {
            return null;
        }

        try {
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, ClassReader.EXPAND_FRAMES);

            boolean modified = VIRTUAL_MACHINE_INTERNAL_NAME.equals(internalClassName)
                    ? transformVirtualMachine(classNode)
                    : transformHotSpotVirtualMachine(classNode);
            if (!modified) {
                return null;
            }

            ClassWriter writer = new ClassWriter(
                    ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS
            );
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (RuntimeException exception) {
            // A failed guard must not prevent the JVM from defining the class.
            return null;
        }
    }

    private static boolean transformVirtualMachine(ClassNode classNode) {
        boolean modified = false;
        for (MethodNode method : classNode.methods) {
            if (!"attach".equals(method.name)) {
                continue;
            }
            replaceWithDefaultReturn(method);
            modified = true;
        }
        return modified;
    }

    private static boolean transformHotSpotVirtualMachine(ClassNode classNode) {
        boolean modified = false;
        for (MethodNode method : classNode.methods) {
            if (!isAgentMethod(method.name)) {
                continue;
            }
            replaceWithDefaultReturn(method);
            modified = true;
        }
        return modified;
    }

    private static boolean isAgentMethod(String methodName) {
        for (String targetMethodName : AGENT_METHOD_NAMES) {
            if (targetMethodName.equals(methodName)) {
                return true;
            }
        }
        return false;
    }

    private static void replaceWithDefaultReturn(MethodNode method) {
        method.access &= ~(Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE);
        method.instructions.clear();
        if (method.tryCatchBlocks != null) {
            method.tryCatchBlocks.clear();
        }
        if (method.localVariables != null) {
            method.localVariables.clear();
        }

        Type returnType = Type.getReturnType(method.desc);
        switch (returnType.getSort()) {
            case Type.VOID -> method.instructions.add(new InsnNode(Opcodes.RETURN));
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> {
                method.instructions.add(new InsnNode(Opcodes.ICONST_0));
                method.instructions.add(new InsnNode(Opcodes.IRETURN));
            }
            case Type.FLOAT -> {
                method.instructions.add(new InsnNode(Opcodes.FCONST_0));
                method.instructions.add(new InsnNode(Opcodes.FRETURN));
            }
            case Type.LONG -> {
                method.instructions.add(new InsnNode(Opcodes.LCONST_0));
                method.instructions.add(new InsnNode(Opcodes.LRETURN));
            }
            case Type.DOUBLE -> {
                method.instructions.add(new InsnNode(Opcodes.DCONST_0));
                method.instructions.add(new InsnNode(Opcodes.DRETURN));
            }
            case Type.ARRAY, Type.OBJECT -> {
                method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported return type in Attach API method: " + method.desc
            );
        }

        int maxLocals = (method.access & Opcodes.ACC_STATIC) == 0 ? 1 : 0;
        for (Type argument : Type.getArgumentTypes(method.desc)) {
            maxLocals += argument.getSize();
        }
        method.maxLocals = maxLocals;
        method.maxStack = returnType.getSize();
    }
}
