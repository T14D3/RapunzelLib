package de.t14d3.rapunzellib.events;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.events.block.BlockBreakPost;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;
import de.t14d3.rapunzellib.events.player.InteractBlockPre;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public final class TestGameEventBridgeInstaller implements GameEventBridgeInstaller {
    static final GameEventSupportManifest MANIFEST = GameEventSupportManifest.builder(PlatformId.PAPER)
        .nativeSupport("test-native", BlockBreakPre.class, BlockBreakPost.class)
        .emulatedSupport("test-emulated", InteractBlockPre.class)
        .build();

    private static final AtomicInteger INSTALL_CALLS = new AtomicInteger();

    static void reset() {
        INSTALL_CALLS.set(0);
    }

    static int installCalls() {
        return INSTALL_CALLS.get();
    }

    @Override
    public @NotNull PlatformId platformId() {
        return PlatformId.PAPER;
    }

    @Override
    public @NotNull GameEventSupportManifest supportManifest() {
        return MANIFEST;
    }

    @Override
    public @NotNull GameEventBridge install(@NotNull RapunzelContext context, @NotNull GameEventBus bus) {
        INSTALL_CALLS.incrementAndGet();
        return () -> {
        };
    }
}
