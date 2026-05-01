plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

dependencies {
    api(project(":commands"))
    compileOnly(libs.annotations)
    implementation(libs.adventure.serializer.gson)
}
