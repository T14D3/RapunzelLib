package de.t14d3.rapunzellib.gradle;

import org.gradle.api.Action;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public abstract class DevRunnerExtension {

    private final NamedDomainObjectContainer<ServerSpecConfig> servers;
    private final NamedDomainObjectContainer<ServiceSpecConfig> services;
    private final PluginJarContainer pluginJars;
    private final LiveTestsConfig liveTests;

    @Inject
    public DevRunnerExtension(ObjectFactory objects) {
        this.servers = objects.domainObjectContainer(ServerSpecConfig.class, name -> objects.newInstance(ServerSpecConfig.class, name));
        this.services = objects.domainObjectContainer(ServiceSpecConfig.class, name -> objects.newInstance(ServiceSpecConfig.class, name));
        this.pluginJars = objects.newInstance(PluginJarContainer.class);
        this.liveTests = objects.newInstance(LiveTestsConfig.class);
    }

    // Global settings
    public abstract Property<String> getJavaBin();

    public abstract ListProperty<String> getJvmArgs();

    public abstract DirectoryProperty getBaseDir();

    public abstract Property<Boolean> getJfrEnabled();

    public abstract Property<String> getJfrSettings();

    /**
     * Whether bots may connect DIRECTLY to backend servers when no velocity
     * proxy is configured.
     *
     * <p>Default {@code false}: the devrunner REQUIRES a velocity server and
     * fails at config time when none is configured. Set to {@code true} to
     * explicitly opt out (single-server topologies, e.g. Zones' paper-only
     * devrun).</p>
     */
    public abstract Property<Boolean> getAllowDirectConnections();

    // Containers
    public NamedDomainObjectContainer<ServerSpecConfig> getServers() {
        return servers;
    }

    public NamedDomainObjectContainer<ServiceSpecConfig> getServices() {
        return services;
    }

    public PluginJarContainer getPluginJars() {
        return pluginJars;
    }

    public LiveTestsConfig getLiveTests() {
        return liveTests;
    }

    public abstract MapProperty<String, Map<String, String>> getFileOverrides();

    // Convenience methods
    public void server(String name, Action<? super ServerSpecConfig> action) {
        servers.named(name).configure(action);
    }

    public void service(String name, Action<? super ServiceSpecConfig> action) {
        services.named(name).configure(action);
    }

    public void pluginJars(Action<? super PluginJarContainer> action) {
        action.execute(pluginJars);
    }

    public void liveTests(Action<? super LiveTestsConfig> action) {
        action.execute(liveTests);
    }

    public void applyConventions(Project project) {
        getJavaBin().convention("");
        getJvmArgs().convention(List.of());
        getBaseDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir("run/devrunner"));
        getJfrEnabled().convention(false);
        getJfrSettings().convention("profile");
        getAllowDirectConnections().convention(false);

        liveTests.applyConventions();
    }

    // --- Nested types ---

    public static abstract class ServerSpecConfig implements Named {
        private final String specName;

        @Inject
        public ServerSpecConfig(String specName) {
            this.specName = specName;
        }

        @Override
        public String getName() {
            return specName;
        }

        public abstract Property<String> getPlatform();

        public abstract Property<String> getVersion();

        public abstract Property<Integer> getPort();

        public abstract RegularFileProperty getPluginJar();

        public abstract ListProperty<String> getExtraPlugins();

        public abstract MapProperty<String, String> getProperties();
    }

    public static abstract class ServiceSpecConfig implements Named {
        private final String specName;

        @Inject
        public ServiceSpecConfig(String specName) {
            this.specName = specName;
        }

        @Override
        public String getName() {
            return specName;
        }

        public abstract Property<String> getType();

        public abstract Property<String> getImage();

        public abstract Property<String> getContainerName();

        public abstract MapProperty<String, String> getPorts();

        public abstract MapProperty<String, String> getEnv();
    }

    public static abstract class PluginJarContainer {
        public abstract RegularFileProperty getPaper();

        public abstract RegularFileProperty getVelocity();

        public abstract RegularFileProperty getFabric();

        public abstract RegularFileProperty getNeoForge();

        public abstract RegularFileProperty getSponge();
    }

    public static abstract class LiveTestsConfig {
        public abstract Property<Boolean> getEnabled();

        public abstract Property<Boolean> getAutoRun();

        public abstract DirectoryProperty getTestSourceDir();

        public abstract ListProperty<String> getTestPackages();

        public abstract Property<Long> getTimeoutMs();

        public abstract Property<Long> getRunTimeoutMs();

        void applyConventions() {
            getEnabled().convention(false);
            getAutoRun().convention(true);
            getTestPackages().convention(List.of());
            getTimeoutMs().convention(30_000L);
            getRunTimeoutMs().convention(300_000L);
        }
    }
}
