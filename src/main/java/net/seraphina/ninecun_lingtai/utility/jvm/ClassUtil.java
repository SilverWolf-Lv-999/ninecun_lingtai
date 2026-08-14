package net.seraphina.ninecun_lingtai.utility.jvm;

import io.netty.util.internal.shaded.org.jctools.util.UnsafeAccess;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.security.SecureRandom;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static net.seraphina.ninecun_lingtai.utility.JDKUtility.*;

public class ClassUtil {
    static Logger LOGGER = LoggerFactory.getLogger(ClassUtil.class);
    private static ClassUtil instance = null;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] NAME_START_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_".toCharArray();
    private static final char[] NAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_".toCharArray();
    private static MethodHandle methodhandle;
    private final Map<String, Class<?>> hiddenClassMap = new HashMap<>();

    public static ClassUtil getInstance() {
        if (instance == null) {
            instance = new ClassUtil();
        }
        return instance;
    }

    static {
        try {
            if (LOOKUP != null) {
                methodhandle = LOOKUP.findStatic(
                        ClassLoader.class,
                        "defineClass0",
                        MethodType.methodType(
                                Class.class,
                                ClassLoader.class,
                                Class.class,
                                String.class,
                                byte[].class,
                                int.class,
                                int.class,
                                ProtectionDomain.class,
                                boolean.class,
                                int.class,
                                Object.class
                        )
                );
            }
        } catch (Throwable e) {
            throw new Error("Could not init ClassUtil", e);
        }
    }

    public static byte[] getClassBytes(String jarPath, String className) throws Exception {
        try (JarFile jarFile = new JarFile(jarPath)) {
            String classPath = className.replace('.', '/') + ".class";
            JarEntry entry = jarFile.getJarEntry(classPath);
            if (entry == null) {
                jarFile.close();
                throw new ClassNotFoundException("Class not found in JAR: " + className);
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                return readAllBytes(is);
            }
        }
    }

    private Class<?> defineHiddenPackageClass(String klassName, ClassLoader loader) {
        return this.defineHiddenPackageClass(klassName, loader, ClassUtil.class, true, ClassOption.STRONG);
    }

    private Class<?> defineHiddenPackageClass(String name) {
        return this.defineHiddenPackageClass(name, ClassLoader.getSystemClassLoader());
    }

    private Class<?> defineHiddenClass(String name, ClassLoader loader, Class<?> lookup, byte[] b, int off, int len, boolean initialize, ClassOption... options) {
        try {
            if (hiddenClassMap.containsKey(name) && hiddenClassMap.get(name) != null) {
                return hiddenClassMap.get(name);
            } else {
                if (methodhandle == null) throw new IllegalStateException("ClassLoader#defineClass0 is unavailable");
                Objects.requireNonNull(options);
                int flags = 2 | ClassOption.optionsToFlag(Set.of(options));
                if (loader == null || loader == ClassLoader.getPlatformClassLoader()) {
                    flags |= 8;
                }
                Class<?> klass = (Class<?>) methodhandle.invoke(loader, lookup, name, b, off, len, null, initialize, flags, null);
                hiddenClassMap.put(name, klass);
                return klass;
            }
        } catch (Throwable e) {
            throw new Error(e);
        }
    }

    private Class<?> defineHiddenPackageClass(String originalName, ClassLoader loader, Class<?> lookup, boolean initialize, ClassOption... options) {
        try {
            if (hiddenClassMap.containsKey(originalName) && hiddenClassMap.get(originalName) != null) {
                return hiddenClassMap.get(originalName);
            }
            String hiddenClassName = randomHiddenClassName(packageName(originalName));

            byte[] dat;
            try (InputStream is = lookup.getResourceAsStream("/" + originalName.replace('.', '/') + ".class")) {
                if (is == null) {
                    throw new RuntimeException("Cannot find class resource: " + originalName);
                }
                dat = is.readAllBytes();
            }
            Objects.requireNonNull(dat, "Class data is null");
            byte[] modifiedBytes = remapClassName(dat, originalName, hiddenClassName);

            Class<?> klass = defineHiddenClass(
                    hiddenClassName,
                    loader,
                    lookup,
                    modifiedBytes,
                    0,
                    modifiedBytes.length,
                    initialize,
                    options
            );

            hiddenClassMap.put(originalName, klass);
            return klass;
        } catch (Throwable e) {
            throw new Error("Failed to define hidden class: " + originalName, e);
        }
    }

    public byte[] remapClassName(byte[] classBytes, String oldClassName) {
        return remapClassName(classBytes, oldClassName, randomHiddenClassName(packageName(oldClassName)));
    }

    public byte[] remapClassName(byte[] classBytes, String oldClassName, String newClassName) {
        String oldInternal = oldClassName.replace('.', '/');
        String newInternal = newClassName.replace('.', '/');
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(ClassReader.EXPAND_FRAMES);
        SimpleRemapper remapper = new SimpleRemapper(oldInternal, newInternal);
        ClassRemapper classRemapper = new ClassRemapper(writer, remapper);
        reader.accept(classRemapper, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    public String randomHiddenClassName(String packageName) {
        String randomName = randomIdentifier(32);
        return packageName == null || packageName.isBlank() ? randomName : packageName + "." + randomName;
    }

    private static String packageName(String binaryName) {
        int index = binaryName.lastIndexOf('.');
        return index < 0 ? "" : binaryName.substring(0, index);
    }

    private static String randomIdentifier(int length) {
        char[] value = new char[Math.max(1, length)];
        value[0] = NAME_START_CHARS[RANDOM.nextInt(NAME_START_CHARS.length)];
        for (int i = 1; i < value.length; i++) {
            value[i] = NAME_CHARS[RANDOM.nextInt(NAME_CHARS.length)];
        }
        return new String(value);
    }

    private Class<?> defineHiddenPackageClass(String name, Class<?> lookup, boolean initialize, ClassOption... options) {
        return defineHiddenPackageClass(name, lookup.getClassLoader(), lookup, initialize, options);
    }

    private Class<?> defineHiddenPackageClass(String name, Class<?> lookup) {
        return defineHiddenPackageClass(name, lookup.getClassLoader(), lookup, true, ClassOption.STRONG);
    }

    public Object newHiddenPackageInstance(String originalName, Class<?> hostClass) {
        Objects.requireNonNull(originalName, "Original name is null");
        Objects.requireNonNull(hostClass, "Host class is null");
        try {
            Class<?> hiddenClass = defineHiddenPackageClass(originalName, hostClass);
            return LOOKUP.findConstructor(hiddenClass, MethodType.methodType(void.class)).invoke();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to instantiate hidden class: " + originalName, throwable);
        }
    }

    public MethodHandle bindHiddenVirtual(Object target, String name, MethodType type) {
        Objects.requireNonNull(target, "Target is null");
        Objects.requireNonNull(name, "Method name is null");
        Objects.requireNonNull(type, "Method type is null");
        try {
            return LOOKUP.findVirtual(target.getClass(), name, type).bindTo(target);
        } catch (Throwable throwable) {
            throw new IllegalStateException("Failed to bind hidden method: " + name, throwable);
        }
    }
    
    public static boolean isClassLoad(String klass) {
        try {
            Class.forName(klass);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }
    
    public static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
    
    public static byte[] getClassBytes(Class<?> clazz) throws Exception {
        InputStream is = clazz.getResourceAsStream("/" + clazz.getName().split("\\$\\$")[0].replace('.', '/') + ".class");
        byte[] dat = new byte[is.available()];
        is.read(dat);
        is.close();
        return dat;
    }
    
    public static void putTarget(Object object, Class<?> targetClass) {
        if (object == null)
            throw new NullPointerException("object null");
        if (targetClass == null)
            throw new NullPointerException("targetClass null");
        try {
            int of = UnsafeAccess.UNSAFE.getIntVolatile(UnsafeAccess.UNSAFE.allocateInstance(targetClass), UnsafeAccess.UNSAFE.addressSize());
            UnsafeAccess.UNSAFE.putIntVolatile(object, UnsafeAccess.UNSAFE.addressSize(), of);
        } catch (InstantiationException ex) {
            LOGGER.warn(ex.getMessage());
            throw new RuntimeException(ex);
        }
    }
    
    public static Method[] getAllDeclaredMethods(Class<?> clazz) {
        ArrayList<Method> methods = new ArrayList<>();
        for (Class<?> currentClass = clazz; currentClass != null && currentClass != Object.class; currentClass = currentClass.getSuperclass()) {
            methods.addAll(Arrays.asList(currentClass.getDeclaredMethods()));
        }
        return methods.toArray(new Method[0]);
    }
    
    public static Field[] getAllDeclaredFields(Class<?> clazz) {
        ArrayList<Field> fields = new ArrayList<>();
        for (Class<?> currentClass = clazz; currentClass != null && currentClass != Object.class; currentClass = currentClass.getSuperclass()) {
            fields.addAll(Arrays.asList(currentClass.getDeclaredFields()));
        }
        return fields.toArray(new Field[0]);
    }

    public static boolean isModClass(Class<?> clazz) {
        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        if (url == null) return false;
        String path = url.getPath();
        return path.endsWith(".jar") && path.contains("/mods/") && !path.contains("/libraries/");
    }
}
