plugins {
    alias(libs.plugins.backend.platform.module.conventions)
    alias(libs.plugins.paper.userdev.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    // Include all feature modules so the shadowJar provides a complete RLib plugin
    implementation(project(":events-paper"))
    implementation(project(":commands-paper"))
    implementation(project(":gui-paper"))
    implementation(project(":visuals-paper"))
    implementation(project(":inventory-paper"))
    implementation(project(":nbt-paper"))

}

// Thin jar (lowercase default name) - published as the main Maven artifact for compile-time consumers
tasks.jar {
    // Keep default project artifact naming: platform-paper-<version>.jar
}

// Full standalone plugin/mod (CamelCase)
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RapunzelLibPaper")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
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
