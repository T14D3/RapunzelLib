plugins {
    alias(libs.plugins.gui.platform.module.conventions)
    alias(libs.plugins.sponge.module.conventions)
}

dependencies {
    implementation(project(":nbt-sponge"))
}
