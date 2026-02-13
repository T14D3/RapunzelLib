plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

sourceSets {
    named("main") {
        java.srcDir("src/generated/java")
    }
}

dependencies {
    api(project(":nbt"))
    implementation(libs.adventure.serializer.gson)
}
