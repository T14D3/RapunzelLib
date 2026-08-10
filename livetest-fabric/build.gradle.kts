plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
}

dependencies {
    implementation(project(":livetest-shared"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
