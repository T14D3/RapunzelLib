plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

dependencies {
    api(project(":events"))
    compileOnly(libs.annotations)
}
