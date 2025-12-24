package de.t14d3.rapunzellib.gradle

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class RapunzelLibExtension {
    abstract val messagesFile: RegularFileProperty
    abstract val additionalMessagesFiles: org.gradle.api.provider.ListProperty<org.gradle.api.file.RegularFile>

    abstract val failOnUnusedKeys: Property<Boolean>
    abstract val alwaysUsedKeys: SetProperty<String>

    abstract val messageKeyCallOwners: SetProperty<String>
    abstract val messageKeyCallMethods: SetProperty<String>
    abstract val messageKeyPrefix: Property<String>

    abstract val templateOutputDir: DirectoryProperty
    abstract val templateBasePackage: Property<String>
    abstract val templateProjectName: Property<String>
}
