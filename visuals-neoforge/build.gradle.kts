plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.neoforge.module.conventions)
}

dependencies {
    implementation(project(":visuals-shared"))
}
