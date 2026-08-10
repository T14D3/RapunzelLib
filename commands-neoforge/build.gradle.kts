plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.neoforge.module.conventions)
}

dependencies {
    implementation(libs.adventure.serializer.gson)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}
