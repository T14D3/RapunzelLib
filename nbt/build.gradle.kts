import de.t14d3.rapunzellib.gradle.tasks.GenerateRNbtSchemaTask
import org.gradle.kotlin.dsl.register

plugins {
    `java-library`
    alias(libs.plugins.rapunzellib)
}

rapunzellib {
    rNbtSchema {
        packageName.set("de.t14d3.rapunzellib.nbt.generated")
        className.set("RItemNbt")
    }
}

val generatedSourcesDir = layout.projectDirectory.dir("src/generated/java")

fun registerRNbtSchemaTask(taskName: String, schemaFile: String, generatedClassName: String) =
    tasks.register<GenerateRNbtSchemaTask>(taskName) {
        group = "rapunzellib"
        description = "Generates $generatedClassName from checked-in RNbt schema inputs."

        inputFiles.from(layout.projectDirectory.file(schemaFile))
        packageName.set("de.t14d3.rapunzellib.nbt.generated")
        className.set(generatedClassName)
        outputDir.set(generatedSourcesDir)
    }

val generateEntityRootNbtSchema = registerRNbtSchemaTask(
    "rapunzellibGenerateEntityRootNbtSchema",
    "src/main/rapunzellib/entity-root-schema.yml",
    "EntityRootNbt"
)

val generateBlockEntityRootNbtSchema = registerRNbtSchemaTask(
    "rapunzellibGenerateBlockEntityRootNbtSchema",
    "src/main/rapunzellib/block-entity-root-schema.yml",
    "BlockEntityRootNbt"
)

val generateBlockStateNbtSchema = registerRNbtSchemaTask(
    "rapunzellibGenerateBlockStateNbtSchema",
    "src/main/rapunzellib/block-state-schema.yml",
    "BlockStateNbt"
)

val generateSchemas = listOf(
    generateEntityRootNbtSchema,
    generateBlockEntityRootNbtSchema,
    generateBlockStateNbtSchema,
)

tasks.named("rapunzellibGenerateRNbtSchema") {
    dependsOn(generateSchemas)
}

tasks.named("compileJava") {
    dependsOn(generateSchemas)
}

tasks.named("sourcesJar") {
    dependsOn(generateSchemas)
}

tasks.named("javadoc") {
    dependsOn(generateSchemas)
}

dependencies {
    api(project(":api"))
    compileOnly(libs.annotations)
    implementation(libs.adventure.serializer.gson)
    testImplementation(libs.junit.jupiter)
}
