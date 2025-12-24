package de.t14d3.rapunzellib.serverrunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"ClassCanBeRecord", "FieldCanBeLocal"})
final class ServerProcess {
    private final String name;
    private final Process process;
    private final OutputStream stdin;
    private final Thread outThread;
    private final Thread errThread;

    private ServerProcess(String name, Process process, OutputStream stdin, Thread outThread, Thread errThread) {
        this.name = name;
        this.process = process;
        this.stdin = stdin;
        this.outThread = outThread;
        this.errThread = errThread;
    }

    @SuppressWarnings("SameParameterValue")
    static ServerProcess start(String name, Path workingDir, List<String> command, Map<String, String> env) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir.toAbsolutePath().normalize().toFile());
        if (env != null) pb.environment().putAll(env);

        Process process = pb.start();
        OutputStream stdin = process.getOutputStream();
        Thread out = streamThread(name, process.getInputStream(), false);
        Thread err = streamThread(name, process.getErrorStream(), true);
        out.start();
        err.start();
        return new ServerProcess(name, process, stdin, out, err);
    }

    String name() {
        return name;
    }

    void waitFor() throws InterruptedException {
        process.waitFor();
    }

    boolean waitFor(long timeoutMs) throws InterruptedException {
        return process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
    }

    void destroy() {
        process.destroy();
    }

    void destroyForcibly() {
        process.destroyForcibly();
    }

    void sendLine(String line) {
        if (stdin == null || line == null) return;
        try {
            stdin.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (IOException ignored) {
        }
    }

    boolean isAlive() {
        return process.isAlive();
    }

    CompletableFuture<Process> onExit() {
        return process.onExit();
    }

    private static Thread streamThread(String name, InputStream in, boolean isErr) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String prefix = "[" + name + (isErr ? "][ERR] " : "] ");
                    System.out.println(prefix + line);
                }
            } catch (IOException ignored) {
            }
        });
        t.setDaemon(true);
        t.setName("server-runner-" + name + (isErr ? "-stderr" : "-stdout"));
        return t;
    }

    static List<String> javaCommand(String javaBin, List<String> jvmArgs, String jarName, List<String> programArgs) {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaBin);
        if (jvmArgs != null) cmd.addAll(jvmArgs);
        cmd.add("-jar");
        cmd.add(jarName);
        if (programArgs != null) cmd.addAll(programArgs);
        return cmd;
    }
}
