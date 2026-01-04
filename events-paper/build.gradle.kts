plugins {
    `java-library`
}

dependencies {
    api(project(":events"))
    compileOnly(libs.paper.api)
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}

