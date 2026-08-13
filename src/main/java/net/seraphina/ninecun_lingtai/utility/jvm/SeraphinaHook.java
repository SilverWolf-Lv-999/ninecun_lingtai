package net.seraphina.ninecun_lingtai.utility.jvm;

import net.seraphina.ninecun_lingtai.utility.JDKUtility;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class SeraphinaHook {

    /*
     * JDK 17 x64 HotSpot layout:
     * ResolvedMethodName::_vmtarget = 16
     * Method::_constMethod = 8
     * Method::_vtable_index = 44
     * ConstMethod::_code_size = 32
     * ConstMethod::code_base() = 48
     *
     * The layout probe below verifies these values before any target method is
     * modified, so an unsupported VM fails before touching Minecraft metadata.
     */
    private static final long RESOLVED_METHOD_NAME_VMTARGET_OFFSET = 16L;
    private static final long METHOD_CONST_METHOD_OFFSET = 8L;
    private static final long METHOD_VTABLE_INDEX_OFFSET = 44L;
    private static final long CONST_METHOD_CODE_SIZE_OFFSET = 32L;
    private static final long CONST_METHOD_CODE_OFFSET = 48L;
    private static final long CLASS_KLASS_OFFSET = 16L;
    private static final long KLASS_JAVA_MIRROR_OFFSET = 112L;
    private static final long KLASS_SUBKLASS_OFFSET = 128L;
    private static final long KLASS_NEXT_SIBLING_OFFSET = 136L;
    private static final long VTABLE_LAYOUT_SCAN_BYTES = 2048L;
    private static final byte NOP = 0x00;
    private static final byte RETURN = (byte) 0xB1;

    private static volatile boolean initialized;
    private static Class<?> directMethodHandleClass;
    private static long directMethodHandleMemberOffset;
    private static long memberNameResolvedMethodOffset;
    private static ReferenceSlot referenceSlot;
    private static long referenceSlotValueOffset;
    private static long narrowOopBase;
    private static int narrowOopShift;
    private static long vtableStartOffset;

    public static void hookMethod(Class<?> klass, SeraMapping methodName, Object returnValue) {
        Objects.requireNonNull(klass, "klass");
        Objects.requireNonNull(methodName, "methodName");

        for (Method method : resolveMethods(klass, methodName, new Class<?>[0])) {
            replaceBytecodes(method, constantReturn(method.getReturnType(), returnValue));
        }
    }

    public static void replaceMethod(Class<?> target, Class<?> now) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(now, "now");

        if (target == now) {
            return;
        }
        if (!target.isAssignableFrom(now)) {
            throw new IllegalArgumentException(
                    now.getName() + " must extend or implement " + target.getName());
        }
        if (target.isInterface()) {
            throw new IllegalArgumentException(
                    "Interface method replacement is not supported: " + target.getName());
        }
        if (target.getClassLoader() != now.getClassLoader()) {
            throw new IllegalArgumentException(
                    target.getName() + " and " + now.getName()
                            + " must be loaded by the same class loader");
        }

        int replacedCount = 0;
        for (Method replacement : now.getDeclaredMethods()) {
            Method targetMethod = findMatchingDeclaredMethod(target, replacement);
            if (targetMethod == null) {
                continue;
            }
            if (Modifier.isAbstract(replacement.getModifiers())
                    || Modifier.isNative(replacement.getModifiers())) {
                throw new IllegalArgumentException(
                        "Replacement method must contain Java bytecode: " + replacement);
            }
            if (Modifier.isAbstract(targetMethod.getModifiers())
                    || Modifier.isNative(targetMethod.getModifiers())) {
                throw new IllegalArgumentException(
                        "Target method must contain Java bytecode: " + targetMethod);
            }

            if (Modifier.isStatic(targetMethod.getModifiers())
                    || Modifier.isPrivate(targetMethod.getModifiers())
                    || Modifier.isFinal(targetMethod.getModifiers())) {
                for (Method method : resolveMethods(target, targetMethod)) {
                    replaceMethodImplementation(method, replacement);
                }
            } else {
                replaceVirtualMethod(target, targetMethod, replacement);
            }
            replacedCount++;
        }

        if (replacedCount == 0) {
            throw new IllegalArgumentException(
                    now.getName() + " does not declare a replaceable method from " + target.getName());
        }
    }

    public static void hookVoidMethod(Class<?> klass, SeraMapping methodName, byte[] bytes) {
        hookVoidMethod(klass, methodName, bytes, new Class<?>[0]);
    }

    public static void hookVoidMethod(
            Class<?> klass, SeraMapping methodName, Class<?>... parameterTypes) {
        hookVoidMethod(klass, methodName, new byte[]{RETURN}, parameterTypes);
    }

    public static void hookVoidMethod(
            Class<?> klass, SeraMapping methodName, byte[] bytes, Class<?>... parameterTypes) {
        Objects.requireNonNull(klass, "klass");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(parameterTypes, "parameterTypes");

        if (bytes.length == 0) {
            throw new IllegalArgumentException("Replacement bytecode must not be empty");
        }

        for (Method method : resolveMethods(klass, methodName, parameterTypes)) {
            if (method.getReturnType() != void.class) {
                throw new IllegalArgumentException(method + " does not return void");
            }
            replaceBytecodes(method, bytes.clone());
        }
    }

    private static List<Method> resolveMethods(
            Class<?> klass, SeraMapping methodName, Class<?>[] parameterTypes) {
        Set<Method> methods = new LinkedHashSet<>();
        Method method = findMethodInHierarchy(klass, methodName.get(), parameterTypes);
        if (method == null) {
            throw new IllegalArgumentException(
                    "No method " + methodName.get() + Arrays.toString(parameterTypes)
                            + " on " + klass.getName());
        }
        methods.add(method);

        for (Class<?> subclass : findLoadedSubclasses(klass)) {
            for (Method declaredMethod : subclass.getDeclaredMethods()) {
                if (declaredMethod.getName().equals(methodName.get())
                        && Arrays.equals(declaredMethod.getParameterTypes(), parameterTypes)) {
                    makeAccessible(declaredMethod);
                    methods.add(declaredMethod);
                }
            }
        }
        return new ArrayList<>(methods);
    }

    private static List<Method> resolveMethods(Class<?> klass, Method template) {
        Set<Method> methods = new LinkedHashSet<>();
        methods.add(template);

        for (Class<?> subclass : findLoadedSubclasses(klass)) {
            Method method = findMatchingDeclaredMethod(subclass, template);
            if (method != null) {
                makeAccessible(method);
                methods.add(method);
            }
        }
        return new ArrayList<>(methods);
    }

    private static Method findMethodInHierarchy(
            Class<?> klass, String methodName, Class<?>[] parameterTypes) {
        for (Class<?> current = klass; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                makeAccessible(method);
                return method;
            } catch (NoSuchMethodException ignored) {
                // Keep looking through the superclass hierarchy.
            }
        }
        return null;
    }

    private static Method findMatchingDeclaredMethod(Class<?> klass, Method template) {
        for (Method method : klass.getDeclaredMethods()) {
            if (!method.getName().equals(template.getName())
                    || method.getReturnType() != template.getReturnType()
                    || Modifier.isStatic(method.getModifiers())
                    != Modifier.isStatic(template.getModifiers())
                    || !Arrays.equals(method.getParameterTypes(), template.getParameterTypes())) {
                continue;
            }
            makeAccessible(method);
            return method;
        }
        return null;
    }

    private static void makeAccessible(Method method) {
        if (!method.trySetAccessible()) {
            throw new IllegalArgumentException("Cannot access method " + method);
        }
    }

    private static List<Class<?>> findLoadedSubclasses(Class<?> klass) {
        try {
            initialize();
            long rootKlass = klassPointer(klass);
            if (rootKlass == 0L) {
                throw new IllegalStateException("Class does not contain a Klass*");
            }

            Deque<Long> pending = new ArrayDeque<>();
            Set<Long> visited = new HashSet<>();
            Set<Class<?>> subclasses = new LinkedHashSet<>();
            pending.addLast(rootKlass);
            visited.add(rootKlass);

            while (!pending.isEmpty()) {
                long parentKlass = pending.removeFirst();
                Set<Long> siblings = new HashSet<>();
                long childKlass = JDKUtility.UNSAFE.getLong(parentKlass + KLASS_SUBKLASS_OFFSET);
                while (childKlass != 0L) {
                    if (!siblings.add(childKlass)) {
                        throw new IllegalStateException("Cycle in HotSpot Klass sibling chain");
                    }

                    if (visited.add(childKlass)) {
                        Class<?> subclass = classMirror(childKlass);
                        if (subclass == null || !klass.isAssignableFrom(subclass)) {
                            throw new IllegalStateException(
                                    "Klass child does not resolve to a subclass of " + klass.getName());
                        }
                        subclasses.add(subclass);
                        pending.addLast(childKlass);
                    }

                    childKlass = JDKUtility.UNSAFE.getLong(childKlass + KLASS_NEXT_SIBLING_OFFSET);
                }
            }
            return new ArrayList<>(subclasses);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not traverse HotSpot Klass subclasses for " + klass.getName(), e);
        }
    }

    private static void replaceBytecodes(Method method, byte[] replacement) {
        MethodLocation location;
        try {
            initialize();
            location = locate(method);
        } catch (Exception e) {
            throw new IllegalStateException("Could not locate bytecodes for " + method, e);
        }

        if (replacement.length > location.codeSize) {
            throw new IllegalArgumentException(
                    method + " has " + location.codeSize + " bytes of code, but replacement needs "
                            + replacement.length);
        }

        try {
            // Keep the allocated code size unchanged. Trailing NOPs make a
            // shorter replacement safe after its terminal return instruction.
            for (int i = replacement.length; i < location.codeSize; i++) {
                JDKUtility.UNSAFE.putByte(location.codeAddress + i, NOP);
            }
            for (int i = replacement.length - 1; i >= 0; i--) {
                JDKUtility.UNSAFE.putByte(location.codeAddress + i, replacement[i]);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not replace bytecodes for " + method, e);
        }
    }

    private static void replaceMethodImplementation(Method target, Method replacement) {
        try {
            initialize();
            MethodLocation targetLocation = locate(target);
            MethodLocation replacementLocation = locate(replacement);
            JDKUtility.UNSAFE.putLong(
                    targetLocation.methodAddress + METHOD_CONST_METHOD_OFFSET,
                    replacementLocation.constMethodAddress);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not replace " + target + " with " + replacement, e);
        }
    }

    private static void replaceVirtualMethod(
            Class<?> targetClass, Method targetMethod, Method replacement) {
        try {
            initialize();
            long replacementMethodAddress = locate(replacement).methodAddress;
            replaceVtableEntry(targetClass, targetMethod, replacementMethodAddress);

            for (Class<?> subclass : findLoadedSubclasses(targetClass)) {
                Method declaredMethod = findMatchingDeclaredMethod(subclass, targetMethod);
                if (declaredMethod != null) {
                    replaceVtableEntry(subclass, declaredMethod, replacementMethodAddress);
                } else {
                    replaceVtableEntry(subclass, targetMethod, replacementMethodAddress);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not replace virtual method " + targetMethod
                            + " with " + replacement, e);
        }
    }

    private static void replaceVtableEntry(
            Class<?> klass, Method method, long replacementMethodAddress) {
        MethodLocation methodLocation;
        try {
            methodLocation = locate(method);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not locate " + method, e);
        }

        long klassAddress = klassPointer(klass);
        int vtableIndex = JDKUtility.UNSAFE.getInt(
                methodLocation.methodAddress + METHOD_VTABLE_INDEX_OFFSET);
        if (vtableIndex < 0) {
            throw new IllegalStateException(
                    "Method does not have a virtual dispatch slot: " + method);
        }

        long vtableEntryAddress = klassAddress + vtableStartOffset
                + (long) vtableIndex * Long.BYTES;
        if (JDKUtility.UNSAFE.getLong(vtableEntryAddress) != methodLocation.methodAddress) {
            throw new IllegalStateException(
                    "Vtable slot does not contain " + klass.getName() + "." + method.getName());
        }
        JDKUtility.UNSAFE.putLongVolatile(
                null, vtableEntryAddress, replacementMethodAddress);
    }

    private static long findVtableEntry(long klassAddress, long methodAddress) {
        long vtableEntryAddress = 0L;
        for (long offset = 0L;
             offset <= VTABLE_LAYOUT_SCAN_BYTES - Long.BYTES;
             offset += Long.BYTES) {
            long candidateAddress = klassAddress + offset;
            if (JDKUtility.UNSAFE.getLong(candidateAddress) != methodAddress) {
                continue;
            }
            if (vtableEntryAddress != 0L) {
                throw new IllegalStateException("Method appears in multiple vtable entries");
            }
            vtableEntryAddress = candidateAddress;
        }

        if (vtableEntryAddress == 0L) {
            throw new IllegalStateException("Could not find vtable entry");
        }
        return vtableEntryAddress;
    }

    private static byte[] constantReturn(Class<?> returnType, Object value) {
        if (returnType == void.class) {
            throw new IllegalArgumentException("Use hookVoidMethod for void methods");
        }

        if (returnType == boolean.class) {
            if (!(value instanceof Boolean)) {
                throw incompatibleReturnValue(returnType, value);
            }
            return new byte[]{(byte) ((Boolean) value ? 0x04 : 0x03), (byte) 0xAC};
        }

        if (returnType == byte.class) {
            if (!(value instanceof Byte)) {
                throw incompatibleReturnValue(returnType, value);
            }
            return intReturn(((Byte) value).intValue());
        }

        if (returnType == short.class) {
            if (!(value instanceof Short)) {
                throw incompatibleReturnValue(returnType, value);
            }
            return intReturn(((Short) value).intValue());
        }

        if (returnType == char.class) {
            if (!(value instanceof Character)) {
                throw incompatibleReturnValue(returnType, value);
            }
            return intReturn(((Character) value).charValue());
        }

        if (returnType == int.class) {
            if (!(value instanceof Integer)) {
                throw incompatibleReturnValue(returnType, value);
            }
            return intReturn((Integer) value);
        }

        if (returnType == long.class) {
            if (!(value instanceof Long)) {
                throw incompatibleReturnValue(returnType, value);
            }
            long result = (Long) value;
            if (result == 0L) return new byte[]{0x09, (byte) 0xAD};
            if (result == 1L) return new byte[]{0x0A, (byte) 0xAD};
            throw unsupportedConstant(returnType, value);
        }

        if (returnType == float.class) {
            if (!(value instanceof Float)) {
                throw incompatibleReturnValue(returnType, value);
            }
            float result = (Float) value;
            if (Float.floatToRawIntBits(result) == Float.floatToRawIntBits(0.0F)) {
                return new byte[]{0x0B, (byte) 0xAE};
            }
            if (result == 1.0F) return new byte[]{0x0C, (byte) 0xAE};
            if (result == 2.0F) return new byte[]{0x0D, (byte) 0xAE};
            throw unsupportedConstant(returnType, value);
        }

        if (returnType == double.class) {
            if (!(value instanceof Double)) {
                throw incompatibleReturnValue(returnType, value);
            }
            double result = (Double) value;
            if (Double.doubleToRawLongBits(result) == Double.doubleToRawLongBits(0.0D)) {
                return new byte[]{0x0E, (byte) 0xAF};
            }
            if (result == 1.0D) return new byte[]{0x0F, (byte) 0xAF};
            throw unsupportedConstant(returnType, value);
        }

        if (value == null) {
            return new byte[]{0x01, (byte) 0xB0};
        }
        throw unsupportedConstant(returnType, value);
    }

    private static byte[] intReturn(int value) {
        if (value >= -1 && value <= 5) {
            int opcode = value == -1 ? 0x02 : 0x03 + value;
            return new byte[]{(byte) opcode, (byte) 0xAC};
        }
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            return new byte[]{0x10, (byte) value, (byte) 0xAC};
        }
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            return new byte[]{
                    0x11,
                    (byte) (value >>> 8),
                    (byte) value,
                    (byte) 0xAC
            };
        }
        throw unsupportedConstant(int.class, value);
    }

    private static IllegalArgumentException incompatibleReturnValue(Class<?> returnType, Object value) {
        String actualType = value == null ? "null" : value.getClass().getName();
        return new IllegalArgumentException(
                "Cannot return " + actualType + " from " + returnType.getTypeName());
    }

    private static IllegalArgumentException unsupportedConstant(Class<?> returnType, Object value) {
        return new IllegalArgumentException(
                "Cannot encode " + value + " as a " + returnType.getTypeName()
                        + " constant without changing the target constant pool");
    }

    private static void initialize() {
        if (initialized) {
            return;
        }

        synchronized (SeraphinaHook.class) {
            if (initialized) {
                return;
            }

            try {
                directMethodHandleClass = Class.forName("java.lang.invoke.DirectMethodHandle");
                Class<?> memberName = Class.forName("java.lang.invoke.MemberName");

                Field directMember = declaredField(directMethodHandleClass, "member");
                Field resolvedMethod = declaredField(memberName, "method");
                Field referenceValue = declaredField(ReferenceSlot.class, "value");

                directMethodHandleMemberOffset = JDKUtility.UNSAFE.objectFieldOffset(directMember);
                memberNameResolvedMethodOffset = JDKUtility.UNSAFE.objectFieldOffset(resolvedMethod);
                referenceSlot = new ReferenceSlot();
                referenceSlotValueOffset = JDKUtility.UNSAFE.objectFieldOffset(referenceValue);

                NarrowOopEncoding encoding = deriveNarrowOopEncoding();
                narrowOopBase = encoding.base;
                narrowOopShift = encoding.shift;
                verifyKlassLayout();
                verifyLayout();
                verifyVtableLayout();
                initialized = true;
            } catch (Exception e) {
                throw new IllegalStateException("JDK 17 HotSpot Unsafe hook is unavailable", e);
            }
        }
    }

    private static void verifyVtableLayout() throws Exception {
        Class<?> probeClass = VtableLayoutProbe.class;
        Method first = probeClass.getDeclaredMethod("first");
        Method second = probeClass.getDeclaredMethod("second");
        Method third = probeClass.getDeclaredMethod("third");
        makeAccessible(first);
        makeAccessible(second);
        makeAccessible(third);

        MethodLocation firstLocation = locate(first);
        long klassAddress = klassPointer(probeClass);
        int firstVtableIndex = JDKUtility.UNSAFE.getInt(
                firstLocation.methodAddress + METHOD_VTABLE_INDEX_OFFSET);
        if (firstVtableIndex < 0) {
            throw new IllegalStateException("Virtual probe method does not have a vtable index");
        }

        long vtableEntry = findVtableEntry(klassAddress, firstLocation.methodAddress);
        long startOffset = vtableEntry - klassAddress
                - (long) firstVtableIndex * Long.BYTES;
        if (startOffset < 0L || startOffset >= VTABLE_LAYOUT_SCAN_BYTES) {
            throw new IllegalStateException("Unsupported HotSpot vtable start offset");
        }

        verifyVtableEntry(klassAddress, startOffset, second);
        verifyVtableEntry(klassAddress, startOffset, third);
        vtableStartOffset = startOffset;
    }

    private static void verifyVtableEntry(
            long klassAddress, long startOffset, Method method) throws Exception {
        MethodLocation location = locate(method);
        int vtableIndex = JDKUtility.UNSAFE.getInt(
                location.methodAddress + METHOD_VTABLE_INDEX_OFFSET);
        if (vtableIndex < 0L
                || JDKUtility.UNSAFE.getLong(
                        klassAddress + startOffset + (long) vtableIndex * Long.BYTES)
                != location.methodAddress) {
            throw new IllegalStateException("Unsupported HotSpot Method/vtable layout");
        }
    }

    private static void verifyLayout() throws Exception {
        Method probe = SeraphinaHook.class.getDeclaredMethod("layoutProbe");
        makeAccessible(probe);
        MethodLocation location = locate(probe);
        if (location.codeSize != 2
                || JDKUtility.UNSAFE.getByte(location.codeAddress) != 0x03
                || JDKUtility.UNSAFE.getByte(location.codeAddress + 1) != (byte) 0xAC) {
            throw new IllegalStateException("Unsupported HotSpot Method/ConstMethod layout");
        }
    }

    private static NarrowOopEncoding deriveNarrowOopEncoding() throws Exception {
        long parentMirror = rawJavaMirror(klassPointer(KlassLayoutParent.class));
        long childMirror = rawJavaMirror(klassPointer(KlassLayoutChild.class));
        long parentNarrow = narrowOop(KlassLayoutParent.class);
        long childNarrow = narrowOop(KlassLayoutChild.class);
        long rawDelta = childMirror - parentMirror;
        long narrowDelta = childNarrow - parentNarrow;

        if (rawDelta == 0L || narrowDelta == 0L || rawDelta % narrowDelta != 0L) {
            throw new IllegalStateException("Could not derive compressed OOP encoding");
        }

        long scale = rawDelta / narrowDelta;
        if (scale <= 0L || Long.bitCount(scale) != 1) {
            throw new IllegalStateException("Unsupported compressed OOP scale " + scale);
        }

        int shift = Long.numberOfTrailingZeros(scale);
        long base = parentMirror - (parentNarrow << shift);
        return new NarrowOopEncoding(base, shift);
    }

    private static void verifyKlassLayout() throws Exception {
        long parentKlass = klassPointer(KlassLayoutParent.class);
        long childKlass = klassPointer(KlassLayoutChild.class);
        long firstChild = JDKUtility.UNSAFE.getLong(parentKlass + KLASS_SUBKLASS_OFFSET);
        if (parentKlass == 0L || childKlass == 0L || firstChild != childKlass) {
            throw new IllegalStateException("Unsupported HotSpot Klass subclass layout");
        }

        if (classMirror(childKlass) != KlassLayoutChild.class) {
            throw new IllegalStateException("Unsupported HotSpot Klass java mirror layout");
        }
    }

    private static long klassPointer(Class<?> klass) {
        return JDKUtility.UNSAFE.getLong(klass, CLASS_KLASS_OFFSET);
    }

    private static long rawJavaMirror(long klassPointer) {
        if (klassPointer == 0L) {
            throw new IllegalStateException("Klass pointer is null");
        }

        long mirrorHandle = JDKUtility.UNSAFE.getLong(klassPointer + KLASS_JAVA_MIRROR_OFFSET);
        if (mirrorHandle == 0L) {
            throw new IllegalStateException("Klass does not contain a java mirror handle");
        }

        long rawMirror = JDKUtility.UNSAFE.getLong(mirrorHandle);
        if (rawMirror == 0L) {
            throw new IllegalStateException("Klass java mirror is null");
        }
        return rawMirror;
    }

    private static Class<?> classMirror(long klassPointer) {
        long rawMirror = rawJavaMirror(klassPointer);
        int narrowMirror = encodeNarrowOop(rawMirror);
        Object mirror;

        synchronized (referenceSlot) {
            try {
                JDKUtility.UNSAFE.putIntVolatile(
                        referenceSlot, referenceSlotValueOffset, narrowMirror);
                mirror = referenceSlot.value;
            } finally {
                referenceSlot.value = null;
            }
        }

        if (!(mirror instanceof Class<?>)) {
            throw new IllegalStateException("Klass java mirror is not a Class instance");
        }
        return (Class<?>) mirror;
    }

    private static int encodeNarrowOop(long rawOop) {
        long delta = rawOop - narrowOopBase;
        long alignmentMask = (1L << narrowOopShift) - 1L;
        if (delta < 0L || (delta & alignmentMask) != 0L) {
            throw new IllegalStateException("OOP does not match the compressed OOP encoding");
        }

        long narrowOop = delta >>> narrowOopShift;
        if (narrowOop > 0xFFFF_FFFFL) {
            throw new IllegalStateException("OOP does not fit in a compressed reference");
        }
        return (int) narrowOop;
    }

    private static long narrowOop(Object value) {
        synchronized (referenceSlot) {
            try {
                referenceSlot.value = value;
                return Integer.toUnsignedLong(
                        JDKUtility.UNSAFE.getIntVolatile(referenceSlot, referenceSlotValueOffset));
            } finally {
                referenceSlot.value = null;
            }
        }
    }

    private static MethodLocation locate(Method method) throws IllegalAccessException {
        MethodHandle methodHandle = JDKUtility.LOOKUP.unreflect(method);
        if (!directMethodHandleClass.isInstance(methodHandle)) {
            throw new IllegalStateException(
                    "Expected DirectMethodHandle, got " + methodHandle.getClass().getName());
        }

        Object member = JDKUtility.UNSAFE.getObject(methodHandle, directMethodHandleMemberOffset);
        if (member == null) {
            throw new IllegalStateException("DirectMethodHandle does not contain a MemberName");
        }

        Object resolvedMethod = JDKUtility.UNSAFE.getObject(member, memberNameResolvedMethodOffset);
        if (resolvedMethod == null) {
            throw new IllegalStateException("MemberName does not contain a ResolvedMethodName");
        }

        long methodPointer = JDKUtility.UNSAFE.getLong(
                resolvedMethod, RESOLVED_METHOD_NAME_VMTARGET_OFFSET);
        if (methodPointer == 0L) {
            throw new IllegalStateException("ResolvedMethodName does not contain a Method*");
        }

        long constMethod = JDKUtility.UNSAFE.getLong(methodPointer + METHOD_CONST_METHOD_OFFSET);
        if (constMethod == 0L) {
            throw new IllegalArgumentException(method + " has no ConstMethod");
        }

        int codeSize = JDKUtility.UNSAFE.getShort(constMethod + CONST_METHOD_CODE_SIZE_OFFSET)
                & 0xFFFF;
        if (codeSize == 0) {
            throw new IllegalArgumentException(method + " has no Java bytecode");
        }

        return new MethodLocation(
                methodPointer,
                constMethod,
                constMethod + CONST_METHOD_CODE_OFFSET,
                codeSize);
    }

    private static Field declaredField(Class<?> type, String fieldName) throws Exception {
        return type.getDeclaredField(fieldName);
    }

    private static int layoutProbe() {
        return 0;
    }

    private static class KlassLayoutParent {
    }

    private static final class KlassLayoutChild extends KlassLayoutParent {
    }

    private static class VtableLayoutProbe {
        int first() {
            return 1;
        }

        int second() {
            return 2;
        }

        int third() {
            return 3;
        }
    }

    private static final class ReferenceSlot {
        private volatile Object value;
    }

    private record MethodLocation(
            long methodAddress, long constMethodAddress, long codeAddress, int codeSize) {
    }

    private record NarrowOopEncoding(long base, int shift) {
    }

}
