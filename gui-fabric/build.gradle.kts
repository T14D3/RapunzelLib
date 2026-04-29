plugins {
    alias(libs.plugins.gui.platform.module.conventions)
    alias(libs.plugins.fabric.module.conventions)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":gui-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-fabric"))
    add("fabricImplementation", libs.fabric.networking.api.v1)
    add("fabricImplementation", libs.adventure.platform.fabric)
}
