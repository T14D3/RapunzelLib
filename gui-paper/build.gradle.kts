plugins {
    alias(libs.plugins.gui.platform.module.conventions)
    alias(libs.plugins.paper.userdev.module.conventions)
}

dependencies {
    implementation(project(":gui-shared"))
    implementation(project(":nbt-shared"))
    implementation(project(":nbt-paper"))
}
