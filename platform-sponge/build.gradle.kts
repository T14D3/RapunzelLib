plugins {
    alias(libs.plugins.backend.platform.module.conventions)
    alias(libs.plugins.sponge.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.fastutil)
    implementation(project(":nbt"))
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("META-INF/sponge_plugins.json") {
        expand(props)
    }
}
