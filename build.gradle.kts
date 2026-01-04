import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask

plugins {
    base
}

val buildVersion = System.getenv("VERSION")?.takeIf { it.isNotBlank() } ?: "0.1.9-SNAPSHOT"

abstract class CheckReposiliteConfig : DefaultTask() {
    @get:Input @get:Optional abstract val reposiliteBaseUrl: Property<String>
    @get:Input @get:Optional abstract val reposiliteUsername: Property<String>
    @get:Input @get:Optional abstract val reposilitePassword: Property<String>

    @TaskAction
    fun run() {
        val missingKeys = mutableListOf<String>()

        fun require(name: String, value: String?) {
            if (value.isNullOrBlank()) missingKeys += name
        }

        require("reposiliteUsername/REPOSILITE_USERNAME", reposiliteUsername.orNull)
        require("reposilitePassword/REPOSILITE_PASSWORD", reposilitePassword.orNull)

        reposiliteBaseUrl.orNull?.let { baseUrl ->
            val url = baseUrl.trim()
            if (url.isNotBlank() && !url.startsWith("http://") && !url.startsWith("https://")) {
                missingKeys += "reposiliteBaseUrl/REPOSILITE_BASE_URL must start with http:// or https://"
            }
        }

        if (missingKeys.isNotEmpty()) {
            throw GradleException(
                "Missing Reposilite publishing configuration:\n" +
                    missingKeys.joinToString(separator = "\n") { "- $it" } +
                    "\n\nConfigure these as Gradle properties (recommended: ~/.gradle/gradle.properties) or environment variables."
            )
        }
    }
}
val reposiliteBaseUrl =
    (findProperty("reposiliteBaseUrl") as String?)
        ?: System.getenv("REPOSILITE_BASE_URL")
        ?: "https://maven.t14d3.de"
val reposiliteUsername: String? = (findProperty("reposiliteUsername") as String?) ?: System.getenv("REPOSILITE_USERNAME")
val reposilitePassword: String? = (findProperty("reposilitePassword") as String?) ?: System.getenv("REPOSILITE_PASSWORD")

allprojects {
    group = "de.t14d3.rapunzellib"
    version = buildVersion

    repositories {
        mavenCentral()
        maven("${reposiliteBaseUrl.trimEnd('/')}/releases")
        maven("${reposiliteBaseUrl.trimEnd('/')}/snapshots")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
        maven("https://jitpack.io")
    }
}

val checkReposiliteConfig = tasks.register<CheckReposiliteConfig>("checkReposiliteConfig") {
    group = "publishing"
    description = "Validates required configuration for publishing to Reposilite."

    reposiliteBaseUrl.set(providers.gradleProperty("reposiliteBaseUrl").orElse(providers.environmentVariable("REPOSILITE_BASE_URL")))
    reposiliteUsername.set(providers.gradleProperty("reposiliteUsername").orElse(providers.environmentVariable("REPOSILITE_USERNAME")))
    reposilitePassword.set(providers.gradleProperty("reposilitePassword").orElse(providers.environmentVariable("REPOSILITE_PASSWORD")))
}

subprojects {
    val rootLibs = rootProject.layout.buildDirectory.dir("libs")

    plugins.withId("base") {
        extensions.configure<BasePluginExtension> {
            archivesName.set("${rootProject.name.lowercase()}-${project.name}")
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        if (archiveExtension.get() == "jar") {
            destinationDirectory.set(rootLibs)
        }
    }

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
            options.release.set(21)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            testLogging {
                events = setOf(TestLogEvent.FAILED)
                exceptionFormat = TestExceptionFormat.FULL
            }
        }
    }

    plugins.withId("java-library") {
        apply(plugin = "maven-publish")
    }

    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications {
                if (!plugins.hasPlugin("java-gradle-plugin") && plugins.hasPlugin("java") && findByName("mavenJava") == null) {
                    create<MavenPublication>("mavenJava") {
                        from(components["java"])

                        // Publish shaded jars (when available) as classifier artifacts.
                        tasks.findByName("shadowJar")?.let { artifact(it) }

                        // Fabric's remapped shaded jar.
                        tasks.findByName("remapShadowJar")?.let { artifact(it) }
                    }
                }

            }

            repositories {
                maven {
                    name = "reposilite"

                    val repo = if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases"

                    url = uri("${reposiliteBaseUrl.trimEnd('/')}/$repo")

                    credentials {
                        username = reposiliteUsername
                        password = reposilitePassword
                    }
                }
            }
        }

        tasks.withType<PublishToMavenRepository>().configureEach {
            if (name.contains("ToReposiliteRepository")) {
                dependsOn(rootProject.tasks.named("checkReposiliteConfig"))
            }
        }
    }

    plugins.withId("com.gradleup.shadow") {
        tasks.named("assemble").configure {
            dependsOn(tasks.named("shadowJar"))
        }
    }
}
