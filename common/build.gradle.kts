plugins {
    alias(libs.plugins.common.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.fastutil)
    implementation(project(":nbt"))
}
