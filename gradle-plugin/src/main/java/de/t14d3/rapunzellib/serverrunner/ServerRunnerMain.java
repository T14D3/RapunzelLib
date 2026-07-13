package de.t14d3.rapunzellib.serverrunner;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/**
 * @deprecated Use {@link de.t14d3.rapunzellib.devrunner.DevRunnerMain} instead.
 */
@Deprecated
public final class ServerRunnerMain {
    private ServerRunnerMain() {
    }

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } catch (Throwable t) {
            System.err.println("[error] Unhandled exception: " + t.getMessage());
            t.printStackTrace(System.err);
            code = 1;
        }
        System.exit(code);
    }

    public static int run(String[] args) throws Exception {
        Config cfg;
        try {
            cfg = Config.parse(args);
        } catch (ServerRunnerUsageException e) {
            System.out.println(e.getMessage());
            return e.code();
        }

        return new ServerRunnerOrchestrator(cfg).run();
    }

    static List<String> jvmArgsForMainServer(
        Config cfg,
        String instanceName,
        Path instanceDir,
        String runId
    ) throws java.io.IOException {
        return ServerBootstrapSupport.jvmArgsForMainServer(cfg, instanceName, instanceDir, runId);
    }

    static record Config(
        String javaBin,
        List<String> jvmArgs,
        String paperVersion,
        int paperCount,
        int paperBasePort,
        Path paperPlugin,
        List<Path> paperExtraPlugins,
        String velocityVersion,
        int velocityPort,
        Path velocityPlugin,
        List<Path> velocityExtraPlugins,
        boolean jfrEnabled,
        String jfrSettings,
        Path baseDir,
        Path cacheDir,
        Path instancesDir,
        boolean mysqlEnabled,
        int mysqlPort,
        String mysqlDatabase,
        String mysqlRootPassword,
        String mysqlImage,
        String mysqlContainerName,
        List<ServerRunnerPatches.RegexReplace> regexReplaces
    ) {
        Config {
            paperExtraPlugins = paperExtraPlugins != null ? List.copyOf(paperExtraPlugins) : List.of();
            velocityExtraPlugins = velocityExtraPlugins != null ? List.copyOf(velocityExtraPlugins) : List.of();
            jfrSettings = jfrSettings != null ? jfrSettings : "profile";
            regexReplaces = regexReplaces != null ? regexReplaces : List.of();
        }

        static Config parse(String[] args) {
            return ServerRunnerCli.parse(args);
        }

        String mysqlJdbc() {
            String user = "root";
            String password = urlEncode(mysqlRootPassword);
            String db = urlEncode(mysqlDatabase);
            return "jdbc:mysql://127.0.0.1:" + mysqlPort + "/" + db
                + "?user=" + user
                + "&password=" + password
                + "&useSSL=false"
                + "&allowPublicKeyRetrieval=true";
        }

        private static String urlEncode(String s) {
            try {
                return URLEncoder.encode(s, StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid value for URL encoding");
            }
        }
    }
}
