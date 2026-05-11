plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

dependencies {
    api(project(":visuals"))
    compileOnly(libs.annotations)
}
