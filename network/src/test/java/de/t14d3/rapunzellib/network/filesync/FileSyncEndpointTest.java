package de.t14d3.rapunzellib.network.filesync;

import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

final class FileSyncEndpointTest {
    @Test
    void followerReceivesAndAppliesFiles(@TempDir Path temp) throws Exception {
        Path authorityDir = temp.resolve("authority");
        Path followerDir = temp.resolve("follower");
        Files.createDirectories(authorityDir);
        Files.createDirectories(followerDir);

        Files.writeString(authorityDir.resolve("a.txt"), "hello", StandardCharsets.UTF_8);

        FileSyncSpec authoritySpec = FileSyncSpec.builder(authorityDir).includeGlob("**").build();
        FileSyncSpec followerSpec = FileSyncSpec.builder(followerDir).includeGlob("**").build();

        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();

        Messenger authorityMessenger = network.create("auth", "proxy");
        Messenger followerMessenger = network.create("fol", "proxy");

        try (
            FileSyncEndpoint _authority = new FileSyncEndpoint(
                authorityMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-auth"),
                "group",
                authoritySpec,
                FileSyncRole.AUTHORITY,
                null,
                false
            );
            FileSyncEndpoint follower = new FileSyncEndpoint(
                followerMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-fol"),
                "group",
                followerSpec,
                FileSyncRole.FOLLOWER,
                "auth",
                false
            )
        ) {
            CompletableFuture<FileSyncResult> future = follower.requestSync();
            FileSyncResult result = future.join();

            Path applied = followerDir.resolve("a.txt");
            assertTrue(Files.isRegularFile(applied));
            assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(applied));
            assertEquals(1, result.filesWritten());
            assertEquals(0, result.filesDeleted());
            assertEquals(List.of("a.txt"), result.writtenPaths());
        }
    }

    @Test
    void followerRejectsOversizedPayload(@TempDir Path temp) throws Exception {
        Path authorityDir = temp.resolve("authority");
        Path followerDir = temp.resolve("follower");
        Files.createDirectories(authorityDir);
        Files.createDirectories(followerDir);

        byte[] large = new byte[256];
        for (int i = 0; i < large.length; i++) large[i] = (byte) (i & 0xFF);
        Files.write(authorityDir.resolve("big.bin"), large);

        FileSyncSpec authoritySpec = FileSyncSpec.builder(authorityDir).includeGlob("**").build();
        FileSyncSpec followerSpec = FileSyncSpec.builder(followerDir).includeGlob("**").build();

        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();

        Messenger authorityMessenger = network.create("auth", "proxy");
        Messenger followerMessenger = network.create("fol", "proxy");

        try (
            FileSyncEndpoint _authority = new FileSyncEndpoint(
                authorityMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-auth"),
                "group",
                authoritySpec,
                FileSyncRole.AUTHORITY,
                null,
                false,
                Duration.ofSeconds(5),
                Duration.ofSeconds(20),
                8 * 1024,
                5L * 1024L * 1024L,
                null,
                null
            );
            FileSyncEndpoint follower = new FileSyncEndpoint(
                followerMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-fol"),
                "group",
                followerSpec,
                FileSyncRole.FOLLOWER,
                "auth",
                false,
                Duration.ofSeconds(5),
                Duration.ofSeconds(20),
                8 * 1024,
                32,
                null,
                null
            )
        ) {
            CompletableFuture<FileSyncResult> future = follower.requestSync();  
            CompletionException ex = assertThrows(CompletionException.class, future::join);
            assertTrue(ex.getCause().getMessage().toLowerCase().contains("payload too large"));
        }
    }

    @Test
    void followerDeletesExtraneousFilesWhenEnabled(@TempDir Path temp) throws Exception {
        Path authorityDir = temp.resolve("authority");
        Path followerDir = temp.resolve("follower");
        Files.createDirectories(authorityDir);
        Files.createDirectories(followerDir);

        Files.writeString(authorityDir.resolve("a.txt"), "hello", StandardCharsets.UTF_8);
        Files.writeString(followerDir.resolve("a.txt"), "hello", StandardCharsets.UTF_8);
        Files.writeString(followerDir.resolve("b.txt"), "remove-me", StandardCharsets.UTF_8);

        FileSyncSpec authoritySpec = FileSyncSpec.builder(authorityDir)
            .includeGlob("**")
            .deleteExtraneous(true)
            .build();
        FileSyncSpec followerSpec = FileSyncSpec.builder(followerDir).includeGlob("**").build();

        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();

        Messenger authorityMessenger = network.create("auth", "proxy");
        Messenger followerMessenger = network.create("fol", "proxy");

        try (
            FileSyncEndpoint _authority = new FileSyncEndpoint(
                authorityMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-auth"),
                "group",
                authoritySpec,
                FileSyncRole.AUTHORITY,
                null,
                false
            );
            FileSyncEndpoint follower = new FileSyncEndpoint(
                followerMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-fol"),
                "group",
                followerSpec,
                FileSyncRole.FOLLOWER,
                "auth",
                false
            )
        ) {
            FileSyncResult result = follower.requestSync().join();

            assertTrue(Files.isRegularFile(followerDir.resolve("a.txt")));
            assertFalse(Files.exists(followerDir.resolve("b.txt")));

            assertEquals(0, result.filesWritten());
            assertEquals(1, result.filesDeleted());
            assertEquals(List.of("b.txt"), result.deletedPaths());
        }
    }

    @Test
    void followerFailsOnChecksumMismatch(@TempDir Path temp) throws Exception {
        Path authorityDir = temp.resolve("authority");
        Path followerDir = temp.resolve("follower");
        Files.createDirectories(authorityDir);
        Files.createDirectories(followerDir);

        Files.writeString(authorityDir.resolve("a.txt"), "hello", StandardCharsets.UTF_8);

        FileSyncSpec authoritySpec = FileSyncSpec.builder(authorityDir).includeGlob("**").build();
        FileSyncSpec followerSpec = FileSyncSpec.builder(followerDir).includeGlob("**").build();

        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();

        Messenger rawAuthority = network.create("auth", "proxy");
        Messenger authorityMessenger = new TamperingMessenger(rawAuthority, TamperingMessenger.Mode.CORRUPT_FIRST_CHUNK);
        Messenger followerMessenger = network.create("fol", "proxy");

        try (
            FileSyncEndpoint _authority = new FileSyncEndpoint(
                authorityMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-auth"),
                "group",
                authoritySpec,
                FileSyncRole.AUTHORITY,
                null,
                false
            );
            FileSyncEndpoint follower = new FileSyncEndpoint(
                followerMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-fol"),
                "group",
                followerSpec,
                FileSyncRole.FOLLOWER,
                "auth",
                false
            )
        ) {
            CompletionException ex = assertThrows(CompletionException.class, () -> follower.requestSync().join());
            assertTrue(ex.getCause().getMessage().toLowerCase().contains("checksum"));
        }
    }

    @Test
    void followerFailsOnInvalidChunkCount(@TempDir Path temp) throws Exception {
        Path authorityDir = temp.resolve("authority");
        Path followerDir = temp.resolve("follower");
        Files.createDirectories(authorityDir);
        Files.createDirectories(followerDir);

        Files.writeString(authorityDir.resolve("a.txt"), "hello", StandardCharsets.UTF_8);

        FileSyncSpec authoritySpec = FileSyncSpec.builder(authorityDir).includeGlob("**").build();
        FileSyncSpec followerSpec = FileSyncSpec.builder(followerDir).includeGlob("**").build();

        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();

        Messenger rawAuthority = network.create("auth", "proxy");
        Messenger authorityMessenger = new TamperingMessenger(rawAuthority, TamperingMessenger.Mode.INVALID_CHUNK_COUNT);
        Messenger followerMessenger = network.create("fol", "proxy");

        try (
            FileSyncEndpoint _authority = new FileSyncEndpoint(
                authorityMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-auth"),
                "group",
                authoritySpec,
                FileSyncRole.AUTHORITY,
                null,
                false
            );
            FileSyncEndpoint follower = new FileSyncEndpoint(
                followerMessenger,
                scheduler,
                LoggerFactory.getLogger("filesync-fol"),
                "group",
                followerSpec,
                FileSyncRole.FOLLOWER,
                "auth",
                false
            )
        ) {
            CompletionException ex = assertThrows(CompletionException.class, () -> follower.requestSync().join());
            assertTrue(ex.getCause().getMessage().toLowerCase().contains("chunk"));
        }
    }

    @Test
    void followerRequestTimesOutWithoutAuthority(@TempDir Path temp) throws Exception {
        Path followerDir = temp.resolve("follower");
        Files.createDirectories(followerDir);

        FileSyncSpec followerSpec = FileSyncSpec.builder(followerDir).includeGlob("**").build();

        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        Messenger followerMessenger = network.create("fol", "proxy");

        try (FileSyncEndpoint follower = new FileSyncEndpoint(
            followerMessenger,
            scheduler,
            LoggerFactory.getLogger("filesync-fol"),
            "group",
            followerSpec,
            FileSyncRole.FOLLOWER,
            "auth",
            false,
            Duration.ofSeconds(1),
            Duration.ofSeconds(5),
            8 * 1024,
            64 * 1024,
            null,
            null
        )) {
            CompletableFuture<FileSyncResult> future = follower.requestSync();
            scheduler.runAllDelayed();

            CompletionException ex = assertThrows(CompletionException.class, future::join);
            assertInstanceOf(TimeoutException.class, ex.getCause());
        }
    }

    @Test
    void followerTransferTimesOutWhenChunksMissing(@TempDir Path temp) throws Exception {
        Path followerDir = temp.resolve("follower");
        Files.createDirectories(followerDir);

        FileSyncSpec followerSpec = FileSyncSpec.builder(followerDir).includeGlob("**").build();

        TestNetwork network = new TestNetwork();
        TestScheduler scheduler = new TestScheduler();
        Messenger authorityMessenger = network.create("auth", "proxy");
        Messenger followerMessenger = network.create("fol", "proxy");

        // Fake authority: responds with a meta indicating a payload with 1 chunk, but never sends a chunk.
        var gson = de.t14d3.rapunzellib.network.json.JsonCodecs.gson();
        authorityMessenger.registerListener(FileSyncChannels.REQUEST, (channel, data, sourceServer) -> {
            FileSyncRequest req = gson.fromJson(data, FileSyncRequest.class);
            FileSyncResponseMeta meta = new FileSyncResponseMeta(
                req.requestId(),
                req.groupId(),
                true,
                null,
                List.of(),
                1,
                1,
                "deadbeef"
            );
            authorityMessenger.sendToServer(FileSyncChannels.RESPONSE_META, sourceServer, gson.toJson(meta));
        });

        try (FileSyncEndpoint follower = new FileSyncEndpoint(
            followerMessenger,
            scheduler,
            LoggerFactory.getLogger("filesync-fol"),
            "group",
            followerSpec,
            FileSyncRole.FOLLOWER,
            "auth",
            false,
            Duration.ofSeconds(5),
            Duration.ofSeconds(1),
            8 * 1024,
            64 * 1024,
            null,
            null
        )) {
            CompletableFuture<FileSyncResult> future = follower.requestSync();
            scheduler.runAllDelayed();

            CompletionException ex = assertThrows(CompletionException.class, future::join);
            assertInstanceOf(TimeoutException.class, ex.getCause());
        }
    }

    private static final class TestScheduler implements Scheduler {
        private final CopyOnWriteArrayList<TestTask> delayed = new CopyOnWriteArrayList<>();

        @Override
        public @NotNull ScheduledTask run(@NotNull Runnable task) {
            task.run();
            return new TestTask(null);
        }

        @Override
        public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
            task.run();
            return new TestTask(null);
        }

        @Override
        public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
            TestTask scheduled = new TestTask(task);
            delayed.add(scheduled);
            return scheduled;
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            return new TestTask(null);
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            return new TestTask(null);
        }

        void runAllDelayed() {
            for (TestTask task : List.copyOf(delayed)) {
                if (task == null) continue;
                if (task.isCancelled()) continue;
                task.runOnce();
            }
        }

        private static final class TestTask implements ScheduledTask {
            private final Runnable runnable;
            private volatile boolean cancelled;
            private final AtomicBoolean ran = new AtomicBoolean(false);

            private TestTask(Runnable runnable) {
                this.runnable = runnable;
            }

            private void runOnce() {
                if (runnable == null) return;
                if (cancelled) return;
                if (!ran.compareAndSet(false, true)) return;
                runnable.run();
            }

            @Override
            public void cancel() {
                cancelled = true;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }
        }
    }

    private static final class TamperingMessenger implements Messenger {
        enum Mode { CORRUPT_FIRST_CHUNK, INVALID_CHUNK_COUNT }

        private final Messenger delegate;
        private final Mode mode;
        private final AtomicBoolean tampered = new AtomicBoolean(false);
        private final com.google.gson.Gson gson = de.t14d3.rapunzellib.network.json.JsonCodecs.gson();

        private TamperingMessenger(Messenger delegate, Mode mode) {
            this.delegate = delegate;
            this.mode = mode;
        }

        @Override
        public void sendToAll(@NotNull String channel, @NotNull String data) {
            delegate.sendToAll(channel, data);
        }

        @Override
        public void sendToServer(@NotNull String channel, @NotNull String serverName, @NotNull String data) {
            if (mode == Mode.CORRUPT_FIRST_CHUNK
                && FileSyncChannels.RESPONSE_CHUNK.equals(channel)
                && tampered.compareAndSet(false, true)) {
                FileSyncResponseChunk chunk = gson.fromJson(data, FileSyncResponseChunk.class);
                byte[] bytes = Base64.getDecoder().decode(chunk.dataBase64());
                if (bytes.length > 0) bytes[0] ^= 0x01;
                String corrupt = Base64.getEncoder().encodeToString(bytes);
                FileSyncResponseChunk modified = new FileSyncResponseChunk(
                    chunk.requestId(),
                    chunk.groupId(),
                    chunk.index(),
                    corrupt
                );
                delegate.sendToServer(channel, serverName, gson.toJson(modified));
                return;
            }

            if (mode == Mode.INVALID_CHUNK_COUNT
                && FileSyncChannels.RESPONSE_META.equals(channel)
                && tampered.compareAndSet(false, true)) {
                FileSyncResponseMeta meta = gson.fromJson(data, FileSyncResponseMeta.class);
                FileSyncResponseMeta modified = new FileSyncResponseMeta(
                    meta.requestId(),
                    meta.groupId(),
                    meta.ok(),
                    meta.error(),
                    meta.deletePaths(),
                    -1,
                    meta.payloadSize(),
                    meta.payloadSha256()
                );
                delegate.sendToServer(channel, serverName, gson.toJson(modified));
                return;
            }

            delegate.sendToServer(channel, serverName, data);
        }

        @Override
        public void sendToProxy(@NotNull String channel, @NotNull String data) {
            delegate.sendToProxy(channel, data);
        }

        @Override
        public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
            delegate.registerListener(channel, listener);
        }

        @Override
        public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
            delegate.unregisterListener(channel, listener);
        }

        @Override
        public boolean isConnected() {
            return delegate.isConnected();
        }

        @Override
        public @NotNull String getServerName() {
            return delegate.getServerName();
        }

        @Override
        public @NotNull String getProxyServerName() {
            return delegate.getProxyServerName();
        }
    }

    private static final class TestNetwork {
        private final Map<String, Node> nodes = new ConcurrentHashMap<>();      

        Messenger create(String serverName, String proxyServerName) {
            Node node = nodes.computeIfAbsent(serverName, _n -> new Node());
            return new Messenger() {
                @Override
                public void sendToAll(@NotNull String channel, @NotNull String data) {
                    for (var entry : nodes.entrySet()) {
                        entry.getValue().deliver(channel, data, serverName);
                    }
                }

                @Override
                public void sendToServer(@NotNull String channel, @NotNull String targetServerName, @NotNull String data) {
                    if (targetServerName == null || targetServerName.isBlank()) return;
                    Node target = nodes.get(targetServerName);
                    if (target == null) return;
                    target.deliver(channel, data, serverName);
                }

                @Override
                public void sendToProxy(@NotNull String channel, @NotNull String data) {
                    sendToServer(channel, proxyServerName, data);
                }

                @Override
                public void registerListener(@NotNull String channel, @NotNull MessageListener listener) {
                    node.listeners.computeIfAbsent(channel, _c -> new CopyOnWriteArrayList<>()).add(listener);
                }

                @Override
                public void unregisterListener(@NotNull String channel, @NotNull MessageListener listener) {
                    List<MessageListener> list = node.listeners.get(channel);
                    if (list == null) return;
                    list.remove(listener);
                }

                @Override
                public boolean isConnected() {
                    return true;
                }

                @Override
                public @NotNull String getServerName() {
                    return serverName;
                }

                @Override
                public @NotNull String getProxyServerName() {
                    return proxyServerName;
                }
            };
        }

        private static final class Node {
            private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

            void deliver(String channel, String data, String sourceServer) {
                List<MessageListener> list = listeners.get(channel);
                if (list == null || list.isEmpty()) return;
                for (MessageListener listener : List.copyOf(list)) {
                    listener.onMessage(channel, data, sourceServer);
                }
            }
        }
    }
}
