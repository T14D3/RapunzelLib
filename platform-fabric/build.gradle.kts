plugins {
    alias(libs.plugins.platform.fabric.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    add("fabricImplementation", libs.fabric.lifecycle.events.v1)
    add("fabricImplementation", libs.fabric.networking.api.v1)

    // Feature modules - shared + fabric-specific implementations
    implementation(project(":events"))
    implementation(project(":events-fabric"))
    implementation(project(":commands"))
    implementation(project(":commands-shared"))
    implementation(project(":commands-fabric"))
    implementation(project(":gui"))
    implementation(project(":gui-shared"))
    implementation(project(":gui-fabric"))
    implementation(project(":visuals"))
    implementation(project(":visuals-shared"))
    implementation(project(":visuals-fabric"))
    implementation(project(":nbt"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-fabric"))
    implementation(project(":inventory"))
    implementation(project(":inventory-shared"))
    implementation(project(":inventory-fabric"))
}

// Thin jar (lowercase default name)
tasks.jar {
    // Keep default project artifact naming: platform-fabric-<version>.jar
}

// Full standalone mod (CamelCase)
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RapunzelLibFabric")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
    exclude("/net/minecraft/**")
    exclude("/com/mojang/**")
    dependencies {
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("net.fabricmc:fabric-loader"))
    }
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}