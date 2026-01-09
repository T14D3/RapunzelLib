package de.t14d3.rapunzellib;

/**
 * Best-effort runtime version information for RapunzelLib.
 *
 * <p>In shaded setups, this may report the embedding artifact's manifest version (or {@code unknown}),
 * depending on how the final jar was built.</p>
 */
public final class RapunzelLibVersion {
    private RapunzelLibVersion() {
    }

    public static String current() {
        Package pkg = RapunzelLibVersion.class.getPackage();
        String version = (pkg != null) ? pkg.getImplementationVersion() : null;
        if (version == null || version.isBlank()) return "unknown";
        return version;
    }
}

