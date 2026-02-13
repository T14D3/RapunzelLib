import de.t14d3.rapunzellib.gradle.tasks.VerifyRegistryCatalogParityTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.the
import org.spongepowered.gradle.vanilla.repository.MinecraftPlatform

plugins {
    `java-library`
    alias(libs.plugins.vanilla.gradle)
    alias(libs.plugins.rapunzellib)
}

val mainCompileClasspath = the<SourceSetContainer>().named("main").map { it.compileClasspath }
val paperMappedServerJar = project(":platform-paper").layout.projectDirectory.file(".gradle/caches/paperweight/taskCache/mappedServerJar.jar")
val paperDevBundleRuntime =
    configurations.detachedConfiguration(
        dependencies.create("io.papermc.paper:dev-bundle:${libs.versions.paper.api.get()}")
    )
val paperParityClasspath = files(paperMappedServerJar, paperDevBundleRuntime)

minecraft {
    version(libs.versions.minecraft.get())
    platform(MinecraftPlatform.SERVER)
}

rapunzellib {
    registryCatalogs {
        create("vanilla-item-types") {
            packageName.set("de.t14d3.rapunzellib.registry.catalog")
            domainName.set("vanilla item types")
            registryValueType.set("de.t14d3.rapunzellib.registry.RItemType")
            registryKeyFieldName.set("ITEM_TYPES")
            source {
                mojangItemTypes()
                classpath.from(mainCompileClasspath)
            }
            verifyAgainst("paper") {
                mojangItemTypes()
                classpath.from(paperParityClasspath)
                allowSupersetOfCanonical.set(true)
            }
        }

        create("vanilla-block-types") {
            packageName.set("de.t14d3.rapunzellib.registry.catalog")
            domainName.set("vanilla block types")
            registryValueType.set("de.t14d3.rapunzellib.registry.RBlockType")
            registryKeyFieldName.set("BLOCK_TYPES")
            source {
                mojangBlockTypes()
                classpath.from(mainCompileClasspath)
            }
            verifyAgainst("paper") {
                mojangBlockTypes()
                classpath.from(paperParityClasspath)
                allowSupersetOfCanonical.set(true)
            }
        }

        create("vanilla-entity-types") {
            packageName.set("de.t14d3.rapunzellib.registry.catalog")
            domainName.set("vanilla entity types")
            registryValueType.set("de.t14d3.rapunzellib.registry.REntityType")
            registryKeyFieldName.set("ENTITY_TYPES")
            source {
                mojangEntityTypes()
                classpath.from(mainCompileClasspath)
            }
            verifyAgainst("paper") {
                bukkitEntityTypes()
                classpath.from(paperParityClasspath)
            }
        }
    }
}

tasks.withType(VerifyRegistryCatalogParityTask::class.java).configureEach {
    dependsOn(":platform-paper:paperweightUserdevSetup")
}

tasks.named("rapunzellibVerifyVanillaBlockTypesParity") {
    mustRunAfter("rapunzellibVerifyVanillaItemTypesParity")
}

tasks.named("rapunzellibVerifyVanillaEntityTypesParity") {
    mustRunAfter("rapunzellibVerifyVanillaBlockTypesParity")
}

dependencies {
    api(libs.adventure.api)
    api(libs.slf4j.api)
    api(libs.annotations)

    testImplementation(libs.junit.jupiter)
    testImplementation(project(":common"))
    testImplementation(project(":nbt"))
}
