plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.velocity.module.conventions)
}

dependencies {
    api(project(":common"))
}
