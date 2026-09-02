package net.seraphina.ninecun_lingtai.transform;

import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.slf4j.Logger;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumSet;

public final class SeraLPService implements ILaunchPluginService {
    private static final Logger LOGGER = LogUtils.getLogger();

    static {
        LOGGER.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!SeraLPService");
    }

    private static final String EVENT_BUS_OWNER = "net/minecraftforge/eventbus/EventBus";
    private static final String EVENT_BUS_REGISTER_METHOD = "register";
    private static final String EVENT_BUS_REGISTER_DESCRIPTOR = "(Ljava/lang/Object;)V";
    private static final String REGISTER_GUARD_METHOD = "shouldRegister";
    private static final String REGISTER_GUARD_DESCRIPTOR = "(Ljava/lang/Object;)Z";

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
        return "SeraLPService";
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

        boolean transformed = transformEventBusRegister(classNode)
                || transformEventListenerFactory(classNode);
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

    private static boolean transformEventBusRegister(ClassNode classNode) {
        if (!EVENT_BUS_OWNER.equals(classNode.name)) {
            return false;
        }

        boolean transformed = false;
        for (MethodNode method : classNode.methods) {
            if (!EVENT_BUS_REGISTER_METHOD.equals(method.name)
                    || !EVENT_BUS_REGISTER_DESCRIPTOR.equals(method.desc)) {
                continue;
            }

            AbstractInsnNode originalEntry = method.instructions.getFirst();
            if (originalEntry == null) {
                continue;
            }

            LabelNode originalEntryLabel = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
            guard.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    Type.getInternalName(SeraLPService.class),
                    REGISTER_GUARD_METHOD,
                    REGISTER_GUARD_DESCRIPTOR,
                    false
            ));
            guard.add(new JumpInsnNode(Opcodes.IFNE, originalEntryLabel));
            guard.add(new InsnNode(Opcodes.RETURN));
            guard.add(originalEntryLabel);
            method.instructions.insertBefore(originalEntry, guard);
            method.maxStack = Math.max(method.maxStack, 1);
            transformed = true;
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

    public static boolean shouldRegister(Object target) {
        if (target == null) {
            return true;
        }

        if (target instanceof Class<?> targetClass) {
            return hasStaticEventMethod(targetClass);
        }
        return hasInstanceEventMethod(target.getClass());
    }

    private static boolean hasStaticEventMethod(Class<?> targetClass) {
        for (Method method : targetClass.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(SubscribeEvent.class)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInstanceEventMethod(Class<?> targetClass) {
        for (Method method : targetClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(SubscribeEvent.class)) {
                return true;
            }
        }

        return hasAnnotatedInstanceMethodInParents(targetClass);
    }

    private static boolean hasAnnotatedInstanceMethodInParents(Class<?> targetClass) {
        for (Class<?> interfaceClass : targetClass.getInterfaces()) {
            if (hasAnnotatedInstanceMethod(interfaceClass)
                    || hasAnnotatedInstanceMethodInParents(interfaceClass)) {
                return true;
            }
        }

        Class<?> superClass = targetClass.getSuperclass();
        return superClass != null
                && superClass != Object.class
                && (hasAnnotatedInstanceMethod(superClass)
                || hasAnnotatedInstanceMethodInParents(superClass));
    }

    private static boolean hasAnnotatedInstanceMethod(Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    && method.isAnnotationPresent(SubscribeEvent.class)) {
                return true;
            }
        }
        return false;
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
            InsnList instructions,
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
