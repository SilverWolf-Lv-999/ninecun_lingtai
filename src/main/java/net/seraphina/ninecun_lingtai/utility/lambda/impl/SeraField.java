package net.seraphina.ninecun_lingtai.utility.lambda.impl;

import net.seraphina.ninecun_lingtai.utility.JDKUtility;
import net.seraphina.ninecun_lingtai.utility.lambda.LambdaBase;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public class SeraField extends LambdaBase {
    private final Field field;
    private final MethodHandle getter;
    private final MethodHandle setter;
    private final Unsafe unsafe = JDKUtility.UNSAFE;
    private final Object base;
    private final long offset;
    private final boolean staticField;

    public SeraField() {
        this.field = null;
        this.getter = null;
        this.setter = null;
        this.base = null;
        this.offset = -1L;
        this.staticField = false;
    }

    public SeraField(Field field) {
        this.field = Objects.requireNonNull(field, "field");
        this.getter = JDKUtility.unreflectGetter(field);
        this.setter = JDKUtility.unreflectSetter(field);
        this.staticField = Modifier.isStatic(field.getModifiers());
        this.base = staticField ? unsafe.staticFieldBase(field) : null;
        this.offset = staticField ? unsafe.staticFieldOffset(field) : unsafe.objectFieldOffset(field);
    }

    public SeraField(Class<?> owner, String name) {
        this(JDKUtility.findField(Objects.requireNonNull(owner, "owner"), Objects.requireNonNull(name, "name")));
    }

    public SeraField(String className, String name) {
        this(JDKUtility.loadClass(className, false, null), name);
    }

    @Override
    public Object ivk(Object... args) {
        if (args == null || args.length == 0) {
            return get(null);
        }
        if (args.length == 1) {
            return get(args[0]);
        }
        set(args[0], args[1]);
        return args[1];
    }

    public Object get(Object owner) {
        ensureBound();
        Object target = staticField ? base : instance(owner);
        Class<?> type = field.getType();
        if (type == boolean.class) return unsafe.getBoolean(target, offset);
        if (type == byte.class) return unsafe.getByte(target, offset);
        if (type == short.class) return unsafe.getShort(target, offset);
        if (type == char.class) return unsafe.getChar(target, offset);
        if (type == int.class) return unsafe.getInt(target, offset);
        if (type == long.class) return unsafe.getLong(target, offset);
        if (type == float.class) return unsafe.getFloat(target, offset);
        if (type == double.class) return unsafe.getDouble(target, offset);
        return unsafe.getObject(target, offset);
    }

    public void set(Object owner, Object value) {
        ensureBound();
        Object target = staticField ? base : instance(owner);
        Class<?> type = field.getType();
        if (type == boolean.class) unsafe.putBoolean(target, offset, (Boolean) value);
        else if (type == byte.class) unsafe.putByte(target, offset, ((Number) value).byteValue());
        else if (type == short.class) unsafe.putShort(target, offset, ((Number) value).shortValue());
        else if (type == char.class) unsafe.putChar(target, offset, (Character) value);
        else if (type == int.class) unsafe.putInt(target, offset, ((Number) value).intValue());
        else if (type == long.class) unsafe.putLong(target, offset, ((Number) value).longValue());
        else if (type == float.class) unsafe.putFloat(target, offset, ((Number) value).floatValue());
        else if (type == double.class) unsafe.putDouble(target, offset, ((Number) value).doubleValue());
        else unsafe.putObject(target, offset, value);
    }

    public Object getVolatile(Object owner) {
        ensureBound();
        Object target = staticField ? base : instance(owner);
        Class<?> type = field.getType();
        if (type == boolean.class) return unsafe.getBooleanVolatile(target, offset);
        if (type == byte.class) return unsafe.getByteVolatile(target, offset);
        if (type == short.class) return unsafe.getShortVolatile(target, offset);
        if (type == char.class) return unsafe.getCharVolatile(target, offset);
        if (type == int.class) return unsafe.getIntVolatile(target, offset);
        if (type == long.class) return unsafe.getLongVolatile(target, offset);
        if (type == float.class) return unsafe.getFloatVolatile(target, offset);
        if (type == double.class) return unsafe.getDoubleVolatile(target, offset);
        return unsafe.getObjectVolatile(target, offset);
    }

    public void setVolatile(Object owner, Object value) {
        ensureBound();
        Object target = staticField ? base : instance(owner);
        Class<?> type = field.getType();
        if (type == boolean.class) unsafe.putBooleanVolatile(target, offset, (Boolean) value);
        else if (type == byte.class) unsafe.putByteVolatile(target, offset, ((Number) value).byteValue());
        else if (type == short.class) unsafe.putShortVolatile(target, offset, ((Number) value).shortValue());
        else if (type == char.class) unsafe.putCharVolatile(target, offset, (Character) value);
        else if (type == int.class) unsafe.putIntVolatile(target, offset, ((Number) value).intValue());
        else if (type == long.class) unsafe.putLongVolatile(target, offset, ((Number) value).longValue());
        else if (type == float.class) unsafe.putFloatVolatile(target, offset, ((Number) value).floatValue());
        else if (type == double.class) unsafe.putDoubleVolatile(target, offset, ((Number) value).doubleValue());
        else unsafe.putObjectVolatile(target, offset, value);
    }

    public Object getWithHandle(Object owner) {
        ensureBound();
        return JDKUtility.invoke(getter, staticField ? new Object[0] : new Object[]{owner});
    }

    public void setWithHandle(Object owner, Object value) {
        ensureBound();
        if (setter == null) {
            throw new IllegalStateException("Field has no writable method handle");
        }
        JDKUtility.invoke(setter, staticField ? new Object[]{value} : new Object[]{owner, value});
    }

    public Field field() {
        return field;
    }

    public MethodHandle getter() {
        return getter;
    }

    public MethodHandle setter() {
        return setter;
    }

    public long offset() {
        return offset;
    }

    public boolean isStatic() {
        return staticField;
    }

    private Object instance(Object owner) {
        ensureBound();
        if (owner == null || !field.getDeclaringClass().isInstance(owner)) {
            throw new IllegalArgumentException("Field owner does not match " + field.getDeclaringClass().getName());
        }
        return owner;
    }

    private void ensureBound() {
        if (field == null) {
            throw new IllegalStateException("Lambda field is not bound");
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, offset);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SeraField other)) {
            return false;
        }
        return Objects.equals(field, other.field) && offset == other.offset;
    }

    @Override
    public String toString() {
        return field == null ? "unbound" : field.toGenericString();
    }
}
