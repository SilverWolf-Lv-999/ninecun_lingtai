package net.seraphina.ninecun_lingtai.modtest;

import net.seraphina.ninecun_lingtai.utility.jvm.JvmUtility;

import java.util.Set;

public class Main1 {
    private static final Object RETAINED_OBJECT = new Object();
    private static final String RETAINED_STRING = new String("ninecun-lingtai-object-test");

    public static void main(String[] args) {
        Set<Class<?>> classes = JvmUtility.getAllLoaedClasses();
        System.out.println("loaded classes = " + classes.size());
        System.out.println("contains Main1 = " + classes.contains(Main1.class));
        System.out.println("contains Object = " + classes.contains(Object.class));

        Set<Object> objects = JvmUtility.getAllLoaedObjects();
        System.out.println("loaded objects = " + objects.size());
        System.out.println("contains retained object = " + objects.contains(RETAINED_OBJECT));
        System.out.println("contains retained string = " + objects.contains(RETAINED_STRING));
        System.out.println("contains Main1 class mirror = " + objects.contains(Main1.class));
    }
}
