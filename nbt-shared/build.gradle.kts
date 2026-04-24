import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask

plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

sourceSets {
    named("main") {
        java.srcDir("src/generated/java")
    }
}

val generatedSourcesDir = layout.projectDirectory.dir("src/generated/java")

fun registerRNbtSchemaTask(taskName: String, schemaFile: String, generatedClassName: String) =
    tasks.register<GenerateRNbtSchemaTask>(taskName) {
        group = "rapunzellib"
        description = "Generates $generatedClassName from checked-in RNbt schema inputs."

        inputFiles.from(layout.projectDirectory.file(schemaFile))
        packageName.set("de.t14d3.rapunzellib.nbt.shared.generated")
        className.set(generatedClassName)
        outputDir.set(generatedSourcesDir)
    }

val generateSharedEntityRootNbtSchema = registerRNbtSchemaTask(
    "rapunzellibGenerateSharedEntityRootNbtSchema",
    "src/main/rapunzellib/shared-entity-root-schema.yml",
    "SharedEntityRootNbt"
)

val generateSharedBlockEntityRootNbtSchema = registerRNbtSchemaTask(
    "rapunzellibGenerateSharedBlockEntityRootNbtSchema",
    "src/main/rapunzellib/shared-block-entity-root-schema.yml",
    "SharedBlockEntityRootNbt"
)

val generateSharedBlockStateNbtSchema = registerRNbtSchemaTask(
    "rapunzellibGenerateSharedBlockStateNbtSchema",
    "src/main/rapunzellib/shared-block-state-schema.yml",
    "SharedBlockStateNbt"
)

val generateSharedSchemas = listOf(
    generateSharedEntityRootNbtSchema,
    generateSharedBlockEntityRootNbtSchema,
    generateSharedBlockStateNbtSchema,
)

tasks.named("rapunzellibGenerateRNbtSchema") {
    dependsOn(generateSharedSchemas)
}

tasks.named("compileJava") {
    dependsOn(generateSharedSchemas)
}

tasks.named("sourcesJar") {
    dependsOn(generateSharedSchemas)
}

tasks.named("javadoc") {
    dependsOn(generateSharedSchemas)
}

dependencies {
    api(project(":nbt"))
    implementation(libs.adventure.serializer.gson)
}
