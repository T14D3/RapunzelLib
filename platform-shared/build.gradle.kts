plugins {
    alias(libs.plugins.vanilla.module.conventions)
    alias(libs.plugins.rapunzellib)
}

dependencies {
    api(project(":api"))
    implementation(project(":common"))
    compileOnly(libs.annotations)
    implementation(project(":nbt-shared"))
    implementation(libs.adventure.serializer.plain)
    api(project(":inventory"))
}
