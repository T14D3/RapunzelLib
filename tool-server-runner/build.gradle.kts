plugins {
    application
    java
    `maven-publish`
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.gson)
    testImplementation(libs.junit.jupiter)
}

application {
    mainClass.set("de.t14d3.rapunzellib.serverrunner.ServerRunnerMain")
}

tasks {
    withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>().configureEach {
        archiveClassifier.set("all")
        manifest {
            attributes["Main-Class"] = "de.t14d3.rapunzellib.serverrunner.ServerRunnerMain"
        }
    }
}
