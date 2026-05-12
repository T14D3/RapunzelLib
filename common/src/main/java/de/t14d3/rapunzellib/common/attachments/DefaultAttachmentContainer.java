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

/**
 * Abstract base for attachment containers that support both transient and persistent attachments.
 * <p>
 * Transient attachments are stored in a mutable {@link RAttachmentContainer} delegate.
 * Persistent attachments are loaded and saved via a {@link PersistentAttachmentSession}.
 * Subclasses must implement {@link #openSession()} to provide persistence.
 */
public abstract class DefaultAttachmentContainer implements RAttachmentContainer, AttachmentMapAccess {
    /** Storage support descriptor */
    private final AttachmentStorageSupport support;
    /** Delegate container for transient (non-persistent) attachments */
    private final RAttachmentContainer transientAttachments;

    /**
     * Creates a container with full transient and persistent support and a lazy mutable transient container.
     */
    protected DefaultAttachmentContainer() {
        this(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, RAttachmentContainer.lazyMutable());
    }

    /**
     * Creates a container with the given storage support and a lazy mutable transient container.
     *
     * @param support the attachment storage support descriptor
     */
    protected DefaultAttachmentContainer(@NotNull AttachmentStorageSupport support) {
        this(support, RAttachmentContainer.lazyMutable());
    }

    /**
     * Creates a container with full storage support and the given transient container.
     *
     * @param transientAttachments the delegate for transient attachments
     */
    protected DefaultAttachmentContainer(@NotNull RAttachmentContainer transientAttachments) {
        this(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, transientAttachments);
    }

    /**
     * Creates a container with the given storage support and transient container.
     *
     * @param support              the attachment storage support descriptor
     * @param transientAttachments the delegate for transient attachments
     */
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

    /**
     * Gets the value for a key. Transient keys are read from the in-memory container;
     * persistent keys are loaded from the session's NBT root.
     *
     * @param key the attachment key
     * @param <T> the value type
     * @return an optional containing the value, or empty if not found
     */
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

    /**
     * Sets the value for a key. Transient keys are stored in the in-memory container;
     * persistent keys are encoded to NBT and saved via the session.
     *
     * @param key   the attachment key
     * @param value the value to store
     * @param <T>   the value type
     */
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

    /**
     * Removes the value for a key. Returns the previous value if present.
     *
     * @param key the attachment key
     * @param <T> the value type
     * @return an optional containing the removed value, or empty if not found
     */
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

    /**
     * Checks whether this container supports the given scope.
     *
     * @param scope the attachment scope
     * @return true if the scope is TRANSIENT or if a persistent session is available
     */
    @Override
    public final boolean supports(@NotNull RAttachmentScope scope) {
        Objects.requireNonNull(scope, "scope");
        return scope == RAttachmentScope.TRANSIENT || openSession() != null;
    }

    /**
     * Gets the storage support descriptor.
     *
     * @return the storage support
     */
    @Override
    public final @NotNull AttachmentStorageSupport support() {
        return support;
    }

    /**
     * Returns the transient attachment entries.
     *
     * @return a map of transient attachment keys to values
     */
    @Override
    public final @NotNull Map<RAttachmentKey<?>, Object> transientEntries() {
        return transientAttachments instanceof AttachmentMapAccess access ? access.transientEntries() : Map.of();
    }

    /**
     * Opens a persistence session for loading and saving persistent attachments.
     *
     * @return a session, or null if persistence is not available
     */
    protected abstract @Nullable PersistentAttachmentSession openSession();

    /**
     * Opens a persistence session, throwing if persistence is not available.
     *
     * @return a session
     * @throws UnsupportedOperationException if persistence is not supported
     */
    protected final @NotNull PersistentAttachmentSession requireSession() {
        PersistentAttachmentSession session = openSession();
        if (session == null) {
            throw new UnsupportedOperationException("attachment scope PERSISTENT is not supported by this container");
        }
        return session;
    }
}
