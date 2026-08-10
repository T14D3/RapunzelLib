plugins {
    alias(libs.plugins.vanilla.module.conventions)
}

dependencies {
    api(project(":events"))
    compileOnly(libs.annotations)
    compileOnly(libs.mixin)
    // SharedAdventureComponentCodec (vanilla <-> adventure Component conversion)
    // for event payloads that carry adventure components (death message,
    // login deny reason).
    implementation(project(":nbt-shared"))
}
