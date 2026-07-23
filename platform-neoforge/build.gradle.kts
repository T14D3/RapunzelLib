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

    // Also add to shadow configuration so they're included in the standalone JAR.
    // shadow() deps are resolved with proper variant attributes (set in afterEvaluate)
    // and won't include NeoForge/Minecraft transitive trees.
    shadow(project(":events"))
    shadow(project(":events-neoforge"))
    shadow(project(":commands"))
    shadow(project(":commands-shared"))
    shadow(project(":commands-neoforge"))
    shadow(project(":gui"))
    shadow(project(":gui-shared"))
    shadow(project(":gui-neoforge"))
    shadow(project(":visuals"))
    shadow(project(":visuals-shared"))
    shadow(project(":visuals-neoforge"))
    shadow(project(":nbt"))
    shadow(project(":nbt-shared"))
    shadow(project(":nbt-neoforge"))
    shadow(project(":inventory"))
    shadow(project(":inventory-shared"))
    shadow(project(":inventory-neoforge"))
}

// Configure the shadow configuration to use the runtime classpath attributes,
// so Gradle can resolve project dependencies without variant ambiguity.
afterEvaluate {
    configurations["shadow"].attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
    }
}

// Thin jar (lowercase default name)
tasks.jar {
    // Keep default project artifact naming: platform-neoforge-<version>.jar
}

// Full standalone mod (CamelCase) - only bundles shadow configuration
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations["shadow"])
    archiveBaseName.set("RapunzelLibNeoForge")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
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
