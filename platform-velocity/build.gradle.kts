plugins {
    alias(libs.plugins.backend.platform.module.conventions)
    alias(libs.plugins.velocity.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":nbt"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("velocity-plugin.json") {
        expand(props)
    }
}
