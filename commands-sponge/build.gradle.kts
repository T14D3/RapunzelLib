plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.sponge.module.conventions)
}

dependencies {
    testImplementation(libs.brigadier)
    testImplementation(libs.sponge.api)
}
