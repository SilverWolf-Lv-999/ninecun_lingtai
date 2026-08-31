package net.seraphina.ninecun_lingtai.modtest;

import net.seraphina.ninecun_lingtai.utility.jvm.SeraLegitHook;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        verifyReplaceMethod();
        verifyAddMethod();
        System.out.println("SeraphinaHook verification passed");
    }

    private static void verifyReplaceMethod() {
        TestKlass1 klass = new TestKlass1();
        assertOutput("Test-1" + System.lineSeparator() + "Test-2" + System.lineSeparator(), () -> {
            TestKlass1.test1();
            klass.test2();
        });

        SeraLegitHook.replaceMethod(TestKlass1.class, TestKlass2.class);

        assertOutput(
                "this is changed" + System.lineSeparator()
                        + "Test-2" + System.lineSeparator()
                        + "114514" + System.lineSeparator(),
                () -> {
                    TestKlass1.test1();
                    klass.test2();
                });
    }

    private static void verifyAddMethod() {
        // Populate the original reflection cache before addMethod invalidates it.
        System.class.getDeclaredMethods();
        SeraLegitHook.addMethod(System.class, "testPrint", () -> System.out.println("123"));

        try {
            MethodHandle methodHandle = MethodHandles.publicLookup()
                    .findStatic(System.class, "testPrint", MethodType.methodType(void.class));
            assertOutput("123" + System.lineSeparator(), () -> invokeMethodHandle(methodHandle));

            Method method = System.class.getMethod("testPrint");
            if (method.getDeclaringClass() != System.class
                    || !Modifier.isPublic(method.getModifiers())
                    || !Modifier.isStatic(method.getModifiers())) {
                throw new AssertionError("System.testPrint() was not exposed as a public static method");
            }

            assertOutput("123" + System.lineSeparator(), () -> {
                try {
                    method.invoke(null);
                } catch (ReflectiveOperationException exception) {
                    throw new AssertionError("Could not invoke System.testPrint()", exception);
                }
            });
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new AssertionError("JVM did not expose System.testPrint() to reflection or MethodHandle", exception);
        }
    }

    private static void invokeMethodHandle(MethodHandle methodHandle) {
        try {
            methodHandle.invokeExact();
        } catch (Throwable throwable) {
            throw new AssertionError("Could not invoke MethodHandle for System.testPrint()", throwable);
        }
    }

    private static void assertOutput(String expected, Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (PrintStream capturedOut = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        String actual = output.toString(StandardCharsets.UTF_8);
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    "Expected output " + quote(expected) + " but received " + quote(actual));
        }
    }

    private static String quote(String value) {
        return '"' + value.replace("\r", "\\r").replace("\n", "\\n") + '"';
    }
}
