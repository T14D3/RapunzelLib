plugins {
    `java-library`
    alias(libs.plugins.paper.api.module.conventions)
}

dependencies {
    api(project(":livetest-shared"))
    compileOnly(libs.annotations)
}
