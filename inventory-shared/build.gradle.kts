plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

dependencies {
    api(project(":inventory"))
    compileOnly(libs.annotations)
}
