plugins {
    alias(libs.plugins.vanilla.module.conventions)
}


dependencies {
    api(project(":nbt"))
    implementation(libs.adventure.serializer.gson)
}
