package net.seraphina.ninecun_lingtai.transform;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventListener;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;

public final class SeraLPService implements ILaunchPluginService {
    private static final String CLASS_LOADER_FACTORY_OWNER = "net/minecraftforge/eventbus/ClassLoaderFactory";
    private static final String EVENT_LISTENER_OWNER = "net/minecraftforge/eventbus/api/IEventListener";
    private static final String EVENT_LISTENER_FACTORY_METHOD = "create";
    private static final String EVENT_LISTENER_FACTORY_DESCRIPTOR =
            "(Ljava/lang/reflect/Method;Ljava/lang/Object;)L" + EVENT_LISTENER_OWNER + ";";
    private static final String LAMBDA_EVENT_LISTENER_METHOD = "createEventListener";

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

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final MethodType EVENT_LISTENER_FACTORY_TYPE =
            MethodType.methodType(IEventListener.class);
    private static final MethodType EVENT_LISTENER_SAM_METHOD_TYPE =
            MethodType.methodType(void.class, Event.class);

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

        boolean transformed = transformEventListenerFactory(classNode);
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

    private static boolean transformEventListenerFactory(ClassNode classNode) {
        if (!CLASS_LOADER_FACTORY_OWNER.equals(classNode.name)) {
            return false;
        }

        boolean transformed = false;
        for (MethodNode method : classNode.methods) {
            if (!EVENT_LISTENER_FACTORY_METHOD.equals(method.name)
                    || !EVENT_LISTENER_FACTORY_DESCRIPTOR.equals(method.desc)) {
                continue;
            }

            method.instructions.clear();
            method.tryCatchBlocks.clear();
            method.localVariables.clear();
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            method.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(SeraLPService.class),
                    LAMBDA_EVENT_LISTENER_METHOD,
                    EVENT_LISTENER_FACTORY_DESCRIPTOR,
                    false
            ));
            method.instructions.add(new InsnNode(Opcodes.ARETURN));
            method.maxStack = 2;
            method.maxLocals = 3;
            transformed = true;
        }
        return transformed;
    }

    public static IEventListener createEventListener(Method method, Object target) {
        try {
            MethodHandle listenerHandle;
            try {
                listenerHandle = LOOKUP.unreflect(method);
            } catch (IllegalAccessException exception) {
                if (!method.trySetAccessible()) {
                    throw exception;
                }
                listenerHandle = LOOKUP.unreflect(method);
            }

            MethodType instantiatedMethodType = MethodType.methodType(
                    void.class,
                    method.getParameterTypes()
            );
            if (Modifier.isStatic(method.getModifiers())) {
                CallSite callSite = LambdaMetafactory.metafactory(
                        LOOKUP,
                        "invoke",
                        EVENT_LISTENER_FACTORY_TYPE,
                        EVENT_LISTENER_SAM_METHOD_TYPE,
                        listenerHandle,
                        instantiatedMethodType
                );
                return (IEventListener) callSite.getTarget().invokeExact();
            }

            CallSite callSite = LambdaMetafactory.metafactory(
                    LOOKUP,
                    "invoke",
                    MethodType.methodType(IEventListener.class, method.getDeclaringClass()),
                    EVENT_LISTENER_SAM_METHOD_TYPE,
                    listenerHandle,
                    instantiatedMethodType
            );
            return (IEventListener) callSite.getTarget().invoke(target);
        } catch (Throwable throwable) {
            throw new IllegalStateException(
                    "Unable to create lambda event listener for " + method,
                    throwable
            );
        }
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
