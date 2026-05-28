plugins {
    alias(libs.plugins.network.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.adventure.serializer.gson)
}
