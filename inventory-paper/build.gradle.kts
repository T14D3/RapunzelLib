plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.paper.userdev.module.conventions)
}

dependencies {
    implementation(project(":inventory-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-paper"))
}
