plugins {
    alias(libs.plugins.backend.platform.module.conventions)
    alias(libs.plugins.sponge.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":nbt"))

    // Feature modules - shared + sponge-specific implementations
    implementation(project(":events"))
    implementation(project(":events-shared"))
    implementation(project(":events-sponge"))
    implementation(project(":commands"))
    implementation(project(":commands-shared"))
    implementation(project(":commands-sponge"))
    implementation(project(":gui"))
    implementation(project(":gui-shared"))
    implementation(project(":gui-sponge"))
    implementation(project(":visuals"))
    implementation(project(":visuals-shared"))
    implementation(project(":visuals-sponge"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-sponge"))
    implementation(project(":inventory"))
    implementation(project(":inventory-shared"))
    implementation(project(":inventory-sponge"))
}

// Thin jar (lowercase default name)
tasks.jar {
    // Keep default project artifact naming: platform-sponge-<version>.jar
}

// Full standalone plugin (CamelCase)
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RapunzelLibSponge")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("META-INF/sponge_plugins.json") {
        expand(props)
    }
}
