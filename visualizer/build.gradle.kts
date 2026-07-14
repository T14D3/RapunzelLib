plugins {
    `java-gradle-plugin`
    `maven-publish`
}

group = "de.t14d3.rapunzellib.gradle"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.gson)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        create("rapunzellibVisualizer") {
            id = "de.t14d3.rapunzellib.visualizer"
            implementationClass = "de.t14d3.rapunzellib.visualizer.VisualizerGradlePlugin"
            displayName = "RapunzelLib Codebase Visualizer"
            description = "Generates a static, interactive HTML report visualizing a multi-module Java project's architecture and relationships."
            version = rootProject.version.toString()
        }
    }
}
