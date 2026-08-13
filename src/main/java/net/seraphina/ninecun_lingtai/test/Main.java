package net.seraphina.ninecun_lingtai.test;

import net.seraphina.ninecun_lingtai.utility.jvm.SeraphinaHook;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        TestKlass1 klass = new TestKlass1();
        assertOutput("Test-1" + System.lineSeparator() + "Test-2" + System.lineSeparator(), () -> {
            TestKlass1.test1();
            klass.test2();
        });

        SeraphinaHook.replaceMethod(TestKlass1.class, TestKlass2.class);

        assertOutput(
                "this is changed" + System.lineSeparator()
                        + "Test-2" + System.lineSeparator()
                        + "114514" + System.lineSeparator(),
                () -> {
            TestKlass1.test1();
            klass.test2();
        });
        System.out.println("SeraphinaHook.replaceMethod verification passed");
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
