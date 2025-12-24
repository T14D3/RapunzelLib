plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

dependencies {
    implementation(project(":tool-server-runner"))
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
    implementation(libs.asm.analysis)
    implementation(libs.snakeyaml)
}

gradlePlugin {
    plugins {
        create("rapunzellib") {
            id = "de.t14d3.rapunzellib"
            implementationClass = "de.t14d3.rapunzellib.gradle.RapunzelLibGradlePlugin"
            displayName = "RapunzelLib tooling"
            description = "Project templates, message validation, and multi-server runner for RapunzelLib-based projects."
            version = rootProject.version.toString()
        }
    }
}
