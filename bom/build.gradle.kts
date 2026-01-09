plugins {
    `java-platform`
    `maven-publish`
}

val excludedProjects = setOf(
    project.path,
    ":gradle-plugin",
)

dependencies {
    constraints {
        rootProject.subprojects
            .asSequence()
            .map { it.path }
            .filterNot { it in excludedProjects }
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
