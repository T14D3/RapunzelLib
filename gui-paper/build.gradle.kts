plugins {
    alias(libs.plugins.gui.platform.module.conventions)
    alias(libs.plugins.paper.userdev.module.conventions)
}

dependencies {
    implementation(project(":nbt-paper"))
}
