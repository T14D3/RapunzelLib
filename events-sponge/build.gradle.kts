plugins {
    `java-library`
}

dependencies {
    api(project(":events"))
    compileOnly(libs.sponge.api)
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}
