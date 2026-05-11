plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":visuals-shared"))
    implementation(project(":nbt-shared"))
    add("fabricImplementation", libs.fabric.networking.api.v1)
}
