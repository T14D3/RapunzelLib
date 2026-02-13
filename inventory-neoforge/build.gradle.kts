plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.neoforge.module.conventions)
}

dependencies {
    implementation(project(":inventory-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-neoforge"))
}
