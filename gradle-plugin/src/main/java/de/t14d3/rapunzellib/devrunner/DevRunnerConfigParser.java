package de.t14d3.rapunzellib.devrunner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DevRunnerConfigParser {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private DevRunnerConfigParser() {
    }

    public static DevRunnerConfig parse(String[] args) {
        if (isJsonConfig(args)) {
            return parseJsonConfig(args);
        }
        return parseLegacyCli(args);
    }

    public static DevRunnerConfig parseJsonFile(Path jsonFile) throws IOException {
        String json = Files.readString(jsonFile, StandardCharsets.UTF_8);
        return parseJson(json);
    }

    public static void writeJson(DevRunnerConfig config, Path target) throws IOException {
        JsonObject root = new JsonObject();

        // Global
        JsonObject global = new JsonObject();
        global.addProperty("javaBin", config.javaBin());
        if (!config.jvmArgs().isEmpty()) {
            JsonArray jvm = new JsonArray();
            for (String arg : config.jvmArgs()) jvm.add(arg);
            global.add("jvmArgs", jvm);
        }
        if (config.baseDir() != null) global.addProperty("baseDir", config.baseDir().toString());
        if (config.jfrEnabled()) global.addProperty("jfrEnabled", true);
        if (!"profile".equals(config.jfrSettings())) global.addProperty("jfrSettings", config.jfrSettings());
        if (config.allowDirectConnections()) global.addProperty("allowDirectConnections", true);
        root.add("global", global);

        // Servers
        if (!config.servers().isEmpty()) {
            JsonObject servers = new JsonObject();
            for (var entry : config.servers().entrySet()) {
                DevRunnerConfig.ServerSpec spec = entry.getValue();
                JsonObject s = new JsonObject();
                s.addProperty("platform", spec.platform());
                s.addProperty("version", spec.version());
                s.addProperty("port", spec.port());
                if (spec.pluginJar() != null) s.addProperty("pluginJar", spec.pluginJar().toString());
                if (!spec.extraPlugins().isEmpty()) {
                    JsonArray extra = new JsonArray();
                    for (Path p : spec.extraPlugins()) extra.add(p.toString());
                    s.add("extraPlugins", extra);
                }
                if (!spec.properties().isEmpty()) {
                    JsonObject props = new JsonObject();
                    for (var pe : spec.properties().entrySet()) props.addProperty(pe.getKey(), pe.getValue());
                    s.add("properties", props);
                }
                servers.add(entry.getKey(), s);
            }
            root.add("servers", servers);
        }

        // Services
        if (!config.services().isEmpty()) {
            JsonObject services = new JsonObject();
            for (var entry : config.services().entrySet()) {
                DevRunnerConfig.ServiceSpec spec = entry.getValue();
                JsonObject svc = new JsonObject();
                svc.addProperty("type", spec.type());
                if (spec.image() != null) svc.addProperty("image", spec.image());
                if (spec.containerName() != null) svc.addProperty("containerName", spec.containerName());
                if (!spec.ports().isEmpty()) {
                    JsonObject ports = new JsonObject();
                    for (var pe : spec.ports().entrySet()) ports.addProperty(pe.getKey(), pe.getValue());
                    svc.add("ports", ports);
                }
                if (!spec.env().isEmpty()) {
                    JsonObject env = new JsonObject();
                    for (var pe : spec.env().entrySet()) env.addProperty(pe.getKey(), pe.getValue());
                    svc.add("env", env);
                }
                services.add(entry.getKey(), svc);
            }
            root.add("services", services);
        }

        // LiveTests
        DevRunnerConfig.LiveTestConfig lt = config.liveTests();
        if (lt.enabled()) {
            JsonObject ltObj = new JsonObject();
            ltObj.addProperty("enabled", true);
            ltObj.addProperty("autoRun", lt.autoRun());
            if (lt.testSourceDir() != null) ltObj.addProperty("testSourceDir", lt.testSourceDir().toString());
            if (!lt.testPackages().isEmpty()) {
                JsonArray pkgs = new JsonArray();
                for (String p : lt.testPackages()) pkgs.add(p);
                ltObj.add("testPackages", pkgs);
            }
            ltObj.addProperty("timeoutMs", lt.timeoutMs());
            ltObj.addProperty("runTimeoutMs", lt.runTimeoutMs());
            root.add("liveTests", ltObj);
        }

        // File overrides
        if (!config.fileOverrides().isEmpty()) {
            JsonObject fos = new JsonObject();
            for (var serverEntry : config.fileOverrides().entrySet()) {
                JsonObject serverFos = new JsonObject();
                for (var fileEntry : serverEntry.getValue().entrySet()) {
                    serverFos.addProperty(fileEntry.getKey(), fileEntry.getValue());
                }
                fos.add(serverEntry.getKey(), serverFos);
            }
            root.add("fileOverrides", fos);
        }

        Files.writeString(target, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static boolean isJsonConfig(String[] args) {
        if (args.length == 0) return false;
        if (args[0].equals("--help") || args[0].equals("-h")) return false;
        return args[0].equals("--config");
    }

    private static DevRunnerConfig parseJsonConfig(String[] args) {
        if (args.length < 2) {
            throw new DevRunnerUsageException(2, "Usage: --config <path-to-json>");
        }
        Path configPath = Path.of(args[1]);
        if (!Files.isRegularFile(configPath)) {
            throw new DevRunnerUsageException(2, "Config file not found: " + configPath);
        }
        try {
            return parseJsonFile(configPath);
        } catch (IOException e) {
            throw new DevRunnerUsageException(2, "Failed to read config: " + e.getMessage());
        }
    }

    private static DevRunnerConfig parseJson(String json) {
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        // Global
        String javaBin = defaultJava();
        List<String> jvmArgs = new ArrayList<>();
        Path baseDir = null;
        boolean jfrEnabled = false;
        String jfrSettings = "profile";
        boolean allowDirectConnections = false;

        if (root.has("global")) {
            JsonObject g = root.getAsJsonObject("global");
            if (g.has("javaBin")) javaBin = g.get("javaBin").getAsString();
            if (g.has("jvmArgs")) {
                for (JsonElement e : g.getAsJsonArray("jvmArgs")) jvmArgs.add(e.getAsString());
            }
            if (g.has("baseDir")) baseDir = Path.of(g.get("baseDir").getAsString());
            if (g.has("jfrEnabled")) jfrEnabled = g.get("jfrEnabled").getAsBoolean();
            if (g.has("jfrSettings")) jfrSettings = g.get("jfrSettings").getAsString();
            if (g.has("allowDirectConnections")) allowDirectConnections = g.get("allowDirectConnections").getAsBoolean();
        }

        // Servers
        Map<String, DevRunnerConfig.ServerSpec> servers = new LinkedHashMap<>();
        if (root.has("servers")) {
            for (var entry : root.getAsJsonObject("servers").entrySet()) {
                JsonObject s = entry.getValue().getAsJsonObject();
                String platform = s.has("platform") ? s.get("platform").getAsString() : "paper";
                String version = s.has("version") ? s.get("version").getAsString() : "latest";
                int port = s.has("port") ? s.get("port").getAsInt() : 25565;
                Path pluginJar = s.has("pluginJar") ? Path.of(s.get("pluginJar").getAsString()) : null;
                List<Path> extraPlugins = new ArrayList<>();
                if (s.has("extraPlugins")) {
                    for (JsonElement e : s.getAsJsonArray("extraPlugins")) extraPlugins.add(Path.of(e.getAsString()));
                }
                Map<String, String> properties = new HashMap<>();
                if (s.has("properties")) {
                    for (var pe : s.getAsJsonObject("properties").entrySet()) {
                        properties.put(pe.getKey(), pe.getValue().getAsString());
                    }
                }
                servers.put(entry.getKey(), new DevRunnerConfig.ServerSpec(
                    platform, version, port, pluginJar, extraPlugins, properties
                ));
            }
        }

        // Services
        Map<String, DevRunnerConfig.ServiceSpec> services = new LinkedHashMap<>();
        if (root.has("services")) {
            for (var entry : root.getAsJsonObject("services").entrySet()) {
                JsonObject svc = entry.getValue().getAsJsonObject();
                String type = svc.has("type") ? svc.get("type").getAsString() : entry.getKey();
                String image = svc.has("image") ? svc.get("image").getAsString() : null;
                String containerName = svc.has("containerName") ? svc.get("containerName").getAsString() : null;
                Map<String, String> ports = new HashMap<>();
                if (svc.has("ports")) {
                    for (var pe : svc.getAsJsonObject("ports").entrySet()) {
                        ports.put(pe.getKey(), pe.getValue().getAsString());
                    }
                }
                Map<String, String> env = new HashMap<>();
                if (svc.has("env")) {
                    for (var pe : svc.getAsJsonObject("env").entrySet()) {
                        env.put(pe.getKey(), pe.getValue().getAsString());
                    }
                }
                services.put(entry.getKey(), new DevRunnerConfig.ServiceSpec(type, image, ports, env, containerName));
            }
        }

        // LiveTests
        DevRunnerConfig.LiveTestConfig liveTests = new DevRunnerConfig.LiveTestConfig(false, false, null, List.of(), 30_000L, 300_000L);
        if (root.has("liveTests")) {
            JsonObject lt = root.getAsJsonObject("liveTests");
            boolean enabled = lt.has("enabled") && lt.get("enabled").getAsBoolean();
            boolean autoRun = lt.has("autoRun") && lt.get("autoRun").getAsBoolean();
            Path testSourceDir = lt.has("testSourceDir") ? Path.of(lt.get("testSourceDir").getAsString()) : null;
            List<String> testPackages = new ArrayList<>();
            if (lt.has("testPackages")) {
                for (JsonElement e : lt.getAsJsonArray("testPackages")) testPackages.add(e.getAsString());
            }
            long timeoutMs = lt.has("timeoutMs") ? lt.get("timeoutMs").getAsLong() : 30_000L;
            long runTimeoutMs = lt.has("runTimeoutMs") ? lt.get("runTimeoutMs").getAsLong() : 300_000L;
            liveTests = new DevRunnerConfig.LiveTestConfig(enabled, autoRun, testSourceDir, testPackages, timeoutMs, runTimeoutMs);
        }

        // File overrides
        Map<String, Map<String, String>> fileOverrides = new LinkedHashMap<>();
        if (root.has("fileOverrides")) {
            for (var serverEntry : root.getAsJsonObject("fileOverrides").entrySet()) {
                Map<String, String> serverFos = new LinkedHashMap<>();
                for (var fileEntry : serverEntry.getValue().getAsJsonObject().entrySet()) {
                    serverFos.put(fileEntry.getKey(), fileEntry.getValue().getAsString());
                }
                fileOverrides.put(serverEntry.getKey(), serverFos);
            }
        }

        // Resolve directories
        Path resolvedBase = baseDir != null ? baseDir.toAbsolutePath().normalize() : Path.of("run", "devrunner").toAbsolutePath().normalize();
        Path resolvedCache = resolvedBase.resolve("cache");
        Path resolvedInstances = resolvedBase.resolve("instances");

        return new DevRunnerConfig(
            javaBin, jvmArgs, resolvedBase, resolvedCache, resolvedInstances,
            servers, services, liveTests, List.of(), fileOverrides, jfrEnabled, jfrSettings,
            allowDirectConnections
        );
    }

    private static DevRunnerConfig parseLegacyCli(String[] args) {
        Map<String, String> flags = new HashMap<>();
        List<String> jvmArgs = new ArrayList<>();
        List<Path> paperExtraPlugins = new ArrayList<>();
        List<Path> velocityExtraPlugins = new ArrayList<>();
        List<DevRunnerConfig.RegexReplace> regexReplaces = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--help") || arg.equals("-h")) {
                throw new DevRunnerUsageException(0, usage());
            }

            if (arg.startsWith("--jvm-arg=")) {
                jvmArgs.add(arg.substring("--jvm-arg=".length()));
                continue;
            }
            if (arg.equals("--jvm-arg")) {
                jvmArgs.add(readNextArg(args, ++i));
                continue;
            }

            if (arg.equals("--replace")) {
                if (i + 3 >= args.length) throw new DevRunnerUsageException(2, "Usage: --replace <path> <regex> <replacement>");
                String relativePath = args[++i];
                String regex = args[++i];
                String replacement = args[++i];
                regexReplaces.add(new DevRunnerConfig.RegexReplace(null, relativePath, regex, replacement));
                continue;
            }

            if (arg.startsWith("--paper-extra-plugin=")) {
                paperExtraPlugins.add(Path.of(arg.substring("--paper-extra-plugin=".length())));
                continue;
            }
            if (arg.equals("--paper-extra-plugin")) {
                paperExtraPlugins.add(Path.of(readNextArg(args, ++i)));
                continue;
            }

            if (arg.startsWith("--velocity-extra-plugin=")) {
                velocityExtraPlugins.add(Path.of(arg.substring("--velocity-extra-plugin=".length())));
                continue;
            }
            if (arg.equals("--velocity-extra-plugin")) {
                velocityExtraPlugins.add(Path.of(readNextArg(args, ++i)));
                continue;
            }

            if (!arg.startsWith("--")) throw new DevRunnerUsageException(2, "Unknown argument: " + arg);

            String key = arg.substring(2);
            if (key.equals("mysql") || key.equals("jfr") || key.equals("allow-direct-connections")) {
                flags.put(key, "true");
                continue;
            }

            String value;
            int equalsIndex = key.indexOf('=');
            if (equalsIndex >= 0) {
                value = key.substring(equalsIndex + 1);
                key = key.substring(0, equalsIndex);
            } else {
                value = readNextArg(args, ++i);
            }
            flags.put(key, value);
        }

        // Build servers from legacy flags
        Map<String, DevRunnerConfig.ServerSpec> servers = new LinkedHashMap<>();

        String velocityVersion = flags.get("velocity-version");
        if (velocityVersion != null && (velocityVersion.isBlank() || velocityVersion.equalsIgnoreCase("latest"))) {
            velocityVersion = "latest";
        }

        if (velocityVersion != null && !velocityVersion.isBlank()) {
            int velocityPort = parseInt(flags.getOrDefault("velocity-port", "25565"), "velocity-port");
            Path velocityPlugin = flags.containsKey("velocity-plugin") ? Path.of(flags.get("velocity-plugin")) : null;
            servers.put("proxy", new DevRunnerConfig.ServerSpec(
                "velocity", velocityVersion, velocityPort, velocityPlugin, velocityExtraPlugins, Map.of()
            ));
        }

        int paperCount = parseInt(flags.getOrDefault("paper-count", "0"), "paper-count");
        String paperVersion = flags.getOrDefault("paper-version", "latest");
        int paperBasePort = parseInt(flags.getOrDefault("paper-base-port", "25566"), "paper-base-port");
        Path paperPlugin = flags.containsKey("paper-plugin") ? Path.of(flags.get("paper-plugin")) : null;

        for (int i = 0; i < paperCount; i++) {
            String name = "paper-" + (i + 1);
            servers.put(name, new DevRunnerConfig.ServerSpec(
                "paper", paperVersion, paperBasePort + i, paperPlugin, paperExtraPlugins, Map.of()
            ));
        }

        // Build services from legacy flags
        Map<String, DevRunnerConfig.ServiceSpec> services = new LinkedHashMap<>();
        if (Boolean.parseBoolean(flags.getOrDefault("mysql", "false"))) {
            int mysqlPort = parseInt(flags.getOrDefault("mysql-port", "3307"), "mysql-port");
            String mysqlDatabase = flags.getOrDefault("mysql-database", "rapunzellib");
            String mysqlPassword = flags.getOrDefault("mysql-root-password", "root");
            String mysqlImage = flags.getOrDefault("mysql-image", "mysql:8.4");
            String mysqlContainer = flags.get("mysql-container-name");

            Map<String, String> ports = Map.of("3306", String.valueOf(mysqlPort));
            Map<String, String> env = Map.of(
                "MYSQL_ROOT_PASSWORD", mysqlPassword,
                "MYSQL_DATABASE", mysqlDatabase
            );
            services.put("mysql", new DevRunnerConfig.ServiceSpec("mysql", mysqlImage, ports, env, mysqlContainer));
        }

        // JFR
        boolean jfrEnabled = Boolean.parseBoolean(flags.getOrDefault("jfr", "false"));
        String jfrSettings = flags.getOrDefault("jfr-settings", "profile");

        // Direct bot connections (opt-out of the velocity requirement).
        boolean allowDirectConnections = Boolean.parseBoolean(flags.getOrDefault("allow-direct-connections", "false"));

        // Directories
        Path baseDir = flags.containsKey("base-dir") ? Path.of(flags.get("base-dir")) : null;
        Path resolvedBase = baseDir != null ? baseDir.toAbsolutePath().normalize() : Path.of("run", "devrunner").toAbsolutePath().normalize();
        Path resolvedCache = resolvedBase.resolve("cache");
        Path resolvedInstances = resolvedBase.resolve("instances");

        String javaBin = flags.getOrDefault("java", defaultJava());

        return new DevRunnerConfig(
            javaBin, jvmArgs, resolvedBase, resolvedCache, resolvedInstances,
            servers, services,
            new DevRunnerConfig.LiveTestConfig(false, false, null, List.of(), 30_000L, 300_000L),
            regexReplaces, Map.of(), jfrEnabled, jfrSettings, allowDirectConnections
        );
    }

    private static String readNextArg(String[] args, int index) {
        if (index >= args.length) throw new DevRunnerUsageException(2, "Missing value for argument");
        return args[index];
    }

    private static int parseInt(String value, String name) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid --" + name + ": " + value);
        }
    }

    private static String defaultJava() {
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isBlank()) return "java";
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String executable = isWindows ? "java.exe" : "java";
        Path candidate = Path.of(javaHome, "bin", executable);
        return java.nio.file.Files.isRegularFile(candidate) ? candidate.toString() : "java";
    }

    private static String usage() {
        return """
            RapunzelLib DevRunner

            Usage:
              --config <path.json>              Load topology from JSON config file

            Legacy mode (backward compatible):
              --paper-count <n>                  Number of Paper instances
              --paper-version <mcVersion>        Minecraft version (default: latest)
              --paper-base-port <port>           default 25566
              --paper-plugin <path>              Plugin JAR for Paper
              --paper-extra-plugin <path>        Additional plugin JAR (repeatable)
              --velocity-version <version>       Velocity version (omit to skip)
              --velocity-port <port>             default 25565
              --velocity-plugin <path>           Plugin JAR for Velocity
              --velocity-extra-plugin <path>     Additional plugin JAR (repeatable)
              --java <javaBin>                   Java binary (default: java)
              --jvm-arg <arg>                    Extra JVM arg (repeatable)
              --base-dir <dir>                   default run/devrunner
              --jfr                              Enable JFR profiling
              --jfr-settings <name>              default profile
              --allow-direct-connections         Opt out of the velocity-proxy requirement (bots
                                                 connect directly to backends; default: off)
              --replace <path> <regex> <repl>    Regex file replacement (repeatable)
              --mysql                            Start MySQL Docker container
              --mysql-port <port>                default 3307
              --mysql-database <name>            default rapunzellib
              --mysql-root-password <pw>         default root
              --mysql-image <image>              default mysql:8.4
              --mysql-container-name <name>      Container name
            """;
    }
}
