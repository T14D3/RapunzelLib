plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
}

dependencies {
    implementation(project(":events-shared"))
    add("fabricImplementation", libs.fabric.events.interaction.v0)
    add("fabricImplementation", libs.fabric.lifecycle.events.v1)
    add("fabricImplementation", libs.fabric.networking.api.v1)
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}
