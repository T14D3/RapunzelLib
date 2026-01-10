package de.t14d3.rapunzellib.network.filesync;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;
import de.t14d3.rapunzellib.network.json.JsonCodecs;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import org.slf4j.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * File-level synchronization over a {@link Messenger}.
 *
 * <p>Authority responds to per-server manifest requests. Payload is transferred in chunks to support transports with
 * small message limits (e.g. plugin messaging).</p>
 */
public final class FileSyncEndpoint implements AutoCloseable {
    public interface Listener {
        default void onInvalidateReceived(FileSyncInvalidate invalidate, String sourceServer) {
        }

        default void onApplied(FileSyncResult result) {
        }

        default void onError(String message, Throwable error) {
        }
    }

    private static final int DEFAULT_MAX_CHUNK_BYTES = 8 * 1024;
    private static final long DEFAULT_MAX_PAYLOAD_BYTES = 5L * 1024L * 1024L;

    private final Messenger messenger;
    private final Scheduler scheduler;
    private final Logger logger;
    private final NetworkEventBus bus;

    private final String groupId;
    private final FileSyncSpec spec;
    private final FileSyncRole role;
    private final String authorityServerName;
    private final Duration requestTimeout;
    private final Duration transferTimeout;
    private final int maxChunkBytes;
    private final long maxPayloadBytes;
    private final Listener listener;
    private final boolean autoRequestOnInvalidate;

    private final NetworkEventBus.Subscription reqSub;
    private final NetworkEventBus.Subscription resMetaSub;
    private final NetworkEventBus.Subscription resChunkSub;
    private final NetworkEventBus.Subscription invalidateSub;

    private final Map<String, PendingSync> pending = new ConcurrentHashMap<>();

    public FileSyncEndpoint(
        Messenger messenger,
        Scheduler scheduler,
        Logger logger,
        String groupId,
        FileSyncSpec spec,
        FileSyncRole role,
        String authorityServerName,
        boolean autoRequestOnInvalidate
    ) {
        this(
            messenger,
            scheduler,
            logger,
            groupId,
            spec,
            role,
            authorityServerName,
            autoRequestOnInvalidate,
            Duration.ofSeconds(5),
            Duration.ofSeconds(20),
            DEFAULT_MAX_CHUNK_BYTES,
            DEFAULT_MAX_PAYLOAD_BYTES,
            null,
            null
        );
    }

    public FileSyncEndpoint(
        Messenger messenger,
        Scheduler scheduler,
        Logger logger,
        String groupId,
        FileSyncSpec spec,
        FileSyncRole role,
        String authorityServerName,
        boolean autoRequestOnInvalidate,
        Duration requestTimeout,
        Duration transferTimeout,
        int maxChunkBytes,
        long maxPayloadBytes,
        Listener listener,
        Gson gson
    ) {
        this.messenger = Objects.requireNonNull(messenger, "messenger");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.groupId = requireNonBlank(groupId, "groupId");
        this.spec = Objects.requireNonNull(spec, "spec");
        this.role = Objects.requireNonNull(role, "role");
        this.authorityServerName = (role == FileSyncRole.FOLLOWER)
            ? requireNonBlank(authorityServerName, "authorityServerName")
            : authorityServerName;
        this.autoRequestOnInvalidate = autoRequestOnInvalidate;
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        this.transferTimeout = Objects.requireNonNull(transferTimeout, "transferTimeout");
        this.maxChunkBytes = (maxChunkBytes <= 0) ? DEFAULT_MAX_CHUNK_BYTES : maxChunkBytes;
        this.maxPayloadBytes = (maxPayloadBytes <= 0) ? DEFAULT_MAX_PAYLOAD_BYTES : maxPayloadBytes;
        this.listener = (listener != null) ? listener : new Listener() {
        };

        Gson effectiveGson = (gson != null) ? gson : JsonCodecs.gson();
        this.bus = new NetworkEventBus(messenger, effectiveGson);

        this.reqSub = bus.register(
            FileSyncChannels.REQUEST,
            FileSyncRequest.class,
            this::handleRequest
        );
        this.resMetaSub = bus.register(
            FileSyncChannels.RESPONSE_META,
            FileSyncResponseMeta.class,
            this::handleResponseMeta
        );
        this.resChunkSub = bus.register(
            FileSyncChannels.RESPONSE_CHUNK,
            FileSyncResponseChunk.class,
            this::handleResponseChunk
        );
        this.invalidateSub = bus.register(
            FileSyncChannels.INVALIDATE,
            FileSyncInvalidate.class,
            this::handleInvalidate
        );
    }

    public String groupId() {
        return groupId;
    }

    public FileSyncRole role() {
        return role;
    }

    public void broadcastInvalidate() {
        if (role != FileSyncRole.AUTHORITY) {
            throw new IllegalStateException("Only AUTHORITY can broadcast invalidates");
        }
        bus.sendToAll(
            FileSyncChannels.INVALIDATE,
            new FileSyncInvalidate(groupId, UUID.randomUUID().toString(), System.currentTimeMillis())
        );
    }

    public CompletableFuture<FileSyncResult> requestSync() {
        if (role != FileSyncRole.FOLLOWER) {
            return CompletableFuture.failedFuture(new IllegalStateException("Only FOLLOWER can request sync"));
        }
        if (!messenger.isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                "Network messenger is not connected (plugin messaging requires at least one player online)"
            ));
        }

        CompletableFuture<FileSyncResult> future = new CompletableFuture<>();
        scheduler.runAsync(() -> {
            String requestId = UUID.randomUUID().toString();

            Map<String, String> manifest;
            try {
                manifest = spec.computeManifest();
            } catch (Exception e) {
                future.completeExceptionally(e);
                return;
            }

            ScheduledTask timeoutTask = scheduler.runLater(requestTimeout, () -> {
                PendingSync removed = pending.remove(requestId);
                if (removed == null) return;
                removed.future.completeExceptionally(new TimeoutException("File sync request timed out: " + groupId));
            });

            PendingSync req = new PendingSync(future, timeoutTask);
            pending.put(requestId, req);

            try {
                bus.sendToServer(FileSyncChannels.REQUEST, authorityServerName, new FileSyncRequest(requestId, groupId, manifest));
            } catch (Exception e) {
                pending.remove(requestId);
                timeoutTask.cancel();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void handleInvalidate(FileSyncInvalidate invalidate, String sourceServer) {
        if (invalidate == null || !groupId.equals(invalidate.groupId())) return;
        listener.onInvalidateReceived(invalidate, sourceServer);

        if (role != FileSyncRole.FOLLOWER || !autoRequestOnInvalidate) return;
        if (authorityServerName != null && sourceServer != null && !authorityServerName.equalsIgnoreCase(sourceServer)) {
            return;
        }

        if (!messenger.isConnected()) return;
        if (!pending.isEmpty()) return;

        requestSync().whenComplete((result, error) -> {
            if (error != null) {
                listener.onError("Auto sync failed: " + error.getMessage(), error);
            }
        });
    }

    private void handleRequest(FileSyncRequest request, String sourceServer) {
        if (role != FileSyncRole.AUTHORITY) return;
        if (request == null || request.requestId() == null || request.groupId() == null) return;
        if (!groupId.equals(request.groupId())) return;
        if (sourceServer == null || sourceServer.isBlank()) return;

        scheduler.runAsync(() -> {
            try {
                Map<String, String> local = spec.computeManifest();
                Map<String, String> remote = (request.fileHashes() == null) ? Collections.emptyMap() : request.fileHashes();

                Set<String> changedOrNew = new HashSet<>();
                for (Map.Entry<String, String> entry : local.entrySet()) {
                    String path = entry.getKey();
                    String hash = entry.getValue();
                    String remoteHash = remote.get(path);
                    if (remoteHash == null || !remoteHash.equalsIgnoreCase(hash)) {
                        changedOrNew.add(path);
                    }
                }

                List<String> deletePaths = new ArrayList<>();
                if (spec.deleteExtraneous() && !remote.isEmpty()) {
                    for (String remotePath : remote.keySet()) {
                        if (!local.containsKey(remotePath)) {
                            deletePaths.add(remotePath);
                        }
                    }
                }

                byte[] zip = changedOrNew.isEmpty() ? new byte[0] : spec.buildZip(changedOrNew);
                if (zip.length > maxPayloadBytes) {
                    sendError(request.requestId(), sourceServer, "Payload too large (" + zip.length + " bytes)");
                    return;
                }

                int chunkCount = (zip.length == 0) ? 0 : (int) ((zip.length + (long) maxChunkBytes - 1L) / (long) maxChunkBytes);
                FileSyncResponseMeta meta = new FileSyncResponseMeta(
                    request.requestId(),
                    groupId,
                    true,
                    null,
                    List.copyOf(deletePaths),
                    chunkCount,
                    zip.length,
                    FileSyncUtil.sha256Hex(zip)
                );
                bus.sendToServer(FileSyncChannels.RESPONSE_META, sourceServer, meta);

                if (zip.length == 0) return;
                for (int i = 0; i < chunkCount; i++) {
                    int from = i * maxChunkBytes;
                    int len = Math.min(maxChunkBytes, zip.length - from);
                    byte[] slice = new byte[len];
                    System.arraycopy(zip, from, slice, 0, len);
                    String b64 = Base64.getEncoder().encodeToString(slice);
                    bus.sendToServer(
                        FileSyncChannels.RESPONSE_CHUNK,
                        sourceServer,
                        new FileSyncResponseChunk(request.requestId(), groupId, i, b64)
                    );
                }
            } catch (Exception e) {
                listener.onError("File sync authority handler failed: " + e.getMessage(), e);
                sendError(request.requestId(), sourceServer, e.getMessage());
            }
        });
    }

    private void sendError(String requestId, String targetServer, String message) {
        try {
            bus.sendToServer(
                FileSyncChannels.RESPONSE_META,
                targetServer,
                new FileSyncResponseMeta(requestId, groupId, false, message, List.of(), 0, 0, null)
            );
        } catch (Exception e) {
            logger.debug("Failed to send file sync error response ({}, requestId={})", groupId, requestId, e);
        }
    }

    private void handleResponseMeta(FileSyncResponseMeta meta, String sourceServer) {
        if (role != FileSyncRole.FOLLOWER) return;
        if (meta == null || meta.requestId() == null || meta.groupId() == null) return;
        if (!groupId.equals(meta.groupId())) return;
        if (authorityServerName != null && sourceServer != null && !authorityServerName.equalsIgnoreCase(sourceServer)) {
            return;
        }

        PendingSync req = pending.get(meta.requestId());
        if (req == null) return;

        if (meta.ok()) {
            if (meta.payloadSize() > maxPayloadBytes) {
                fail(meta.requestId(), new IllegalStateException("Payload too large (" + meta.payloadSize() + " bytes)"));
                return;
            }
            if (meta.chunkCount() < 0) {
                fail(meta.requestId(), new IllegalStateException("Invalid chunk count: " + meta.chunkCount()));
                return;
            }
        }

        try {
            req.requestTimeout.cancel();
        } catch (Exception e) {
            logger.debug("Failed to cancel file sync request timeout ({}, requestId={})", groupId, meta.requestId(), e);
        }

        if (!meta.ok()) {
            pending.remove(meta.requestId());
            req.future.completeExceptionally(new IllegalStateException(
                (meta.error() == null || meta.error().isBlank()) ? "Authority returned an error" : meta.error()
            ));
            return;
        }

        if (meta.chunkCount() == 0) {
            scheduler.runAsync(() -> {
                try {
                    FileSyncSpec.ApplyResult applied = spec.applyZip(new byte[0], meta.deletePaths() == null ? List.of() : meta.deletePaths());
                    FileSyncResult result = new FileSyncResult(
                        groupId,
                        0,
                        applied.deletedPaths().size(),
                        List.of(),
                        applied.deletedPaths()
                    );
                    pending.remove(meta.requestId());
                    listener.onApplied(result);
                    req.future.complete(result);
                } catch (Exception e) {
                    fail(meta.requestId(), e);
                }
            });
            return;
        }

        req.transfer = new InFlightTransfer(meta, scheduler.runLater(transferTimeout, () -> {
            PendingSync removed = pending.remove(meta.requestId());
            if (removed == null) return;
            removed.future.completeExceptionally(new TimeoutException("File sync transfer timed out: " + groupId));
        }));
    }

    private void handleResponseChunk(FileSyncResponseChunk chunk, String sourceServer) {
        if (role != FileSyncRole.FOLLOWER) return;
        if (chunk == null || chunk.requestId() == null || chunk.groupId() == null) return;
        if (!groupId.equals(chunk.groupId())) return;
        if (authorityServerName != null && sourceServer != null && !authorityServerName.equalsIgnoreCase(sourceServer)) {
            return;
        }

        PendingSync req = pending.get(chunk.requestId());
        if (req == null || req.transfer == null) return;
        InFlightTransfer transfer = req.transfer;

        if (chunk.index() < 0 || chunk.index() >= transfer.chunks.length) return;
        if (chunk.dataBase64() == null) return;
        transfer.chunks[chunk.index()] = chunk.dataBase64();

        if (!transfer.isComplete()) return;
        try {
            transfer.timeoutTask.cancel();
        } catch (Exception e) {
            logger.debug("Failed to cancel file sync transfer timeout ({}, requestId={})", groupId, chunk.requestId(), e);
        }

        scheduler.runAsync(() -> {
            try {
                byte[] zip = transfer.assemble();
                if (transfer.meta.payloadSha256() != null) {
                    String actual = FileSyncUtil.sha256Hex(zip);
                    if (!transfer.meta.payloadSha256().equalsIgnoreCase(actual)) {
                        throw new IllegalStateException("Payload checksum mismatch");
                    }
                }

                FileSyncSpec.ApplyResult applied = spec.applyZip(zip, transfer.meta.deletePaths() == null ? List.of() : transfer.meta.deletePaths());

                FileSyncResult result = new FileSyncResult(
                    groupId,
                    applied.writtenPaths().size(),
                    applied.deletedPaths().size(),
                    applied.writtenPaths(),
                    applied.deletedPaths()
                );

                pending.remove(chunk.requestId());
                listener.onApplied(result);
                req.future.complete(result);
            } catch (Exception e) {
                fail(chunk.requestId(), e);
            }
        });
    }

    private void fail(String requestId, Exception e) {
        PendingSync removed = pending.remove(requestId);
        if (removed == null) return;
        try {
            removed.requestTimeout.cancel();
        } catch (Exception cancelError) {
            logger.debug("Failed to cancel file sync request timeout ({}, requestId={})", groupId, requestId, cancelError);
        }
        if (removed.transfer != null) {
            try {
                removed.transfer.timeoutTask.cancel();
            } catch (Exception cancelError) {
                logger.debug("Failed to cancel file sync transfer timeout ({}, requestId={})", groupId, requestId, cancelError);
            }
        }
        logger.warn("File sync failed ({})", groupId, e);
        removed.future.completeExceptionally(e);
    }

    @Override
    public void close() {
        reqSub.close();
        resMetaSub.close();
        resChunkSub.close();
        invalidateSub.close();

        for (PendingSync req : pending.values()) {
            try {
                req.requestTimeout.cancel();
            } catch (Exception e) {
                logger.debug("Failed to cancel file sync request timeout during close ({})", groupId, e);
            }
            if (req.transfer != null) {
                try {
                    req.transfer.timeoutTask.cancel();
                } catch (Exception e) {
                    logger.debug("Failed to cancel file sync transfer timeout during close ({})", groupId, e);
                }
            }
            req.future.completeExceptionally(new IllegalStateException("FileSyncEndpoint closed"));
        }
        pending.clear();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " cannot be blank");
        return value;
    }

    private static final class PendingSync {
        private final CompletableFuture<FileSyncResult> future;
        private final ScheduledTask requestTimeout;
        private volatile InFlightTransfer transfer;

        private PendingSync(CompletableFuture<FileSyncResult> future, ScheduledTask requestTimeout) {
            this.future = Objects.requireNonNull(future, "future");
            this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        }
    }

    private static final class InFlightTransfer {
        private final FileSyncResponseMeta meta;
        private final String[] chunks;
        private final ScheduledTask timeoutTask;

        private InFlightTransfer(FileSyncResponseMeta meta, ScheduledTask timeoutTask) {
            this.meta = Objects.requireNonNull(meta, "meta");
            this.timeoutTask = Objects.requireNonNull(timeoutTask, "timeoutTask");
            this.chunks = new String[meta.chunkCount()];
        }

        private boolean isComplete() {
            for (String chunk : chunks) {
                if (chunk == null) return false;
            }
            return true;
        }

        private byte[] assemble() throws IOException {
            int total = 0;
            byte[][] decoded = new byte[chunks.length][];
            for (int i = 0; i < chunks.length; i++) {
                byte[] bytes = Base64.getDecoder().decode(chunks[i]);
                decoded[i] = bytes;
                total += bytes.length;
            }
            byte[] out = new byte[total];
            int at = 0;
            for (byte[] part : decoded) {
                System.arraycopy(part, 0, out, at, part.length);
                at += part.length;
            }
            return out;
        }
    }
}

