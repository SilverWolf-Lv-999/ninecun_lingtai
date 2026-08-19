package external;

import net.seraphina.ninecun_lingtai.security.ProtectedFixture;
import net.seraphina.ninecun_lingtai.utility.jvm.trans.ReflectiveAccessTransformer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReflectiveAccessTransformerTest {
    private static final String CALLER_CLASS_NAME = "external.ReflectiveAccessCaller";
    private static final String CALLER_CLASS_RESOURCE =
            "/external/ReflectiveAccessCaller.class";
    private static final String SPOOF_CALLER_CLASS_NAME =
            "net.seraphina.ninecun_lingtai.spoof.ProtectedPackageCaller";
    private static final String SPOOF_CALLER_CLASS_RESOURCE =
            "/net/seraphina/ninecun_lingtai/spoof/ProtectedPackageCaller.class";

    @Test
    void blocksReflectiveAndMethodHandleAccessFromExternalClass() throws Exception {
        Class<?> callerClass = loadTransformedCaller();
        Method secret = ProtectedFixture.class.getDeclaredMethod("secret");
        Field secretField = ProtectedFixture.class.getDeclaredField("SECRET");

        assertFalse(invokeBoolean(callerClass, "canLoadProtectedClass"));
        assertFalse(invokeBoolean(callerClass, "canReadProtectedField"));
        assertFalse(invokeBoolean(callerClass, "canInvokeLeakedMethod", Method.class, secret));
        assertFalse(invokeBoolean(callerClass, "canReadLeakedField", Field.class, secretField));
        assertFalse(invokeBoolean(callerClass, "canFindProtectedMethodHandle"));
        assertFalse(invokeBoolean(callerClass, "canUnreflectLeakedMethod", Method.class, secret));
        assertFalse(invokeBoolean(callerClass, "canCreatePrivateLookup"));
        assertTrue(invokeBoolean(callerClass, "canUseUnrelatedClassReflectionGateway"));
        assertTrue(invokeBoolean(callerClass, "canFindUnrelatedMethodHandle"));

        Class<?> spoofCallerClass = loadTransformedCaller(
                SPOOF_CALLER_CLASS_NAME,
                SPOOF_CALLER_CLASS_RESOURCE
        );
        assertFalse(invokeBoolean(spoofCallerClass, "canLoadProtectedClass"));
    }

    private static Class<?> loadTransformedCaller() throws IOException {
        return loadTransformedCaller(CALLER_CLASS_NAME, CALLER_CLASS_RESOURCE);
    }

    private static Class<?> loadTransformedCaller(
            String className,
            String classResource
    ) throws IOException {
        byte[] originalBytes = readCallerBytes(classResource);
        ByteArrayClassLoader loader = new ByteArrayClassLoader(
                ReflectiveAccessTransformerTest.class.getClassLoader());
        byte[] transformedBytes = new ReflectiveAccessTransformer().transform(
                null,
                loader,
                className.replace('.', '/'),
                null,
                null,
                originalBytes
        );
        assertNotNull(transformedBytes);
        return loader.define(className, transformedBytes);
    }

    private static byte[] readCallerBytes() throws IOException {
        return readCallerBytes(CALLER_CLASS_RESOURCE);
    }

    private static byte[] readCallerBytes(String resource) throws IOException {
        try (InputStream input = ReflectiveAccessTransformerTest.class.getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("Could not read " + resource);
            }
            return input.readAllBytes();
        }
    }

    private static boolean invokeBoolean(
            Class<?> callerClass,
            String methodName,
            Class<?> parameterType,
            Object argument
    ) throws ReflectiveOperationException {
        return (boolean) callerClass.getMethod(methodName, parameterType).invoke(null, argument);
    }

    private static boolean invokeBoolean(
            Class<?> callerClass,
            String methodName
    ) throws ReflectiveOperationException {
        return (boolean) callerClass.getMethod(methodName).invoke(null);
    }

    private static final class ByteArrayClassLoader extends ClassLoader {
        private ByteArrayClassLoader(ClassLoader parent) {
            super(parent);
        }

        private Class<?> define(String className, byte[] classBytes) {
            return defineClass(className, classBytes, 0, classBytes.length);
        }
    }
}
