import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    `java-gradle-plugin`
}

group = "de.t14d3.rapunzellib.buildlogic"

fun String.toMinecraftTargetToken(): String =
    replace(Regex("[^A-Za-z0-9]"), "_")

val minecraftTargetMatrixFile = listOf(
    rootProject.file("gradle/minecraft-targets.properties"),
    rootProject.file("../gradle/minecraft-targets.properties"),
).firstOrNull { it.isFile }
    ?: error("Could not locate gradle/minecraft-targets.properties")

val minecraftTargetMatrixProperties = Properties().apply {
    minecraftTargetMatrixFile.inputStream().use { load(it) }
}

val activeMinecraftTarget = providers.gradleProperty("rapunzellib.minecraftTarget")
    .orElse(providers.gradleProperty("rapunzellib.minecraftCoreVersion"))
    .orElse(minecraftTargetMatrixProperties.getProperty("core") ?: error("No core Minecraft target configured"))

fun targetMatrixProperty(target: String, key: String): String? =
    minecraftTargetMatrixProperties.getProperty("target.${target.toMinecraftTargetToken()}.$key")

fun pluginVersion(alias: String): String? =
    providers.gradleProperty("rapunzellib.plugin.$alias").orNull
        ?: targetMatrixProperty(activeMinecraftTarget.get(), "plugin.$alias")
        ?: minecraftTargetMatrixProperties.getProperty("plugin.$alias")

fun dependencyVersion(alias: String): String? =
    providers.gradleProperty("rapunzellib.version.${activeMinecraftTarget.get()}.$alias").orNull
        ?: providers.gradleProperty("rapunzellib.version.$alias").orNull
        ?: targetMatrixProperty(activeMinecraftTarget.get(), "version.$alias")

fun pluginDependency(module: String, alias: String): String =
    "$module:${pluginVersion(alias) ?: error("No plugin dependency version configured for $alias")}"

fun targetDependency(module: String, alias: String): String =
    "$module:${dependencyVersion(alias) ?: error("No target dependency version configured for $alias")}"

configurations.configureEach {
    resolutionStrategy.eachDependency {
        when ("${requested.group}:${requested.name}") {
            "io.papermc.paperweight:paperweight-userdev" -> pluginVersion("paperweight-userdev")?.let(::useVersion)
            "org.spongepowered:vanillagradle" -> pluginVersion("vanilla-gradle")?.let(::useVersion)
            "net.fabricmc:fabric-loom" -> pluginVersion("fabric-loom")?.let(::useVersion)
            "net.neoforged:moddev-gradle" -> pluginVersion("neoforge-moddev")?.let(::useVersion)
            "io.papermc.paper:paper-api" -> dependencyVersion("paper-api")?.let(::useVersion)
        }
    }
}

dependencies {
    // Depend on the public gradle-plugin for shared classes
    // (RegistryCatalogSpec, RegistryCatalogSourceExtractor, GeneratedTextFile, etc.)
    implementation("de.t14d3.rapunzellib.gradle:gradle-plugin:0")

    // Platform plugin tooling - needed at compile time by BuildLogicPluginSupport
    compileOnly(pluginDependency("org.spongepowered:vanillagradle", "vanilla-gradle"))
    implementation(pluginDependency("net.fabricmc:fabric-loom", "fabric-loom"))
    implementation(pluginDependency("net.neoforged:moddev-gradle", "neoforge-moddev"))

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(targetDependency("io.papermc.paper:paper-api", "paper-api"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("rootSubprojectConventions") {
            id = "de.t14d3.rapunzellib.root-subproject-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.RootSubprojectConventionsPlugin"
        }
        create("rootPublishingConventions") {
            id = "de.t14d3.rapunzellib.root-publishing-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.RootPublishingConventionsPlugin"
        }
        create("featurePlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.feature-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.FeaturePlatformModuleConventionsPlugin"
        }
        create("fabricModuleConventions") {
            id = "de.t14d3.rapunzellib.fabric-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.FabricModuleConventionsPlugin"
        }
        create("neoforgeModuleConventions") {
            id = "de.t14d3.rapunzellib.neoforge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.NeoForgeModuleConventionsPlugin"
        }
        create("paperApiModuleConventions") {
            id = "de.t14d3.rapunzellib.paper-api-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.PaperApiModuleConventionsPlugin"
        }
        create("paperUserdevModuleConventions") {
            id = "de.t14d3.rapunzellib.paper-userdev-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.PaperUserdevModuleConventionsPlugin"
        }
        create("vanillaModuleConventions") {
            id = "de.t14d3.rapunzellib.vanilla-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.VanillaModuleConventionsPlugin"
        }
        create("spongeModuleConventions") {
            id = "de.t14d3.rapunzellib.sponge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.SpongeModuleConventionsPlugin"
        }
        create("velocityModuleConventions") {
            id = "de.t14d3.rapunzellib.velocity-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.VelocityModuleConventionsPlugin"
        }
        create("commonModuleConventions") {
            id = "de.t14d3.rapunzellib.common-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.CommonModuleConventionsPlugin"
        }
        create("databaseSpoolModuleConventions") {
            id = "de.t14d3.rapunzellib.database-spool-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.DatabaseSpoolModuleConventionsPlugin"
        }
        create("guiPlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.gui-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.GuiPlatformModuleConventionsPlugin"
        }
        create("networkModuleConventions") {
            id = "de.t14d3.rapunzellib.network-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.NetworkModuleConventionsPlugin"
        }
        create("backendPlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.backend-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.BackendPlatformModuleConventionsPlugin"
        }
        create("platformFabricModuleConventions") {
            id = "de.t14d3.rapunzellib.platform-fabric-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.PlatformFabricModuleConventionsPlugin"
        }
        create("platformNeoForgeModuleConventions") {
            id = "de.t14d3.rapunzellib.platform-neoforge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.PlatformNeoForgeModuleConventionsPlugin"
        }
        create("internalTasks") {
            id = "de.t14d3.rapunzellib.internal-tasks"
            implementationClass = "de.t14d3.rapunzellib.buildlogic.conventions.InternalTasksPlugin"
        }
    }
}
