plugins {
    alias(libs.plugins.backend.platform.module.conventions)
    alias(libs.plugins.velocity.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":nbt"))

    // Feature modules - shared modules (no velocity-specific feature modules exist yet)
    implementation(project(":events"))
    implementation(project(":events-shared"))
    implementation(project(":commands"))
    implementation(project(":commands-shared"))
    implementation(project(":gui"))
    implementation(project(":gui-shared"))
    implementation(project(":visuals"))
    implementation(project(":visuals-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":inventory"))
    implementation(project(":inventory-shared"))
}

// Thin jar (lowercase default name)
tasks.jar {
    // Keep default project artifact naming: platform-velocity-<version>.jar
}

// Full standalone plugin (CamelCase)
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RapunzelLibVelocity")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("velocity-plugin.json") {
        expand(props)
    }
}
