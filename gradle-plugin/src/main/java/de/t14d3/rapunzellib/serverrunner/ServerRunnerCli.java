package de.t14d3.rapunzellib.serverrunner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Use {@link de.t14d3.rapunzellib.devrunner.DevRunnerConfigParser} instead.
 */
@Deprecated
final class ServerRunnerCli {
    private static final String DEFAULT_JAVA = defaultJava();

    private ServerRunnerCli() {
    }

    static ServerRunnerMain.Config parse(String[] args) {
        Map<String, String> flags = new HashMap<>();
        List<String> jvmArgs = new ArrayList<>();
        List<Path> paperExtraPlugins = new ArrayList<>();
        List<Path> velocityExtraPlugins = new ArrayList<>();
        List<ServerRunnerPatches.RegexReplace> regexReplaces = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("--help") || arg.equals("-h")) usageAndExit(0);

            if (arg.startsWith("--jvm-arg=")) {
                jvmArgs.add(arg.substring("--jvm-arg=".length()));
                continue;
            }
            if (arg.equals("--jvm-arg")) {
                jvmArgs.add(readNextArg(args, ++i));
                continue;
            }

            if (arg.equals("--replace")) {
                if (i + 3 >= args.length) usageAndExit(2);
                String relativePath = args[++i];
                String regex = args[++i];
                String replacement = args[++i];
                regexReplaces.add(new ServerRunnerPatches.RegexReplace(relativePath, regex, replacement));
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

            if (!arg.startsWith("--")) usageAndExit(2);

            String key = arg.substring(2);
            if (key.equals("mysql") || key.equals("jfr")) {
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

        String velocityVersion = flags.get("velocity-version");
        if (velocityVersion != null && velocityVersion.isBlank()) {
            velocityVersion = null;
        }
        if (velocityVersion != null && velocityVersion.equalsIgnoreCase("latest")) {
            velocityVersion = "latest";
        }

        int paperCount = parseInt(flags.getOrDefault("paper-count", "0"), "paper-count");

        return new ServerRunnerMain.Config(
            flags.getOrDefault("java", DEFAULT_JAVA),
            jvmArgs,
            flags.getOrDefault("paper-version", "latest"),
            paperCount,
            parseInt(flags.getOrDefault("paper-base-port", "25566"), "paper-base-port"),
            flags.containsKey("paper-plugin") ? Path.of(flags.get("paper-plugin")) : null,
            paperExtraPlugins,
            velocityVersion,
            parseInt(flags.getOrDefault("velocity-port", "25565"), "velocity-port"),
            flags.containsKey("velocity-plugin") ? Path.of(flags.get("velocity-plugin")) : null,
            velocityExtraPlugins,
            Boolean.parseBoolean(flags.getOrDefault("jfr", "false")),
            flags.getOrDefault("jfr-settings", "profile"),
            flags.containsKey("base-dir") ? Path.of(flags.get("base-dir")) : null,
            flags.containsKey("cache-dir") ? Path.of(flags.get("cache-dir")) : null,
            flags.containsKey("instances-dir") ? Path.of(flags.get("instances-dir")) : null,
            Boolean.parseBoolean(flags.getOrDefault("mysql", "false")),
            parseInt(flags.getOrDefault("mysql-port", "3307"), "mysql-port"),
            flags.getOrDefault("mysql-database", "rapunzellib"),
            flags.getOrDefault("mysql-root-password", "root"),
            flags.getOrDefault("mysql-image", "mysql:latest"),
            flags.get("mysql-container-name"),
            regexReplaces
        );
    }

    private static String readNextArg(String[] args, int index) {
        if (index >= args.length) usageAndExit(2);
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

    private static void usageAndExit(int code) {
        throw new ServerRunnerUsageException(code, """
            RapunzelLib server-runner (Fill v3)

            Downloads (via fill.papermc.io v3) and starts temporary Paper + Velocity instances in parallel.

            Required:
              --paper-count <n>              e.g. 2 (use 0 to skip Paper)
            Optional:
              --paper-version <mcVersion>    e.g. 1.21.10 or 'latest' (default: latest)
              --velocity-version <version>   e.g. 3.4.0-SNAPSHOT or 'latest' (omit to skip Velocity, default: latest if provided)
              --paper-base-port <port>       default 25566
              --velocity-port <port>         default 25565
              --paper-plugin <pathToJar>     copied into each Paper plugins/
              --paper-extra-plugin <pathToJar> repeatable (additional plugins copied into each Paper plugins/)
              --velocity-plugin <pathToJar>  copied into Velocity plugins/
              --velocity-extra-plugin <pathToJar> repeatable (additional plugins copied into Velocity plugins/)
              --java <javaBin>               default 'java'
              --jvm-arg <arg>                repeatable (e.g. --jvm-arg -Xmx2G)
              --jfr                         enable JFR recordings for long-running servers (written to instances/<name>/jfr/)
              --jfr-settings <name>          default 'profile' (e.g. 'default', 'profile')
              --base-dir <dir>               default run/server-runner
              --cache-dir <dir>              default <base-dir>/cache
              --instances-dir <dir>          default <base-dir>/instances

            Regex file replacements (repeatable, best-effort):
              --replace <relativePath> <regex> <replacement>
                  - Path is relative to the server root (instance directory)
                  - Java regex; replacement uses Java regex replacement syntax
                  - Variables are substituted as {{var}}, e.g. {{velocity_secret}}

            MySQL (Docker, optional):
              --mysql                        start a local MySQL container (requires docker)
              --mysql-port <port>            host port to bind (default 3307)
              --mysql-database <name>        default rapunzellib
              --mysql-root-password <pw>     default root
              --mysql-image <image:tag>      default mysql:8.4
              --mysql-container-name <name>  default rapunzellib-mysql-<timestamp>
            """);
    }
}
