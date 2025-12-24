plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":api"))
    implementation(libs.adventure.minimessage)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
}

tasks {
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
        archiveClassifier.set("shaded")
        relocate("org.yaml.snakeyaml", "de.t14d3.rapunzellib.libs.snakeyaml")
    }
}
