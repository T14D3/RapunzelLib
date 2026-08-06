import org.gradle.api.tasks.testing.Test

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "de.t14d3.rapunzellib.gradle"

version = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.3.1-SNAPSHOT"

dependencies {
    implementation(libs.gson)
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
    implementation(libs.asm.analysis)
    implementation(libs.snakeyaml)

    implementation("org.geysermc.mcprotocollib:protocol:26.2-SNAPSHOT")
    implementation(libs.adventure.serializer.plain)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.paper.api)
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
