package de.t14d3.rapunzellib.devrunner.platform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class PlatformRegistry {
    private static final PlatformRegistry INSTANCE = new PlatformRegistry();
    private final Map<String, PlatformAdapter> adapters = new LinkedHashMap<>();

    private PlatformRegistry() {
        ServiceLoader<PlatformAdapter> loader = ServiceLoader.load(PlatformAdapter.class);
        for (PlatformAdapter adapter : loader) {
            adapters.put(adapter.key(), adapter);
        }
    }

    public static PlatformRegistry getInstance() {
        return INSTANCE;
    }

    public PlatformAdapter get(String key) {
        PlatformAdapter adapter = adapters.get(key);
        if (adapter == null) {
            throw new IllegalArgumentException(
                "Unknown platform: '" + key + "'. Available: " + adapters.keySet()
            );
        }
        return adapter;
    }

    public boolean isRegistered(String key) {
        return adapters.containsKey(key);
    }

    public Map<String, PlatformAdapter> all() {
        return Map.copyOf(adapters);
    }
}
