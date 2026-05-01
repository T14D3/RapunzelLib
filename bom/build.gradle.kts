plugins {
    `java-platform`
    `maven-publish`
}

val excludedProjects = setOf(
    project.path,
    ":gradle-plugin",
)

fun isPublicBomModule(path: String): Boolean {
    return path !in excludedProjects
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
