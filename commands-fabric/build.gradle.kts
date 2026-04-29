plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    add("fabricImplementation", libs.fabric.command.api.v2)
}
