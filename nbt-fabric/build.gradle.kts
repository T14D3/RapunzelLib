plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
}

dependencies {
    implementation(project(":nbt-shared"))
    testImplementation(libs.adventure.serializer.gson)
}
