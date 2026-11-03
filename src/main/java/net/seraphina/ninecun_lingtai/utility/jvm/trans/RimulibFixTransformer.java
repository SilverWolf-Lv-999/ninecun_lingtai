package net.seraphina.ninecun_lingtai.utility.jvm.trans;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class RimulibFixTransformer {
    private static final Pattern SRG_NAME_PATTERN = Pattern.compile("[mf]_\\d+_");

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Expected input JAR, output JAR, and SRG mappings paths");
        }

        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        Map<String, String> mappings = loadMappings(Path.of(args[2]));
        Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
        Files.createDirectories(output.getParent());
        Files.deleteIfExists(temporary);

        try (ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(input));
             ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(temporary.toFile()))) {
            ZipEntry entry;
            while ((entry = inputStream.getNextEntry()) != null) {
                byte[] data = inputStream.readAllBytes();
                if (entry.getName().endsWith(".class") && containsSrgName(data)) {
                    data = patchClass(data, mappings);
                }
                ZipEntry outputEntry = new ZipEntry(entry.getName());
                outputEntry.setTime(entry.getTime());
                outputStream.putNextEntry(outputEntry);
                outputStream.write(data);
                outputStream.closeEntry();
            }
        }

        Files.move(temporary, output, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static Map<String, String> loadMappings(Path mappingsFile) throws IOException {
        Map<String, String> mappings = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(mappingsFile), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(" ");
                if (!"MD:".equals(columns[0]) && !"FD:".equals(columns[0])) {
                    continue;
                }
                int namedIndex = "MD:".equals(columns[0]) ? 3 : 2;
                if (columns.length <= namedIndex) {
                    continue;
                }
                int srgSeparator = columns[1].lastIndexOf('/');
                int namedSeparator = columns[namedIndex].lastIndexOf('/');
                if (srgSeparator < 0 || namedSeparator < 0) {
                    continue;
                }
                mappings.put(
                        columns[1].substring(srgSeparator + 1),
                        columns[namedIndex].substring(namedSeparator + 1)
                );
            }
        }
        return mappings;
    }

    private static boolean containsSrgName(byte[] data) {
        return SRG_NAME_PATTERN.matcher(new String(data, StandardCharsets.ISO_8859_1)).find();
    }

    private static byte[] patchClass(byte[] data, Map<String, String> mappings) {
        ClassReader reader = new ClassReader(data);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);
        remapAnnotations(classNode.visibleAnnotations, mappings);
        remapAnnotations(classNode.invisibleAnnotations, mappings);
        for (var method : classNode.methods) {
            remapAnnotations(method.visibleAnnotations, mappings);
            remapAnnotations(method.invisibleAnnotations, mappings);
            remapInstructions(method.instructions, mappings);
        }
        for (var field : classNode.fields) {
            remapAnnotations(field.visibleAnnotations, mappings);
            remapAnnotations(field.invisibleAnnotations, mappings);
            remapFieldConstant(field, mappings);
        }
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static void remapInstructions(
            Iterable<AbstractInsnNode> instructions,
            Map<String, String> mappings
    ) {
        for (AbstractInsnNode instruction : instructions) {
            if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof String text) {
                ldc.cst = remapText(text, mappings);
            } else if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                for (int index = 0; index < dynamic.bsmArgs.length; index++) {
                    if (dynamic.bsmArgs[index] instanceof String text) {
                        dynamic.bsmArgs[index] = remapText(text, mappings);
                    }
                }
            }
        }
    }

    private static void remapFieldConstant(FieldNode field, Map<String, String> mappings) {
        if (field.value instanceof String text) {
            field.value = remapText(text, mappings);
        }
    }

    private static void remapAnnotations(List<AnnotationNode> annotations, Map<String, String> mappings) {
        if (annotations == null) {
            return;
        }
        for (AnnotationNode annotation : annotations) {
            remapAnnotationValues(annotation.values, mappings);
        }
    }

    private static void remapAnnotationValues(List<Object> values, Map<String, String> mappings) {
        if (values == null) {
            return;
        }
        for (int index = 1; index < values.size(); index += 2) {
            Object value = values.get(index);
            if (value instanceof String text) {
                values.set(index, remapText(text, mappings));
            } else if (value instanceof AnnotationNode annotation) {
                remapAnnotationValues(annotation.values, mappings);
            } else if (value instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Object> nestedValues = (List<Object>) list;
                remapNestedValues(nestedValues, mappings);
            }
        }
    }

    private static void remapNestedValues(List<Object> values, Map<String, String> mappings) {
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (value instanceof String text) {
                values.set(index, remapText(text, mappings));
            } else if (value instanceof AnnotationNode annotation) {
                remapAnnotationValues(annotation.values, mappings);
            } else if (value instanceof List<?> list) {
                @SuppressWarnings("unchecked")
                List<Object> nestedValues = (List<Object>) list;
                remapNestedValues(nestedValues, mappings);
            }
        }
    }

    private static String remapText(String text, Map<String, String> mappings) {
        Matcher matcher = SRG_NAME_PATTERN.matcher(text);
        if (!matcher.find()) {
            return text;
        }
        StringBuilder result = new StringBuilder(text.length());
        do {
            String mappedName = mappings.getOrDefault(matcher.group(), matcher.group());
            matcher.appendReplacement(result, Matcher.quoteReplacement(mappedName));
        } while (matcher.find());
        matcher.appendTail(result);
        return result.toString();
    }
}
