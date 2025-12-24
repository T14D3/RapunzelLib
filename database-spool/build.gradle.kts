plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":api"))
    api(project(":network"))

    api(libs.spool)
    implementation(libs.slf4j.api)
    compileOnly(libs.annotations)

    testImplementation(libs.junit.jupiter)
}

tasks {
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
        archiveClassifier.set("shaded")
    }
}
