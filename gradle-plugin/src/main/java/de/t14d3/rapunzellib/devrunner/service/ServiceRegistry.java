package de.t14d3.rapunzellib.devrunner.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class ServiceRegistry {
    private static final ServiceRegistry INSTANCE = new ServiceRegistry();
    private final Map<String, ServiceAdapter> adapters = new LinkedHashMap<>();

    private ServiceRegistry() {
        ServiceLoader<ServiceAdapter> loader = ServiceLoader.load(ServiceAdapter.class);
        for (ServiceAdapter adapter : loader) {
            adapters.put(adapter.key(), adapter);
        }
    }

    public static ServiceRegistry getInstance() {
        return INSTANCE;
    }

    public ServiceAdapter get(String key) {
        ServiceAdapter adapter = adapters.get(key);
        if (adapter == null) {
            throw new IllegalArgumentException(
                "Unknown service type: '" + key + "'. Available: " + adapters.keySet()
            );
        }
        return adapter;
    }

    public boolean isRegistered(String key) {
        return adapters.containsKey(key);
    }

    public Map<String, ServiceAdapter> all() {
        return Map.copyOf(adapters);
    }
}
