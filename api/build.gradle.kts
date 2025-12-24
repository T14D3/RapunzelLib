plugins {
    `java-library`
}

dependencies {
    api(libs.adventure.api)
    api(libs.slf4j.api)
    implementation(libs.snakeyaml)
    compileOnly(libs.annotations)

    testImplementation(libs.junit.jupiter)
}
