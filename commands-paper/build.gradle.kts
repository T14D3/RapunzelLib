plugins {
    `java-library`
}

dependencies {
    api(project(":commands"))
    compileOnly(libs.paper.api)
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}

