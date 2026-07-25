package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.multiversion.MultiVersionExtension;
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
    private final MultiVersionExtension multiVersion;
    private final ContextWrapperGenerationExtension contextWrapper;

    @Inject
    public RapunzelLibExtension(ObjectFactory objects) {
        this.keyCatalog = objects.newInstance(KeyCatalogGenerationExtension.class);
        this.registryCatalogs = objects.domainObjectContainer(
            RegistryCatalogSpec.class,
            name -> objects.newInstance(RegistryCatalogSpec.class, name)
        );
        this.rNbtSchema = objects.newInstance(RNbtSchemaGenerationExtension.class);
        this.multiVersion = objects.newInstance(MultiVersionExtension.class);
        this.contextWrapper = objects.newInstance(ContextWrapperGenerationExtension.class);
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

    public KeyCatalogGenerationExtension getKeyCatalog() {
        return keyCatalog;
    }

    public NamedDomainObjectContainer<RegistryCatalogSpec> getRegistryCatalogs() {
        return registryCatalogs;
    }

public RNbtSchemaGenerationExtension getRNbtSchema() {
    return rNbtSchema;
}

public MultiVersionExtension getMultiVersion() {
        return multiVersion;
    }

    public ContextWrapperGenerationExtension getContextWrapper() {
        return contextWrapper;
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

public void multiVersion(Action<? super MultiVersionExtension> action) {
        action.execute(multiVersion);
    }

    public void contextWrapper(Action<? super ContextWrapperGenerationExtension> action) {
        action.execute(contextWrapper);
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

    multiVersion.getEnabled().convention(false);
    multiVersion.getTargetVersions().convention(List.of());

    keyCatalog.applyDefaultConventions(project);
    rNbtSchema.applyDefaultConventions(project);
    registryCatalogs.configureEach(spec -> spec.applyDefaultConventions(project));
    contextWrapper.applyDefaultConventions(project);
}
}
