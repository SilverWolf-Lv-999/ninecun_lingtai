package net.seraphina.ninecun_lingtai.utility.lambda.impl;

import net.seraphina.ninecun_lingtai.utility.JDKUtility;
import net.seraphina.ninecun_lingtai.utility.lambda.LambdaBase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SeraClass extends LambdaBase {
    public final Set<SeraMethod> methods = new HashSet<>();
    public final Set<SeraField> fields = new HashSet<>();
    private final String className;
    private final Class<?> type;
    private final boolean initialized;

    public SeraClass(String className) {
        this(className, false, null);
    }

    public SeraClass(String className, boolean init, ClassLoader loader) {
        this.className = Objects.requireNonNull(className, "className");
        this.type = JDKUtility.loadClass(className, init, loader);
        this.initialized = init;
        for (Method method : JDKUtility.findMethods(type)) {
            methods.add(new SeraMethod(method));
        }
        for (Field field : JDKUtility.findFields(type)) {
            fields.add(new SeraField(field));
        }
    }

    public SeraClass(Class<?> type) {
        this(Objects.requireNonNull(type, "type").getName(), false, type.getClassLoader());
    }

    @Override
    public Object ivk(Object... args) {
        return newInstance(args);
    }

    public Object newInstance(Object... args) {
        return JDKUtility.newInstance(type, args);
    }

    public Object allocateInstance() {
        return JDKUtility.allocateInstance(type);
    }

    public SeraMethod method(String name, Class<?>... parameterTypes) {
        return new SeraMethod(type, name, parameterTypes);
    }

    public SeraMethod dynamicMethod(String name) {
        return new SeraMethod(type, name);
    }

    public SeraMethod constructor(Class<?>... parameterTypes) {
        return new SeraMethod(type, parameterTypes);
    }

    public SeraField field(String name) {
        return new SeraField(type, name);
    }

    public Class<?> type() {
        return type;
    }

    public Class<?> clazz() {
        return type;
    }

    public String className() {
        return className;
    }

    public boolean initialized() {
        return initialized;
    }

    public Set<SeraMethod> methods() {
        return Collections.unmodifiableSet(methods);
    }

    public Set<SeraField> fields() {
        return Collections.unmodifiableSet(fields);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof SeraClass other && type == other.type;
    }

    @Override
    public String toString() {
        return type.toGenericString();
    }
}
