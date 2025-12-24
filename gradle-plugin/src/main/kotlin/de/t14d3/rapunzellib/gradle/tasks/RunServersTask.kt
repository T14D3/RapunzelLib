package de.t14d3.rapunzellib.gradle.tasks

import de.t14d3.rapunzellib.serverrunner.ServerRunnerMain
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

abstract class RunServersTask : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
    }

    @get:Input
    abstract val paperVersion: Property<String>

    @get:Input
    abstract val paperCount: Property<Int>

    @get:Input
    abstract val paperBasePort: Property<Int>

    @get:InputFile
    @get:Optional
    abstract val paperPluginJar: RegularFileProperty

    @get:Input
    abstract val velocityEnabled: Property<Boolean>

    @get:Input
    abstract val velocityVersion: Property<String>

    @get:Input
    abstract val velocityPort: Property<Int>

    @get:InputFile
    @get:Optional
    abstract val velocityPluginJar: RegularFileProperty

    @get:Input
    abstract val javaBin: Property<String>

    @get:Input
    abstract val jvmArgs: ListProperty<String>

    @get:Input
    abstract val mysqlEnabled: Property<Boolean>

    @get:Input
    abstract val mysqlPort: Property<Int>

    @get:Input
    abstract val mysqlDatabase: Property<String>

    @get:Input
    abstract val mysqlRootPassword: Property<String>

    @get:Input
    abstract val mysqlImage: Property<String>

    @get:Input
    abstract val mysqlContainerName: Property<String>

    /**
     * Flat list configured in triples: <relativePath>, <regex>, <replacement>.
     * Use [replace] for a friendlier DSL.
     */
    @get:Input
    abstract val regexReplaces: ListProperty<String>

    @get:Input
    abstract val additionalArgs: ListProperty<String>

    @get:OutputDirectory
    abstract val baseDir: DirectoryProperty

    fun replace(relativePath: String, regex: String, replacement: String) {
        regexReplaces.add(relativePath)
        regexReplaces.add(regex)
        regexReplaces.add(replacement)
    }

    @TaskAction
    fun runServers() {
        val cliArgs = mutableListOf<String>()

        cliArgs.add("--paper-version")
        cliArgs.add(paperVersion.get())
        cliArgs.add("--paper-count")
        cliArgs.add(paperCount.get().toString())
        cliArgs.add("--paper-base-port")
        cliArgs.add(paperBasePort.get().toString())

        paperPluginJar.orNull?.asFile?.let { jar ->
            cliArgs.add("--paper-plugin")
            cliArgs.add(jar.absolutePath)
        }

        if (velocityEnabled.get()) {
            cliArgs.add("--velocity-version")
            cliArgs.add(velocityVersion.get())
            cliArgs.add("--velocity-port")
            cliArgs.add(velocityPort.get().toString())

            velocityPluginJar.orNull?.asFile?.let { jar ->
                cliArgs.add("--velocity-plugin")
                cliArgs.add(jar.absolutePath)
            }
        }

        val javaBinValue = javaBin.orNull?.trim()
        if (!javaBinValue.isNullOrEmpty()) {
            cliArgs.add("--java")
            cliArgs.add(javaBinValue)
        }

        jvmArgs.get().forEach { arg ->
            cliArgs.add("--jvm-arg")
            cliArgs.add(arg)
        }

        cliArgs.add("--base-dir")
        cliArgs.add(baseDir.get().asFile.absolutePath)

        if (mysqlEnabled.get()) {
            cliArgs.add("--mysql")
            cliArgs.add("--mysql-port")
            cliArgs.add(mysqlPort.get().toString())
            cliArgs.add("--mysql-database")
            cliArgs.add(mysqlDatabase.get())
            cliArgs.add("--mysql-root-password")
            cliArgs.add(mysqlRootPassword.get())
            cliArgs.add("--mysql-image")
            cliArgs.add(mysqlImage.get())

            val containerName = mysqlContainerName.orNull?.trim()
            if (!containerName.isNullOrEmpty()) {
                cliArgs.add("--mysql-container-name")
                cliArgs.add(containerName)
            }
        }

        val replaceArgs = regexReplaces.get()
        if (replaceArgs.size % 3 != 0) {
            throw GradleException("regexReplaces must be configured in groups of 3: <path>, <regex>, <replacement>")
        }
        for (i in replaceArgs.indices step 3) {
            cliArgs.add("--replace")
            cliArgs.add(replaceArgs[i])
            cliArgs.add(replaceArgs[i + 1])
            cliArgs.add(replaceArgs[i + 2])
        }

        cliArgs.addAll(additionalArgs.get())

        val exitCode = ServerRunnerMain.run(cliArgs.toTypedArray())
        if (exitCode != 0) {
            throw GradleException("server-runner exited with code $exitCode")
        }
    }
}
