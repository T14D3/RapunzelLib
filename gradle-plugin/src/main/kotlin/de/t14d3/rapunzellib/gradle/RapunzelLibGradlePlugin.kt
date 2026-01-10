package de.t14d3.rapunzellib.gradle

import de.t14d3.rapunzellib.gradle.tasks.InitTemplateTask
import de.t14d3.rapunzellib.gradle.tasks.RunServersTask
import de.t14d3.rapunzellib.gradle.tasks.ValidateMessagesTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

class RapunzelLibGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create<RapunzelLibExtension>("rapunzellib")

        ext.messagesFile.convention(project.layout.projectDirectory.file("src/main/resources/messages.yml"))
        ext.additionalMessagesFiles.convention(emptyList())
        ext.failOnUnusedKeys.convention(true)
        ext.alwaysUsedKeys.convention(setOf("prefix"))
        ext.messageKeyCallOwners.convention(emptySet())
        ext.messageKeyCallMethods.convention(setOf("getMessage", "getRaw"))
        ext.messageKeyPrefix.convention("")
        ext.templateOutputDir.convention(project.layout.projectDirectory.dir("template"))
        ext.templateBasePackage.convention("de.t14d3")
        ext.templateProjectName.convention(project.name)

        val validate = project.tasks.register<ValidateMessagesTask>("rapunzellibValidateMessages") {
            group = "verification"
            description = "Validates messages.yml keys against compiled bytecode usage."

            messagesFile.set(ext.messagesFile)
            additionalMessagesFiles.set(ext.additionalMessagesFiles)
            failOnUnusedKeys.set(ext.failOnUnusedKeys)
            alwaysUsedKeys.set(ext.alwaysUsedKeys)
            messageKeyCallOwners.set(ext.messageKeyCallOwners)
            messageKeyCallMethods.set(ext.messageKeyCallMethods)
            messageKeyPrefix.set(ext.messageKeyPrefix)
        }

        project.plugins.withId("java") {
            val sourceSets = project.extensions.getByType<SourceSetContainer>()
            val main = sourceSets.getByName("main")
            validate.configure {
                classesDirs.from(main.output.classesDirs)
            }
        }

        project.tasks.register<RunServersTask>("rapunzellibRunServers") {
            group = "run"
            description = "Runs Velocity + multiple Paper backends via Fill v3 (RapunzelLib runner)."

            paperVersion.convention(project.providers.gradleProperty("multiPaperVersion").orElse("1.21.10"))
            paperCount.convention(project.providers.gradleProperty("multiPaperCount").map { it.toInt() }.orElse(2))
            paperBasePort.convention(project.providers.gradleProperty("multiPaperBasePort").map { it.toInt() }.orElse(25566))

            velocityEnabled.convention(project.providers.gradleProperty("multiVelocityEnabled").map { it.toBoolean() }.orElse(true))
            velocityVersion.convention(project.providers.gradleProperty("multiVelocityVersion").orElse("latest"))
            velocityPort.convention(project.providers.gradleProperty("multiVelocityPort").map { it.toInt() }.orElse(25565))

            javaBin.convention(project.providers.gradleProperty("multiRunnerJava").orElse(""))
            jvmArgs.convention(
                project.providers.gradleProperty("multiRunnerJvmArgs")
                    .map { raw -> raw.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
                    .orElse(emptyList())
            )

            mysqlEnabled.convention(project.providers.gradleProperty("multiMysql").map { it.toBoolean() }.orElse(false))
            mysqlPort.convention(project.providers.gradleProperty("multiMysqlPort").map { it.toInt() }.orElse(3307))
            mysqlDatabase.convention(project.providers.gradleProperty("multiMysqlDatabase").orElse("rapunzellib"))
            mysqlRootPassword.convention(project.providers.gradleProperty("multiMysqlRootPassword").orElse("root"))
            mysqlImage.convention(project.providers.gradleProperty("multiMysqlImage").orElse("mysql:latest"))
            mysqlContainerName.convention(project.providers.gradleProperty("multiMysqlContainerName").orElse(""))

            regexReplaces.convention(emptyList())
            additionalArgs.convention(perfAdditionalArgs(project, forceJfr = false))
            // Prefer a single shared runner directory for multi-module builds.
            baseDir.convention(project.rootProject.layout.projectDirectory.dir("run/server-runner"))
        }

        project.tasks.register<RunServersTask>("rapunzellibRunPerfServers") {
            group = "run"
            description = "Runs Velocity + multiple Paper backends with JFR enabled (RapunzelLib runner)."

            paperVersion.convention(project.providers.gradleProperty("multiPaperVersion").orElse("1.21.10"))
            paperCount.convention(project.providers.gradleProperty("multiPaperCount").map { it.toInt() }.orElse(2))
            paperBasePort.convention(project.providers.gradleProperty("multiPaperBasePort").map { it.toInt() }.orElse(25566))

            velocityEnabled.convention(project.providers.gradleProperty("multiVelocityEnabled").map { it.toBoolean() }.orElse(true))
            velocityVersion.convention(project.providers.gradleProperty("multiVelocityVersion").orElse("latest"))
            velocityPort.convention(project.providers.gradleProperty("multiVelocityPort").map { it.toInt() }.orElse(25565))

            javaBin.convention(project.providers.gradleProperty("multiRunnerJava").orElse(""))
            jvmArgs.convention(
                project.providers.gradleProperty("multiRunnerJvmArgs")
                    .map { raw -> raw.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
                    .orElse(emptyList())
            )

            // Perf runs should resemble prod defaults: enable MySQL unless explicitly disabled.
            mysqlEnabled.convention(project.providers.gradleProperty("multiMysql").map { it.toBoolean() }.orElse(true))
            mysqlPort.convention(project.providers.gradleProperty("multiMysqlPort").map { it.toInt() }.orElse(3307))
            mysqlDatabase.convention(project.providers.gradleProperty("multiMysqlDatabase").orElse("rapunzellib"))
            mysqlRootPassword.convention(project.providers.gradleProperty("multiMysqlRootPassword").orElse("root"))
            mysqlImage.convention(project.providers.gradleProperty("multiMysqlImage").orElse("mysql:latest"))
            mysqlContainerName.convention(project.providers.gradleProperty("multiMysqlContainerName").orElse(""))

            regexReplaces.convention(emptyList())
            additionalArgs.convention(perfAdditionalArgs(project, forceJfr = true))
            // Prefer a single shared runner directory for multi-module builds.
            baseDir.convention(project.rootProject.layout.projectDirectory.dir("run/server-runner"))
        }

        project.afterEvaluate {
            val runTasks = listOf(
                project.tasks.named("rapunzellibRunServers", RunServersTask::class.java),
                project.tasks.named("rapunzellibRunPerfServers", RunServersTask::class.java),
            )

            (project.tasks.findByName("paperJar") as? AbstractArchiveTask)?.let { jar ->
                runTasks.forEach { t ->
                    t.configure {
                        dependsOn(jar)
                        paperPluginJar.set(jar.archiveFile)
                    }
                }
            }

            (project.tasks.findByName("velocityJar") as? AbstractArchiveTask)?.let { jar ->
                runTasks.forEach { t ->
                    t.configure {
                        dependsOn(jar)
                        velocityPluginJar.set(jar.archiveFile)
                    }
                }
            }
        }

        project.tasks.register<InitTemplateTask>("rapunzellibInitTemplate") {   
            group = "rapunzellib"
            description = "Generates a small RapunzelLib starter template into template/."

            outputDir.set(ext.templateOutputDir)
            basePackage.set(ext.templateBasePackage)
            projectName.set(ext.templateProjectName)
        }
    }

    private fun perfAdditionalArgs(project: Project, forceJfr: Boolean): Provider<List<String>> = project.provider {
        val args = mutableListOf<String>()

        val jfrEnabled = forceJfr || ((project.findProperty("multiJfr") as String?)?.toBoolean() == true)
        if (jfrEnabled) {
            args += "--jfr"
            val settings = (project.findProperty("multiJfrSettings") as String?)?.trim()
            if (!settings.isNullOrBlank()) {
                args += "--jfr-settings"
                args += settings
            }
        }

        val paperExtraPlugins = csvPaths(project.findProperty("multiPaperExtraPlugins") as String?)
        val velocityExtraPlugins = csvPaths(project.findProperty("multiVelocityExtraPlugins") as String?)

        val sparkPaper = (project.findProperty("multiSparkPaperPlugin") as String?)?.trim().orEmpty()
        val sparkVelocity = (project.findProperty("multiSparkVelocityPlugin") as String?)?.trim().orEmpty()

        (paperExtraPlugins + listOf(sparkPaper).filter { it.isNotBlank() }).forEach { rawPath ->
            val jarFile = project.file(rawPath)
            if (!jarFile.isFile) throw GradleException("Paper extra plugin jar does not exist: $rawPath")
            args += "--paper-extra-plugin"
            args += jarFile.absolutePath
        }

        (velocityExtraPlugins + listOf(sparkVelocity).filter { it.isNotBlank() }).forEach { rawPath ->
            val jarFile = project.file(rawPath)
            if (!jarFile.isFile) throw GradleException("Velocity extra plugin jar does not exist: $rawPath")
            args += "--velocity-extra-plugin"
            args += jarFile.absolutePath
        }

        args
    }

    private fun csvPaths(raw: String?): List<String> =
        raw?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}
