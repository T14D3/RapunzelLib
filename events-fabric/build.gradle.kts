plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":events-shared"))
    modImplementation(libs.fabric.events.interaction.v0)
    modImplementation(libs.fabric.lifecycle.events.v1)
    modImplementation(libs.fabric.networking.api.v1)
}
