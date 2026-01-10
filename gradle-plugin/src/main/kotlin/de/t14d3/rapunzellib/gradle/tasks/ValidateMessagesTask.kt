package de.t14d3.rapunzellib.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Frame
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File

abstract class ValidateMessagesTask : DefaultTask() {
    @get:InputFiles
    abstract val messagesFile: RegularFileProperty

    @get:InputFiles
    abstract val additionalMessagesFiles: ListProperty<RegularFile>

    @get:InputFiles
    val classesDirs: ConfigurableFileCollection = project.objects.fileCollection()

    @get:Input
    abstract val failOnUnusedKeys: Property<Boolean>

    @get:Input
    abstract val alwaysUsedKeys: SetProperty<String>

    @get:Input
    abstract val messageKeyCallOwners: SetProperty<String>

    @get:Input
    abstract val messageKeyCallMethods: SetProperty<String>

    @get:Input
    abstract val messageKeyPrefix: Property<String>

    @TaskAction
    fun validate() {
        val messageFiles = buildList {
            add(messagesFile.get().asFile)
            addAll(additionalMessagesFiles.get().map { it.asFile })
        }.filter { it.exists() }

        if (messageFiles.isEmpty()) {
            throw GradleException("No message files found. Configure rapunzellib.messagesFile / rapunzellib.additionalMessagesFiles.")
        }

        val definedKeys = parseYamlKeys(messageFiles)
        val usedKeys = scanMessageKeyUsage(classesDirs.files)

        val missing = (usedKeys - definedKeys).sorted()
        if (missing.isNotEmpty()) {
            throw GradleException("Missing message keys in YAML: ${missing.joinToString(", ")}")
        }

        val ignoredUnused = alwaysUsedKeys.get().toSet()
        val unused = (definedKeys - usedKeys - ignoredUnused).sorted()
        if (unused.isNotEmpty() && failOnUnusedKeys.get()) {
            throw GradleException("Unused message keys in YAML: ${unused.joinToString(", ")}")
        }

        if (unused.isNotEmpty()) {
            logger.warn("Unused message keys in YAML: {}", unused.joinToString(", "))
        }
    }

    private fun parseYamlKeys(files: List<File>): Set<String> {
        val keys = mutableSetOf<String>()
        val yaml = Yaml(SafeConstructor(LoaderOptions()))
        for (file in files) {
            val root = try {
                yaml.load<Any?>(file.readText(Charsets.UTF_8))
            } catch (e: Exception) {
                throw GradleException("Failed to parse YAML file: ${file.absolutePath}", e)
            }

            val flattened = LinkedHashMap<String, Any?>()
            flattenYaml(root, "", flattened)

            for ((key, value) in flattened) {
                if (value !is String) continue
                keys.add(key)
            }
        }
        return keys
    }

    private fun flattenYaml(value: Any?, prefix: String, out: MutableMap<String, Any?>) {
        when (value) {
            is Map<*, *> -> {
                for ((rawKey, child) in value) {
                    val key = rawKey?.toString() ?: continue
                    val path = if (prefix.isEmpty()) key else "$prefix.$key"
                    out[path] = child
                    flattenYaml(child, path, out)
                }
            }
            else -> Unit
        }
    }

    private fun scanMessageKeyUsage(files: Set<File>): Set<String> {
        val classFiles = files.flatMap { file ->
            if (file.isFile && file.extension == "class") listOf(file)
            else if (file.isDirectory) file.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()
            else emptyList()
        }

        val keys = mutableSetOf<String>()
        val ownerAllowList = messageKeyCallOwners.get().map { it.replace('.', '/') }.toSet()
        val methodAllowList = messageKeyCallMethods.get().toSet()
        val prefix = messageKeyPrefix.get()

        val analyzer: Analyzer<BasicValue> = Analyzer(StringConstInterpreter())

        for (classFile in classFiles) {
            try {
                val node = ClassNode(Opcodes.ASM9)
                ClassReader(classFile.readBytes()).accept(node, ClassReader.SKIP_FRAMES)
                scanClass(node, analyzer, keys, ownerAllowList, methodAllowList, prefix)
            } catch (e: Exception) {
                logger.debug("Failed to scan class file for message key usage: {}", classFile.absolutePath, e)
            }
        }

        return keys
    }

    private fun scanClass(
        node: ClassNode,
        analyzer: Analyzer<BasicValue>,
        out: MutableSet<String>,
        ownerAllowList: Set<String>,
        methodAllowList: Set<String>,
        prefix: String,
    ) {
        @Suppress("UNCHECKED_CAST")
        val methods = node.methods as List<MethodNode>
        for (method in methods) {
            scanMethod(node.name, method, analyzer, out, ownerAllowList, methodAllowList, prefix)
        }
    }

    private fun scanMethod(
        ownerInternalName: String,
        method: MethodNode,
        analyzer: Analyzer<BasicValue>,
        out: MutableSet<String>,
        ownerAllowList: Set<String>,
        methodAllowList: Set<String>,
        prefix: String,
    ) {
        val frames: Array<Frame<BasicValue>?> = try {
            analyzer.analyze(ownerInternalName, method)
        } catch (_: Exception) {
            return
        }

        val instructions = method.instructions
        var insn: AbstractInsnNode? = instructions.first
        var index = 0
        while (insn != null) {
            val frame = frames.getOrNull(index)
            if (frame != null && insn is MethodInsnNode) {
                val key = extractKeyIfMatch(insn, frame, ownerAllowList, methodAllowList, prefix)
                if (key != null) out.add(key)
            }
            insn = insn.next
            index++
        }
    }

    private fun extractKeyIfMatch(
        insn: MethodInsnNode,
        frame: Frame<BasicValue>,
        ownerAllowList: Set<String>,
        methodAllowList: Set<String>,
        prefix: String,
    ): String? {
        val argTypes = Type.getArgumentTypes(insn.desc)
        if (argTypes.isEmpty()) return null
        if (argTypes[0].sort != Type.OBJECT || argTypes[0].internalName != "java/lang/String") return null

        val isMessageService = insn.owner == "de/t14d3/rapunzellib/message/MessageService"
            && (insn.name == "component" || insn.name == "raw" || insn.name == "contains")
            && insn.desc.startsWith("(Ljava/lang/String;")

        val isWrapper = insn.owner in ownerAllowList && insn.name in methodAllowList

        if (!isMessageService && !isWrapper) return null

        val argCount = argTypes.size
        val start = frame.stackSize - argCount
        if (start < 0) return null

        val rawKey = (frame.getStack(start) as? StringConstValue)?.const ?: return null
        val trimmed = rawKey.trim()
        if (trimmed.isBlank()) return null

        if (!looksLikeMessageKey(trimmed, prefix)) return null

        val fullKey = if (prefix.isNotEmpty() && trimmed.startsWith(prefix)) trimmed else prefix + trimmed
        return fullKey
    }

    private fun looksLikeMessageKey(value: String, prefix: String): Boolean {   
        if (value.contains('/')) return false
        if (!value.all { it.isLetterOrDigit() || it == '.' || it == '_' || it == '-' }) return false
        if (prefix.isNotEmpty() && value.startsWith(prefix)) return true
        return value == "prefix" || value.contains('.')
    }

    private class StringConstInterpreter : BasicInterpreter(ASM9) {
        override fun newOperation(insn: AbstractInsnNode): BasicValue {
            if (insn.opcode == LDC) {
                val cst = (insn as LdcInsnNode).cst
                if (cst is String) return StringConstValue(Type.getType(String::class.java), cst)
            }
            val v = super.newOperation(insn)
            return StringConstValue(v.type, null)
        }

        override fun copyOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue {
            return value
        }

        override fun merge(value1: BasicValue, value2: BasicValue): BasicValue {
            if (value1 !is StringConstValue || value2 !is StringConstValue) return super.merge(value1, value2)
            if (value1 == value2) return value1
            return StringConstValue(super.merge(value1, value2).type, null)
        }
    }

    private class StringConstValue(type: Type?, val const: String?) : BasicValue(type) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is StringConstValue) return false
            if (!super.equals(other)) return false
            return const == other.const
        }

        override fun hashCode(): Int {
            return 31 * super.hashCode() + (const?.hashCode() ?: 0)
        }
    }
}
