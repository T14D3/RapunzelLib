package de.t14d3.rapunzellib.common.attachments;

import de.t14d3.rapunzellib.attachments.RAttachmentContainer;
import de.t14d3.rapunzellib.attachments.RAttachmentKey;
import de.t14d3.rapunzellib.attachments.RAttachmentScope;
import de.t14d3.rapunzellib.nbt.RNbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultAttachmentContainerTest {
    @Test
    void storesTransientAndPersistentAttachmentsThroughSingleContainer() {
        TestContainer container = new TestContainer();
        RAttachmentKey<String> transientKey = RAttachmentKey.transientKey("test:transient", String.class);
        RAttachmentKey<Integer> persistentKey = RAttachmentKey.persistent("test:persistent", Integer.class);
        RAttachmentKey<byte[]> codedKey = RAttachmentKey.persistent("test:bytes", byte[].class);

        container.put(transientKey, "value");
        container.put(persistentKey, 7);
        container.put(codedKey, new byte[] {1, 2, 3});

        assertEquals("value", container.get(transientKey).orElseThrow());
        assertEquals(7, container.get(persistentKey).orElseThrow());
        assertArrayEquals(new byte[] {1, 2, 3}, container.get(codedKey).orElseThrow());
        assertTrue(container.supports(RAttachmentScope.PERSISTENT));
        assertFalse(container.transientEntries().isEmpty());
    }

    private static final class TestContainer extends DefaultAttachmentContainer {
        private RNbtCompound root = RNbtCompound.empty();

        @Override
        protected PersistentAttachmentSession openSession() {
            return PersistentAttachmentSession.of(() -> root, updated -> TestContainer.this.root = updated);
        }
    }
}
