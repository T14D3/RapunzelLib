import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    `java-library`
    id("fabric-loom") version "1.11.7"
    `maven-publish`
    alias(libs.plugins.shadow)
}

val shade: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(
        configurations.getByName("api"),
        configurations.getByName("implementation"),
    )
}

dependencies {
    api(project(":events"))

    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }

    withType<ShadowJar>().configureEach {
        configurations.set(listOf(shade))
        archiveClassifier.set("dev-shaded")
    }

    val shadowJar = named<ShadowJar>("shadowJar")

    register<RemapJarTask>("remapShadowJar") {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.flatMap { it.archiveFile })
        archiveClassifier.set("shaded")
    }

    named("assemble") {
        dependsOn("remapShadowJar")
    }
}
