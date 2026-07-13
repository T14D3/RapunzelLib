package de.t14d3.rapunzellib.devrunner;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public record DevRunnerConfig(
    String javaBin,
    List<String> jvmArgs,
    Path baseDir,
    Path cacheDir,
    Path instancesDir,
    Map<String, ServerSpec> servers,
    Map<String, ServiceSpec> services,
    LiveTestConfig liveTests,
    List<RegexReplace> regexReplaces,
    Map<String, Map<String, String>> fileOverrides,
    boolean jfrEnabled,
    String jfrSettings
) {
    public DevRunnerConfig {
        servers = servers != null ? Map.copyOf(servers) : Map.of();
        services = services != null ? Map.copyOf(services) : Map.of();
        liveTests = liveTests != null ? liveTests : new LiveTestConfig(false, false, null, List.of(), 30_000L, 300_000L);
        regexReplaces = regexReplaces != null ? List.copyOf(regexReplaces) : List.of();
        fileOverrides = fileOverrides != null ? Map.copyOf(fileOverrides) : Map.of();
        jvmArgs = jvmArgs != null ? List.copyOf(jvmArgs) : List.of();
        jfrSettings = jfrSettings != null ? jfrSettings : "profile";
    }

    public record ServerSpec(
        String platform,
        String version,
        int port,
        Path pluginJar,
        List<Path> extraPlugins,
        Map<String, String> properties
    ) {
        public ServerSpec {
            extraPlugins = extraPlugins != null ? List.copyOf(extraPlugins) : List.of();
            properties = properties != null ? Map.copyOf(properties) : Map.of();
        }
    }

    public record ServiceSpec(
        String type,
        String image,
        Map<String, String> ports,
        Map<String, String> env,
        String containerName
    ) {
        public ServiceSpec {
            ports = ports != null ? Map.copyOf(ports) : Map.of();
            env = env != null ? Map.copyOf(env) : Map.of();
        }
    }

    public record RegexReplace(
        String serverPattern,
        String relativePath,
        String regex,
        String replacement
    ) {}

    public record LiveTestConfig(
        boolean enabled,
        boolean autoRun,
        Path testSourceDir,
        List<String> testPackages,
        long timeoutMs,
        long runTimeoutMs
    ) {
        public LiveTestConfig {
            testPackages = testPackages != null ? List.copyOf(testPackages) : List.of();
            if (timeoutMs <= 0) timeoutMs = 30_000L;
            if (runTimeoutMs <= 0) runTimeoutMs = 300_000L;
        }
    }

    public String mysqlJdbc(String serverName) {
        ServiceSpec mysql = services.get("mysql");
        if (mysql == null) return null;

        String host = "127.0.0.1";
        String port = "3306";
        String database = "rapunzellib";
        String password = "root";

        for (Map.Entry<String, String> entry : mysql.env().entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            if ("MYSQL_DATABASE".equals(key)) database = val;
            if ("MYSQL_ROOT_PASSWORD".equals(key)) password = val;
        }

        for (Map.Entry<String, String> entry : mysql.ports().entrySet()) {
            if ("3306".equals(entry.getKey())) {
                port = entry.getValue();
                break;
            }
        }

        return "jdbc:mysql://" + host + ":" + port + "/" + database
            + "?user=root"
            + "&password=" + urlEncode(password)
            + "&useSSL=false"
            + "&allowPublicKeyRetrieval=true";
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value for URL encoding");
        }
    }
}
