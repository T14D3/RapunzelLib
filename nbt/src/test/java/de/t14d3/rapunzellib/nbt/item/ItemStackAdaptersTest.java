package de.t14d3.rapunzellib.nbt.item;

import de.t14d3.rapunzellib.PlatformId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ItemStackAdaptersTest {
    @Test
    void resolvesAdaptersByRegisteredHandleTypeAndInstance() {
        DefaultItemStackAdapters adapters = new DefaultItemStackAdapters(PlatformId.PAPER);
        TestAdapter adapter = new TestAdapter();
        adapters.register(BaseHandle.class, adapter);

        assertSame(adapter, adapters.require(BaseHandle.class));
        assertSame(adapter, adapters.require(DerivedHandle.class));
        assertSame(adapter, adapters.require(new DerivedHandle()));
    }

    @Test
    void reportsRegisteredHandleTypesWhenLookupFails() {
        DefaultItemStackAdapters adapters = new DefaultItemStackAdapters(PlatformId.SPONGE);
        adapters.register(BaseHandle.class, new TestAdapter());

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> adapters.require(UnrelatedHandle.class)
        );

        assertTrue(error.getMessage().contains(PlatformId.SPONGE.name()));
        assertTrue(error.getMessage().contains(BaseHandle.class.getName()));
    }

    private static class BaseHandle {
    }

    private static final class DerivedHandle extends BaseHandle {
    }

    private static final class UnrelatedHandle {
    }

    private static final class TestAdapter implements ItemStackAdapter<BaseHandle> {
        @Override
        public @NotNull RItem snapshot(@NotNull BaseHandle nativeItem) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull BaseHandle create(@NotNull RItem item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull BaseHandle apply(@NotNull BaseHandle nativeItem, @NotNull RItem item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean supports(@Nullable Object object) {
            return object instanceof BaseHandle;
        }
    }
}
