package de.t14d3.rapunzellib.devrunner.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MysqlServiceAdapter implements ServiceAdapter {

    @Override
    public String key() {
        return "mysql";
    }

    @Override
    public String defaultImage() {
        return "mysql:8.4";
    }

    @Override
    public List<String> dockerRunCommand(String containerName, ServiceSpec spec, Path workDir) {
        String image = spec.image() != null ? spec.image() : defaultImage();
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("run");
        cmd.add("-d");
        cmd.add("--name");
        cmd.add(containerName);

        for (Map.Entry<String, String> port : spec.ports().entrySet()) {
            cmd.add("-p");
            cmd.add(port.getValue() + ":" + port.getKey());
        }

        for (Map.Entry<String, String> env : spec.env().entrySet()) {
            cmd.add("-e");
            cmd.add(env.getKey() + "=" + env.getValue());
        }

        cmd.add(image);
        return cmd;
    }

    @Override
    public boolean containerRunning(Path workDir, String containerName) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "inspect", "-f", "{{.State.Running}}", containerName
            );
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output;
            try (var in = process.getInputStream()) {
                output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) return false;
            return output.trim().equalsIgnoreCase("true");
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void startContainer(String containerName, ServiceSpec spec, Path workDir) throws Exception {
        List<String> cmd = dockerRunCommand(containerName, spec, workDir);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (var in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to start MySQL container '" + containerName + "': " + output);
        }
    }

    @Override
    public boolean waitForReady(String containerName, Path workDir, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        // Wait for port
        while (System.currentTimeMillis() < deadline) {
            // Find the host port from the container
            try {
                String hostPort = getHostPort(containerName, workDir);
                if (hostPort != null && isPortOpen("127.0.0.1", Integer.parseInt(hostPort))) {
                    // Port is open, try mysqladmin ping
                    if (mysqlAdminPing(containerName, workDir)) {
                        return true;
                    }
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

    @Override
    public void cleanup(String containerName, Path workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("docker", "rm", "-f", containerName);
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
        } catch (Exception e) {
            System.err.println("[mysql] Failed to cleanup container '" + containerName + "': " + e.getMessage());
        }
    }

    private String getHostPort(String containerName, Path workDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "inspect", "-f",
            "{{(index (index .NetworkSettings.Ports \"3306/tcp\") 0).HostPort}}",
            containerName
        );
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (var in = process.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        process.waitFor();
        if (process.exitValue() != 0) return null;
        return output.isEmpty() ? null : output;
    }

    private boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 750);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean mysqlAdminPing(String containerName, Path workDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "exec", containerName,
                "mysqladmin", "ping", "-h", "127.0.0.1", "-uroot", "--silent"
            );
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
