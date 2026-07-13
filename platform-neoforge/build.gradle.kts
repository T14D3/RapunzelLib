plugins {
    alias(libs.plugins.platform.neoforge.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    // Feature modules - shared + neoforge-specific implementations
    implementation(project(":events"))
    implementation(project(":events-neoforge"))
    implementation(project(":commands"))
    implementation(project(":commands-shared"))
    implementation(project(":commands-neoforge"))
    implementation(project(":gui"))
    implementation(project(":gui-shared"))
    implementation(project(":gui-neoforge"))
    implementation(project(":visuals"))
    implementation(project(":visuals-shared"))
    implementation(project(":visuals-neoforge"))
    implementation(project(":nbt"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-neoforge"))
    implementation(project(":inventory"))
    implementation(project(":inventory-shared"))
    implementation(project(":inventory-neoforge"))
}

// Thin jar (lowercase default name)
tasks.jar {
    // Keep default project artifact naming: platform-neoforge-<version>.jar
}

// Full standalone mod (CamelCase)
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RapunzelLibNeoForge")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
    dependencies {
        exclude(dependency("net.neoforged:neoforge"))
        exclude(dependency("org.jetbrains:annotations"))
    }
    exclude("/net/minecraft/**")
    exclude("/com/mojang/**")
    exclude("/assets/minecraft/**")
    exclude("/data/minecraft/**")
    exclude("/log4j*.properties")
    exclude("/log4j*.xml")
    exclude("/LICENSE*")
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}