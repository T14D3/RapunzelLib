import de.t14d3.rapunzellib.gradle.RapunzelLibExtension
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import java.util.Properties

plugins {
    base
    alias(libs.plugins.idea.ext)
    alias(libs.plugins.root.subproject.conventions)
    alias(libs.plugins.root.publishing.conventions)
    alias(libs.plugins.dokka)
    alias(libs.plugins.visualizer.collector.java)
    alias(libs.plugins.visualizer.bundler)
    alias(libs.plugins.userdev) apply false
    alias(libs.plugins.vanilla.gradle) apply false
}

val buildVersion = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.3.1-SNAPSHOT"

val reposiliteBaseUrl =
    (findProperty("reposiliteBaseUrl") as String?)
        ?: System.getenv("REPOSILITE_BASE_URL")
        ?: "https://maven.t14d3.de"

val parityInCheck =
    providers.gradleProperty("rapunzellib.parityInCheck")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

data class MinecraftTargetMatrix(
    val properties: Properties,
    val targetVersions: List<String>,
    val coreVersion: String,
) {
    fun targetProperty(target: String, key: String): String? =
        properties.getProperty("target.${target.toMinecraftTargetToken()}.$key")

    fun version(target: String, alias: String): String? =
        targetProperty(target, "version.$alias")

    fun targetProperties(target: String, prefix: String): Map<String, String> {
        val propertyPrefix = "target.${target.toMinecraftTargetToken()}.$prefix"
        return properties.stringPropertyNames()
            .asSequence()
            .filter { it.startsWith(propertyPrefix) }
            .associate { key -> key.removePrefix(propertyPrefix) to properties.getProperty(key) }
    }
}

fun String.toMinecraftTargetToken(): String =
    replace(Regex("[^A-Za-z0-9]"), "_")

fun parseMinecraftTargetList(value: String): List<String> =
    value.split(',').map { it.trim() }.filter { it.isNotEmpty() }

val minecraftTargetMatrixFile = layout.projectDirectory.file("gradle/minecraft-targets.properties").asFile
val minecraftTargetMatrixProperties = Properties().apply {
    minecraftTargetMatrixFile.inputStream().use { load(it) }
}
val minecraftTargetMatrix = MinecraftTargetMatrix(
    properties = minecraftTargetMatrixProperties,
    targetVersions = parseMinecraftTargetList(minecraftTargetMatrixProperties.getProperty("targets") ?: ""),
    coreVersion = minecraftTargetMatrixProperties.getProperty("core")
        ?: error("No core Minecraft target configured in ${minecraftTargetMatrixFile.relativeTo(rootDir)}"),
)

val minecraftTargetVersions =
    providers.gradleProperty("rapunzellib.minecraftTargetVersions")
        .map(::parseMinecraftTargetList)
        .orElse(minecraftTargetMatrix.targetVersions)

val minecraftCoreVersion =
    providers.gradleProperty("rapunzellib.minecraftCoreVersion")
        .orElse(minecraftTargetMatrix.coreVersion)

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
    providers.gradleProperty("rapunzellib.version.$target.$alias").orNull?.let { return it }
    providers.gradleProperty("rapunzellib.version.$alias").orNull?.let { return it }
    minecraftTargetMatrix.version(target, alias)?.let { return it }

    return when (alias) {
        "minecraft" -> target
        "paper-api" -> if (target.startsWith("1.")) "$target-R0.1-SNAPSHOT" else "$target.build.+"
        else -> null
    }
}

fun String.toMinecraftTaskSuffix(): String =
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



fun Project.setDefaultProjectProperty(name: String, value: String) {
    if (findProperty(name) == null) {
        extensions.extraProperties[name] = value
    }
}

val activeTargetVersionDefaults = minecraftTargetMatrix.targetProperties(activeMinecraftTarget.get(), "version.")
val activeFabricDefaults = minecraftTargetMatrix.targetProperties(activeMinecraftTarget.get(), "fabric.")


allprojects {
    group = "de.t14d3.rapunzellib"
    version = buildVersion

    activeTargetVersionDefaults.forEach { (alias, version) ->
        setDefaultProjectProperty("rapunzellib.version.$alias", version)
    }
    setDefaultProjectProperty("rapunzellib.minecraftCoreVersion", minecraftCoreVersion.get())
    setDefaultProjectProperty("rapunzellib.minecraftTargetVersions", minecraftTargetVersions.get().joinToString(","))
    activeFabricDefaults.forEach { (key, value) ->
        setDefaultProjectProperty("rapunzellib.fabric.$key", value)
    }

    repositories {
        mavenCentral()
        maven("${reposiliteBaseUrl.trimEnd('/')}/releases")
        maven("${reposiliteBaseUrl.trimEnd('/')}/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://repo.opencollab.dev/maven-snapshots")
        maven("https://repo.opencollab.dev/maven-releases")
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

    plugins.withId("java-base") {
        pluginManager.apply("org.jetbrains.dokka")
    }

    plugins.withId("org.jetbrains.dokka") {
        dependencies {
            add(
                "dokkaPlugin",
                "org.jetbrains.dokka:kotlin-as-java-plugin:2.2.0"
            )
        }
    }

    tasks.withType<Javadoc>().configureEach {
        val opts = options as StandardJavadocDocletOptions
        opts.addStringOption("Xdoclint:none", "-quiet")
        opts.addBooleanOption("quiet", true)
        isFailOnError = false
    }
}
// Collect gradle-plugin JARs into the root build/libs/<version>/ directory
collectAllJars.configure {
    from(layout.projectDirectory.dir("gradle-plugin/build/libs")) {
        include("*.jar")
        rename { "RapunzelLibGradlePlugin-$it" }
    }
}

// After each Gradle build, aggregate all subproject JAR outputs into root build/libs/<version>/
gradle.buildFinished {
    val targetDir = rootProject.layout.buildDirectory.dir("libs/$collectedJarVersion").get().asFile
    targetDir.mkdirs()
    rootProject.subprojects.forEach { subproject ->
        val buildLibs = File(subproject.buildDir, "libs")
        if (buildLibs.isDirectory()) {
            buildLibs.listFiles { f -> f.name.endsWith(".jar") }?.forEach { jarFile ->
                val destFile = File(targetDir, jarFile.name)
                if (!destFile.exists()) {
                    jarFile.copyTo(destFile)
                }
            }
        }
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
        environment("JAVA_HOME", System.getenv("JAVA_HOME")?.takeIf { it.isNotBlank() }
            ?: System.getProperty("java.home"))
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
    if (!hasExplicitMinecraftTarget.get()) {
        dependsOn("buildAllMinecraftVersions")
    } else {
        dependsOn(collectAllJars)
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

val excludedFromAggregation = setOf(":bom")
val aggregationProjects = subprojects.filter {
    it.path !in excludedFromAggregation && !it.name.startsWith("gradle")
}

dependencies {
    aggregationProjects.forEach { p ->
        dokka(project(p.path))
    }
}

dokka {
    dokkaPublications.html {
        moduleName.set("RapunzelLib")
        moduleVersion.set(buildVersion)
        outputDirectory.set(layout.buildDirectory.dir("dokka"))
    }
}

tasks.register("javadoc") {
    group = "documentation"
    description = "Generates unified HTML documentation via Dokka"
    dependsOn("dokkaGenerate")
}

codebaseCollector {
    //excludePaths.set(listOf("**/generated-sources/**"))
}

codebaseBundler {
    // graphFile defaults to build/visualizer/graph.json (from collector)
    // outputDir defaults to build/reports/codebase
}
