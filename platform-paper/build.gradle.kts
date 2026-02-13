import org.gradle.api.tasks.bundling.Jar

plugins {
    alias(libs.plugins.backend.platform.module.conventions)
    alias(libs.plugins.paper.userdev.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":platform-shared"))
    implementation(project(":nbt"))
}

val companionPluginJar by tasks.registering(Jar::class) {
    archiveBaseName.set("platform-paper")
    archiveClassifier.set("plugin")
    dependsOn(tasks.named("compileJava"))

    from(layout.buildDirectory.dir("classes/java/main")) {
        include("de/t14d3/rapunzellib/platform/paper/PaperPlatformPlugin.class")
    }
    from(layout.projectDirectory.dir("src/companion/resources")) {
        expand("version" to project.version.toString())
    }
}

tasks.processResources {
    from(companionPluginJar) {
        into("META-INF/rapunzellib")
        rename { "platform-paper-plugin.jar" }
    }
}
