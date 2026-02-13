package de.t14d3.rapunzellib.common.attachments;

import de.t14d3.rapunzellib.attachments.AttachmentMapAccess;
import de.t14d3.rapunzellib.attachments.AttachmentStorageSupport;
import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.attachments.RAttachmentScope;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.attachments.NbtAttachmentValueMapper;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public abstract class DefaultAttachmentContainer implements RAttachmentContainer, AttachmentMapAccess {
    private final AttachmentStorageSupport support;
    private final RAttachmentContainer transientAttachments;

    protected DefaultAttachmentContainer() {
        this(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, RAttachmentContainer.lazyMutable());
    }

    protected DefaultAttachmentContainer(@NotNull AttachmentStorageSupport support) {
        this(support, RAttachmentContainer.lazyMutable());
    }

    protected DefaultAttachmentContainer(@NotNull RAttachmentContainer transientAttachments) {
        this(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, transientAttachments);
    }

    protected DefaultAttachmentContainer(
        @NotNull AttachmentStorageSupport support,
        @NotNull RAttachmentContainer transientAttachments
    ) {
        AttachmentStorageSupport resolvedSupport = Objects.requireNonNull(support, "support");
        if (!resolvedSupport.supported() || !resolvedSupport.supportsTransient()) {
            throw new IllegalArgumentException("DefaultAttachmentContainer requires transient attachment support");
        }
        if (!resolvedSupport.supportsPersistent()) {
            throw new IllegalArgumentException("DefaultAttachmentContainer requires persistent attachment support");
        }
        this.support = resolvedSupport;
        this.transientAttachments = Objects.requireNonNull(transientAttachments, "transientAttachments");
    }

    @Override
    public final <T> @NotNull Optional<T> get(@NotNull RAttachmentKey<T> key) {
        Objects.requireNonNull(key, "key");
        if (key.scope() == RAttachmentScope.TRANSIENT) {
            return transientAttachments.get(key);
        }
        PersistentAttachmentSession session = openSession();
        if (session == null) {
            return Optional.empty();
        }
        RNbtValue value = session.load().get(key.id().asString()).orElse(null);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(NbtAttachmentValueMapper.decode(key, value));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public final <T> void put(@NotNull RAttachmentKey<T> key, @NotNull T value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        if (key.scope() == RAttachmentScope.TRANSIENT) {
            transientAttachments.put(key, value);
            return;
        }
        PersistentAttachmentSession session = requireSession();
        RNbtCompound root = session.load().put(key.id().asString(), NbtAttachmentValueMapper.encode(key, value));
        session.save(root);
    }

    @Override
    public final <T> @NotNull Optional<T> remove(@NotNull RAttachmentKey<T> key) {
        Objects.requireNonNull(key, "key");
        if (key.scope() == RAttachmentScope.TRANSIENT) {
            return transientAttachments.remove(key);
        }
        PersistentAttachmentSession session = openSession();
        if (session == null) {
            return Optional.empty();
        }
        RNbtCompound root = session.load();
        Optional<T> existing = get(key);
        session.save(root.remove(key.id().asString()));
        return existing;
    }

    @Override
    public final boolean supports(@NotNull RAttachmentScope scope) {
        Objects.requireNonNull(scope, "scope");
        return scope == RAttachmentScope.TRANSIENT || openSession() != null;
    }

    @Override
    public final @NotNull AttachmentStorageSupport support() {
        return support;
    }

    @Override
    public final @NotNull Map<RAttachmentKey<?>, Object> transientEntries() {
        return transientAttachments instanceof AttachmentMapAccess access ? access.transientEntries() : Map.of();
    }

    protected abstract @Nullable PersistentAttachmentSession openSession();

    protected final @NotNull PersistentAttachmentSession requireSession() {
        PersistentAttachmentSession session = openSession();
        if (session == null) {
            throw new UnsupportedOperationException("attachment scope PERSISTENT is not supported by this container");
        }
        return session;
    }
}
