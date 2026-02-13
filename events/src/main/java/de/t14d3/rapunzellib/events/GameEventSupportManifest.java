package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record GameEventSupportManifest(
    @NotNull PlatformId platformId,
    @NotNull Map<Class<? extends GameEvent>, GameEventSupport> supportByEvent
) {
    public GameEventSupportManifest {
        Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(supportByEvent, "supportByEvent");
        supportByEvent = Map.copyOf(supportByEvent);
    }

    public static @NotNull GameEventSupportManifest empty(@NotNull PlatformId platformId) {
        return builder(platformId).build();
    }

    public static @NotNull Builder builder(@NotNull PlatformId platformId) {
        return new Builder(platformId);
    }

    public @NotNull Builder toBuilder() {
        return builder(platformId).include(this);
    }

    public @NotNull GameEventSupportManifest overlay(@NotNull GameEventSupportManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        if (platformId != manifest.platformId()) {
            throw new IllegalArgumentException(
                "Cannot overlay support manifest for " + manifest.platformId() + " onto " + platformId
            );
        }
        return toBuilder().include(manifest).build();
    }

    public @NotNull GameEventSupportManifest overlayUnsupported(@NotNull GameEventSupportManifest manifest) {
        Objects.requireNonNull(manifest, "manifest");
        if (platformId != manifest.platformId()) {
            throw new IllegalArgumentException(
                "Cannot overlay support manifest for " + manifest.platformId() + " onto " + platformId
            );
        }
        return toBuilder().includeUnsupported(manifest).build();
    }

    public @NotNull GameEventSupport support(@NotNull Class<? extends GameEvent> eventType) {
        Objects.requireNonNull(eventType, "eventType");
        return supportByEvent.getOrDefault(eventType, GameEventSupport.unsupported(eventType));
    }

    public boolean supports(@NotNull Class<? extends GameEvent> eventType) {
        return support(eventType).supported();
    }

    public static final class Builder {
        private final PlatformId platformId;
        private final Map<Class<? extends GameEvent>, GameEventSupport> supportByEvent = new LinkedHashMap<>();

        private Builder(@NotNull PlatformId platformId) {
            this.platformId = Objects.requireNonNull(platformId, "platformId");
        }

        @SafeVarargs
        public final @NotNull Builder nativeSupport(
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            return support(GameEventSupportParity.NATIVE, details, eventTypes);
        }

        @SafeVarargs
        public final @NotNull Builder emulatedSupport(
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            return support(GameEventSupportParity.EMULATED, details, eventTypes);
        }

        @SafeVarargs
        public final @NotNull Builder partialSupport(
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            return support(GameEventSupportParity.PARTIAL, details, eventTypes);
        }

        @SafeVarargs
        public final @NotNull Builder support(
            @NotNull GameEventSupportParity parity,
            @NotNull String details,
            @NotNull Class<? extends GameEvent>... eventTypes
        ) {
            Objects.requireNonNull(parity, "parity");
            Objects.requireNonNull(details, "details");
            Objects.requireNonNull(eventTypes, "eventTypes");
            for (Class<? extends GameEvent> eventType : eventTypes) {
                supportByEvent.put(eventType, new GameEventSupport(eventType, parity, details));
            }
            return this;
        }

        public @NotNull Builder include(@NotNull GameEventSupportManifest manifest) {
            Objects.requireNonNull(manifest, "manifest");
            if (platformId != manifest.platformId()) {
                throw new IllegalArgumentException(
                    "Cannot include support manifest for " + manifest.platformId() + " into " + platformId
                );
            }
            for (GameEventSupport support : manifest.supportByEvent.values()) {
                if (support.supported()) {
                    supportByEvent.put(support.eventType(), support);
                }
            }
            return this;
        }

        public @NotNull Builder includeUnsupported(@NotNull GameEventSupportManifest manifest) {
            Objects.requireNonNull(manifest, "manifest");
            if (platformId != manifest.platformId()) {
                throw new IllegalArgumentException(
                    "Cannot include support manifest for " + manifest.platformId() + " into " + platformId
                );
            }
            for (GameEventSupport support : manifest.supportByEvent.values()) {
                if (!support.supported()) {
                    continue;
                }
                GameEventSupport existing = supportByEvent.get(support.eventType());
                if (existing == null || !existing.supported()) {
                    supportByEvent.put(support.eventType(), support);
                }
            }
            return this;
        }

        public @NotNull GameEventSupportManifest build() {
            Map<Class<? extends GameEvent>, GameEventSupport> resolved = new LinkedHashMap<>();
            for (Class<? extends GameEvent> eventType : GameEventCatalog.sharedEventTypes()) {
                resolved.put(eventType, supportByEvent.getOrDefault(eventType, GameEventSupport.unsupported(eventType)));
            }
            return new GameEventSupportManifest(platformId, resolved);
        }
    }
}
