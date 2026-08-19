package net.seraphina.ninecun_lingtai.transform;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.EnumSet;

public final class HealthLaunchPluginService implements ILaunchPluginService {
    private static final String ENTITY_OWNER = "net/minecraft/world/entity/Entity";
    private static final String LIVING_ENTITY_OWNER = "net/minecraft/world/entity/LivingEntity";
    private static final String HEALTH_DESCRIPTOR = "()F";
    private static final String BOOLEAN_DESCRIPTOR = "()Z";
    private static final String OBFUSCATED_GET_HEALTH = "m_21223_";
    private static final String OBFUSCATED_IS_ALIVE = "m_6084_";
    private static final String OBFUSCATED_IS_DEAD_OR_DYING = "m_21224_";
    private static final String PROTECT_HEALTH = "ninecunLingtai$protectHealth";
    private static final String PROTECT_ALIVE = "ninecunLingtai$protectAlive";
    private static final String PROTECT_DEAD_OR_DYING = "ninecunLingtai$protectDeadOrDying";
    private static final String PROTECT_HEALTH_DESCRIPTOR =
            "(F)F";
    private static final String PROTECT_BOOLEAN_DESCRIPTOR = "(Z)Z";
    private static final String CLIENT_DEFENSE_UTILITY_OWNER =
            "net/seraphina/ninecun_lingtai/client/utility/ClientCultivationDefenseUtility";
    private static final String CLIENT_PROTECT_HEALTH = "getProtectedHealth";
    private static final String CLIENT_PROTECT_ALIVE = "getProtectedAlive";
    private static final String CLIENT_PROTECT_DEAD_OR_DYING = "getProtectedDeadOrDying";
    private static final String CLIENT_PROTECT_HEALTH_DESCRIPTOR =
            "(Lnet/minecraft/world/entity/LivingEntity;F)F";
    private static final String CLIENT_PROTECT_ALIVE_DESCRIPTOR =
            "(Lnet/minecraft/world/entity/Entity;Z)Z";
    private static final String CLIENT_PROTECT_DEAD_OR_DYING_DESCRIPTOR =
            "(Lnet/minecraft/world/entity/LivingEntity;Z)Z";

    @Override
    public String name() {
        return "Ninecun Lingtai Health LaunchPluginService";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return EnumSet.of(Phase.BEFORE);
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        if (phase != Phase.BEFORE) {
            return false;
        }

        boolean transformed = false;
        for (var method : classNode.methods) {
            for (var instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || !isSupportedMethodCall(call)) {
                    continue;
                }
                addProtectionAroundCall(method.instructions, call);
                transformed = true;
            }
        }
        return transformed;
    }

    static boolean isSupportedMethodCall(MethodInsnNode call) {
        if (call.getOpcode() != Opcodes.INVOKEVIRTUAL || call.itf) {
            return false;
        }
        if (LIVING_ENTITY_OWNER.equals(call.owner)
                && HEALTH_DESCRIPTOR.equals(call.desc)
                && isMethodName(call.name, OBFUSCATED_GET_HEALTH, "getHealth")) {
            return true;
        }
        if ((ENTITY_OWNER.equals(call.owner) || LIVING_ENTITY_OWNER.equals(call.owner))
                && BOOLEAN_DESCRIPTOR.equals(call.desc)
                && isMethodName(call.name, OBFUSCATED_IS_ALIVE, "isAlive")) {
            return true;
        }
        return LIVING_ENTITY_OWNER.equals(call.owner)
                && BOOLEAN_DESCRIPTOR.equals(call.desc)
                && isMethodName(call.name, OBFUSCATED_IS_DEAD_OR_DYING, "isDeadOrDying");
    }

    private static boolean isMethodName(String actualName, String obfuscatedName, String namedName) {
        return obfuscatedName.equals(actualName) || namedName.equals(actualName);
    }

    private static void addProtectionAroundCall(
            org.objectweb.asm.tree.InsnList instructions,
            MethodInsnNode call
    ) {
        instructions.insertBefore(call, new InsnNode(Opcodes.DUP));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            addClientProtectionAroundCall(instructions, call);
            return;
        }
        addServerProtectionAroundCall(instructions, call);
    }

    private static void addClientProtectionAroundCall(
            org.objectweb.asm.tree.InsnList instructions,
            MethodInsnNode call
    ) {
        if (HEALTH_DESCRIPTOR.equals(call.desc)) {
            instructions.insert(call, new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    CLIENT_DEFENSE_UTILITY_OWNER,
                    CLIENT_PROTECT_HEALTH,
                    CLIENT_PROTECT_HEALTH_DESCRIPTOR,
                    false
            ));
            return;
        }
        if (LIVING_ENTITY_OWNER.equals(call.owner)
                && isMethodName(call.name, OBFUSCATED_IS_DEAD_OR_DYING, "isDeadOrDying")) {
            instructions.insert(call, new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    CLIENT_DEFENSE_UTILITY_OWNER,
                    CLIENT_PROTECT_DEAD_OR_DYING,
                    CLIENT_PROTECT_DEAD_OR_DYING_DESCRIPTOR,
                    false
            ));
            return;
        }
        instructions.insert(call, new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                CLIENT_DEFENSE_UTILITY_OWNER,
                CLIENT_PROTECT_ALIVE,
                CLIENT_PROTECT_ALIVE_DESCRIPTOR,
                false
        ));
    }

    private static void addServerProtectionAroundCall(
            org.objectweb.asm.tree.InsnList instructions,
            MethodInsnNode call
    ) {
        if (HEALTH_DESCRIPTOR.equals(call.desc)) {
            instructions.insert(call, new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    LIVING_ENTITY_OWNER,
                    PROTECT_HEALTH,
                    PROTECT_HEALTH_DESCRIPTOR,
                    false
            ));
            return;
        }
        if (LIVING_ENTITY_OWNER.equals(call.owner)
                && isMethodName(call.name, OBFUSCATED_IS_DEAD_OR_DYING, "isDeadOrDying")) {
            instructions.insert(call, new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    LIVING_ENTITY_OWNER,
                    PROTECT_DEAD_OR_DYING,
                    PROTECT_BOOLEAN_DESCRIPTOR,
                    false
            ));
            return;
        }
        instructions.insert(call, new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                ENTITY_OWNER,
                PROTECT_ALIVE,
                PROTECT_BOOLEAN_DESCRIPTOR,
                false
        ));
    }
}
