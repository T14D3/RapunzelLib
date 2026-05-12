package de.t14d3.rapunzellib.attachments;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RNative;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.block.RBlock;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Defines the attachment support capabilities for a platform, including which target
 * types are supported and the storage mechanisms available.
 */
public final class AttachmentSupport {
    private final PlatformId platformId;
    private final Map<AttachmentTargetType, AttachmentStorageSupport> targetSupport;
    private final AttachmentStorageSupport itemSupport;

    private AttachmentSupport(
        @NotNull PlatformId platformId,
        @NotNull Map<AttachmentTargetType, AttachmentStorageSupport> targetSupport,
        @NotNull AttachmentStorageSupport itemSupport
    ) {
        this.platformId = Objects.requireNonNull(platformId, "platformId");
        Objects.requireNonNull(targetSupport, "targetSupport");
        this.itemSupport = Objects.requireNonNull(itemSupport, "itemSupport");

        EnumMap<AttachmentTargetType, AttachmentStorageSupport> resolved = new EnumMap<>(AttachmentTargetType.class);
        for (AttachmentTargetType targetType : AttachmentTargetType.values()) {
            resolved.put(targetType, targetSupport.getOrDefault(targetType, AttachmentStorageSupport.UNSUPPORTED));
        }
        this.targetSupport = Collections.unmodifiableMap(resolved);
    }

    public static @NotNull AttachmentSupport empty(@NotNull PlatformId platformId) {
        return builder(platformId).build();
    }

    public static @NotNull Builder builder(@NotNull PlatformId platformId) {
        return new Builder(platformId);
    }

    public @NotNull PlatformId platformId() {
        return platformId;
    }

    public @NotNull Set<AttachmentTargetType> supportedTargetTypes() {
        EnumSet<AttachmentTargetType> supported = EnumSet.noneOf(AttachmentTargetType.class);
        for (Map.Entry<AttachmentTargetType, AttachmentStorageSupport> entry : targetSupport.entrySet()) {
            if (entry.getValue().supported()) {
                supported.add(entry.getKey());
            }
        }
        return Collections.unmodifiableSet(supported);
    }

    public @NotNull AttachmentStorageSupport targetSupport(@NotNull AttachmentTargetType targetType) {
        Objects.requireNonNull(targetType, "targetType");
        return targetSupport.getOrDefault(targetType, AttachmentStorageSupport.UNSUPPORTED);
    }

    public @NotNull AttachmentStorageSupport itemSupport() {
        return itemSupport;
    }

    public boolean supports(@NotNull RNative target) {
        return effectiveSupport(target).supported();
    }

    public @NotNull AttachmentStorageSupport declaredSupport(@NotNull RNative target) {
        return classify(target).map(this::targetSupport).orElse(AttachmentStorageSupport.UNSUPPORTED);
    }

    public @NotNull AttachmentStorageSupport effectiveSupport(@NotNull RNative target) {
        Objects.requireNonNull(target, "target");
        AttachmentStorageSupport declaredSupport = declaredSupport(target);
        if (!declaredSupport.supported()) {
            return AttachmentStorageSupport.UNSUPPORTED;
        }
        return declaredSupport.intersect(target.attachments().support());
    }

    public @NotNull Optional<AttachmentTargetType> classify(@NotNull RNative target) {
        Objects.requireNonNull(target, "target");

        if (target.platformId() != platformId) {
            return Optional.empty();
        }

        if (targetSupport(AttachmentTargetType.PLAYER).supported() && target instanceof RPlayer) {
            return Optional.of(AttachmentTargetType.PLAYER);
        }
        if (targetSupport(AttachmentTargetType.ENTITY).supported() && target instanceof REntity) {
            return Optional.of(AttachmentTargetType.ENTITY);
        }
        if (targetSupport(AttachmentTargetType.WORLD).supported() && target instanceof RWorld) {
            return Optional.of(AttachmentTargetType.WORLD);
        }
        if (targetSupport(AttachmentTargetType.BLOCK).supported() && target instanceof RBlock) {
            return Optional.of(AttachmentTargetType.BLOCK);
        }
        return Optional.empty();
    }

    public <T extends RNative> @NotNull RAttachmentContainer attachments(@NotNull T target) {
        Objects.requireNonNull(target, "target");
        requireSupported(target);
        return target.attachments();
    }

    public <T extends RNative> @NotNull T requireSupported(@NotNull T target) {
        Objects.requireNonNull(target, "target");
        classify(target).orElseThrow(() -> new IllegalArgumentException(
            "Unsupported attachment target " + target.getClass().getName() + " for platform " + platformId
        ));
        return target;
    }

    public static final class Builder {
        private final PlatformId platformId;
        private final EnumMap<AttachmentTargetType, AttachmentStorageSupport> targetSupport = new EnumMap<>(AttachmentTargetType.class);
        private AttachmentStorageSupport itemSupport = AttachmentStorageSupport.UNSUPPORTED;

        private Builder(@NotNull PlatformId platformId) {
            this.platformId = Objects.requireNonNull(platformId, "platformId");
        }

        public @NotNull Builder itemSupport(@NotNull AttachmentStorageSupport itemSupport) {
            AttachmentStorageSupport resolved = Objects.requireNonNull(itemSupport, "itemSupport");
            if (resolved.supportsTransient()) {
                throw new IllegalArgumentException("Item attachment support cannot expose transient storage");
            }
            this.itemSupport = resolved;
            return this;
        }

        public @NotNull Builder persistentItems() {
            return itemSupport(AttachmentStorageSupport.PERSISTENT_ONLY);
        }

        public @NotNull Builder support(
            @NotNull AttachmentStorageSupport support,
            @NotNull AttachmentTargetType... targetTypes
        ) {
            AttachmentStorageSupport resolvedSupport = Objects.requireNonNull(support, "support");
            if (!resolvedSupport.supportsTransient() && resolvedSupport.supported()) {
                throw new IllegalArgumentException("Live attachment targets must expose transient storage");
            }

            Objects.requireNonNull(targetTypes, "targetTypes");
            for (AttachmentTargetType targetType : targetTypes) {
                targetSupport.put(Objects.requireNonNull(targetType, "targetType"), resolvedSupport);
            }
            return this;
        }

        public @NotNull Builder transientOnly(@NotNull AttachmentTargetType... targetTypes) {
            return support(AttachmentStorageSupport.TRANSIENT_ONLY, targetTypes);
        }

        public @NotNull Builder persistent(@NotNull AttachmentTargetType... targetTypes) {
            return support(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, targetTypes);
        }

        public @NotNull Builder optionalPersistent(@NotNull AttachmentTargetType... targetTypes) {
            return support(AttachmentStorageSupport.TRANSIENT_AND_OPTIONAL_PERSISTENT, targetTypes);
        }

        public @NotNull AttachmentSupport build() {
            return new AttachmentSupport(platformId, targetSupport, itemSupport);
        }
    }
}
