import org.gradle.api.tasks.bundling.Zip

plugins {
    base
    alias(libs.plugins.root.subproject.conventions)
    alias(libs.plugins.root.publishing.conventions)
    alias(libs.plugins.userdev) apply false
    alias(libs.plugins.vanilla.gradle) apply false
}

val buildVersion = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.3.0-SNAPSHOT"

val reposiliteBaseUrl =
    (findProperty("reposiliteBaseUrl") as String?)
        ?: System.getenv("REPOSILITE_BASE_URL")
        ?: "https://maven.t14d3.de"

val parityInCheck =
    providers.gradleProperty("rapunzellib.parityInCheck")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

allprojects {
    group = "de.t14d3.rapunzellib"
    version = buildVersion

    repositories {
        mavenCentral()
        maven("${reposiliteBaseUrl.trimEnd('/')}/releases")
        maven("${reposiliteBaseUrl.trimEnd('/')}/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://jitpack.io")
    }

    tasks.withType<Zip>().configureEach {
        if (name == "shadowJar") {
            isZip64 = true
        }
    }
}

val checkParity = tasks.register("checkParity") {
    group = "verification"
    description = "Runs opt-in parity verification tasks that are excluded from the default build lifecycle."

    dependsOn(
        ":api:rapunzellibVerifyRegistryCatalogParity",
    )
}

tasks.named("check") {
    if (parityInCheck.get()) {
        dependsOn(checkParity)
    }
}

gradle.projectsEvaluated {
    checkParity.configure {
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("rapunzellibVerifySharedParity") })
        dependsOn(allprojects.mapNotNull { it.tasks.findByName("rapunzellibVerifyInstallerWiring") })
    }

    val gradlePluginTests = project(":gradle-plugin").tasks.matching { it.name == "test" }
    project(":api").tasks.matching { it.name == "compileJava" }.configureEach {
        mustRunAfter(gradlePluginTests)
    }
    project(":api").tasks.matching { it.name == "test" }.configureEach {
        mustRunAfter(gradlePluginTests)
    }
}
