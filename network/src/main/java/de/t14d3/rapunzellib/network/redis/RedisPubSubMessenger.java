package de.t14d3.rapunzellib.network.redis;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.network.MessageListener;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RedisPubSubMessenger implements Messenger, AutoCloseable {
    private final RedisPubSubConfig config;
    private final Logger logger;
    private final Gson gson = new Gson();

    private final Map<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();

    private final Object publishLock = new Object();
    private volatile RedisConnection publishConnection;
    private volatile RedisConnection subscribeConnection;

    private volatile boolean running = true;
    private volatile boolean connected;
    private final Thread subscribeThread;

    public RedisPubSubMessenger(RedisPubSubConfig config) {
        this(config, LoggerFactory.getLogger(RedisPubSubMessenger.class));
    }

    public RedisPubSubMessenger(RedisPubSubConfig config, Logger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.subscribeThread = new Thread(this::runSubscribeLoop, "RapunzelLib-RedisSub-" + config.serverName());
        this.subscribeThread.setDaemon(true);
        this.subscribeThread.start();
    }

    @Override
    public void sendToAll(String channel, String data) {
        publish(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.ALL, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToServer(String channel, String serverName, String data) {
        publish(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.SERVER, serverName, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void sendToProxy(String channel, String data) {
        publish(new NetworkEnvelope(channel, data, NetworkEnvelope.Target.PROXY, null, getServerName(), System.currentTimeMillis()));
    }

    @Override
    public void registerListener(String channel, MessageListener listener) {
        listeners.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    @Override
    public void unregisterListener(String channel, MessageListener listener) {
        List<MessageListener> list = listeners.get(channel);
        if (list == null) return;
        list.remove(listener);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public String getServerName() {
        return config.serverName();
    }

    @Override
    public String getProxyServerName() {
        return config.proxyServerName();
    }

    private void publish(NetworkEnvelope env) {
        String payload = gson.toJson(env);

        synchronized (publishLock) {
            if (!running) return;

            if (publishConnection == null || !publishConnection.isOpen()) {
                try {
                    publishConnection = RedisConnection.connect(config, false);
                } catch (Exception e) {
                    logger.warn("Redis publish connect failed: {}", e.getMessage());
                    closePublishConnection();
                    return;
                }
            }

            try {
                publishConnection.publish(config.transportChannel(), payload);
                return;
            } catch (Exception first) {
                logger.warn("Redis publish failed (will retry once): {}", first.getMessage());
                closePublishConnection();
            }

            try {
                publishConnection = RedisConnection.connect(config, false);
                publishConnection.publish(config.transportChannel(), payload);
            } catch (Exception second) {
                logger.warn("Redis publish retry failed: {}", second.getMessage());
                closePublishConnection();
            }
        }
    }

    private void closePublishConnection() {
        RedisConnection conn = publishConnection;
        publishConnection = null;
        if (conn != null) {
            conn.close();
        }
    }

    private void runSubscribeLoop() {
        while (running) {
            try (RedisConnection conn = RedisConnection.connect(config, true)) {
                subscribeConnection = conn;
                conn.subscribe(config.transportChannel());
                connected = true;

                while (running) {
                    Object reply = conn.readReply();
                    if (reply instanceof List<?> list) {
                        handlePubSubReply(list);
                    }
                }
            } catch (Exception e) {
                connected = false;
                if (running) {
                    logger.warn("Redis subscribe loop error: {}", e.getMessage());
                    sleepQuietly(config.reconnectDelayMillis());
                }
            } finally {
                subscribeConnection = null;
            }
        }
    }

    private void handlePubSubReply(List<?> reply) {
        if (reply.isEmpty()) return;
        Object kind = reply.getFirst();
        if (!(kind instanceof String type)) return;

        if ("message".equals(type)) {
            if (reply.size() < 3) return;
            Object payloadObj = reply.get(2);
            if (!(payloadObj instanceof String payload)) return;

            NetworkEnvelope env;
            try {
                env = gson.fromJson(payload, NetworkEnvelope.class);
            } catch (Exception ignored) {
                return;
            }

            if (!shouldDeliver(env)) return;
            deliverToLocalListeners(env);
        }
    }

    private boolean shouldDeliver(NetworkEnvelope env) {
        if (env == null || env.getChannel() == null) return false;
        NetworkEnvelope.Target target = env.getTarget();
        if (target == null) return false;

        String local = getServerName();
        String source = env.getSourceServer();
        boolean isProxy = local.equalsIgnoreCase(getProxyServerName());

        return switch (target) {
            case PROXY -> isProxy;
            case ALL -> source == null || !source.equalsIgnoreCase(local);
            case SERVER -> {
                if (source != null && source.equalsIgnoreCase(local)) yield false;
                if (isProxy) yield true;
                yield env.getTargetServer() != null && env.getTargetServer().equalsIgnoreCase(local);
            }
        };
    }

    private void deliverToLocalListeners(NetworkEnvelope env) {
        List<MessageListener> list = listeners.get(env.getChannel());
        if (list == null || list.isEmpty()) return;
        for (MessageListener listener : List.copyOf(list)) {
            try {
                listener.onMessage(env.getChannel(), env.getData(), env.getSourceServer());
            } catch (Exception e) {
                logger.warn("Network listener error on channel {}: {}", env.getChannel(), e.getMessage());
            }
        }
    }

    private static void sleepQuietly(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        running = false;
        connected = false;

        subscribeThread.interrupt();
        RedisConnection sub = subscribeConnection;
        subscribeConnection = null;
        if (sub != null) sub.close();
        closePublishConnection();
    }
}
