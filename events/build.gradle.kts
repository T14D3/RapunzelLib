plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    api(project(":inventory"))
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}
