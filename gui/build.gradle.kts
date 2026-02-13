plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    api(project(":events"))
    api(project(":inventory"))
    api(project(":nbt"))
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}
