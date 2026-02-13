plugins {
    `java-library`
}

sourceSets {
    named("main") {
        java.srcDir("src/generated/java")
    }
}

dependencies {
    api(project(":api"))
    compileOnly(libs.annotations)
    implementation(libs.adventure.serializer.gson)
    testImplementation(libs.junit.jupiter)
}
