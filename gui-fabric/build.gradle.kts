plugins {
    alias(libs.plugins.gui.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":gui-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-fabric"))
    modImplementation(libs.fabric.networking.api.v1)
    modImplementation(libs.adventure.platform.fabric)
}
