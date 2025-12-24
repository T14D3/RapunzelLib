plugins {
    `java-library`
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":api"))
    implementation(project(":common"))
    implementation(project(":network"))
    implementation(project(":database-spool"))

    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
    compileOnly(libs.annotations)

    testImplementation(libs.junit.jupiter)
}

tasks {
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
        archiveClassifier.set("shaded")
        relocate("org.yaml.snakeyaml", "de.t14d3.rapunzellib.libs.snakeyaml")
        relocate("com.google.gson", "de.t14d3.rapunzellib.libs.gson")
    }
}
