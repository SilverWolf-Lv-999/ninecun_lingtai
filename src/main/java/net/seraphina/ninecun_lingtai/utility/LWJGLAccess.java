package net.seraphina.ninecun_lingtai.utility;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LWJGLAccess {

    private static Object getUnsafe() throws Exception {
        Class<?> accessClass = Class.forName(
                "sun.misc.Unsafe");
        Field unsafeField = accessClass.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        return unsafeField.get(null);
    }

    public static void putInt(long address, int value) {
        try {
            Object unsafe = getUnsafe();
            Method putIntMethod = unsafe.getClass().getMethod("putInt", long.class, int.class);
            putIntMethod.invoke(unsafe, address, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
