plugins {
    alias(libs.plugins.backend.platform.module.conventions)
    alias(libs.plugins.paper.userdev.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":platform-shared"))
    implementation(project(":nbt"))
    // Include all feature modules so the shadowJar provides a complete RLib plugin
    implementation(project(":events"))
    implementation(project(":events-paper"))
    implementation(project(":commands"))
    implementation(project(":commands-shared"))
    implementation(project(":commands-paper"))
    implementation(project(":gui"))
    implementation(project(":visuals"))
    implementation(project(":visuals-paper"))
}

// Thin jar (lowercase default name) - published as the main Maven artifact for compile-time consumers
tasks.jar {
    // Keep default project artifact naming: platform-paper-<version>.jar
}

// Full standalone plugin/mod (CamelCase) - the deployable bundle containing all RLib classes
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RapunzelLibPaper")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
    dependencies {
        exclude(dependency("io.papermc.paper:paper-api"))
        exclude(dependency("io.papermc.paper:paper-mojangapi"))
        exclude(dependency("com.mojang:minecraft"))
        exclude(dependency("org.jetbrains:annotations"))
    }
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}