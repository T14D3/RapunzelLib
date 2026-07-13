pluginManagement {
    val minecraftTargets = java.util.Properties().apply {
        file("../gradle/minecraft-targets.properties").inputStream().use { load(it) }
    }

    fun targetToken(target: String): String =
        target.replace(Regex("[^A-Za-z0-9]"), "_")

    val activeMinecraftTarget =
        providers.gradleProperty("rapunzellib.minecraftTarget").orNull
            ?: providers.gradleProperty("rapunzellib.minecraftCoreVersion").orNull
            ?: minecraftTargets.getProperty("core")
            ?: error("No core Minecraft target configured")

    fun targetProperty(key: String): String? =
        minecraftTargets.getProperty("target.${targetToken(activeMinecraftTarget)}.$key")

    fun pluginVersion(alias: String, legacyProperty: String): String =
        providers.gradleProperty("rapunzellib.plugin.$alias").orNull
            ?: targetProperty("plugin.$alias")
            ?: minecraftTargets.getProperty("plugin.$alias")
            ?: providers.gradleProperty(legacyProperty).orNull
            ?: error("No plugin version configured for $alias")

    val shadowPluginVersion = pluginVersion("shadow", "shadowVersion")
    val fabricLoomPluginVersion = pluginVersion("fabric-loom", "fabricLoomVersion")
    val neoforgeModdevPluginVersion = pluginVersion("neoforge-moddev", "neoforgeModdevVersion")
    val vanillaGradlePluginVersion = pluginVersion("vanilla-gradle", "vanillaGradleVersion")

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "com.gradleup.shadow" -> useVersion(shadowPluginVersion)
                "net.fabricmc.fabric-loom" -> useVersion(fabricLoomPluginVersion)
                "net.neoforged.moddev" -> useVersion(neoforgeModdevPluginVersion)
                "org.spongepowered.gradle.vanilla" -> useVersion(vanillaGradlePluginVersion)
            }
        }
    }

    plugins {
        id("com.gradleup.shadow") version shadowPluginVersion
        id("net.fabricmc.fabric-loom") version fabricLoomPluginVersion
        id("net.neoforged.moddev") version neoforgeModdevPluginVersion
        id("org.spongepowered.gradle.vanilla") version vanillaGradlePluginVersion
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://repo.opencollab.dev/maven-snapshots")
        maven("https://repo.opencollab.dev/maven-releases")
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
