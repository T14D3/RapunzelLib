plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.sponge.module.conventions)
}

dependencies {
    implementation(project(":livetest-shared"))
}
