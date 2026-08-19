package external;

import net.seraphina.ninecun_lingtai.security.ProtectedFixture;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ReflectiveAccessCaller {
    public static boolean canLoadProtectedClass() {
        try {
            Class.forName("net.seraphina.ninecun_lingtai.security.ProtectedFixture");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    public static boolean canReadProtectedField() {
        try {
            return ProtectedFixture.class.getDeclaredField("SECRET") != null;
        } catch (NoSuchFieldException | SecurityException exception) {
            return false;
        }
    }

    public static boolean canInvokeLeakedMethod(Method method) {
        try {
            return method.invoke(null) != null;
        } catch (ReflectiveOperationException | SecurityException exception) {
            return false;
        }
    }

    public static boolean canReadLeakedField(Field field) {
        try {
            return field.get(null) != null;
        } catch (IllegalAccessException | SecurityException exception) {
            return false;
        }
    }

    public static boolean canFindProtectedMethodHandle() {
        try {
            return MethodHandles.publicLookup().findStatic(
                    ProtectedFixture.class,
                    "secret",
                    MethodType.methodType(String.class)
            ) != null;
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            return false;
        }
    }

    public static boolean canUnreflectLeakedMethod(Method method) {
        try {
            return MethodHandles.lookup().unreflect(method) != null;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    public static boolean canCreatePrivateLookup() {
        try {
            return MethodHandles.privateLookupIn(
                    ProtectedFixture.class,
                    MethodHandles.lookup()
            ) != null;
        } catch (IllegalAccessException exception) {
            return false;
        }
    }

    public static boolean canUseUnrelatedClassReflectionGateway() {
        try {
            Method method = Class.class.getMethod("getDeclaredMethods");
            return ((Method[]) method.invoke(String.class)).length > 0;
        } catch (ReflectiveOperationException | SecurityException exception) {
            return false;
        }
    }

    public static boolean canFindUnrelatedMethodHandle() {
        try {
            return MethodHandles.lookup().findVirtual(
                    Class.class,
                    "getName",
                    MethodType.methodType(String.class)
            ).invoke(String.class).equals(String.class.getName());
        } catch (Throwable throwable) {
            return false;
        }
    }
}
