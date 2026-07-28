package com.seraphina.ninecun_lingtai.transform;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthLaunchPluginServiceTest {
    @Test
    void matchesOnlyExactHealthSignatures() {
        assertTrue(HealthLaunchPluginService.isSupportedMethodCall(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/LivingEntity",
                "m_21223_",
                "()F",
                false
        )));
        assertTrue(HealthLaunchPluginService.isSupportedMethodCall(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/Entity",
                "m_6084_",
                "()Z",
                false
        )));
        assertTrue(HealthLaunchPluginService.isSupportedMethodCall(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/LivingEntity",
                "m_21224_",
                "()Z",
                false
        )));
        assertFalse(HealthLaunchPluginService.isSupportedMethodCall(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "some/other/Owner",
                "m_21223_",
                "()F",
                false
        )));
        assertFalse(HealthLaunchPluginService.isSupportedMethodCall(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/LivingEntity",
                "m_21223_",
                "(F)F",
                false
        )));
        assertFalse(HealthLaunchPluginService.isSupportedMethodCall(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/world/entity/LivingEntity",
                "m_21223_",
                "()F",
                false
        )));
    }

    @Test
    void wrapsSupportedCallsWithInstanceBridgeDescriptors() {
        HealthLaunchPluginService plugin = new HealthLaunchPluginService();
        ClassNode classNode = new ClassNode();
        classNode.name = "some/mod/EntityConsumer";
        classNode.methods = new java.util.ArrayList<>();

        org.objectweb.asm.tree.MethodNode method = new org.objectweb.asm.tree.MethodNode(
                Opcodes.ACC_PUBLIC,
                "check",
                "(Lnet/minecraft/world/entity/LivingEntity;)Z",
                null,
                null
        );
        InsnList instructions = method.instructions;
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/LivingEntity",
                "m_21223_",
                "()F",
                false
        ));
        instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/Entity",
                "m_6084_",
                "()Z",
                false
        ));
        instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
        instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/LivingEntity",
                "m_21224_",
                "()Z",
                false
        ));
        instructions.add(new org.objectweb.asm.tree.InsnNode(Opcodes.IRETURN));
        classNode.methods.add(method);

        assertTrue(plugin.processClass(
                ILaunchPluginService.Phase.BEFORE,
                classNode,
                org.objectweb.asm.Type.getObjectType(classNode.name)
        ));

        MethodInsnNode healthBridge = findCall(method.instructions, "ninecunLingtai$protectHealth");
        MethodInsnNode aliveBridge = findCall(method.instructions, "ninecunLingtai$protectAlive");
        MethodInsnNode deadOrDyingBridge = findCall(
                method.instructions,
                "ninecunLingtai$protectDeadOrDying"
        );
        assertEquals("net/minecraft/world/entity/LivingEntity", healthBridge.owner);
        assertEquals(Opcodes.INVOKEVIRTUAL, healthBridge.getOpcode());
        assertEquals("(F)F", healthBridge.desc);
        assertEquals("net/minecraft/world/entity/Entity", aliveBridge.owner);
        assertEquals(Opcodes.INVOKEVIRTUAL, aliveBridge.getOpcode());
        assertEquals("(Z)Z", aliveBridge.desc);
        assertEquals("net/minecraft/world/entity/LivingEntity", deadOrDyingBridge.owner);
        assertEquals(Opcodes.INVOKEVIRTUAL, deadOrDyingBridge.getOpcode());
        assertEquals("(Z)Z", deadOrDyingBridge.desc);
    }

    private static MethodInsnNode findCall(InsnList instructions, String name) {
        for (var instruction : instructions) {
            if (instruction instanceof MethodInsnNode methodInsn
                    && name.equals(methodInsn.name)) {
                return methodInsn;
            }
        }
        throw new AssertionError("Missing transformed call: " + name);
    }
}
