plugins {
    alias(libs.plugins.gui.platform.module.conventions)
    alias(libs.plugins.neoforge.module.conventions)
}

dependencies {
    implementation(project(":gui-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-neoforge"))
    implementation(libs.adventure.platform.neoforge)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}
