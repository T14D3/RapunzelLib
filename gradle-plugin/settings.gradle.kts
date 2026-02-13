pluginManagement {
    plugins {
        id("com.gradleup.shadow") version providers.gradleProperty("shadowVersion")
        id("fabric-loom") version providers.gradleProperty("fabricLoomVersion")
        id("net.neoforged.moddev") version providers.gradleProperty("neoforgeModdevVersion")
        id("org.spongepowered.gradle.vanilla") version providers.gradleProperty("vanillaGradleVersion")
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
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
