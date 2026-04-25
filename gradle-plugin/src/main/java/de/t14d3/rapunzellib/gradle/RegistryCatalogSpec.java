package de.t14d3.rapunzellib.gradle;

import de.t14d3.rapunzellib.gradle.catalog.RegistryCatalogNormalizationProfile;
import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

public abstract class RegistryCatalogSpec implements Named {
    private final String specName;
    private final RegistryCatalogSourceSpec source;
    private final NamedDomainObjectContainer<NamedRegistryCatalogSourceSpec> paritySources;

    @Inject
    public RegistryCatalogSpec(String specName, ObjectFactory objects) {
        this.specName = specName;
        this.source = objects.newInstance(RegistryCatalogSourceSpec.class);
        this.paritySources = objects.domainObjectContainer(
            NamedRegistryCatalogSourceSpec.class,
            name -> objects.newInstance(NamedRegistryCatalogSourceSpec.class, name)
        );
    }

    @Override
    public String getName() {
        return specName;
    }

    public abstract Property<String> getPackageName();

    public abstract Property<String> getClassName();

    public abstract Property<String> getDomainName();

    public abstract Property<String> getRegistryValueType();

    public abstract Property<String> getRegistryKeyOwnerType();

    public abstract Property<String> getRegistryKeyFieldName();

    public abstract DirectoryProperty getOutputDir();

    public RegistryCatalogSourceSpec getSource() {
        return source;
    }

    public NamedDomainObjectContainer<NamedRegistryCatalogSourceSpec> getParitySources() {
        return paritySources;
    }

    public void source(Action<? super RegistryCatalogSourceSpec> action) {
        action.execute(source);
    }

    public void parity(Action<? super NamedDomainObjectContainer<NamedRegistryCatalogSourceSpec>> action) {
        action.execute(paritySources);
    }

    public void verifyAgainst(String name, Action<? super RegistryCatalogSourceSpec> action) {
        action.execute(paritySources.maybeCreate(name));
    }

    public void applyDefaultConventions(Project project) {
        getPackageName().convention("generated.rapunzellib.registry");
        getClassName().convention(RapunzelLibPluginDefaults.defaultRegistryCatalogClassName(getName()));
        getDomainName().convention(getName());
        getRegistryValueType().convention("");
        getRegistryKeyOwnerType().convention("de.t14d3.rapunzellib.registry.RRegistries");
        getRegistryKeyFieldName().convention("");
        getOutputDir().convention(project.getLayout().getProjectDirectory().dir("src/generated/java"));

        getSource().applyDefaultConventions();
        getParitySources().configureEach(NamedRegistryCatalogSourceSpec::applyDefaultConventions);
    }

    public static abstract class RegistryCatalogSourceSpec {
        private final ConfigurableFileCollection classpath;

        @Inject
        public RegistryCatalogSourceSpec(ObjectFactory objects) {
            this.classpath = objects.fileCollection();
        }

        public abstract Property<String> getType();

        public ConfigurableFileCollection getClasspath() {
            return classpath;
        }

        public abstract Property<String> getNormalizationProfile();

        public abstract Property<Boolean> getAllowSupersetOfCanonical();

        public abstract Property<String> getEnumClassName();

        public abstract Property<String> getStaticFieldOwnerClassName();

        public abstract Property<String> getStaticFieldValueTypeName();

        public abstract ListProperty<String> getIncludePredicateMethods();

        public abstract ListProperty<String> getExcludePredicateMethods();

        public abstract Property<String> getKeyAccessorMethodName();

        public abstract SetProperty<String> getExcludedEnumConstants();

        public void applyDefaultConventions() {
            getType().convention(RegistryCatalogSourceType.NATIVE_STATIC_FIELDS);
            getNormalizationProfile().convention(RegistryCatalogNormalizationProfile.NONE);
            getAllowSupersetOfCanonical().convention(false);
            getEnumClassName().convention("");
            getStaticFieldOwnerClassName().convention("");
            getStaticFieldValueTypeName().convention("");
            getKeyAccessorMethodName().convention("getKey");
            getIncludePredicateMethods().convention(List.of());
            getExcludePredicateMethods().convention(List.of());
            getExcludedEnumConstants().convention(Set.of());
        }

        public void bukkitItemTypes() {
            configureStaticFieldSource(
                RegistryCatalogNormalizationProfile.NONE,
                "org.bukkit.inventory.ItemType",
                "org.bukkit.inventory.ItemType",
                "getKey"
            );
        }

        public void bukkitBlockTypes() {
            configureStaticFieldSource(
                RegistryCatalogNormalizationProfile.VANILLA_PAPER_BLOCK_TYPES,
                "org.bukkit.block.BlockType",
                "org.bukkit.block.BlockType",
                "getKey"
            );
        }

        public void bukkitEntityTypes() {
            configureEnumSource(
                RegistryCatalogNormalizationProfile.NONE,
                "org.bukkit.entity.EntityType",
                "getKey",
                Set.of("UNKNOWN")
            );
        }

        public void mojangItemTypes() {
            configureStaticFieldSource(
                RegistryCatalogNormalizationProfile.VANILLA_MOJANG_ITEM_TYPES,
                "net.minecraft.world.item.Items",
                "net.minecraft.world.item.Item",
                "builtInRegistryHolder.key.identifier|builtInRegistryHolder.key.location"
            );
        }

        public void mojangBlockTypes() {
            configureStaticFieldSource(
                RegistryCatalogNormalizationProfile.NONE,
                "net.minecraft.world.level.block.Blocks",
                "net.minecraft.world.level.block.Block",
                "builtInRegistryHolder.key.identifier|builtInRegistryHolder.key.location"
            );
        }

        public void mojangEntityTypes() {
            configureStaticFieldSource(
                RegistryCatalogNormalizationProfile.NONE,
                "net.minecraft.world.entity.EntityType",
                "net.minecraft.world.entity.EntityType",
                "builtInRegistryHolder.key.identifier|builtInRegistryHolder.key.location"
            );
        }

        private void configureStaticFieldSource(
            String normalization,
            String ownerClassName,
            String valueTypeName,
            String keyAccessor
        ) {
            getType().set(RegistryCatalogSourceType.NATIVE_STATIC_FIELDS);
            getNormalizationProfile().set(normalization);
            getStaticFieldOwnerClassName().set(ownerClassName);
            getStaticFieldValueTypeName().set(valueTypeName);
            getIncludePredicateMethods().set(List.of());
            getExcludePredicateMethods().set(List.of());
            getKeyAccessorMethodName().set(keyAccessor);
            getExcludedEnumConstants().empty();
        }

        private void configureEnumSource(
            String normalization,
            String enumTypeName,
            String keyAccessor,
            Set<String> excludedConstants
        ) {
            getType().set(RegistryCatalogSourceType.NATIVE_ENUM);
            getNormalizationProfile().set(normalization);
            getEnumClassName().set(enumTypeName);
            getIncludePredicateMethods().set(List.of());
            getExcludePredicateMethods().set(List.of());
            getKeyAccessorMethodName().set(keyAccessor);
            getExcludedEnumConstants().set(excludedConstants);
        }
    }

    public static abstract class NamedRegistryCatalogSourceSpec extends RegistryCatalogSourceSpec implements Named {
        private final String specName;

        @Inject
        public NamedRegistryCatalogSourceSpec(String specName, ObjectFactory objects) {
            super(objects);
            this.specName = specName;
        }

        @Override
        public String getName() {
            return specName;
        }
    }
}
