package de.t14d3.rapunzellib.devrunner.bot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages multiple bot connections for the DevRunner.
 */
public class BotManager {

    private final Map<String, BotClient> bots = new ConcurrentHashMap<>();

    public void connectBot(String name, String host, int port) throws Exception {
        BotClient existing = bots.get(name);
        if (existing != null && existing.isConnected()) {
            System.out.println("[devrunner] Bot '" + name + "' already connected to " + host + ":" + port);
            return;
        }
        if (existing != null) {
            existing.disconnect();
            bots.remove(name);
        }
        BotClient client = new BotClient(name, host, port);
        client.connect();
        bots.put(name, client);
        System.out.println("[devrunner] Bot '" + name + "' connected to " + host + ":" + port);
    }

    public void disconnectBot(String name) {
        BotClient client = bots.remove(name);
        if (client != null) {
            client.disconnect();
            System.out.println("[devrunner] Bot '" + name + "' disconnected");
        }
    }

    public void execute(String name, String command) {
        BotClient client = bots.get(name);
        if (client == null) { System.out.println("[devrunner] Cannot execute on bot '" + name + "': not found"); return; }
        client.sendChat(command);
        System.out.println("[devrunner] Bot '" + name + "' executing: " + command);
    }

    public BotClient getBot(String name) { return bots.get(name); }

    public void digBlock(String name, int x, int y, int z, int direction) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.digBlock(x, y, z, direction);
    }

    public void useItemOn(String name, int x, int y, int z, int hand, int direction) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.useItemOn(x, y, z, hand, direction);
    }

    public void disconnectAll() {
        for (Map.Entry<String, BotClient> entry : bots.entrySet()) {
            entry.getValue().disconnect();
        }
        bots.clear();
    }

    public boolean hasBot(String name) { return bots.containsKey(name); }

    public double[] queryPosition(String name) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return new double[]{client.getX(), client.getY(), client.getZ(), client.getYaw(), client.getPitch()};
    }

    public float[] queryHealth(String name) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return new float[]{client.getHealth(), client.getFood(), client.getSaturation()};
    }

    public int[] queryHeldItem(String name) {
        BotClient client = bots.get(name);
        if (client == null) return null;
        return new int[]{client.getHeldItemSlot()};
    }

    public String queryGameMode(String name) {
        BotClient client = bots.get(name);
        if (client == null) return "unknown";
        return client.getGameMode();
    }

    public int queryOpenContainerId(String name) {
        BotClient client = bots.get(name);
        if (client == null) return -1;
        return client.getOpenContainerId();
    }

    public int[] findEntities(String name, String typeName) {
        BotClient client = bots.get(name);
        if (client == null) return new int[0];
        java.util.List<Integer> ids = client.findEntities(typeName);
        return ids.stream().mapToInt(i -> i).toArray();
    }

    public void moveTo(String name, int x, int y, int z) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.moveTo(x, y, z);
    }

    public void attackEntity(String name, int entityId) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.attackEntity(entityId);
    }

    public void interactEntity(String name, int entityId, int hand) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.interactEntity(entityId, hand);
    }

    public void swingHand(String name, int hand) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.swingHand(hand);
    }

    public void setHeldItemSlot(String name, int slot) {
        BotClient client = bots.get(name);
        if (client == null) return;
        client.setHeldItemSlot(slot);
    }

    public int botCount() { return bots.size(); }
}