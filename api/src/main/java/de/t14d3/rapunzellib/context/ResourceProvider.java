package de.t14d3.rapunzellib.context;

import java.io.InputStream;
import java.util.Optional;

@FunctionalInterface
public interface ResourceProvider {
    /**
     * Opens a classpath resource.
     *
     * @param path resource path (e.g. {@code "messages.yml"}).
     * @return input stream, if found. Caller must close it.
     */
    Optional<InputStream> open(String path);
}

