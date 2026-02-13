package de.t14d3.rapunzellib.platform.paper.objects;

import de.t14d3.rapunzellib.attachments.AttachmentStorageSupport;
import de.t14d3.rapunzellib.common.attachments.DefaultAttachmentContainer;
import de.t14d3.rapunzellib.common.attachments.PersistentAttachmentSession;
import de.t14d3.rapunzellib.nbt.RNbtByteArray;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import de.t14d3.rapunzellib.nbt.RNbtPrimitive;
import de.t14d3.rapunzellib.nbt.RNbtValue;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

final class PaperPersistentAttachments extends DefaultAttachmentContainer {
    private static final NamespacedKey ROOT_KEY = NamespacedKey.fromString("rapunzellib:attachments");

    private final Supplier<@Nullable PersistentAttachmentSession> sessionSupplier;

    private PaperPersistentAttachments(
        @NotNull AttachmentStorageSupport support,
        Supplier<@Nullable PersistentAttachmentSession> sessionSupplier
    ) {
        super(support);
        this.sessionSupplier = Objects.requireNonNull(sessionSupplier, "sessionSupplier");
    }

    static @NotNull PaperPersistentAttachments forPlayer(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return new PaperPersistentAttachments(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, () -> {
            Player player = Bukkit.getPlayer(uuid);
            return player == null ? null : new PdcSession(player.getPersistentDataContainer(), () -> {
            });
        });
    }

    static @NotNull PaperPersistentAttachments forEntity(@NotNull UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return new PaperPersistentAttachments(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, () -> {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(uuid);
            return entity == null ? null : new PdcSession(entity.getPersistentDataContainer(), () -> {
            });
        });
    }

    static @NotNull PaperPersistentAttachments forWorld(@NotNull UUID worldUid) {
        Objects.requireNonNull(worldUid, "worldUid");
        return new PaperPersistentAttachments(AttachmentStorageSupport.TRANSIENT_AND_PERSISTENT, () -> {
            World world = Bukkit.getWorld(worldUid);
            return world == null ? null : new PdcSession(world.getPersistentDataContainer(), () -> {
            });
        });
    }

    static @NotNull PaperPersistentAttachments forBlock(@NotNull UUID worldUid, int x, int y, int z) {
        Objects.requireNonNull(worldUid, "worldUid");
        return new PaperPersistentAttachments(AttachmentStorageSupport.TRANSIENT_AND_OPTIONAL_PERSISTENT, () -> {
            World world = Bukkit.getWorld(worldUid);
            if (world == null) {
                return null;
            }
            BlockState state = world.getBlockAt(x, y, z).getState();
            if (!(state instanceof TileState tile)) {
                return null;
            }
            return new PdcSession(tile.getPersistentDataContainer(), () -> tile.update(true, false));
        });
    }

    @Override
    protected @Nullable PersistentAttachmentSession openSession() {
        return sessionSupplier.get();
    }

    private record PdcSession(@NotNull PersistentDataContainer pdc, @NotNull Runnable commit) implements PersistentAttachmentSession {
        private PdcSession {
            Objects.requireNonNull(pdc, "pdc");
            Objects.requireNonNull(commit, "commit");
            if (ROOT_KEY == null) {
                throw new IllegalStateException("Invalid Paper attachment root key");
            }
        }

        @Override
        public @NotNull RNbtCompound load() {
            PersistentDataContainer root = pdc.get(ROOT_KEY, PersistentDataType.TAG_CONTAINER);
            if (root == null || root.getKeys().isEmpty()) {
                return RNbtCompound.empty();
            }
            RNbtCompound out = RNbtCompound.empty();
            for (NamespacedKey key : root.getKeys()) {
                RNbtValue value = read(root, key);
                if (value != null) {
                    out = out.put(key.asString(), value);
                }
            }
            return out;
        }

        @Override
        public void save(@NotNull RNbtCompound root) {
            Objects.requireNonNull(root, "root");
            if (root.isEmpty()) {
                pdc.remove(ROOT_KEY);
                commit.run();
                return;
            }
            PersistentDataContainer nested = pdc.getAdapterContext().newPersistentDataContainer();
            for (Map.Entry<String, RNbtValue> entry : root.asMap().entrySet()) {
                write(nested, toNamespacedKey(entry.getKey()), entry.getValue());
            }
            pdc.set(ROOT_KEY, PersistentDataType.TAG_CONTAINER, nested);
            commit.run();
        }

        private static @Nullable RNbtValue read(@NotNull PersistentDataContainer container, @NotNull NamespacedKey key) {
            byte[] bytes = container.get(key, PersistentDataType.BYTE_ARRAY);
            if (bytes != null) return new RNbtByteArray(bytes);
            Byte byteValue = container.get(key, PersistentDataType.BYTE);
            if (byteValue != null) return RNbtPrimitive.ofByte(byteValue);
            Short shortValue = container.get(key, PersistentDataType.SHORT);
            if (shortValue != null) return RNbtPrimitive.ofShort(shortValue);
            Integer intValue = container.get(key, PersistentDataType.INTEGER);
            if (intValue != null) return RNbtPrimitive.ofInt(intValue);
            Long longValue = container.get(key, PersistentDataType.LONG);
            if (longValue != null) return RNbtPrimitive.ofLong(longValue);
            Float floatValue = container.get(key, PersistentDataType.FLOAT);
            if (floatValue != null) return RNbtPrimitive.ofFloat(floatValue);
            Double doubleValue = container.get(key, PersistentDataType.DOUBLE);
            if (doubleValue != null) return RNbtPrimitive.ofDouble(doubleValue);
            String stringValue = container.get(key, PersistentDataType.STRING);
            return stringValue == null ? null : RNbtPrimitive.ofString(stringValue);
        }

        private static void write(@NotNull PersistentDataContainer container, @NotNull NamespacedKey key, @NotNull RNbtValue value) {
            if (value instanceof RNbtByteArray byteArray) {
                container.set(key, PersistentDataType.BYTE_ARRAY, byteArray.value());
                return;
            }
            if (!(value instanceof RNbtPrimitive primitive)) {
                throw new IllegalArgumentException("Unsupported Paper attachment payload type " + value.type());
            }
            Object raw = primitive.value();
            if (raw instanceof Byte b) {
                container.set(key, PersistentDataType.BYTE, b);
                return;
            }
            if (raw instanceof Short s) {
                container.set(key, PersistentDataType.SHORT, s);
                return;
            }
            if (raw instanceof Integer i) {
                container.set(key, PersistentDataType.INTEGER, i);
                return;
            }
            if (raw instanceof Long l) {
                container.set(key, PersistentDataType.LONG, l);
                return;
            }
            if (raw instanceof Float f) {
                container.set(key, PersistentDataType.FLOAT, f);
                return;
            }
            if (raw instanceof Double d) {
                container.set(key, PersistentDataType.DOUBLE, d);
                return;
            }
            if (raw instanceof String s) {
                container.set(key, PersistentDataType.STRING, s);
                return;
            }
            throw new IllegalArgumentException("Unsupported Paper attachment payload type " + value.type());
        }

        private static @NotNull NamespacedKey toNamespacedKey(@NotNull String value) {
            NamespacedKey key = NamespacedKey.fromString(value);
            if (key == null) {
                throw new IllegalArgumentException("Invalid attachment key: " + value);
            }
            return key;
        }
    }
}
