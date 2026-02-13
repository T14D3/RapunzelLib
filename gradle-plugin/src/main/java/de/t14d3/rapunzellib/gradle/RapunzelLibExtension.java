package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

public abstract class RapunzelLibExtension {
    private final KeyCatalogGenerationExtension keyCatalog;
    private final NamedDomainObjectContainer<RegistryCatalogSpec> registryCatalogs;
    private final RNbtSchemaGenerationExtension rNbtSchema;

    @Inject
    public RapunzelLibExtension(ObjectFactory objects) {
        this.keyCatalog = objects.newInstance(KeyCatalogGenerationExtension.class);
        this.registryCatalogs = objects.domainObjectContainer(
            RegistryCatalogSpec.class,
            name -> objects.newInstance(RegistryCatalogSpec.class, name)
        );
        this.rNbtSchema = objects.newInstance(RNbtSchemaGenerationExtension.class);
    }

    public abstract RegularFileProperty getMessagesFile();

    public abstract ListProperty<RegularFile> getAdditionalMessagesFiles();

    public abstract Property<Boolean> getFailOnUnusedKeys();

    public abstract SetProperty<String> getAlwaysUsedKeys();

    public abstract SetProperty<String> getMessageKeyCallOwners();

    public abstract SetProperty<String> getMessageKeyCallMethods();

    public abstract Property<String> getMessageKeyPrefix();

    public abstract DirectoryProperty getTemplateOutputDir();

    public abstract Property<String> getTemplateBasePackage();

    public abstract Property<String> getTemplateProjectName();

    public abstract DirectoryProperty getScaffoldOutputDir();

    public abstract Property<String> getScaffoldBasePackage();

    public abstract Property<String> getScaffoldPlatformKey();

    public abstract Property<String> getScaffoldSharedCoreFamily();

    public abstract SetProperty<String> getScaffoldSharedCoreFeatures();

    public abstract SetProperty<String> getScaffoldFeatures();

    public KeyCatalogGenerationExtension getKeyCatalog() {
        return keyCatalog;
    }

    public NamedDomainObjectContainer<RegistryCatalogSpec> getRegistryCatalogs() {
        return registryCatalogs;
    }

    public RNbtSchemaGenerationExtension getRNbtSchema() {
        return rNbtSchema;
    }

    public void keyCatalog(Action<? super KeyCatalogGenerationExtension> action) {
        action.execute(keyCatalog);
    }

    public void registryCatalogs(Action<? super NamedDomainObjectContainer<RegistryCatalogSpec>> action) {
        action.execute(registryCatalogs);
    }

    public void rNbtSchema(Action<? super RNbtSchemaGenerationExtension> action) {
        action.execute(rNbtSchema);
    }

    public void applyDefaultConventions(Project project) {
        getMessagesFile().convention(project.getLayout().getProjectDirectory().file("src/main/resources/messages.yml"));
        getAdditionalMessagesFiles().convention(List.of());
        getFailOnUnusedKeys().convention(true);
        getAlwaysUsedKeys().convention(Set.of("prefix"));
        getMessageKeyCallOwners().convention(Set.of());
        getMessageKeyCallMethods().convention(Set.of("getMessage", "getRaw"));
        getMessageKeyPrefix().convention("");

        getTemplateOutputDir().convention(project.getLayout().getProjectDirectory().dir("template"));
        getTemplateBasePackage().convention("de.t14d3");
        getTemplateProjectName().convention(project.getName());

        getScaffoldOutputDir().convention(project.getLayout().getProjectDirectory().dir("platform-adapter-scaffold"));
        getScaffoldBasePackage().convention("de.t14d3.rapunzellib");
        getScaffoldPlatformKey().convention("custom");
        getScaffoldSharedCoreFamily().convention(ModuleMatrix.SHARED_CORE_FAMILY_AUTO);
        getScaffoldSharedCoreFeatures().convention(Set.of());
        getScaffoldFeatures().convention(Set.of("commands", "events", "gui", "inventory", "nbt"));

        keyCatalog.applyDefaultConventions(project);
        rNbtSchema.applyDefaultConventions(project);
        registryCatalogs.configureEach(spec -> spec.applyDefaultConventions(project));
    }
}
