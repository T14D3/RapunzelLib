package de.t14d3.rapunzellib.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ValidateMessagesTask extends DefaultTask {
    private final ConfigurableFileCollection classesDirs = getProject().getObjects().fileCollection();

    @InputFiles
    public abstract RegularFileProperty getMessagesFile();

    @InputFiles
    public abstract ListProperty<RegularFile> getAdditionalMessagesFiles();

    @InputFiles
    public ConfigurableFileCollection getClassesDirs() {
        return classesDirs;
    }

    @Input
    public abstract Property<Boolean> getFailOnUnusedKeys();

    @Input
    public abstract SetProperty<String> getAlwaysUsedKeys();

    @Input
    public abstract SetProperty<String> getMessageKeyCallOwners();

    @Input
    public abstract SetProperty<String> getMessageKeyCallMethods();

    @Input
    public abstract Property<String> getMessageKeyPrefix();

    @TaskAction
    public void validate() {
        List<File> messageFiles = new ArrayList<>();
        messageFiles.add(getMessagesFile().get().getAsFile());
        for (RegularFile file : getAdditionalMessagesFiles().get()) {
            messageFiles.add(file.getAsFile());
        }
        messageFiles.removeIf(file -> !file.exists());
        if (messageFiles.isEmpty()) {
            throw new GradleException("No message files found. Configure rapunzellib.messagesFile / rapunzellib.additionalMessagesFiles.");
        }

        Set<String> definedKeys = parseYamlKeys(messageFiles);
        Set<String> usedKeys = scanMessageKeyUsage(getClassesDirs().getFiles());

        Set<String> missing = new java.util.TreeSet<>(usedKeys);
        missing.removeAll(definedKeys);
        if (!missing.isEmpty()) {
            throw new GradleException("Missing message keys in YAML: " + String.join(", ", missing));
        }

        Set<String> ignoredUnused = new LinkedHashSet<>(getAlwaysUsedKeys().get());
        Set<String> unused = new java.util.TreeSet<>(definedKeys);
        unused.removeAll(usedKeys);
        unused.removeAll(ignoredUnused);
        if (!unused.isEmpty() && getFailOnUnusedKeys().get()) {
            throw new GradleException("Unused message keys in YAML: " + String.join(", ", unused));
        }
        if (!unused.isEmpty()) {
            getLogger().warn("Unused message keys in YAML: {}", String.join(", ", unused));
        }
    }

    private Set<String> parseYamlKeys(List<File> files) {
        Set<String> keys = new LinkedHashSet<>();
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        for (File file : files) {
            Object root;
            try {
                root = yaml.load(java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8));
            } catch (Exception ex) {
                throw new GradleException("Failed to parse YAML file: " + file.getAbsolutePath(), ex);
            }

            Map<String, Object> flattened = new LinkedHashMap<>();
            flattenYaml(root, "", flattened);
            for (var entry : flattened.entrySet()) {
                if (entry.getValue() instanceof String) {
                    keys.add(entry.getKey());
                }
            }
        }
        return keys;
    }

    private void flattenYaml(Object value, String prefix, Map<String, Object> out) {
        if (value instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = entry.getKey().toString();
                String path = prefix.isEmpty() ? key : prefix + "." + key;
                out.put(path, entry.getValue());
                flattenYaml(entry.getValue(), path, out);
            }
        }
    }

    private Set<String> scanMessageKeyUsage(Set<File> files) {
        List<File> classFiles = new ArrayList<>();
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                classFiles.add(file);
            } else if (file.isDirectory()) {
                try (var stream = java.nio.file.Files.walk(file.toPath())) {
                    stream.filter(path -> java.nio.file.Files.isRegularFile(path) && path.toString().endsWith(".class"))
                        .forEach(path -> classFiles.add(path.toFile()));
                } catch (Exception ex) {
                    getLogger().debug("Failed to walk class directory {}", file, ex);
                }
            }
        }

        Set<String> keys = new LinkedHashSet<>();
        Set<String> ownerAllowList = getMessageKeyCallOwners().get().stream().map(value -> value.replace('.', '/')).collect(java.util.stream.Collectors.toSet());
        Set<String> methodAllowList = new LinkedHashSet<>(getMessageKeyCallMethods().get());
        String prefix = getMessageKeyPrefix().get();

        Analyzer<BasicValue> analyzer = new Analyzer<>(new StringConstInterpreter());
        for (File classFile : classFiles) {
            try {
                ClassNode node = new ClassNode(Opcodes.ASM9);
                new ClassReader(java.nio.file.Files.readAllBytes(classFile.toPath())).accept(node, ClassReader.SKIP_FRAMES);
                scanClass(node, analyzer, keys, ownerAllowList, methodAllowList, prefix);
            } catch (Exception ex) {
                getLogger().debug("Failed to scan class file for message key usage: {}", classFile.getAbsolutePath(), ex);
            }
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private void scanClass(
        ClassNode node,
        Analyzer<BasicValue> analyzer,
        Set<String> out,
        Set<String> ownerAllowList,
        Set<String> methodAllowList,
        String prefix
    ) {
        for (MethodNode method : (List<MethodNode>) node.methods) {
            scanMethod(node.name, method, analyzer, out, ownerAllowList, methodAllowList, prefix);
        }
    }

    private void scanMethod(
        String ownerInternalName,
        MethodNode method,
        Analyzer<BasicValue> analyzer,
        Set<String> out,
        Set<String> ownerAllowList,
        Set<String> methodAllowList,
        String prefix
    ) {
        Frame<BasicValue>[] frames;
        try {
            frames = analyzer.analyze(ownerInternalName, method);
        } catch (Exception ex) {
            return;
        }

        AbstractInsnNode insn = method.instructions.getFirst();
        int index = 0;
        while (insn != null) {
            Frame<BasicValue> frame = index < frames.length ? frames[index] : null;
            if (frame != null && insn instanceof MethodInsnNode methodInsn) {
                String key = extractKeyIfMatch(methodInsn, frame, ownerAllowList, methodAllowList, prefix);
                if (key != null) {
                    out.add(key);
                }
            }
            insn = insn.getNext();
            index++;
        }
    }

    private String extractKeyIfMatch(
        MethodInsnNode insn,
        Frame<BasicValue> frame,
        Set<String> ownerAllowList,
        Set<String> methodAllowList,
        String prefix
    ) {
        Type[] argTypes = Type.getArgumentTypes(insn.desc);
        if (argTypes.length == 0) {
            return null;
        }
        if (argTypes[0].getSort() != Type.OBJECT || !"java/lang/String".equals(argTypes[0].getInternalName())) {
            return null;
        }

        boolean isMessageService =
            "de/t14d3/rapunzellib/message/MessageService".equals(insn.owner)
                && ("component".equals(insn.name) || "raw".equals(insn.name) || "contains".equals(insn.name))
                && insn.desc.startsWith("(Ljava/lang/String;");
        boolean isWrapper = ownerAllowList.contains(insn.owner) && methodAllowList.contains(insn.name);
        if (!isMessageService && !isWrapper) {
            return null;
        }

        int start = frame.getStackSize() - argTypes.length;
        if (start < 0) {
            return null;
        }
        BasicValue value = frame.getStack(start);
        String rawKey = value instanceof StringConstValue stringConstValue ? stringConstValue.constant : null;
        if (rawKey == null) {
            return null;
        }
        String trimmed = rawKey.trim();
        if (trimmed.isBlank() || !looksLikeMessageKey(trimmed, prefix)) {
            return null;
        }
        return !prefix.isEmpty() && trimmed.startsWith(prefix) ? trimmed : prefix + trimmed;
    }

    private boolean looksLikeMessageKey(String value, String prefix) {
        if (value.contains("/")) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (!(Character.isLetterOrDigit(ch) || ch == '.' || ch == '_' || ch == '-')) {
                return false;
            }
        }
        if (!prefix.isEmpty() && value.startsWith(prefix)) {
            return true;
        }
        return "prefix".equals(value) || value.contains(".");
    }

    private static final class StringConstInterpreter extends BasicInterpreter {
        private StringConstInterpreter() {
            super(Opcodes.ASM9);
        }

        @Override
        public BasicValue newOperation(AbstractInsnNode insn) throws AnalyzerException {
            if (insn.getOpcode() == Opcodes.LDC) {
                Object cst = ((LdcInsnNode) insn).cst;
                if (cst instanceof String string) {
                    return new StringConstValue(Type.getType(String.class), string);
                }
            }
            BasicValue value = super.newOperation(insn);
            return new StringConstValue(value.getType(), null);
        }

        @Override
        public BasicValue copyOperation(AbstractInsnNode insn, BasicValue value) {
            return value;
        }

        @Override
        public BasicValue merge(BasicValue value1, BasicValue value2) {
            if (!(value1 instanceof StringConstValue left) || !(value2 instanceof StringConstValue right)) {
                return super.merge(value1, value2);
            }
            if (left.equals(right)) {
                return left;
            }
            return new StringConstValue(super.merge(value1, value2).getType(), null);
        }
    }

    private static final class StringConstValue extends BasicValue {
        private final String constant;

        private StringConstValue(Type type, String constant) {
            super(type);
            this.constant = constant;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof StringConstValue that)) {
                return false;
            }
            return super.equals(other) && java.util.Objects.equals(constant, that.constant);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + (constant != null ? constant.hashCode() : 0);
        }
    }
}
