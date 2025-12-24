plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":api"))

    implementation(libs.gson)
    implementation(libs.slf4j.api)
    compileOnly(libs.annotations)

    testImplementation(libs.junit.jupiter)
}

tasks {
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
        archiveClassifier.set("shaded")
        relocate("com.google.gson", "de.t14d3.rapunzellib.libs.gson")
    }
}

