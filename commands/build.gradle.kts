plugins {
    `java-library`
}

dependencies {
    api(project(":api"))
    compileOnlyApi(libs.brigadier)
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.brigadier)
}
