package net.seraphina.ninecun_lingtai.utility.jvm.trans;

import net.seraphina.ninecun_lingtai.utility.jvm.ReflectiveAccessGuard;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;

/**
 * Rewrites common reflective and MethodHandle entry points to the guarded
 * static facade. The transformer intentionally skips the mod's own package.
 */
public final class ReflectiveAccessTransformer implements ClassFileTransformer {
    private static final String CLASS_OWNER = "java/lang/Class";
    private static final String CLASS_LOADER_OWNER = "java/lang/ClassLoader";
    private static final String ACCESSIBLE_OBJECT_OWNER = "java/lang/reflect/AccessibleObject";
    private static final String METHOD_OWNER = "java/lang/reflect/Method";
    private static final String CONSTRUCTOR_OWNER = "java/lang/reflect/Constructor";
    private static final String FIELD_OWNER = "java/lang/reflect/Field";
    private static final String METHOD_HANDLES_OWNER = "java/lang/invoke/MethodHandles";
    private static final String LOOKUP_OWNER = "java/lang/invoke/MethodHandles$Lookup";
    private static final String GUARD_OWNER = ReflectiveAccessGuard.GUARD_INTERNAL_NAME;
    private static final String GUARD_CLASS_NAME = GUARD_OWNER.replace('/', '.');
    private static final String TARGET_PACKAGE_PREFIX = "net/seraphina/ninecun_lingtai/";
    private static final String MODS_DIRECTORY_MARKER = "/mods/";

    private static final Map<String, Boolean> MOD_SOURCE_CACHE = new ConcurrentHashMap<>();

    private static final Map<CallKey, String> STATIC_METHODS = createStaticMethods();
    private static final Map<CallKey, String> INSTANCE_METHODS = createInstanceMethods();

    private final Instrumentation instrumentation;
    private final Module guardModule;
    private final Path ownCodeSource;
    private final Set<Module> configuredModules = ConcurrentHashMap.newKeySet();

    public ReflectiveAccessTransformer() {
        this(null, null);
    }

    public ReflectiveAccessTransformer(
            Instrumentation instrumentation,
            ProtectionDomain ownProtectionDomain
    ) {
        this.instrumentation = instrumentation;
        this.guardModule = resolveBootstrapGuardModule();
        this.ownCodeSource = getCodeSourcePath(ownProtectionDomain);
    }

    @Override
    public byte[] transform(
            Module module,
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (loader == null
                || className == null
                || isOwnClass(className, protectionDomain)
                || classfileBuffer == null) {
            return null;
        }
        if (this.instrumentation != null
                && (!isExternalModClass(protectionDomain)
                || !prepareModuleAccess(module))) {
            return null;
        }

        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        boolean transformed = false;
        for (var method : classNode.methods) {
            for (var instruction = method.instructions.getFirst();
                 instruction != null;
                 instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)) {
                    continue;
                }

                String wrapperName;
                boolean instanceCall;
                CallKey key = new CallKey(call.owner, call.name, call.desc);
                if (call.getOpcode() == INVOKESTATIC) {
                    wrapperName = STATIC_METHODS.get(key);
                    instanceCall = false;
                } else {
                    wrapperName = INSTANCE_METHODS.get(key);
                    instanceCall = true;
                }
                if (wrapperName == null || instanceCall != (call.getOpcode() != INVOKESTATIC)) {
                    continue;
                }

                String originalOwner = call.owner;
                call.setOpcode(INVOKESTATIC);
                call.owner = GUARD_OWNER;
                call.name = wrapperName;
                call.desc = instanceCall
                        ? prependReceiver(originalOwner, call.desc)
                        : call.desc;
                call.itf = false;
                transformed = true;
            }
        }

        if (!transformed) {
            return null;
        }

        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    public static boolean isExternalModClass(ProtectionDomain protectionDomain) {
        if (protectionDomain == null
                || protectionDomain.getCodeSource() == null
                || protectionDomain.getCodeSource().getLocation() == null) {
            return false;
        }

        String location = protectionDomain.getCodeSource()
                .getLocation()
                .toExternalForm();
        return MOD_SOURCE_CACHE.computeIfAbsent(location,
                ignored -> isModCodeSource(getCodeSourcePath(protectionDomain)));
    }

    private boolean isOwnClass(
            String className,
            ProtectionDomain protectionDomain
    ) {
        if (!className.startsWith(TARGET_PACKAGE_PREFIX)
                || this.ownCodeSource == null) {
            return false;
        }

        Path classCodeSource = getCodeSourcePath(protectionDomain);
        if (classCodeSource == null) {
            return false;
        }
        return this.ownCodeSource.equals(classCodeSource);
    }

    private static Module resolveBootstrapGuardModule() {
        try {
            return Class.forName(GUARD_CLASS_NAME, false, null).getModule();
        } catch (ClassNotFoundException | LinkageError exception) {
            return null;
        }
    }

    private boolean prepareModuleAccess(Module module) {
        if (this.guardModule == null) {
            return false;
        }
        if (module == null
                || !module.isNamed()
                || module == this.guardModule
                || module.canRead(this.guardModule)) {
            return true;
        }
        if (!this.configuredModules.add(module)) {
            return true;
        }

        try {
            this.instrumentation.redefineModule(
                    module,
                    Set.of(this.guardModule),
                    Map.of(),
                    Map.of(),
                    Set.of(),
                    Map.of()
            );
            return true;
        } catch (RuntimeException exception) {
            this.configuredModules.remove(module);
            return false;
        }
    }

    private static Path getCodeSourcePath(ProtectionDomain protectionDomain) {
        if (protectionDomain == null
                || protectionDomain.getCodeSource() == null
                || protectionDomain.getCodeSource().getLocation() == null) {
            return null;
        }
        try {
            URI location = protectionDomain.getCodeSource().getLocation().toURI();
            return Path.of(location).toAbsolutePath().normalize();
        } catch (Exception exception) {
            return null;
        }
    }

    private static boolean isModCodeSource(Path codeSource) {
        if (codeSource == null) {
            return false;
        }

        String normalizedPath = codeSource.toString()
                .replace('\\', '/')
                .toLowerCase(Locale.ROOT);
        if (normalizedPath.contains("/libraries/")) {
            return false;
        }
        if (normalizedPath.contains(MODS_DIRECTORY_MARKER)) {
            return true;
        }

        if (Files.isDirectory(codeSource)) {
            return containsModMetadata(codeSource);
        }
        if (!normalizedPath.endsWith(".jar")) {
            return false;
        }

        try (JarFile jarFile = new JarFile(codeSource.toFile())) {
            return jarFile.getEntry("META-INF/mods.toml") != null
                    || jarFile.getEntry("META-INF/neoforge.mods.toml") != null
                    || jarFile.getEntry("fabric.mod.json") != null
                    || jarFile.getEntry("quilt.mod.json") != null;
        } catch (Exception exception) {
            return false;
        }
    }

    private static boolean containsModMetadata(Path directory) {
        return Files.isRegularFile(directory.resolve("META-INF/mods.toml"))
                || Files.isRegularFile(directory.resolve("META-INF/neoforge.mods.toml"))
                || Files.isRegularFile(directory.resolve("fabric.mod.json"))
                || Files.isRegularFile(directory.resolve("quilt.mod.json"));
    }

    private static String prependReceiver(String owner, String descriptor) {
        Type methodType = Type.getMethodType(descriptor);
        Type[] arguments = methodType.getArgumentTypes();
        Type[] guardedArguments = new Type[arguments.length + 1];
        guardedArguments[0] = Type.getObjectType(owner);
        System.arraycopy(arguments, 0, guardedArguments, 1, arguments.length);
        return Type.getMethodDescriptor(methodType.getReturnType(), guardedArguments);
    }

    private static Map<CallKey, String> createStaticMethods() {
        Map<CallKey, String> methods = new HashMap<>();
        addStatic(methods, METHOD_HANDLES_OWNER, "privateLookupIn");
        addStatic(methods, METHOD_HANDLES_OWNER, "reflectAs");
        addStatic(methods, CLASS_OWNER, "forName");
        addStatic(methods, ACCESSIBLE_OBJECT_OWNER, "setAccessible");
        return Map.copyOf(methods);
    }

    private static Map<CallKey, String> createInstanceMethods() {
        Map<CallKey, String> methods = new HashMap<>();

        addInstance(methods, CLASS_OWNER, "getDeclaredMethod");
        addInstance(methods, CLASS_OWNER, "getMethod");
        addInstance(methods, CLASS_OWNER, "getDeclaredMethods");
        addInstance(methods, CLASS_OWNER, "getMethods");
        addInstance(methods, CLASS_OWNER, "getDeclaredField");
        addInstance(methods, CLASS_OWNER, "getField");
        addInstance(methods, CLASS_OWNER, "getDeclaredFields");
        addInstance(methods, CLASS_OWNER, "getFields");
        addInstance(methods, CLASS_OWNER, "getDeclaredConstructor");
        addInstance(methods, CLASS_OWNER, "getConstructor");
        addInstance(methods, CLASS_OWNER, "getDeclaredConstructors");
        addInstance(methods, CLASS_OWNER, "getConstructors");
        addInstance(methods, CLASS_OWNER, "getDeclaredClasses");
        addInstance(methods, CLASS_OWNER, "getClasses");
        addInstance(methods, CLASS_OWNER, "getResource");
        addInstance(methods, CLASS_OWNER, "getResourceAsStream");
        addInstance(methods, CLASS_OWNER, "newInstance");

        addInstance(methods, CLASS_LOADER_OWNER, "loadClass");
        addInstance(methods, CLASS_LOADER_OWNER, "getResource");
        addInstance(methods, CLASS_LOADER_OWNER, "getResourceAsStream");
        addInstance(methods, CLASS_LOADER_OWNER, "getResources");

        addInstance(methods, ACCESSIBLE_OBJECT_OWNER, "setAccessible");
        addInstance(methods, ACCESSIBLE_OBJECT_OWNER, "trySetAccessible");

        addInstance(methods, METHOD_OWNER, "invoke");
        addInstance(methods, CONSTRUCTOR_OWNER, "newInstance");
        addInstance(methods, FIELD_OWNER, "get");
        addInstance(methods, FIELD_OWNER, "getBoolean");
        addInstance(methods, FIELD_OWNER, "getByte");
        addInstance(methods, FIELD_OWNER, "getChar");
        addInstance(methods, FIELD_OWNER, "getShort");
        addInstance(methods, FIELD_OWNER, "getInt");
        addInstance(methods, FIELD_OWNER, "getLong");
        addInstance(methods, FIELD_OWNER, "getFloat");
        addInstance(methods, FIELD_OWNER, "getDouble");
        addInstance(methods, FIELD_OWNER, "set");
        addInstance(methods, FIELD_OWNER, "setBoolean");
        addInstance(methods, FIELD_OWNER, "setByte");
        addInstance(methods, FIELD_OWNER, "setChar");
        addInstance(methods, FIELD_OWNER, "setShort");
        addInstance(methods, FIELD_OWNER, "setInt");
        addInstance(methods, FIELD_OWNER, "setLong");
        addInstance(methods, FIELD_OWNER, "setFloat");
        addInstance(methods, FIELD_OWNER, "setDouble");

        addInstance(methods, LOOKUP_OWNER, "in");
        addInstance(methods, LOOKUP_OWNER, "findStatic");
        addInstance(methods, LOOKUP_OWNER, "findVirtual");
        addInstance(methods, LOOKUP_OWNER, "findConstructor");
        addInstance(methods, LOOKUP_OWNER, "findSpecial");
        addInstance(methods, LOOKUP_OWNER, "findGetter");
        addInstance(methods, LOOKUP_OWNER, "findSetter");
        addInstance(methods, LOOKUP_OWNER, "findStaticGetter");
        addInstance(methods, LOOKUP_OWNER, "findStaticSetter");
        addInstance(methods, LOOKUP_OWNER, "findVarHandle");
        addInstance(methods, LOOKUP_OWNER, "findStaticVarHandle");
        addInstance(methods, LOOKUP_OWNER, "findClass");
        addInstance(methods, LOOKUP_OWNER, "bind");
        addInstance(methods, LOOKUP_OWNER, "unreflect");
        addInstance(methods, LOOKUP_OWNER, "unreflectSpecial");
        addInstance(methods, LOOKUP_OWNER, "unreflectConstructor");
        addInstance(methods, LOOKUP_OWNER, "unreflectGetter");
        addInstance(methods, LOOKUP_OWNER, "unreflectSetter");
        addInstance(methods, LOOKUP_OWNER, "unreflectVarHandle");
        addInstance(methods, LOOKUP_OWNER, "accessClass");
        addInstance(methods, LOOKUP_OWNER, "revealDirect");

        return Map.copyOf(methods);
    }

    private static void addStatic(
            Map<CallKey, String> methods,
            String owner,
            String name
    ) {
        methods.put(new CallKey(owner, name, null), name);
    }

    private static void addInstance(
            Map<CallKey, String> methods,
            String owner,
            String name
    ) {
        methods.put(new CallKey(owner, name, null), name);
    }

    private record CallKey(String owner, String name, String descriptor) {
        @Override
        public boolean equals(Object object) {
            if (!(object instanceof CallKey other)) {
                return false;
            }
            return owner.equals(other.owner)
                    && name.equals(other.name)
                    && (descriptor == null
                    || descriptor.equals(other.descriptor)
                    || other.descriptor == null);
        }

        @Override
        public int hashCode() {
            return owner.hashCode() * 31 + name.hashCode();
        }
    }
}
