plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.paper.userdev.module.conventions)
}

dependencies {
    api(project(":api"))
    implementation(project(":common"))
    implementation(project(":nbt-shared"))
}
