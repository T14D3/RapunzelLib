import de.t14d3.rapunzellib.gradle.RapunzelLibExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.TaskTriggersConfig
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip

plugins {
    base
    alias(libs.plugins.idea.ext)
    alias(libs.plugins.root.subproject.conventions)
    alias(libs.plugins.root.publishing.conventions)
    alias(libs.plugins.userdev) apply false
    alias(libs.plugins.vanilla.gradle) apply false
}

val buildVersion = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.3.0-SNAPSHOT"

val reposiliteBaseUrl =
    (findProperty("reposiliteBaseUrl") as String?)
        ?: System.getenv("REPOSILITE_BASE_URL")
        ?: "https://maven.t14d3.de"

val parityInCheck =
    providers.gradleProperty("rapunzellib.parityInCheck")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

val minecraftTargetVersions =
    providers.gradleProperty("rapunzellib.minecraftTargetVersions")
        .map { value -> value.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
        .orElse(listOf("1.21.11", "1.21.10"))

val minecraftCoreVersion =
    providers.gradleProperty("rapunzellib.minecraftCoreVersion")
        .orElse("1.21.11")

val activeMinecraftTarget =
    providers.gradleProperty("rapunzellib.minecraftTarget")
    .orElse(minecraftCoreVersion)

val hasExplicitMinecraftTarget =
    providers.gradleProperty("rapunzellib.minecraftTarget")
        .map { it.isNotBlank() }
        .orElse(false)

val collectedJarVersion = activeMinecraftTarget.get()

fun multiversionDependencyVersion(alias: String): String? {
    val target = activeMinecraftTarget.orNull ?: return null
    versionCatalogTargetOverride(alias, target)?.let { return it }
    providers.gradleProperty("rapunzellib.version.$target.$alias").orNull?.let { return it }
    providers.gradleProperty("rapunzellib.version.$alias").orNull?.let { return it }

    return when (alias) {
        "minecraft" -> target
        "paper-api" -> "$target-R0.1-SNAPSHOT"
        else -> null
    }
}

fun String.toMinecraftTaskSuffix(): String =
    replace(Regex("[^A-Za-z0-9]"), "_")

fun String.toVersionCatalogTargetToken(): String =
    replace(Regex("[^A-Za-z0-9]"), "_")

fun Project.versionCatalogAliasFor(group: String?, name: String): String? {
    val catalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    return catalog.libraryAliases.firstNotNullOfOrNull { alias ->
        val dependency = catalog.findLibrary(alias).orElse(null)?.get() ?: return@firstNotNullOfOrNull null
        if (dependency.module.group == group && dependency.module.name == name) {
            alias.replace('.', '-')
        } else {
            null
        }
    }
}

fun Project.versionCatalogTargetOverride(alias: String, target: String): String? {
    val catalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
    val targetAlias = "$alias-ver-$target"
    catalog.findVersion(targetAlias).orElse(null)?.requiredVersion?.let { return it }

    val sanitizedTargetAlias = "$alias-ver-${target.toVersionCatalogTargetToken()}"
    if (sanitizedTargetAlias == targetAlias) {
        return null
    }
    return catalog.findVersion(sanitizedTargetAlias).orElse(null)?.requiredVersion
}

val syncGeneratedSources = tasks.register("rapunzellibSyncGeneratedSources") {
    group = "rapunzellib"
    description = "Generates RapunzelLib checked-in derived sources before IntelliJ Gradle sync."
}

val collectAllJars = tasks.register<Copy>("collectAllJars") {
    group = "distribution"
    description = "Copies all jar artifacts into build/libs/$collectedJarVersion."
    into(layout.buildDirectory.dir("libs/$collectedJarVersion"))
    includeEmptyDirs = false
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

allprojects {
    group = "de.t14d3.rapunzellib"
    version = buildVersion

    repositories {
        mavenCentral()
        maven("${reposiliteBaseUrl.trimEnd('/')}/releases")
        maven("${reposiliteBaseUrl.trimEnd('/')}/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://jitpack.io")
    }

    tasks.withType<Zip>().configureEach {
        if (name == "shadowJar") {
            isZip64 = true
        }
    }

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val alias = versionCatalogAliasFor(requested.group, requested.name) ?: return@eachDependency
            val version = multiversionDependencyVersion(alias) ?: return@eachDependency
            useVersion(version)
            because("RapunzelLib active Minecraft target is ${activeMinecraftTarget.get()}")
        }
    }
}

subprojects {
    plugins.withId("de.t14d3.rapunzellib") {
        extensions.configure<RapunzelLibExtension>("rapunzellib") {
            multiVersion {
                enabled.set(true)
                targetVersions.set(minecraftTargetVersions)
                coreVersion.set(minecraftCoreVersion)
            }
        }
    }

    tasks.withType<Jar>().configureEach {
        val jarPrefix = path.removePrefix(":").replace(':', '-')
        collectAllJars.configure {
            dependsOn(this@configureEach)
            from(archiveFile) {
                rename { "$jarPrefix-$it" }
            }
        }
    }
}

collectAllJars.configure {
    from(layout.projectDirectory.dir("gradle-plugin/build/libs")) {
        include("*.jar")
        rename { "gradle-plugin-build-$it" }
    }
}

val minecraftVersionBuilds = minecraftTargetVersions.get().map { minecraftVersion ->
    tasks.register<Exec>("buildMinecraft${minecraftVersion.toMinecraftTaskSuffix()}") {
        group = "verification"
        description = "Builds RapunzelLib for Minecraft $minecraftVersion."

        val wrapper = if (System.getProperty("os.name").lowercase().contains("windows")) {
            rootProject.file("gradlew.bat")
        } else {
            rootProject.file("gradlew")
        }

        workingDir = rootProject.rootDir
        commandLine(
            wrapper.absolutePath,
            "build",
            *gradle.startParameter.projectProperties
                .filterKeys { it != "rapunzellib.minecraftTarget" }
                .flatMap { (key, value) -> listOf("-P$key=$value") }
                .toTypedArray(),
            "-Prapunzellib.minecraftTarget=$minecraftVersion"
        )
    }
}

tasks.register("buildAllMinecraftVersions") {
    group = "verification"
    description = "Builds RapunzelLib once for each configured Minecraft target version."
    dependsOn(minecraftVersionBuilds)
}

tasks.named("build") {
    dependsOn(collectAllJars)
    if (!hasExplicitMinecraftTarget.get()) {
        dependsOn("buildAllMinecraftVersions")
    }
}

syncGeneratedSources.configure {
    dependsOn(
        ":nbt:rapunzellibGenerateRNbtSchema",
        ":nbt:rapunzellibGenerateKeyCatalog",
        ":nbt:rapunzellibGenerateRegistryCatalogs",
        ":nbt:rapunzellibGenerateBlockEntityRootNbtSchema",
        ":nbt:rapunzellibGenerateBlockStateNbtSchema",
        ":nbt:rapunzellibGenerateEntityRootNbtSchema",
        ":api:rapunzellibGenerateRegistryCatalogs",
    )
}

idea.project.settings {
    taskTriggers {
        afterSync(syncGeneratedSources.get())
    }
}

val checkParity = tasks.register("checkParity") {
    group = "verification"
    description = "Runs opt-in parity verification tasks that are excluded from the default build lifecycle."

    dependsOn(
        ":api:rapunzellibVerifyRegistryCatalogParity",
    )
}

tasks.named("check") {
    if (parityInCheck.get()) {
        dependsOn(checkParity)
    }
}

gradle.projectsEvaluated {
    checkParity.configure {
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("rapunzellibVerifySharedParity") })
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("rapunzellibVerifyInstallerWiring") })
    }

    val gradlePluginTests = project(":gradle-plugin").tasks.matching { it.name == "test" }
    project(":api").tasks.matching { it.name == "compileJava" }.configureEach {
        mustRunAfter(gradlePluginTests)
    }
    project(":api").tasks.matching { it.name == "test" }.configureEach {
        mustRunAfter(gradlePluginTests)
    }
}
