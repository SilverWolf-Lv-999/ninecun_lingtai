package net.seraphina.ninecun_lingtai.utility;

import java.lang.reflect.Method;

public class LWJGLAccess {
    public static void putInt(long address, int value) {
        try {
            Object unsafe = JDKUtility.UNSAFE;
            Method putIntMethod = unsafe.getClass().getMethod("putInt", long.class, int.class);
            putIntMethod.invoke(unsafe, address, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
