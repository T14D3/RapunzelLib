package de.t14d3.rapunzellib.serverrunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

final class MysqlDockerSupport {
    private MysqlDockerSupport() {
    }

    static void ensureContainerRunning(ServerRunnerMain.Config cfg, Path workingDir, String containerName)
        throws IOException, InterruptedException {
        DockerContainerState state = dockerContainerState(workingDir, containerName);
        if (state == DockerContainerState.RUNNING) return;

        if (state == DockerContainerState.EXISTS_STOPPED) {
            DockerCommandResult start = runCommand(workingDir, List.of("docker", "start", containerName));
            if (start.exitCode() != 0) {
                throw new IOException("Failed to start existing MySQL container '" + containerName + "': " + start.output());
            }
            return;
        }

        DockerCommandResult run = runCommand(workingDir, dockerRunCommand(cfg, containerName));
        if (run.exitCode() != 0) {
            throw new IOException("Failed to start MySQL container '" + containerName + "': " + run.output());
        }
    }

    static void cleanupContainer(Path workingDir, String containerName) {
        try {
            runCommand(workingDir, List.of("docker", "rm", "-f", containerName));
        } catch (Exception e) {
            System.err.println("[mysql] Failed to cleanup container '" + containerName + "': " + e.getMessage());
        }
    }

    static void waitForPortOpen(String host, int port, long timeoutMs) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        IOException last = null;
        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 750);
                return;
            } catch (IOException e) {
                last = e;
                Thread.sleep(500);
            }
        }
        throw new IOException("Timed out waiting for " + host + ":" + port + " to accept connections", last);
    }

    static boolean waitForMysqlAdminPing(ServerRunnerMain.Config cfg, Path workingDir, String containerName, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                DockerCommandResult ping = runCommand(workingDir, List.of(
                    "docker", "exec",
                    containerName,
                    "mysqladmin", "ping",
                    "-h", "127.0.0.1",
                    "-uroot",
                    "-p" + cfg.mysqlRootPassword(),
                    "--silent"
                ));
                if (ping.exitCode() == 0) {
                    System.out.println("[mysql] mysqladmin ping OK");
                    return true;
                }
            } catch (Exception ignored) {
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static List<String> dockerRunCommand(ServerRunnerMain.Config cfg, String containerName) {
        return Arrays.asList(
            "docker", "run", "-d",
            "--name", containerName,
            "-p", cfg.mysqlPort() + ":3306",
            "-e", "MYSQL_ROOT_PASSWORD=" + cfg.mysqlRootPassword(),
            "-e", "MYSQL_DATABASE=" + cfg.mysqlDatabase(),
            cfg.mysqlImage()
        );
    }

    private static DockerContainerState dockerContainerState(Path workingDir, String containerName)
        throws IOException, InterruptedException {
        DockerCommandResult result = runCommand(
            workingDir,
            List.of("docker", "inspect", "-f", "{{.State.Running}}", containerName)
        );
        if (result.exitCode() != 0) {
            if (result.output().toLowerCase().contains("no such object")) {
                return DockerContainerState.NOT_FOUND;
            }
            return DockerContainerState.NOT_FOUND;
        }

        return result.output().trim().equalsIgnoreCase("true")
            ? DockerContainerState.RUNNING
            : DockerContainerState.EXISTS_STOPPED;
    }

    private static DockerCommandResult runCommand(Path workingDir, List<String> command)
        throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir.toAbsolutePath().normalize().toFile());
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        String output;
        try (var in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return new DockerCommandResult(process.waitFor(), output);
    }

    private enum DockerContainerState {
        NOT_FOUND,
        EXISTS_STOPPED,
        RUNNING
    }

    private record DockerCommandResult(int exitCode, String output) {
    }
}
