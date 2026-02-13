plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

dependencies {
    api(project(":api"))
    implementation(project(":common"))
    compileOnly(libs.annotations)
    implementation(libs.fastutil)
}
