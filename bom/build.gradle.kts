plugins {
    `java-platform`
    `maven-publish`
}

val excludedProjects = setOf(
    project.path,
    ":gradle-plugin",
)

fun isPublicBomModule(path: String): Boolean {
    if (path in excludedProjects) {
        return false
    }
    val projectName = path.removePrefix(":")
    if (projectName == "common") {
        return false
    }
    if (projectName.endsWith("-shared") || projectName == "platform-shared") {
        return false
    }
    return true
}

dependencies {
    constraints {
        rootProject.subprojects
            .asSequence()
            .map { it.path }
            .filter(::isPublicBomModule)
            .sorted()
            .forEach { api(project(it)) }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenBom") {
            from(components["javaPlatform"])
            artifactId = "bom"
        }
    }
}
