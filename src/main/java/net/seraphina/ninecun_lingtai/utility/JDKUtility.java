package net.seraphina.ninecun_lingtai.utility;

import net.minecraftforge.fml.unsafe.UnsafeHacks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import net.seraphina.ninecun_lingtai.utility.jvm.JvmUtility;

public final class JDKUtility {
    public static final int REQUIRED_JDK_VERSION = 17;
    public static final Unsafe UNSAFE;
    public static final MethodHandles.Lookup LOOKUP;

    private static final String JAVA_VERSION = "java.version";
    private static final String JAVA_VM_NAME = "java.vm.name";
    private static final AtomicBoolean JDK_VERSION_WARNING_LOGGED = new AtomicBoolean();
    private static final Logger LOGGER = LogManager.getLogger(JDKUtility.class);
    private static volatile Map<String, Class<?>> loadedClasses;

    public static int getJDKVersion() {
        return Runtime.version().feature();
    }

    public static boolean isJDK17() {
        return getJDKVersion() == REQUIRED_JDK_VERSION;
    }

    public static void warnIfUnsupportedJDK() {
        int jdkVersion = getJDKVersion();
        if (jdkVersion == REQUIRED_JDK_VERSION || !JDK_VERSION_WARNING_LOGGED.compareAndSet(false, true)) {
            return;
        }
        LOGGER.warn(
                "Ninecun Lingtai's JVM hooks require JDK {}, but the current runtime is JDK {} ({}; {}). JVM hooks will be skipped.",
                REQUIRED_JDK_VERSION,
                jdkVersion,
                getSystemProperty(JAVA_VERSION, "unknown version"),
                getSystemProperty(JAVA_VM_NAME, "unknown VM"));
    }

    public static Class<?> loadClass(String className, boolean initialize, ClassLoader loader) {
        Objects.requireNonNull(className, "className");
        Class<?> primitive = primitiveType(className);
        if (primitive != null) {
            return primitive;
        }
        String binaryName = className.replace('/', '.');
        if (binaryName.endsWith("[]")) {
            int dimensions = 0;
            while (binaryName.endsWith("[]")) {
                dimensions++;
                binaryName = binaryName.substring(0, binaryName.length() - 2);
            }
            String component = primitiveDescriptor(binaryName);
            if (component == null) {
                component = "L" + binaryName + ";";
            }
            binaryName = "[".repeat(dimensions) + component;
        }
        ClassLoader actualLoader = loader;
        if (actualLoader == null) {
            actualLoader = Thread.currentThread().getContextClassLoader();
        }
        if (actualLoader == null) {
            actualLoader = JDKUtility.class.getClassLoader();
        }
        Class<?> loaded = findLoadedClass(actualLoader, binaryName);
        if (loaded != null) {
            if (initialize) {
                ensureInitialized(loaded);
            }
            return loaded;
        }
        try {
            return Class.forName(binaryName, initialize, actualLoader);
        } catch (ClassNotFoundException first) {
            try {
                return Class.forName(binaryName, initialize, null);
            } catch (ClassNotFoundException second) {
                Map<String, Class<?>> snapshot = loadedClassSnapshot();
                loaded = snapshot.get(binaryName);
                if (loaded != null) {
                    if (initialize) {
                        ensureInitialized(loaded);
                    }
                    return loaded;
                }
                second.addSuppressed(first);
                throw new IllegalArgumentException("Cannot load class " + className, second);
            }
        }
    }

    public static void ensureInitialized(Class<?> clazz) {
        try {
            LOOKUP.ensureInitialized(clazz);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot initialize " + clazz.getName(), exception);
        }
    }

    private static Class<?> findLoadedClass(ClassLoader loader, String name) {
        if (loader == null) {
            return null;
        }
        try {
            MethodHandle handle = LOOKUP.findVirtual(ClassLoader.class, "findLoadedClass", MethodType.methodType(Class.class, String.class));
            return (Class<?>) handle.invoke(loader, name);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Map<String, Class<?>> loadedClassSnapshot() {
        Map<String, Class<?>> snapshot = loadedClasses;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (JDKUtility.class) {
            snapshot = loadedClasses;
            if (snapshot == null) {
                Map<String, Class<?>> classes = new HashMap<>();
                try {
                    for (Class<?> clazz : JvmUtility.getAllLoaedClasses()) {
                        classes.putIfAbsent(clazz.getName(), clazz);
                    }
                } catch (Throwable ignored) {
                }
                snapshot = Collections.unmodifiableMap(classes);
                loadedClasses = snapshot;
            }
            return snapshot;
        }
    }

    private static String primitiveDescriptor(String name) {
        return switch (name) {
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "void" -> "V";
            default -> null;
        };
    }

    private static Class<?> primitiveType(String name) {
        return switch (name) {
            case "boolean" -> boolean.class;
            case "byte" -> byte.class;
            case "char" -> char.class;
            case "short" -> short.class;
            case "int" -> int.class;
            case "long" -> long.class;
            case "float" -> float.class;
            case "double" -> double.class;
            case "void" -> void.class;
            default -> null;
        };
    }

    public static <T> T allocateInstance(Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz");
        try {
            return clazz.cast(UNSAFE.allocateInstance(clazz));
        } catch (InstantiationException exception) {
            throw new IllegalArgumentException("Cannot allocate " + clazz.getName(), exception);
        }
    }

    public static <T> T newInstance(Class<T> clazz, Object... arguments) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(arguments, "arguments");
        Constructor<?> constructor = findMatchingConstructor(clazz, arguments);
        return clazz.cast(invoke(unreflect(constructor), arguments));
    }

    public static <T> T newInstance(Class<T> clazz, Class<?>[] parameterTypes, Object... arguments) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(parameterTypes, "parameterTypes");
        Objects.requireNonNull(arguments, "arguments");
        if (parameterTypes.length != arguments.length) {
            throw new IllegalArgumentException("Parameter type count does not match argument count for " + clazz.getName());
        }
        return clazz.cast(invoke(unreflect(findConstructor(clazz, parameterTypes)), arguments));
    }

    public static Constructor<?> findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        Objects.requireNonNull(clazz, "clazz");
        Objects.requireNonNull(parameterTypes, "parameterTypes");
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException ignored) {
            for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
                if (Arrays.equals(constructor.getParameterTypes(), parameterTypes)) {
                    return constructor;
                }
            }
            throw new IllegalArgumentException("No constructor " + clazz.getName() + Arrays.toString(parameterTypes));
        }
    }

    public static Constructor<?>[] findConstructors(Class<?> clazz) {
        return clazz.getDeclaredConstructors();
    }

    public static Method findMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(parameterTypes, "parameterTypes");
        for (Method method : findMethods(owner, name)) {
            if (Arrays.equals(method.getParameterTypes(), parameterTypes)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No method " + owner.getName() + '#' + name + Arrays.toString(parameterTypes));
    }

    public static Method[] findMethods(Class<?> owner, String name) {
        List<Method> methods = new ArrayList<>();
        for (Method method : findMethods(owner)) {
            if (method.getName().equals(name)) {
                methods.add(method);
            }
        }
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("No method " + owner.getName() + '#' + name);
        }
        return methods.toArray(Method[]::new);
    }

    public static Method[] findMethods(Class<?> owner) {
        Map<String, Method> methods = new LinkedHashMap<>();
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                methods.putIfAbsent(methodKey(method), method);
            }
            for (Class<?> iface : current.getInterfaces()) {
                collectInterfaceMethods(iface, methods);
            }
        }
        return methods.values().toArray(Method[]::new);
    }

    public static Field findField(Class<?> owner, String name) {
        for (Field field : findFields(owner)) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        throw new IllegalArgumentException("No field " + owner.getName() + '#' + name);
    }

    public static Field[] findFields(Class<?> owner) {
        Map<String, Field> fields = new LinkedHashMap<>();
        for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                fields.putIfAbsent(field.getName(), field);
            }
            for (Class<?> iface : current.getInterfaces()) {
                collectInterfaceFields(iface, fields);
            }
        }
        return fields.values().toArray(Field[]::new);
    }

    public static MethodHandle unreflect(Method method) {
        Objects.requireNonNull(method, "method");
        try {
            return LOOKUP.unreflect(method);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot unreflect " + method, exception);
        }
    }

    public static MethodHandle unreflect(Constructor<?> constructor) {
        Objects.requireNonNull(constructor, "constructor");
        try {
            return LOOKUP.unreflectConstructor(constructor);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot unreflect " + constructor, exception);
        }
    }

    public static MethodHandle unreflectGetter(Field field) {
        Objects.requireNonNull(field, "field");
        try {
            return LOOKUP.unreflectGetter(field);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot unreflect getter " + field, exception);
        }
    }

    public static MethodHandle unreflectSetter(Field field) {
        Objects.requireNonNull(field, "field");
        if (Modifier.isFinal(field.getModifiers())) {
            return null;
        }
        try {
            return LOOKUP.unreflectSetter(field);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }

    public static Method selectMethod(Method[] methods, Object[] arguments) {
        Method selected = null;
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != arguments.length || !areCompatible(parameterTypes, arguments)) {
                continue;
            }
            if (selected == null || isMoreSpecific(parameterTypes, selected.getParameterTypes())) {
                selected = method;
            } else if (!isMoreSpecific(selected.getParameterTypes(), parameterTypes)) {
                throw new IllegalArgumentException("Ambiguous method overload " + method.getName());
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("No compatible method overload");
        }
        return selected;
    }

    public static Object invoke(MethodHandle handle, Object... arguments) {
        Objects.requireNonNull(handle, "handle");
        try {
            return handle.invokeWithArguments(arguments);
        } catch (Throwable throwable) {
            return throwInvocation(throwable);
        }
    }

    public static RuntimeException throwInvocation(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        return new IllegalStateException(throwable);
    }

    private static String getSystemProperty(String key, String defaultValue) {
        try {
            return System.getProperty(key, defaultValue);
        } catch (SecurityException ignored) {
            return defaultValue;
        }
    }

    private static Unsafe getUnsafe() {
        try {
            return UnsafeHacks.newInstance(Unsafe.class);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not obtain sun.misc.Unsafe", exception);
        }
    }

    private static MethodHandles.Lookup getTrustedLookup() {
        try {
            Field implLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object base = UNSAFE.staticFieldBase(implLookupField);
            long offset = UNSAFE.staticFieldOffset(implLookupField);
            MethodHandles.Lookup lookup = (MethodHandles.Lookup) UNSAFE.getObject(base, offset);
            if (lookup == null) {
                throw new IllegalStateException("MethodHandles.Lookup.IMPL_LOOKUP is null");
            }
            return lookup;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not obtain the trusted method lookup", exception);
        }
    }

    private static Constructor<?> findMatchingConstructor(Class<?> clazz, Object[] arguments) {
        Constructor<?> selected = null;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != arguments.length || !areCompatible(parameterTypes, arguments)) {
                continue;
            }
            if (selected == null || isMoreSpecific(parameterTypes, selected.getParameterTypes())) {
                selected = constructor;
            } else if (!isMoreSpecific(selected.getParameterTypes(), parameterTypes)) {
                throw new IllegalArgumentException("Ambiguous constructor for " + clazz.getName() + "; specify parameter types explicitly");
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("No matching constructor for " + clazz.getName() + " with arguments " + Arrays.toString(arguments));
        }
        return selected;
    }

    private static boolean areCompatible(Class<?>[] parameterTypes, Object[] arguments) {
        for (int index = 0; index < parameterTypes.length; index++) {
            Object argument = arguments[index];
            if (argument == null) {
                if (parameterTypes[index].isPrimitive()) {
                    return false;
                }
                continue;
            }
            if (!box(parameterTypes[index]).isInstance(argument)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMoreSpecific(Class<?>[] first, Class<?>[] second) {
        boolean moreSpecific = false;
        for (int index = 0; index < first.length; index++) {
            Class<?> firstType = box(first[index]);
            Class<?> secondType = box(second[index]);
            if (!secondType.isAssignableFrom(firstType)) {
                return false;
            }
            moreSpecific |= firstType != secondType;
        }
        return moreSpecific;
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == char.class) return Character.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        return Void.class;
    }

    private static String methodKey(Method method) {
        return method.getName() + MethodType.methodType(method.getReturnType(), method.getParameterTypes());
    }

    private static void collectInterfaceMethods(Class<?> iface, Map<String, Method> methods) {
        for (Method method : iface.getDeclaredMethods()) {
            methods.putIfAbsent(methodKey(method), method);
        }
        for (Class<?> parent : iface.getInterfaces()) {
            collectInterfaceMethods(parent, methods);
        }
    }

    private static void collectInterfaceFields(Class<?> iface, Map<String, Field> fields) {
        for (Field field : iface.getDeclaredFields()) {
            fields.putIfAbsent(field.getName(), field);
        }
        for (Class<?> parent : iface.getInterfaces()) {
            collectInterfaceFields(parent, fields);
        }
    }

    static {
        UNSAFE = getUnsafe();
        LOOKUP = getTrustedLookup();
    }
}
