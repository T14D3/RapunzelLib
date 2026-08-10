plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.neoforge.module.conventions)
}

dependencies {
    implementation(project(":events-shared"))
    implementation(libs.mixin)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}
