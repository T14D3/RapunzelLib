package de.t14d3.rapunzellib.buildlogic;

import java.util.List;
import java.util.Set;

public record PlatformAdapterScaffoldSpec(
    String basePackageName,
    String platformKey,
    String platformPackageSegment,
    String platformClassPrefix,
    List<String> featureKeys,
    String sharedCoreFamily,
    Set<String> sharedCoreFeatures
) {
    public String packagePath() {
        return basePackageName.replace('.', '/');
    }
}
