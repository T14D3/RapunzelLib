package de.t14d3.rapunzellib.devrunner;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public final class DevRunnerConsole {
    private final Map<String, ServerProcessWrapper> processes = new ConcurrentHashMap<>();
    private final List<LineListener> lineListeners = new CopyOnWriteArrayList<>();
    private volatile String focus;
    private volatile boolean running;

    public record ServerProcessWrapper(String name, Process process, OutputStream stdin) {}

    /**
     * Listener for lines read from server process stdout/stderr.
     */
    @FunctionalInterface
    public interface LineListener {
        /**
         * Called when a line is read from a server process stream.
         *
         * @param sourceName the server name
         * @param line       the line content
         * @param isError    true if this came from stderr, false for stdout
         */
        void onLine(String sourceName, String line, boolean isError);
    }

    /**
     * Registers a listener that will be called for every line read from any server process.
     */
    public void addLineListener(LineListener listener) {
        lineListeners.add(listener);
    }

    /**
     * Removes a previously registered line listener.
     */
    public void removeLineListener(LineListener listener) {
        lineListeners.remove(listener);
    }

    public void registerSource(String name, Process process) {
        OutputStream stdin = process.getOutputStream();
        processes.put(name, new ServerProcessWrapper(name, process, stdin));

        Thread outThread = streamThread(name, process.getInputStream(), false);
        Thread errThread = streamThread(name, process.getErrorStream(), true);
        outThread.start();
        errThread.start();
    }

    public void startInputRouting() {
        running = true;
        Thread inputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while (running && (line = reader.readLine()) != null) {
                    routeInput(line);
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("[devrunner] Console input error: " + e.getMessage());
                }
            }
        }, "devrunner-console-input");
        inputThread.setDaemon(true);
        inputThread.start();
    }

    public void routeInput(String line) {
        if (line == null || line.isBlank()) return;

        // Meta-commands
        if (line.startsWith(":")) {
            handleMetaCommand(line.substring(1).trim());
            return;
        }

        // Focus: -<name> (no space) -> set focus
        if (line.startsWith("-") && !line.startsWith("- ") && !line.contains(" ")) {
            String name = line.substring(1).trim();
            if (processes.containsKey(name)) {
                setFocus(name);
                System.out.println("[devrunner] Focus set to " + name);
            } else {
                System.out.println("[devrunner] Unknown server: " + name + ". Available: " + processes.keySet());
            }
            return;
        }

        // Targeted: -<name> <command> -> send to that server
        if (line.startsWith("-")) {
            String remainder = line.substring(1).trim();
            int spaceIdx = remainder.indexOf(' ');
            if (spaceIdx > 0) {
                String name = remainder.substring(0, spaceIdx);
                String command = remainder.substring(spaceIdx + 1);
                sendToServer(name, command);
                return;
            }
        }

        // Focus+send: +<name> <command> -> send and keep focus
        if (line.startsWith("+")) {
            String remainder = line.substring(1).trim();
            int spaceIdx = remainder.indexOf(' ');
            if (spaceIdx > 0) {
                String name = remainder.substring(0, spaceIdx);
                String command = remainder.substring(spaceIdx + 1);
                sendToServer(name, command);
                setFocus(name);
                return;
            }
        }

        // botcallback commands are always broadcast to all servers
        if (line.startsWith("botcallback ")) {
            for (String name : processes.keySet()) {
                sendToServer(name, line);
            }
            return;
        }

        // Bare input -> send to focused server
        if (focus != null) {
            sendToServer(focus, line);
        } else {
            // Broadcast to all
            for (String name : processes.keySet()) {
                sendToServer(name, line);
            }
        }
    }

    public void setFocus(String serverName) {
        this.focus = serverName;
    }

    public String getFocus() {
        return focus;
    }

    public void sendToServer(String name, String command) {
        ServerProcessWrapper wrapper = processes.get(name);
        if (wrapper == null) {
            System.out.println("[devrunner] Unknown server: " + name);
            return;
        }
        try {
            wrapper.stdin().write((command + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
            wrapper.stdin().flush();
        } catch (IOException e) {
            System.err.println("[devrunner] Failed to send to " + name + ": " + e.getMessage());
        }
    }

    public void shutdown() {
        running = false;
    }

    private void handleMetaCommand(String cmd) {
        String[] parts = cmd.split("\\s+", 2);
        switch (parts[0]) {
            case "focus" -> {
                if (parts.length > 1 && processes.containsKey(parts[1])) {
                    setFocus(parts[1]);
                    System.out.println("[devrunner] Focus set to " + parts[1]);
                } else {
                    System.out.println("[devrunner] Current focus: " + (focus != null ? focus : "(none)"));
                }
            }
            case "list" -> {
                System.out.println("[devrunner] Servers: " + processes.keySet());
            }
            case "unfocus" -> {
                setFocus(null);
                System.out.println("[devrunner] Focus cleared");
            }
            default -> System.out.println("[devrunner] Unknown meta-command: " + parts[0]);
        }
    }

    private Thread streamThread(String name, InputStream in, boolean isErr) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String prefix = "[" + name + (isErr ? "][ERR] " : "] ");
                    System.out.println(prefix + line);
                    // Notify line listeners (on the raw line, without the prefix)
                    for (LineListener listener : lineListeners) {
                        try {
                            listener.onLine(name, line, isErr);
                        } catch (Exception ignored) {
                            // Listener must not disrupt the stream thread
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        });
        t.setDaemon(true);
        t.setName("devrunner-" + name + (isErr ? "-stderr" : "-stdout"));
        return t;
    }
}
