plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}

