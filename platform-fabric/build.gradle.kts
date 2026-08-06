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

    // Also add to shadow configuration so they're included in the standalone JAR.
    // shadow() deps are resolved with proper variant attributes (set in afterEvaluate)
    // and won't include Fabric API or its transitive dependency tree.
    shadow(project(":events"))
    shadow(project(":events-fabric"))
    shadow(project(":commands"))
    shadow(project(":commands-shared"))
    shadow(project(":commands-fabric"))
    shadow(project(":gui"))
    shadow(project(":gui-shared"))
    shadow(project(":gui-fabric"))
    shadow(project(":visuals"))
    shadow(project(":visuals-shared"))
    shadow(project(":visuals-fabric"))
    shadow(project(":nbt"))
    shadow(project(":nbt-shared"))
    shadow(project(":nbt-fabric"))
    shadow(project(":inventory"))
    shadow(project(":inventory-shared"))
    shadow(project(":inventory-fabric"))
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
    // Keep default project artifact naming: platform-fabric-<version>.jar
}

// Full standalone mod (CamelCase) - only bundles shadow configuration
val expandStandaloneFabricModJson = tasks.register("expandStandaloneFabricModJson") {
    val props = mapOf("version" to project.version.toString())
    val input = file("src/standalone/resources/fabric.mod.json")
    val output = layout.buildDirectory.file("generated/standalone/resources/fabric.mod.json")
    inputs.file(input)
    inputs.properties(props)
    outputs.file(output)
    doLast {
        val text = input.readText()
        var expanded = text
        for ((k, v) in props) {
            expanded = expanded.replace("\${$k}", v)
        }
        val out = output.get().asFile
        out.parentFile.mkdirs()
        out.writeText(expanded)
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations["shadow"])
    archiveBaseName.set("RapunzelLibFabric")
    archiveClassifier.set("standalone")
    mergeServiceFiles()
    dependsOn(expandStandaloneFabricModJson)
    dependencies {
        // Exclude platform-provided dependencies (Fabric API, Minecraft, and their transitive trees)
        exclude("net.fabricmc:fabric-loader")
        exclude("net.fabricmc.fabric-api:fabric-api")
        exclude("net.fabricmc.fabric-api:fabric-api-base")
        exclude("net.fabricmc.fabric-api:fabric-command-api-v2")
        exclude("net.fabricmc.fabric-api:fabric-lifecycle-events-v1")
        exclude("net.fabricmc.fabric-api:fabric-networking-api-v1")
        exclude("net.fabricmc.fabric-api:fabric-events-interaction-v0")
        exclude("com.mojang:minecraft")
        exclude("org.jetbrains:annotations")
    }
    // The bundled events-fabric module ships its own fabric.mod.json declaring the
    // mixin configs; the standalone must present a single merged manifest that also
    // wires those mixins so emulated events actually fire.
    exclude("fabric.mod.json")
    transform(com.github.jengelman.gradle.plugins.shadow.transformers.IncludeResourceTransformer::class.java) {
        file.set(layout.buildDirectory.file("generated/standalone/resources/fabric.mod.json"))
        resource.set("fabric.mod.json")
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
