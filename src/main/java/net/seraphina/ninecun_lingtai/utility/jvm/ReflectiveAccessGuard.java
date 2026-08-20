package net.seraphina.ninecun_lingtai.utility.jvm;

import net.seraphina.ninecun_lingtai.utility.jvm.trans.ReflectiveAccessTransformer;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;

/**
 * Runtime boundary used by {@link ReflectiveAccessTransformer}.
 *z
 * <p>This is deliberately a policy layer rather than a replacement for the JVM
 * security model. Code with an already attached agent, JVMTI access, or
 * {@code Unsafe} access can still bypass this boundary.</p>
 */
@SuppressWarnings("all")
public final class ReflectiveAccessGuard {
    public static final String TARGET_PACKAGE_PREFIX =
            "net.seraphina.ninecun_lingtai.";
    public static final String GUARD_INTERNAL_NAME =
            "net/seraphina/ninecun_lingtai/utility/jvm/ReflectiveAccessGuard";

    private static final StackWalker STACK_WALKER = StackWalker.getInstance(
            StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static Class<?> forName(String className) throws ClassNotFoundException {
        checkClassName(className, "Class.forName");
        return Class.forName(className);
    }

    public static Class<?> forName(
            String className,
            boolean initialize,
            ClassLoader loader
    ) throws ClassNotFoundException {
        checkClassName(className, "Class.forName");
        return Class.forName(className, initialize, loader);
    }

    public static Class<?> loadClass(
            ClassLoader loader,
            String className
    ) throws ClassNotFoundException {
        Objects.requireNonNull(loader, "loader");
        checkClassName(className, "ClassLoader.loadClass");
        return loader.loadClass(className);
    }

    public static Class<?> loadClass(
            ClassLoader loader,
            String className,
            boolean resolve
    ) throws ClassNotFoundException {
        Objects.requireNonNull(loader, "loader");
        checkClassName(className, "ClassLoader.loadClass");
        return resolve
                ? Class.forName(className, true, loader)
                : loader.loadClass(className);
    }

    public static Method getDeclaredMethod(
            Class<?> targetClass,
            String name,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        checkClass(targetClass, "Class.getDeclaredMethod");
        return targetClass.getDeclaredMethod(name, parameterTypes);
    }

    public static Method getMethod(
            Class<?> targetClass,
            String name,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        checkClass(targetClass, "Class.getMethod");
        return targetClass.getMethod(name, parameterTypes);
    }

    public static Method[] getDeclaredMethods(Class<?> targetClass) {
        checkClass(targetClass, "Class.getDeclaredMethods");
        return targetClass.getDeclaredMethods();
    }

    public static Method[] getMethods(Class<?> targetClass) {
        checkClass(targetClass, "Class.getMethods");
        return targetClass.getMethods();
    }

    public static Field getDeclaredField(
            Class<?> targetClass,
            String name
    ) throws NoSuchFieldException {
        checkClass(targetClass, "Class.getDeclaredField");
        return targetClass.getDeclaredField(name);
    }

    public static Field getField(
            Class<?> targetClass,
            String name
    ) throws NoSuchFieldException {
        checkClass(targetClass, "Class.getField");
        return targetClass.getField(name);
    }

    public static Field[] getDeclaredFields(Class<?> targetClass) {
        checkClass(targetClass, "Class.getDeclaredFields");
        return targetClass.getDeclaredFields();
    }

    public static Field[] getFields(Class<?> targetClass) {
        checkClass(targetClass, "Class.getFields");
        return targetClass.getFields();
    }

    public static Constructor<?> getDeclaredConstructor(
            Class<?> targetClass,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        checkClass(targetClass, "Class.getDeclaredConstructor");
        return targetClass.getDeclaredConstructor(parameterTypes);
    }

    public static Constructor<?> getConstructor(
            Class<?> targetClass,
            Class<?>... parameterTypes
    ) throws NoSuchMethodException {
        checkClass(targetClass, "Class.getConstructor");
        return targetClass.getConstructor(parameterTypes);
    }

    public static Constructor<?>[] getDeclaredConstructors(Class<?> targetClass) {
        checkClass(targetClass, "Class.getDeclaredConstructors");
        return targetClass.getDeclaredConstructors();
    }

    public static Constructor<?>[] getConstructors(Class<?> targetClass) {
        checkClass(targetClass, "Class.getConstructors");
        return targetClass.getConstructors();
    }

    public static Class<?>[] getDeclaredClasses(Class<?> targetClass) {
        checkClass(targetClass, "Class.getDeclaredClasses");
        return targetClass.getDeclaredClasses();
    }

    public static Class<?>[] getClasses(Class<?> targetClass) {
        checkClass(targetClass, "Class.getClasses");
        return targetClass.getClasses();
    }

    public static URL getResource(Class<?> targetClass, String name) {
        checkClass(targetClass, "Class.getResource");
        return targetClass.getResource(name);
    }

    @SuppressWarnings("deprecation")
    public static Object newInstance(Class<?> targetClass)
            throws InstantiationException, IllegalAccessException {
        checkClass(targetClass, "Class.newInstance");
        return targetClass.newInstance();
    }

    public static InputStream getResourceAsStream(Class<?> targetClass, String name) {
        checkClass(targetClass, "Class.getResourceAsStream");
        return targetClass.getResourceAsStream(name);
    }

    public static URL getResource(ClassLoader loader, String name) {
        Objects.requireNonNull(loader, "loader");
        checkResourceName(name, "ClassLoader.getResource");
        return loader.getResource(name);
    }

    public static InputStream getResourceAsStream(ClassLoader loader, String name) {
        Objects.requireNonNull(loader, "loader");
        checkResourceName(name, "ClassLoader.getResourceAsStream");
        return loader.getResourceAsStream(name);
    }

    public static Enumeration<URL> getResources(
            ClassLoader loader,
            String name
    ) throws IOException {
        Objects.requireNonNull(loader, "loader");
        checkResourceName(name, "ClassLoader.getResources");
        return loader.getResources(name);
    }

    public static void setAccessible(
            AccessibleObject object,
            boolean accessible
    ) {
        checkMember(object, "AccessibleObject.setAccessible");
        object.setAccessible(accessible);
    }

    public static boolean trySetAccessible(AccessibleObject object) {
        checkMember(object, "AccessibleObject.trySetAccessible");
        return object.trySetAccessible();
    }

    public static void setAccessible(
            AccessibleObject[] objects,
            boolean accessible
    ) {
        Objects.requireNonNull(objects, "objects");
        for (AccessibleObject object : objects) {
            checkMember(object, "AccessibleObject.setAccessible");
        }
        AccessibleObject.setAccessible(objects, accessible);
    }

    public static Object invoke(
            Method method,
            Object target,
            Object... arguments
    ) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Objects.requireNonNull(method, "method");
        checkMember(method, "Method.invoke");
        checkReflectiveGateway(method, target, arguments);
        return method.invoke(target, arguments);
    }

    public static Object newInstance(
            Constructor<?> constructor,
            Object... arguments
    ) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Objects.requireNonNull(constructor, "constructor");
        checkMember(constructor, "Constructor.newInstance");
        return constructor.newInstance(arguments);
    }

    public static Object get(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.get");
        return field.get(target);
    }

    public static boolean getBoolean(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getBoolean");
        return field.getBoolean(target);
    }

    public static byte getByte(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getByte");
        return field.getByte(target);
    }

    public static char getChar(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getChar");
        return field.getChar(target);
    }

    public static short getShort(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getShort");
        return field.getShort(target);
    }

    public static int getInt(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getInt");
        return field.getInt(target);
    }

    public static long getLong(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getLong");
        return field.getLong(target);
    }

    public static float getFloat(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getFloat");
        return field.getFloat(target);
    }

    public static double getDouble(Field field, Object target) throws IllegalAccessException {
        checkMember(field, "Field.getDouble");
        return field.getDouble(target);
    }

    public static void set(
            Field field,
            Object target,
            Object value
    ) throws IllegalAccessException {
        checkMember(field, "Field.set");
        field.set(target, value);
    }

    public static void setBoolean(
            Field field,
            Object target,
            boolean value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setBoolean");
        field.setBoolean(target, value);
    }

    public static void setByte(
            Field field,
            Object target,
            byte value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setByte");
        field.setByte(target, value);
    }

    public static void setChar(
            Field field,
            Object target,
            char value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setChar");
        field.setChar(target, value);
    }

    public static void setShort(
            Field field,
            Object target,
            short value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setShort");
        field.setShort(target, value);
    }

    public static void setInt(
            Field field,
            Object target,
            int value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setInt");
        field.setInt(target, value);
    }

    public static void setLong(
            Field field,
            Object target,
            long value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setLong");
        field.setLong(target, value);
    }

    public static void setFloat(
            Field field,
            Object target,
            float value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setFloat");
        field.setFloat(target, value);
    }

    public static void setDouble(
            Field field,
            Object target,
            double value
    ) throws IllegalAccessException {
        checkMember(field, "Field.setDouble");
        field.setDouble(target, value);
    }

    public static MethodHandles.Lookup privateLookupIn(
            Class<?> targetClass,
            MethodHandles.Lookup callerLookup
    ) throws IllegalAccessException {
        checkLookupTarget(callerLookup, targetClass, "privateLookupIn");
        return MethodHandles.privateLookupIn(targetClass, callerLookup);
    }

    public static <T extends Member> T reflectAs(
            Class<T> expected,
            MethodHandle target
    ) {
        if (!isTrustedCaller()) {
            throw new SecurityException("MethodHandles.reflectAs is denied");
        }
        return MethodHandles.reflectAs(expected, target);
    }

    public static MethodHandles.Lookup in(
            MethodHandles.Lookup lookup,
            Class<?> targetClass
    ) {
        checkClass(targetClass, "MethodHandles.Lookup.in");
        return lookup.in(targetClass);
    }

    public static Class<?> accessClass(
            MethodHandles.Lookup lookup,
            Class<?> targetClass
    ) throws IllegalAccessException {
        checkLookupTarget(lookup, targetClass, "accessClass");
        return lookup.accessClass(targetClass);
    }

    public static MethodHandle findStatic(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            MethodType type
    ) throws NoSuchMethodException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findStatic(targetClass, name, type);
    }

    public static MethodHandle findVirtual(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            MethodType type
    ) throws NoSuchMethodException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findVirtual(targetClass, name, type);
    }

    public static MethodHandle findConstructor(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            MethodType type
    ) throws NoSuchMethodException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, "<init>");
        return lookup.findConstructor(targetClass, type);
    }

    public static MethodHandle findSpecial(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            MethodType type,
            Class<?> specialCaller
    ) throws NoSuchMethodException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        checkLookupTarget(lookup, specialCaller, name);
        return lookup.findSpecial(targetClass, name, type, specialCaller);
    }

    public static MethodHandle findGetter(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            Class<?> fieldType
    ) throws NoSuchFieldException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findGetter(targetClass, name, fieldType);
    }

    public static MethodHandle findSetter(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            Class<?> fieldType
    ) throws NoSuchFieldException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findSetter(targetClass, name, fieldType);
    }

    public static MethodHandle findStaticGetter(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            Class<?> fieldType
    ) throws NoSuchFieldException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findStaticGetter(targetClass, name, fieldType);
    }

    public static MethodHandle findStaticSetter(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            Class<?> fieldType
    ) throws NoSuchFieldException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findStaticSetter(targetClass, name, fieldType);
    }

    public static VarHandle findVarHandle(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            Class<?> fieldType
    ) throws NoSuchFieldException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findVarHandle(targetClass, name, fieldType);
    }

    public static VarHandle findStaticVarHandle(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String name,
            Class<?> fieldType
    ) throws NoSuchFieldException, IllegalAccessException {
        checkLookupTarget(lookup, targetClass, name);
        return lookup.findStaticVarHandle(targetClass, name, fieldType);
    }

    public static Class<?> findClass(
            MethodHandles.Lookup lookup,
            String className
    ) throws ClassNotFoundException, IllegalAccessException {
        checkClassName(className, "MethodHandles.Lookup.findClass");
        return lookup.findClass(className);
    }

    public static MethodHandle bind(
            MethodHandles.Lookup lookup,
            Object receiver,
            String name,
            MethodType type
    ) throws NoSuchMethodException, IllegalAccessException {
        checkClass(lookup.lookupClass(), "MethodHandles.Lookup.bind");
        return lookup.bind(receiver, name, type);
    }

    public static MethodHandle unreflect(
            MethodHandles.Lookup lookup,
            Method method
    ) throws IllegalAccessException {
        checkLookupMember(lookup, method);
        return lookup.unreflect(method);
    }

    public static MethodHandle unreflectSpecial(
            MethodHandles.Lookup lookup,
            Method method,
            Class<?> specialCaller
    ) throws IllegalAccessException {
        checkLookupMember(lookup, method);
        checkLookupTarget(lookup, specialCaller, "unreflectSpecial");
        return lookup.unreflectSpecial(method, specialCaller);
    }

    public static MethodHandle unreflectConstructor(
            MethodHandles.Lookup lookup,
            Constructor<?> constructor
    ) throws IllegalAccessException {
        checkLookupMember(lookup, constructor);
        return lookup.unreflectConstructor(constructor);
    }

    public static MethodHandle unreflectGetter(
            MethodHandles.Lookup lookup,
            Field field
    ) throws IllegalAccessException {
        checkLookupMember(lookup, field);
        return lookup.unreflectGetter(field);
    }

    public static MethodHandle unreflectSetter(
            MethodHandles.Lookup lookup,
            Field field
    ) throws IllegalAccessException {
        checkLookupMember(lookup, field);
        return lookup.unreflectSetter(field);
    }

    public static VarHandle unreflectVarHandle(
            MethodHandles.Lookup lookup,
            Field field
    ) throws IllegalAccessException {
        checkLookupMember(lookup, field);
        return lookup.unreflectVarHandle(field);
    }

    public static MethodHandleInfo revealDirect(
            MethodHandles.Lookup lookup,
            MethodHandle target
    ) {
        if (!isTrustedCaller()) {
            throw new SecurityException("MethodHandles.Lookup.revealDirect is denied");
        }
        return lookup.revealDirect(target);
    }

    public static boolean isProtectedClass(Class<?> targetClass) {
        return targetClass != null
                && targetClass.getName().startsWith(TARGET_PACKAGE_PREFIX);
    }

    public static void checkClass(Class<?> targetClass, String operation) {
        Objects.requireNonNull(targetClass, "targetClass");
        if (!isProtectedClass(targetClass) || isTrustedCaller()) {
            return;
        }
        throw accessDenied(operation, targetClass.getName());
    }

    private static void checkClassName(String className, String operation)
            throws ClassNotFoundException {
        Objects.requireNonNull(className, "className");
        if (!isProtectedClassName(className) || isTrustedCaller()) {
            return;
        }
        throw new ClassNotFoundException(className);
    }

    private static void checkResourceName(String resourceName, String operation) {
        Objects.requireNonNull(resourceName, "resourceName");
        String normalizedName = resourceName.startsWith("/")
                ? resourceName.substring(1)
                : resourceName;
        if (!normalizedName.startsWith(TARGET_PACKAGE_PREFIX.replace('.', '/'))
                || isTrustedCaller()) {
            return;
        }
        throw accessDenied(operation, resourceName);
    }

    private static void checkMember(AccessibleObject object, String operation) {
        Objects.requireNonNull(object, "object");
        if (!(object instanceof Member member)) {
            return;
        }
        checkClass(member.getDeclaringClass(), operation);
    }

    private static void checkReflectiveGateway(
            Method method,
            Object target,
            Object[] arguments
    ) {
        Class<?> declaringClass = method.getDeclaringClass();
        String methodName = method.getName();
        if (declaringClass == Class.class) {
            if (target instanceof Class<?> targetClass) {
                checkClass(targetClass, "Method.invoke -> Class." + methodName);
            }
            if ("forName".equals(methodName)) {
                checkClassNameArgument(arguments, "Method.invoke -> Class.forName");
            }
            return;
        }
        if (declaringClass == ClassLoader.class) {
            if ("loadClass".equals(methodName)) {
                checkClassNameArgument(arguments, "Method.invoke -> ClassLoader.loadClass");
            } else if ("getResource".equals(methodName)
                    || "getResourceAsStream".equals(methodName)
                    || "getResources".equals(methodName)) {
                checkResourceNameArgument(arguments, "Method.invoke -> ClassLoader." + methodName);
            }
            return;
        }
        if (AccessibleObject.class.isAssignableFrom(declaringClass)
                && target instanceof AccessibleObject object) {
            checkMember(object, "Method.invoke -> " + declaringClass.getSimpleName()
                    + "." + methodName);
            return;
        }
        if (declaringClass == MethodHandles.Lookup.class) {
            checkLookupGatewayInvocation(methodName, target, arguments);
            return;
        }
        if (declaringClass == MethodHandles.class
                && ("privateLookupIn".equals(methodName)
                || "reflectAs".equals(methodName))) {
            if ("privateLookupIn".equals(methodName)) {
                checkClassArgument(arguments, "Method.invoke -> MethodHandles.privateLookupIn");
            } else {
                denyUntrustedGateway("MethodHandles.reflectAs");
            }
        }
    }

    private static void checkLookupGatewayInvocation(
            String methodName,
            Object target,
            Object[] arguments
    ) {
        if (isTrustedCaller()) {
            return;
        }
        if (target instanceof MethodHandles.Lookup lookup) {
            checkClass(lookup.lookupClass(), "Method.invoke -> Lookup." + methodName);
        }
        switch (methodName) {
            case "in", "findStatic", "findVirtual", "findConstructor",
                    "findSpecial", "findGetter", "findSetter",
                    "findStaticGetter", "findStaticSetter", "findVarHandle",
                    "findStaticVarHandle", "accessClass" ->
                    checkClassArgument(arguments, "Method.invoke -> Lookup." + methodName);
            case "findClass" ->
                    checkClassNameArgument(arguments, "Method.invoke -> Lookup.findClass");
            case "bind" -> {
                if (arguments != null
                        && arguments.length > 0
                        && arguments[0] != null) {
                    checkClass(arguments[0].getClass(), "Method.invoke -> Lookup.bind");
                }
            }
            case "unreflect", "unreflectSpecial", "unreflectConstructor",
                    "unreflectGetter", "unreflectSetter", "unreflectVarHandle" -> {
                if (arguments != null
                        && arguments.length > 0
                        && arguments[0] instanceof Member member) {
                    checkClass(member.getDeclaringClass(),
                            "Method.invoke -> Lookup." + methodName);
                    if (isSensitiveReflectionGateway(
                            member.getDeclaringClass(), member.getName())) {
                        denyUntrustedGateway("Lookup." + methodName);
                    }
                }
            }
            case "revealDirect" -> denyUntrustedGateway("Lookup.revealDirect");
            default -> {
            }
        }
    }

    private static void checkLookupTarget(
            MethodHandles.Lookup lookup,
            Class<?> targetClass,
            String methodName
    ) throws IllegalAccessException {
        Objects.requireNonNull(lookup, "lookup");
        Objects.requireNonNull(targetClass, "targetClass");
        if (isTrustedCaller()) {
            return;
        }
        if (isProtectedClass(targetClass)) {
            throw new IllegalAccessException(
                    "Access to " + targetClass.getName() + " was denied");
        }
        if (isSensitiveReflectionGateway(targetClass, methodName)) {
            throw new IllegalAccessException(
                    "Lookup access to " + targetClass.getName()
                            + "." + methodName + " was denied");
        }
    }

    private static void checkLookupMember(
            MethodHandles.Lookup lookup,
            Member member
    ) throws IllegalAccessException {
        Objects.requireNonNull(member, "member");
        checkLookupTarget(lookup, member.getDeclaringClass(), member.getName());
    }

    private static void checkClassArgument(Object[] arguments, String operation) {
        if (arguments == null || arguments.length == 0
                || !(arguments[0] instanceof Class<?> targetClass)) {
            return;
        }
        checkClass(targetClass, operation);
    }

    private static void checkClassNameArgument(Object[] arguments, String operation) {
        if (arguments == null || arguments.length == 0
                || !(arguments[0] instanceof String className)) {
            return;
        }
        try {
            checkClassName(className, operation);
        } catch (ClassNotFoundException exception) {
            throw new SecurityException("Access to a protected class was denied", exception);
        }
    }

    private static void checkResourceNameArgument(Object[] arguments, String operation) {
        if (arguments == null || arguments.length == 0
                || !(arguments[0] instanceof String resourceName)) {
            return;
        }
        checkResourceName(resourceName, operation);
    }

    private static boolean isSensitiveReflectionGateway(
            Class<?> targetClass,
            String methodName
    ) {
        if (targetClass == Class.class) {
            return switch (methodName) {
                case "forName", "newInstance", "getDeclaredMethod", "getMethod",
                        "getDeclaredMethods", "getMethods", "getDeclaredField",
                        "getField", "getDeclaredFields", "getFields",
                        "getDeclaredConstructor", "getConstructor",
                        "getDeclaredConstructors", "getConstructors",
                        "getDeclaredClasses", "getClasses", "getResource",
                        "getResourceAsStream" -> true;
                default -> false;
            };
        }
        if (targetClass == ClassLoader.class) {
            return "loadClass".equals(methodName)
                    || "getResource".equals(methodName)
                    || "getResourceAsStream".equals(methodName)
                    || "getResources".equals(methodName);
        }
        if (targetClass == Method.class) {
            return "invoke".equals(methodName)
                    || "setAccessible".equals(methodName)
                    || "trySetAccessible".equals(methodName);
        }
        if (targetClass == Constructor.class) {
            return "newInstance".equals(methodName)
                    || "setAccessible".equals(methodName)
                    || "trySetAccessible".equals(methodName);
        }
        if (targetClass == Field.class) {
            return methodName.startsWith("get")
                    || methodName.startsWith("set")
                    || "trySetAccessible".equals(methodName);
        }
        if (targetClass == AccessibleObject.class) {
            return "setAccessible".equals(methodName)
                    || "trySetAccessible".equals(methodName);
        }
        if (targetClass == MethodHandles.Lookup.class) {
            return switch (methodName) {
                case "in", "findStatic", "findVirtual", "findConstructor",
                        "findSpecial", "findGetter", "findSetter",
                        "findStaticGetter", "findStaticSetter", "findVarHandle",
                        "findStaticVarHandle", "findClass", "bind", "unreflect",
                        "unreflectSpecial", "unreflectConstructor",
                        "unreflectGetter", "unreflectSetter", "unreflectVarHandle",
                        "accessClass", "revealDirect" -> true;
                default -> false;
            };
        }
        return false;
    }

    private static void denyUntrustedGateway(String operation) {
        if (!isTrustedCaller()) {
            throw new SecurityException("Reflective gateway access was denied: " + operation);
        }
    }

    private static boolean isProtectedClassName(String className) {
        String normalizedName = className.replace('/', '.');
        return normalizedName.startsWith(TARGET_PACKAGE_PREFIX);
    }

    private static SecurityException accessDenied(String operation, String target) {
        return new SecurityException(operation + " is denied for " + target);
    }

    private static boolean isTrustedCaller() {
        Class<?> callerClass = STACK_WALKER.<Class<?>>walk(frames -> {
            var iterator = frames.iterator();
            while (iterator.hasNext()) {
                Class<?> type = iterator.next().getDeclaringClass();
                if (type != ReflectiveAccessGuard.class) {
                    return type;
                }
            }
            return null;
        });
        if (callerClass == null) {
            return false;
        }

        if (!callerClass.getName().startsWith(TARGET_PACKAGE_PREFIX)) {
            return false;
        }

        return sameCodeSource(callerClass, ReflectiveAccessGuard.class);
    }

    private static boolean sameCodeSource(Class<?> first, Class<?> second) {
        if (first.getClassLoader() != second.getClassLoader()) {
            return false;
        }
        if (first.getProtectionDomain() == null
                || second.getProtectionDomain() == null
                || first.getProtectionDomain().getCodeSource() == null
                || second.getProtectionDomain().getCodeSource() == null
                || first.getProtectionDomain().getCodeSource().getLocation() == null
                || second.getProtectionDomain().getCodeSource().getLocation() == null) {
            return false;
        }
        return first.getProtectionDomain().getCodeSource().getLocation().equals(
                second.getProtectionDomain().getCodeSource().getLocation());
    }

}
