import de.t14d3.rapunzellib.gradle.tasks.GenerateDisplayMetadataTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.the
import org.spongepowered.gradle.vanilla.repository.MinecraftPlatform

plugins {
    `java-library`
    alias(libs.plugins.vanilla.gradle)
    alias(libs.plugins.rapunzellib)
}

val activeMinecraftTarget = providers.gradleProperty("rapunzellib.minecraftTarget")
    .orElse(providers.provider { findProperty("rapunzellib.minecraftCoreVersion") as? String })
    .orElse(providers.provider { findProperty("rapunzellib.version.minecraft") as? String })
val activePaperApiVersion = providers.gradleProperty("rapunzellib.version.${activeMinecraftTarget.get()}.paper-api")
    .orElse(providers.gradleProperty("rapunzellib.version.paper-api"))
    .orElse(providers.provider { findProperty("rapunzellib.version.paper-api") as? String })
    .orElse(activeMinecraftTarget.map { "$it-R0.1-SNAPSHOT" })

val mainCompileClasspath = the<SourceSetContainer>().named("main").map { it.compileClasspath }
val paperMappedServerJar = project(":platform-paper").layout.projectDirectory.file(".gradle/caches/paperweight/taskCache/mappedServerJar.jar")
val paperDevBundleRuntime =
    configurations.detachedConfiguration(
        dependencies.create("io.papermc.paper:dev-bundle:${activePaperApiVersion.get()}")
    )
val paperParityClasspath = files(paperMappedServerJar, paperDevBundleRuntime)

minecraft {
    version(activeMinecraftTarget.get())
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

tasks.matching { it.name.startsWith("rapunzellibVerify") && it.name.endsWith("Parity") }.configureEach {
    dependsOn(":platform-paper:paperweightUserdevSetup")
}

val displayMetadataGen = tasks.register<GenerateDisplayMetadataTask>("rapunzellibGenerateDisplayMetadata") {
    description = "Generates BlockDisplayMetadata.java by reflecting on Minecraft Display/BlockDisplay classes"
    group = "rapunzellib"

    minecraftClasspath.from(mainCompileClasspath)
    packageName.set("de.t14d3.rapunzellib.visuals.metadata")
    className.set("BlockDisplayMetadata")
    outputDir.set(layout.projectDirectory.dir("src/generated/java"))
}

val mainSourceSet = the<SourceSetContainer>().named("main")
mainSourceSet.configure {
    java.srcDir(layout.projectDirectory.dir("src/generated/java"))
}

tasks.named("compileJava") {
    dependsOn(displayMetadataGen)
}
tasks.named("sourcesJar") {
    dependsOn(displayMetadataGen)
    (this as org.gradle.api.tasks.bundling.Jar).duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}
tasks.named("javadoc") {
    dependsOn(displayMetadataGen)
}

tasks.named("rapunzellibVerifyVanillaBlockTypesParity") {
    mustRunAfter("rapunzellibVerifyVanillaItemTypesParity")
}

tasks.named("rapunzellibVerifyVanillaEntityTypesParity") {
    mustRunAfter("rapunzellibVerifyVanillaBlockTypesParity")
}

dependencies {
    api(libs.adventure.api)
    api(libs.adventure.serializer.plain)
    api(libs.adventure.serializer.json)
    api(libs.slf4j.api)
    api(libs.annotations)

    testImplementation(libs.junit.jupiter)
    testImplementation(project(":common"))
    testImplementation(project(":nbt"))
}
