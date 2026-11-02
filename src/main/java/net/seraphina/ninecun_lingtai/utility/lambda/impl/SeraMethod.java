package net.seraphina.ninecun_lingtai.utility.lambda.impl;

import net.seraphina.ninecun_lingtai.utility.JDKUtility;
import net.seraphina.ninecun_lingtai.utility.lambda.LambdaBase;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class SeraMethod extends LambdaBase {
    private final Class<?> declaringClass;
    private final String name;
    private final Method method;
    private final Constructor<?> constructor;
    private final MethodHandle handle;
    private final Method[] candidates;

    public SeraMethod() {
        this.declaringClass = null;
        this.name = null;
        this.method = null;
        this.constructor = null;
        this.handle = null;
        this.candidates = null;
    }

    public SeraMethod(MethodHandle handle) {
        this.declaringClass = null;
        this.name = null;
        this.method = null;
        this.constructor = null;
        this.handle = Objects.requireNonNull(handle, "handle");
        this.candidates = null;
    }

    public SeraMethod(Method method) {
        this.declaringClass = Objects.requireNonNull(method, "method").getDeclaringClass();
        this.name = method.getName();
        this.method = method;
        this.constructor = null;
        this.handle = JDKUtility.unreflect(method);
        this.candidates = null;
    }

    public SeraMethod(Constructor<?> constructor) {
        this.declaringClass = Objects.requireNonNull(constructor, "constructor").getDeclaringClass();
        this.name = "<init>";
        this.method = null;
        this.constructor = constructor;
        this.handle = JDKUtility.unreflect(constructor);
        this.candidates = null;
    }

    public SeraMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        this.declaringClass = Objects.requireNonNull(owner, "owner");
        this.name = Objects.requireNonNull(name, "name");
        this.method = JDKUtility.findMethod(owner, name, parameterTypes);
        this.constructor = null;
        this.handle = JDKUtility.unreflect(method);
        this.candidates = null;
    }

    public SeraMethod(Class<?> owner, String name) {
        this.declaringClass = Objects.requireNonNull(owner, "owner");
        this.name = Objects.requireNonNull(name, "name");
        this.method = null;
        this.constructor = null;
        this.handle = null;
        this.candidates = JDKUtility.findMethods(owner, name);
    }

    public SeraMethod(String className, String name, Class<?>... parameterTypes) {
        this(JDKUtility.loadClass(className, false, null), name, parameterTypes);
    }

    public SeraMethod(String className, String name) {
        this(JDKUtility.loadClass(className, false, null), name);
    }

    public SeraMethod(Class<?> owner, Class<?>... parameterTypes) {
        this.declaringClass = Objects.requireNonNull(owner, "owner");
        this.name = "<init>";
        this.method = null;
        this.constructor = JDKUtility.findConstructor(owner, parameterTypes);
        this.handle = JDKUtility.unreflect(constructor);
        this.candidates = null;
    }

    public SeraMethod(String className, Class<?>... parameterTypes) {
        this(JDKUtility.loadClass(className, false, null), parameterTypes);
    }

    @Override
    public Object ivk(Object... args) {
        Objects.requireNonNull(args, "args");
        MethodHandle target = handle;
        if (target == null && candidates != null) {
            List<Method> instanceMethods = new ArrayList<>();
            List<Method> staticMethods = new ArrayList<>();
            for (Method candidate : candidates) {
                if (Modifier.isStatic(candidate.getModifiers())) {
                    staticMethods.add(candidate);
                } else {
                    instanceMethods.add(candidate);
                }
            }
            Method selected = null;
            if (!instanceMethods.isEmpty() && args.length > 0) {
                try {
                    selected = JDKUtility.selectMethod(instanceMethods.toArray(Method[]::new), Arrays.copyOfRange(args, 1, args.length));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (selected == null && !staticMethods.isEmpty()) {
                selected = JDKUtility.selectMethod(staticMethods.toArray(Method[]::new), args);
            }
            if (selected == null) {
                throw new IllegalArgumentException("No compatible method overload");
            }
            target = JDKUtility.unreflect(selected);
        }
        if (target == null) {
            throw new IllegalStateException("Lambda method is not bound");
        }
        return JDKUtility.invoke(target, args);
    }

    public Object invoke(Object... args) {
        return ivk(args);
    }

    public MethodHandle handle() {
        return handle;
    }

    public Method method() {
        return method;
    }

    public Constructor<?> constructor() {
        return constructor;
    }

    public Class<?> declaringClass() {
        return declaringClass;
    }

    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(declaringClass, name, method, constructor, handle);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SeraMethod other)) {
            return false;
        }
        return Objects.equals(declaringClass, other.declaringClass)
                && Objects.equals(name, other.name)
                && Objects.equals(method, other.method)
                && Objects.equals(constructor, other.constructor)
                && Objects.equals(handle, other.handle);
    }

    @Override
    public String toString() {
        if (method != null) {
            return method.toGenericString();
        }
        if (constructor != null) {
            return constructor.toGenericString();
        }
        return name + Arrays.toString(candidates == null ? new Method[0] : candidates);
    }
}
