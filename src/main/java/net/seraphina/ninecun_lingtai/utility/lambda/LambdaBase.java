package net.seraphina.ninecun_lingtai.utility.lambda;

import net.seraphina.ninecun_lingtai.utility.lambda.impl.SeraField;
import net.seraphina.ninecun_lingtai.utility.lambda.impl.SeraMethod;
import net.seraphina.ninecun_lingtai.utility.lambda.impl.SeraClass;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;

public abstract class LambdaBase implements SeraLambda {
    public static SeraMethod getMethod() {
        return new SeraMethod();
    }

    public static SeraMethod getMethod(Method method) {
        return new SeraMethod(Objects.requireNonNull(method, "method"));
    }

    public static SeraMethod getMethod(MethodHandle handle) {
        return new SeraMethod(Objects.requireNonNull(handle, "handle"));
    }

    public static SeraMethod getMethod(Class<?> owner, String name, Class<?>... parameterTypes) {
        return new SeraMethod(owner, name, parameterTypes);
    }

    public static SeraMethod getMethod(String className, String name, Class<?>... parameterTypes) {
        return new SeraMethod(className, name, parameterTypes);
    }

    public static SeraField getField() {
        return new SeraField();
    }

    public static SeraField getField(Field field) {
        return new SeraField(Objects.requireNonNull(field, "field"));
    }

    public static SeraField getField(Class<?> owner, String name) {
        return new SeraField(owner, name);
    }

    public static SeraField getField(String className, String name) {
        return new SeraField(className, name);
    }

    public static SeraClass getClass(String className) {
        return new SeraClass(className);
    }
}
