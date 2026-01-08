import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.neoforge.moddev)
}

val versions: VersionCatalog = extensions
    .getByType<VersionCatalogsExtension>()
    .named("libs")

neoForge {
    version = versions.findVersion("neoforge").get().requiredVersion

    mods {
        create("rapunzellib_platform_neoforge") {
            sourceSet(sourceSets.main.get())
        }
    }

    addModdingDependenciesTo(sourceSets.main.get())
}

val shade: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(
        configurations.getByName("api"),
        configurations.getByName("implementation"),
    )
}

dependencies {
    api(project(":api"))
    implementation(project(":common"))
    implementation(project(":network"))
    implementation(project(":database-spool"))

    implementation(libs.adventure.platform.neoforge)
    implementation(libs.slf4j.api)

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

    withType<ShadowJar>().configureEach {
        configurations.set(listOf(shade))
        archiveClassifier.set("shaded")
        isZip64 = true
        relocate("org.yaml.snakeyaml", "de.t14d3.rapunzellib.libs.snakeyaml")
        relocate("com.google.gson", "de.t14d3.rapunzellib.libs.gson")
    }
}
