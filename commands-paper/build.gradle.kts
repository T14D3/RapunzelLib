plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.paper.api.module.conventions)
}

dependencies {
    testImplementation(libs.paper.api)
}
