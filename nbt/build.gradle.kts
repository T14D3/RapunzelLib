plugins {
    `java-library`
    alias(libs.plugins.rapunzellib)
}

rapunzellib {
    rNbtSchema {
        packageName.set("de.t14d3.rapunzellib.nbt.generated")
        className.set("RItemNbt")
        outputDir.set(layout.projectDirectory.dir("src/generated/java"))
    }
}

val generateRNbtSchema = tasks.named("rapunzellibGenerateRNbtSchema")

tasks.matching { it.name == "sourcesJar" }.configureEach {
    dependsOn(generateRNbtSchema)
}

tasks.named("javadoc") {
    dependsOn(generateRNbtSchema)
}

dependencies {
    api(project(":api"))
    compileOnly(libs.annotations)
    implementation(libs.adventure.serializer.gson)
    testImplementation(libs.junit.jupiter)
}
