plugins {
    `java-library`
    alias(libs.plugins.neoforge.moddev)
}

val versions: VersionCatalog = extensions
    .getByType<VersionCatalogsExtension>()
    .named("libs")

neoForge {
    version = versions.findVersion("neoforge").get().requiredVersion
    mods {
        create("rapunzellib_commands_neoforge") {
            sourceSet(sourceSets.main.get())
        }
    }
    addModdingDependenciesTo(sourceSets.main.get())
}

dependencies {
    api(project(":commands"))
    compileOnly(libs.annotations)
    testImplementation(libs.junit.jupiter)
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(props)
        }
    }
}

