import java.util.Properties
import org.gradle.api.tasks.testing.Test

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "de.t14d3.rapunzellib.gradle"

version = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.3.1-SNAPSHOT"

// Minimal Minecraft target resolution for test dependencies.
val minecraftTargetMatrixFile = listOf(
    rootProject.file("gradle/minecraft-targets.properties"),
    rootProject.file("../gradle/minecraft-targets.properties"),
).firstOrNull { it.isFile }

val paperApiVersion = if (minecraftTargetMatrixFile != null) {
    val props = Properties().apply { minecraftTargetMatrixFile.inputStream().use { load(it) } }
    val activeTarget = providers.gradleProperty("rapunzellib.minecraftTarget")
        .orElse(providers.gradleProperty("rapunzellib.minecraftCoreVersion"))
        .orElse(props.getProperty("core") ?: "26.1.2")
    val token = activeTarget.get().replace(Regex("[^A-Za-z0-9]"), "_")
    props.getProperty("target.$token.version.paper-api")
        ?: props.getProperty("version.paper-api")
        ?: if (activeTarget.get().startsWith("1.")) "${activeTarget.get()}-R0.1-SNAPSHOT" else "${activeTarget.get()}.build.+"
} else {
    "26.1.2.build.+"
}

dependencies {
    implementation(libs.gson)
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
    implementation(libs.asm.analysis)
    implementation(libs.snakeyaml)

    // MCProtocolLib for bot clients - needed at runtime by DevRunner
    implementation("org.geysermc.mcprotocollib:protocol:26.2-SNAPSHOT")
    // Adventure plain-text serializer for bot chat extraction
    implementation(libs.adventure.serializer.plain)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation("io.papermc.paper:paper-api:$paperApiVersion")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("rapunzellib") {
            id = "de.t14d3.rapunzellib"
            implementationClass = "de.t14d3.rapunzellib.gradle.RapunzelLibGradlePlugin"
            displayName = "RapunzelLib tooling"
            description = "Project templates, message validation, code generation, and multi-server runner for RapunzelLib-based projects."
            version = project.version.toString()
        }
    }
}
