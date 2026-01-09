plugins {
    `java-library`
}

dependencies {
    api(project(":commands"))
    compileOnly(libs.sponge.api)
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}
