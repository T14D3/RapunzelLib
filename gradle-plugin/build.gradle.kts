import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    `java-gradle-plugin`
    `maven-publish`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation(libs.gson)
    implementation(libs.asm)
    implementation(libs.asm.commons)
    implementation(libs.asm.tree)
    implementation(libs.asm.analysis)
    implementation(libs.snakeyaml)
    implementation(libs.paperweight.userdev)
    implementation(libs.vanillagradle)
    implementation(libs.fabric.loom.gradle)
    implementation(libs.neoforge.moddev.gradle)

    testImplementation(gradleTestKit())
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.paper.api)
}

gradlePlugin {
    plugins {
        create("rapunzellib") {
            id = "de.t14d3.rapunzellib"
            implementationClass = "de.t14d3.rapunzellib.gradle.RapunzelLibGradlePlugin"
            displayName = "RapunzelLib tooling"
            description = "Project templates, message validation, and multi-server runner for RapunzelLib-based projects."
            version = rootProject.version.toString()
        }
        create("rootSubprojectConventions") {
            id = "de.t14d3.rapunzellib.root-subproject-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.RootSubprojectConventionsPlugin"
        }
        create("rootPublishingConventions") {
            id = "de.t14d3.rapunzellib.root-publishing-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.RootPublishingConventionsPlugin"
        }
        create("featurePlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.feature-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.FeaturePlatformModuleConventionsPlugin"
        }
        create("fabricModuleConventions") {
            id = "de.t14d3.rapunzellib.fabric-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.FabricModuleConventionsPlugin"
        }
        create("neoforgeModuleConventions") {
            id = "de.t14d3.rapunzellib.neoforge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.NeoForgeModuleConventionsPlugin"
        }
        create("paperApiModuleConventions") {
            id = "de.t14d3.rapunzellib.paper-api-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.PaperApiModuleConventionsPlugin"
        }
        create("paperUserdevModuleConventions") {
            id = "de.t14d3.rapunzellib.paper-userdev-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.PaperUserdevModuleConventionsPlugin"
        }
        create("vanillaModuleConventions") {
            id = "de.t14d3.rapunzellib.vanilla-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.VanillaModuleConventionsPlugin"
        }
        create("spongeModuleConventions") {
            id = "de.t14d3.rapunzellib.sponge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.SpongeModuleConventionsPlugin"
        }
        create("velocityModuleConventions") {
            id = "de.t14d3.rapunzellib.velocity-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.VelocityModuleConventionsPlugin"
        }
        create("commonModuleConventions") {
            id = "de.t14d3.rapunzellib.common-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.CommonModuleConventionsPlugin"
        }
        create("databaseSpoolModuleConventions") {
            id = "de.t14d3.rapunzellib.database-spool-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.DatabaseSpoolModuleConventionsPlugin"
        }
        create("guiPlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.gui-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.GuiPlatformModuleConventionsPlugin"
        }
        create("networkModuleConventions") {
            id = "de.t14d3.rapunzellib.network-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.NetworkModuleConventionsPlugin"
        }
        create("backendPlatformModuleConventions") {
            id = "de.t14d3.rapunzellib.backend-platform-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.BackendPlatformModuleConventionsPlugin"
        }
        create("platformFabricModuleConventions") {
            id = "de.t14d3.rapunzellib.platform-fabric-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.PlatformFabricModuleConventionsPlugin"
        }
        create("platformNeoForgeModuleConventions") {
            id = "de.t14d3.rapunzellib.platform-neoforge-module-conventions"
            implementationClass = "de.t14d3.rapunzellib.gradle.PlatformNeoForgeModuleConventionsPlugin"
        }
    }
}
