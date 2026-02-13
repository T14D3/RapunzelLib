package de.t14d3.rapunzellib.nbt;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RNbtPath<T> implements Serializable {
    private final @NotNull List<Segment> segments;
    private final @NotNull RNbtCodec<T> codec;

    private RNbtPath(@NotNull List<Segment> segments, @NotNull RNbtCodec<T> codec) {
        this.segments = List.copyOf(Objects.requireNonNull(segments, "segments"));
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    public static <T> @NotNull RNbtPath<T> of(@NotNull RNbtCodec<T> codec) {
        return new RNbtPath<>(List.of(), codec);
    }

    public static <T> @NotNull RNbtPath<T> of(@NotNull RNbtCodec<T> codec, @NotNull String key) {
        return of(codec).key(key);
    }

    public @NotNull RNbtCodec<T> codec() {
        return codec;
    }

    public @NotNull List<Segment> segments() {
        return segments;
    }

    public @NotNull RNbtPath<T> key(@NotNull String key) {
        ArrayList<Segment> updated = new ArrayList<>(segments);
        updated.add(new KeySegment(Objects.requireNonNull(key, "key")));
        return new RNbtPath<>(updated, codec);
    }

    public @NotNull RNbtPath<T> index(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be >= 0");
        }
        ArrayList<Segment> updated = new ArrayList<>(segments);
        updated.add(new IndexSegment(index));
        return new RNbtPath<>(updated, codec);
    }

    public @NotNull Optional<T> read(@NotNull RNbtCompound root) {
        return resolve(Objects.requireNonNull(root, "root")).map(codec::decode);
    }

    public boolean exists(@NotNull RNbtCompound root) {
        return resolve(Objects.requireNonNull(root, "root")).isPresent();
    }

    public @NotNull RNbtCompound write(@NotNull RNbtCompound root, @NotNull T value) {
        RNbtValue encoded = codec.encode(Objects.requireNonNull(value, "value"));
        if (segments.isEmpty()) {
            if (encoded instanceof RNbtCompound compound) {
                return compound;
            }
            throw new IllegalStateException("Root path can only write compounds, got " + encoded.type());
        }
        RNbtValue updated = writeValue(Objects.requireNonNull(root, "root"), 0, encoded);
        return updated.asCompound();
    }

    public @NotNull RNbtCompound remove(@NotNull RNbtCompound root) {
        if (segments.isEmpty()) {
            return Objects.requireNonNull(root, "root");
        }
        RNbtValue updated = removeValue(Objects.requireNonNull(root, "root"), 0);
        return updated.asCompound();
    }

    private @NotNull Optional<RNbtValue> resolve(@NotNull RNbtCompound root) {
        RNbtValue current = root;
        for (Segment segment : segments) {
            if (segment instanceof KeySegment keySegment) {
                if (!(current instanceof RNbtCompound compound)) {
                    return Optional.empty();
                }
                current = compound.get(keySegment.key()).orElse(null);
            } else if (segment instanceof IndexSegment indexSegment) {
                if (!(current instanceof RNbtList list)) {
                    return Optional.empty();
                }
                current = list.get(indexSegment.index()).orElse(null);
            } else {
                throw new IllegalStateException("Unknown path segment: " + segment);
            }
            if (current == null) {
                return Optional.empty();
            }
        }
        return Optional.of(current);
    }

    private @NotNull RNbtValue writeValue(RNbtValue current, int segmentIndex, @NotNull RNbtValue leafValue) {
        Segment segment = segments.get(segmentIndex);
        boolean last = segmentIndex == segments.size() - 1;
        if (segment instanceof KeySegment keySegment) {
            RNbtCompound compound = current instanceof RNbtCompound nbtCompound ? nbtCompound : RNbtCompound.empty();
            if (last) {
                return compound.put(keySegment.key(), leafValue);
            }
            RNbtValue child = compound.get(keySegment.key()).orElse(null);
            RNbtValue updatedChild = writeValue(child, segmentIndex + 1, leafValue);
            return compound.put(keySegment.key(), updatedChild);
        }
        IndexSegment indexSegment = (IndexSegment) segment;
        RNbtList list = current instanceof RNbtList nbtList ? nbtList : RNbtList.empty();
        if (indexSegment.index() > list.size()) {
            throw new IllegalArgumentException("Cannot write path " + this + ": missing list element at index " + indexSegment.index());
        }
        if (last) {
            return indexSegment.index() == list.size()
                ? list.add(leafValue)
                : list.set(indexSegment.index(), leafValue);
        }
        RNbtValue child = indexSegment.index() < list.size() ? list.getOrThrow(indexSegment.index()) : nextContainer(segmentIndex + 1);
        RNbtValue updatedChild = writeValue(child, segmentIndex + 1, leafValue);
        return indexSegment.index() == list.size()
            ? list.add(updatedChild)
            : list.set(indexSegment.index(), updatedChild);
    }

    private @NotNull RNbtValue removeValue(@NotNull RNbtValue current, int segmentIndex) {
        Segment segment = segments.get(segmentIndex);
        boolean last = segmentIndex == segments.size() - 1;
        if (segment instanceof KeySegment keySegment) {
            if (!(current instanceof RNbtCompound compound)) {
                return current;
            }
            if (last) {
                return compound.remove(keySegment.key());
            }
            RNbtValue child = compound.get(keySegment.key()).orElse(null);
            if (child == null) {
                return current;
            }
            RNbtValue updatedChild = removeValue(child, segmentIndex + 1);
            if (updatedChild == child) {
                return current;
            }
            return updatedChild.type() == RNbtType.COMPOUND && updatedChild.asCompound().isEmpty()
                ? compound.remove(keySegment.key())
                : updatedChild.type() == RNbtType.LIST && updatedChild.asList().isEmpty()
                    ? compound.remove(keySegment.key())
                    : compound.put(keySegment.key(), updatedChild);
        }
        if (!(current instanceof RNbtList list)) {
            return current;
        }
        IndexSegment indexSegment = (IndexSegment) segment;
        if (indexSegment.index() >= list.size()) {
            return current;
        }
        if (last) {
            return list.remove(indexSegment.index());
        }
        RNbtValue child = list.getOrThrow(indexSegment.index());
        RNbtValue updatedChild = removeValue(child, segmentIndex + 1);
        if (updatedChild == child) {
            return current;
        }
        return updatedChild.type() == RNbtType.COMPOUND && updatedChild.asCompound().isEmpty()
            ? list.remove(indexSegment.index())
            : updatedChild.type() == RNbtType.LIST && updatedChild.asList().isEmpty()
                ? list.remove(indexSegment.index())
                : list.set(indexSegment.index(), updatedChild);
    }

    private @NotNull RNbtValue nextContainer(int segmentIndex) {
        return segments.get(segmentIndex) instanceof KeySegment ? RNbtCompound.empty() : RNbtList.empty();
    }

    @Override
    public String toString() {
        return segments.toString();
    }

    public sealed interface Segment permits KeySegment, IndexSegment {
    }

    public record KeySegment(@NotNull String key) implements Segment {
        public KeySegment {
            Objects.requireNonNull(key, "key");
        }
    }

    public record IndexSegment(int index) implements Segment {
        public IndexSegment {
            if (index < 0) {
                throw new IllegalArgumentException("Index must be >= 0");
            }
        }
    }
}
