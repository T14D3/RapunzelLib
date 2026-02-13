plugins {
    alias(libs.plugins.gui.platform.module.conventions)
    alias(libs.plugins.neoforge.module.conventions)
}

dependencies {
    implementation(project(":gui-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-neoforge"))
    implementation(libs.adventure.platform.neoforge)
}
