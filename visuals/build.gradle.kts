plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    api(project(":events"))
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}
