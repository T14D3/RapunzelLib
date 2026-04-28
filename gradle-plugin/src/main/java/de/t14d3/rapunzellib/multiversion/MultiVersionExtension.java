package de.t14d3.rapunzellib.multiversion;

import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

public interface MultiVersionExtension {
    ListProperty<String> getTargetVersions();
    
    Property<String> getCoreVersion();
    
    Property<Boolean> getEnabled();
}
