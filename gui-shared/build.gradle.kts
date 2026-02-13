plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

dependencies {
    api(project(":gui"))
    implementation(project(":nbt-shared"))
    implementation(libs.adventure.serializer.plain)
}
