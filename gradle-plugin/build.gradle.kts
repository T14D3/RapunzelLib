import java.util.Properties

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "de.t14d3.rapunzellib.gradle"

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
    implementation(libs.gson)
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
    implementation(libs.asm.analysis)
    implementation(libs.snakeyaml)
    //compileOnly(pluginDependency("io.papermc.paperweight:paperweight-userdev", "paperweight-userdev"))
    compileOnly(pluginDependency("org.spongepowered:vanillagradle", "vanilla-gradle"))
    implementation(pluginDependency("net.fabricmc:fabric-loom", "fabric-loom"))
    implementation(pluginDependency("net.neoforged:moddev-gradle", "neoforge-moddev"))

    // MCProtocolLib for bot clients - needed at runtime by DevRunner
    implementation("org.geysermc.mcprotocollib:protocol:26.2-SNAPSHOT")
    // Adventure plain-text serializer for bot chat extraction
    implementation(libs.adventure.serializer.plain)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testImplementation(targetDependency("io.papermc.paper:paper-api", "paper-api"))
}

gradlePlugin {
    plugins {
        create("rapunzellib") {
            id = "de.t14d3.rapunzellib"
            implementationClass = "de.t14d3.rapunzellib.gradle.RapunzelLibGradlePlugin"
            displayName = "RapunzelLib tooling"
            description = "Project templates, message validation, and multi-server runner for RapunzelLib-based projects."
            version = rootProject.version.toString()
        }
        create("rootSubprojectConventions") {
            id = "de.t14d3.rapunzellib.root-subproject-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.RootSubprojectConventionsPlugin"
        }
        create("rootPublishingConventions") {
            id = "de.t14d3.rapunzellib.root-publishing-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.RootPublishingConventionsPlugin"
        }
        create("featurePlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.feature-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.FeaturePlatformModuleConventionsPlugin"
        }
        create("fabricModuleConventions") {
            id = "de.t14d3.rapunzellib.fabric-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.FabricModuleConventionsPlugin"
        }
        create("neoforgeModuleConventions") {
            id = "de.t14d3.rapunzellib.neoforge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.NeoForgeModuleConventionsPlugin"
        }
        create("paperApiModuleConventions") {
            id = "de.t14d3.rapunzellib.paper-api-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.PaperApiModuleConventionsPlugin"
        }
        create("paperUserdevModuleConventions") {
            id = "de.t14d3.rapunzellib.paper-userdev-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.PaperUserdevModuleConventionsPlugin"
        }
        create("vanillaModuleConventions") {
            id = "de.t14d3.rapunzellib.vanilla-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.VanillaModuleConventionsPlugin"
        }
        create("spongeModuleConventions") {
            id = "de.t14d3.rapunzellib.sponge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.SpongeModuleConventionsPlugin"
        }
        create("velocityModuleConventions") {
            id = "de.t14d3.rapunzellib.velocity-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.VelocityModuleConventionsPlugin"
        }
        create("commonModuleConventions") {
            id = "de.t14d3.rapunzellib.common-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.CommonModuleConventionsPlugin"
        }
        create("databaseSpoolModuleConventions") {
            id = "de.t14d3.rapunzellib.database-spool-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.DatabaseSpoolModuleConventionsPlugin"
        }
        create("guiPlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.gui-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.GuiPlatformModuleConventionsPlugin"
        }
        create("networkModuleConventions") {
            id = "de.t14d3.rapunzellib.network-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.NetworkModuleConventionsPlugin"
        }
        create("backendPlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.backend-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.BackendPlatformModuleConventionsPlugin"
        }
        create("platformFabricModuleConventions") {
            id = "de.t14d3.rapunzellib.platform-fabric-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.PlatformFabricModuleConventionsPlugin"
        }
        create("platformNeoForgeModuleConventions") {
            id = "de.t14d3.rapunzellib.platform-neoforge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.conventions.PlatformNeoForgeModuleConventionsPlugin"
        }
    }
}
