package de.t14d3.rapunzellib.platform.sponge.objects;

import de.t14d3.rapunzellib.objects.RKey;
import de.t14d3.rapunzellib.common.objects.CachedWrapperStore;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.Worlds;
import de.t14d3.rapunzellib.platform.sponge.attachments.SpongeAttachmentService;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.Collection;
import java.util.Optional;

public final class SpongeWorlds extends CachedWrapperStore<String, ServerWorld, SpongeWorld> implements Worlds {
    private final SpongeAttachmentService attachmentService;

    public SpongeWorlds(@NotNull SpongeAttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @Override
    public @NotNull Collection<RWorld> all() {
        if (!Sponge.isServerAvailable()) return java.util.List.of();
        return Sponge.server().worldManager().worlds().stream().map(this::wrapInternal).map(RWorld.class::cast).toList();
    }

    @Override
    public @NotNull Optional<RWorld> getByName(@NotNull String name) {
        if (name == null || name.isBlank() || !Sponge.isServerAvailable()) return Optional.empty();
        String target = name.trim();
        return Sponge.server().worldManager().worlds().stream()
            .filter(w -> w.properties().name().equalsIgnoreCase(target))
            .findFirst()
            .map(this::wrapInternal)
            .map(RWorld.class::cast);
    }

    @Override
    public @NotNull Optional<RWorld> get(@NotNull RKey key) {
        if (!Sponge.isServerAvailable()) return Optional.empty();
        return Sponge.server().worldManager().world(org.spongepowered.api.ResourceKey.resolve(key.asString()))
            .map(this::wrapInternal)
            .map(RWorld.class::cast);
    }

    @Override
    public @NotNull Optional<RWorld> wrap(@NotNull Object nativeWorld) {
        return adaptNativeWorld(nativeWorld).flatMap(this::wrapNative).map(RWorld.class::cast);
    }

    public @NotNull Optional<SpongeWorld> wrapNative(@NotNull ServerWorld world) {
        return Optional.of(wrapInternal(world));
    }

    public @NotNull SpongeWorld requireNative(@NotNull ServerWorld world) {
        return wrapInternal(world);
    }

    protected @NotNull Optional<? extends ServerWorld> adaptNativeWorld(@NotNull Object nativeWorld) {
        if (!(nativeWorld instanceof ServerWorld world)) return Optional.empty();
        return Optional.of(world);
    }

    @Override
    protected @NotNull SpongeWorld createWrapper(@NotNull ServerWorld nativeHandle) {
        return new SpongeWorld(nativeHandle, attachmentService);
    }

    @Override
    protected void updateWrapper(@NotNull SpongeWorld existingWrapper, @NotNull ServerWorld nativeHandle) {
        existingWrapper.updateHandle(nativeHandle);
    }

    private @NotNull SpongeWorld wrapInternal(@NotNull ServerWorld world) {
        return wrapCached(world.key().asString(), world);
    }
}
