plugins {
    `java-library`
}

dependencies {
    api(libs.adventure.api)
    api(libs.slf4j.api)
    api(libs.annotations)

    testImplementation(libs.junit.jupiter)
    testImplementation(project(":common"))
}
