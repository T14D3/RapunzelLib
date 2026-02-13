import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar

plugins {
    alias(libs.plugins.platform.neoforge.module.conventions)
    alias(libs.plugins.shadow)
}

val companionModJar by tasks.registering(Jar::class) {
    archiveBaseName.set("platform-neoforge")
    archiveClassifier.set("companion")
    dependsOn(tasks.named("compileJava"))

    from(layout.buildDirectory.dir("classes/java/main")) {
        include("de/t14d3/rapunzellib/platform/neoforge/NeoForgePlatformMod.class")
    }
    from(layout.projectDirectory.dir("src/companion/resources")) {
        expand("version" to project.version.toString())
    }
}

extensions.configure(PublishingExtension::class.java) {
    publications.withType(MavenPublication::class.java).configureEach {
        artifact(companionModJar)
    }
}
