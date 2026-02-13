package de.t14d3.rapunzellib.serverrunner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ServerRunnerMainTest {
    @TempDir
    Path tempDir;

    @Test
    void helpReturnsZeroExitCode() throws Exception {
        assertEquals(0, ServerRunnerMain.run(new String[]{"--help"}));
        assertEquals(0, ServerRunnerMain.run(new String[]{"-h"}));
    }

    @Test
    void unknownArgsReturnUsageErrorCode() throws Exception {
        assertEquals(2, ServerRunnerMain.run(new String[]{"foo"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--unknown"}));
    }

    @Test
    void missingValueArgsReturnUsageErrorCode() throws Exception {
        assertEquals(2, ServerRunnerMain.run(new String[]{"--paper-count"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--jvm-arg"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--paper-extra-plugin"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--velocity-extra-plugin"}));
        assertEquals(2, ServerRunnerMain.run(new String[]{"--replace", "a.txt", "x"}));
    }

    @Test
    void jfrJvmArgsAreAddedWhenEnabled() throws Exception {
        ServerRunnerMain.Config cfg = ServerRunnerMain.Config.parse(new String[]{
            "--paper-count", "2",
            "--velocity-version", "latest",
            "--jfr",
            "--jfr-settings", "default"
        });

        List<String> args = ServerRunnerMain.jvmArgsForMainServer(cfg, "paper-1", tempDir.resolve("instance"), "run-1");

        assertTrue(args.stream().anyMatch(arg -> arg.startsWith("-XX:StartFlightRecording=name=paper-1,settings=default,")));
        assertTrue(args.contains("-XX:FlightRecorderOptions=stackdepth=128"));
        assertTrue(tempDir.resolve("instance").resolve("jfr").toFile().isDirectory());
    }

    @Test
    void jfrJvmArgsAreNotDuplicatedWhenAlreadyConfigured() throws Exception {
        ServerRunnerMain.Config cfg = ServerRunnerMain.Config.parse(new String[]{
            "--paper-count", "1",
            "--jfr",
            "--jvm-arg", "-XX:StartFlightRecording=name=custom,settings=profile,filename=custom.jfr,dumponexit=true",
            "--jvm-arg", "-XX:FlightRecorderOptions=stackdepth=64"
        });

        List<String> args = ServerRunnerMain.jvmArgsForMainServer(cfg, "paper-1", tempDir.resolve("instance"), "run-1");

        assertEquals(
            1,
            args.stream().filter(arg -> arg.startsWith("-XX:StartFlightRecording")).count()
        );
        assertEquals(
            1,
            args.stream().filter(arg -> arg.startsWith("-XX:FlightRecorderOptions")).count()
        );
        assertTrue(args.contains("-XX:StartFlightRecording=name=custom,settings=profile,filename=custom.jfr,dumponexit=true"));
        assertTrue(args.contains("-XX:FlightRecorderOptions=stackdepth=64"));
    }

    @Test
    void mysqlJdbcUrlEncodesDatabaseAndPassword() {
        ServerRunnerMain.Config cfg = ServerRunnerMain.Config.parse(new String[]{
            "--paper-count", "1",
            "--mysql",
            "--mysql-port", "3309",
            "--mysql-database", "rapunzel test",
            "--mysql-root-password", "p@ss word"
        });

        assertEquals(
            "jdbc:mysql://127.0.0.1:3309/rapunzel+test?user=root&password=p%40ss+word&useSSL=false&allowPublicKeyRetrieval=true",
            cfg.mysqlJdbc()
        );
    }

    @Test
    void workspaceDefaultsToRunServerRunnerUnderCurrentProject() {
        ServerRunnerMain.Config cfg = ServerRunnerMain.Config.parse(new String[]{"--paper-count", "1"});
        ServerRunnerWorkspace workspace = ServerRunnerWorkspace.resolve(cfg);

        assertTrue(workspace.baseDir().endsWith(Path.of("run", "server-runner")));
        assertEquals(workspace.baseDir().resolve("cache"), workspace.cacheDir());
        assertEquals(workspace.baseDir().resolve("instances"), workspace.instancesDir());
        assertTrue(workspace.baseDir().isAbsolute());
        assertNotEquals(Path.of("run", "server-runner"), workspace.baseDir());
    }
}
