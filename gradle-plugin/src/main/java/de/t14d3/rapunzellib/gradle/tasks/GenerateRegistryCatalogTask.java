package de.t14d3.rapunzellib.gradle.tasks;

import de.t14d3.rapunzellib.gradle.RegistryCatalogSourceType;
import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogGenerator;
import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogNormalizationProfile;
import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogSourceExtractor;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@CacheableTask
public abstract class GenerateRegistryCatalogTask extends DefaultTask {
    private final ConfigurableFileCollection nativeSourceClasspath = getProject().getObjects().fileCollection();

    public GenerateRegistryCatalogTask() {
        getSourceType().convention(RegistryCatalogSourceType.NATIVE_STATIC_FIELDS);
        getNormalizationProfile().convention(RegistryCatalogNormalizationProfile.NONE);
        getNativeEnumClassName().convention("");
        getNativeStaticFieldOwnerClassName().convention("");
        getNativeStaticFieldValueTypeName().convention("");
        getNativeIncludePredicateMethods().convention(List.of());
        getNativeExcludePredicateMethods().convention(List.of());
        getNativeKeyAccessorMethodName().convention("getKey");
        getNativeExcludedEnumConstants().convention(Set.of());
    }

    @Input
    public abstract Property<String> getSourceType();

    @Classpath
    public ConfigurableFileCollection getNativeSourceClasspath() {
        return nativeSourceClasspath;
    }

    @Input
    public abstract Property<String> getNormalizationProfile();

    @Input
    public abstract Property<String> getNativeEnumClassName();

    @Input
    public abstract Property<String> getNativeStaticFieldOwnerClassName();

    @Input
    public abstract Property<String> getNativeStaticFieldValueTypeName();

    @Input
    public abstract ListProperty<String> getNativeIncludePredicateMethods();

    @Input
    public abstract ListProperty<String> getNativeExcludePredicateMethods();

    @Input
    public abstract Property<String> getNativeKeyAccessorMethodName();

    @Input
    public abstract SetProperty<String> getNativeExcludedEnumConstants();

    @Input
    public abstract Property<String> getPackageName();

    @Input
    public abstract Property<String> getClassName();

    @Input
    public abstract Property<String> getDomainName();

    @Input
    public abstract Property<String> getRegistryValueType();

    @Input
    public abstract Property<String> getRegistryKeyOwnerType();

    @Input
    public abstract Property<String> getRegistryKeyFieldName();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void generate() {
        var extracted = RegistryCatalogSourceExtractor.extract(
            getSourceType().get(),
            nativeSourceClasspath.getFiles().stream().sorted(Comparator.comparing(File::getAbsolutePath)).toList(),
            getNormalizationProfile().get(),
            getNativeEnumClassName().get(),
            getNativeStaticFieldOwnerClassName().get(),
            getNativeStaticFieldValueTypeName().get(),
            getNativeIncludePredicateMethods().get(),
            getNativeExcludePredicateMethods().get(),
            getNativeKeyAccessorMethodName().get(),
            getNativeExcludedEnumConstants().get()
        );
        if (extracted.keys().isEmpty()) {
            throw new GradleException("Registry catalog generation requires at least one namespaced key.");
        }

        String source = RegistryCatalogGenerator.renderJavaSource(
            getPackageName().get(),
            getClassName().get(),
            getDomainName().get(),
            extracted.description(),
            extracted.keys(),
            getRegistryValueType().get(),
            getRegistryKeyOwnerType().get(),
            getRegistryKeyFieldName().get()
        );

        File outputRoot = getOutputDir().get().getAsFile();

        File targetFile = new File(new File(outputRoot, getPackageName().get().replace('.', '/')), getClassName().get() + ".java");
        File targetParent = targetFile.getParentFile();
        if (targetFile.exists()) {
            getProject().delete(targetFile);
        }
        if (!targetParent.exists() && !targetParent.mkdirs()) {
            throw new GradleException("Failed to create registry catalog output directory " + targetParent + ".");
        }
        try {
            java.nio.file.Files.writeString(targetFile.toPath(), source, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new GradleException("Failed to write generated registry catalog source.", ex);
        }

        getLogger().lifecycle(
            "Generated {} registry refs for domain '{}' at {}",
            extracted.keys().size(),
            getDomainName().get(),
            getProject().relativePath(targetFile)
        );
    }
}
