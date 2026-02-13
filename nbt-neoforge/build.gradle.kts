plugins {
    alias(libs.plugins.feature.platform.module.conventions)
    alias(libs.plugins.neoforge.module.conventions)
}

neoForge {
    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    implementation(project(":nbt-shared"))
    testImplementation(libs.adventure.serializer.gson)
}
