package net.seraphina.ninecun_lingtai.utility.jvm.trans;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class BytecodeRepairTransformer implements ClassFileTransformer {
    public static final String TARGET_PACKAGE_PREFIX = "net.seraphina.ninecun_lingtai.";

    private static final String TARGET_INTERNAL_PACKAGE_PREFIX =
            "net/seraphina/ninecun_lingtai/";

    @Override
    public byte[] transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (className == null || !className.startsWith(TARGET_INTERNAL_PACKAGE_PREFIX)) {
            return null;
        }

        try {
            return readOriginalClassBytes(className);
        } catch (IOException exception) {
            return null;
        }
    }

    private static byte[] readOriginalClassBytes(String className) throws IOException {
        CodeSource codeSource = BytecodeRepairTransformer.class
                .getProtectionDomain()
                .getCodeSource();
        if (codeSource == null || codeSource.getLocation() == null) {
            return null;
        }

        Path codeSourcePath;
        try {
            URI codeSourceUri = codeSource.getLocation().toURI();
            codeSourcePath = Path.of(codeSourceUri);
        } catch (Exception exception) {
            return null;
        }

        String classResource = className + ".class";
        if (Files.isDirectory(codeSourcePath)) {
            Path classFile = codeSourcePath.resolve(classResource);
            return Files.exists(classFile) ? Files.readAllBytes(classFile) : null;
        }

        try (JarFile jarFile = new JarFile(codeSourcePath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(classResource);
            if (entry == null) {
                return null;
            }
            try (InputStream input = jarFile.getInputStream(entry)) {
                return input.readAllBytes();
            }
        }
    }
}
