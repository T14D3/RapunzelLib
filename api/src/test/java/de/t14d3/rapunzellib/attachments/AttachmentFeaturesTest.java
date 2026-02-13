package de.t14d3.rapunzellib.attachments;

import de.t14d3.rapunzellib.PlatformId;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.attachments.fixtures.TestAttachmentFeatureInstallers;
import de.t14d3.rapunzellib.common.attachments.DefaultAttachmentContainer;
import de.t14d3.rapunzellib.common.attachments.PersistentAttachmentSession;
import de.t14d3.rapunzellib.context.RapunzelContext;
import de.t14d3.rapunzellib.context.ResourceProvider;
import de.t14d3.rapunzellib.context.ServiceRegistry;
import de.t14d3.rapunzellib.objects.REntity;
import de.t14d3.rapunzellib.objects.RLocation;
import de.t14d3.rapunzellib.objects.RNativeHandle;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorld;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.rapunzellib.objects.block.RBlock;
import de.t14d3.rapunzellib.objects.block.RBlockData;
import de.t14d3.rapunzellib.registry.RBlockType;
import de.t14d3.rapunzellib.registry.REntityType;
import de.t14d3.rapunzellib.registry.RRegistryRef;
import de.t14d3.rapunzellib.runtime.EngineFamily;
import de.t14d3.rapunzellib.runtime.LifecycleOwner;
import de.t14d3.rapunzellib.runtime.PlatformRuntime;
import de.t14d3.rapunzellib.runtime.RuntimeCapability;
import de.t14d3.rapunzellib.runtime.RuntimeRole;
import de.t14d3.rapunzellib.scheduler.ScheduledTask;
import de.t14d3.rapunzellib.scheduler.Scheduler;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AttachmentFeaturesTest {
    @BeforeEach
    void setUp() {
        Rapunzel.shutdownAll();
        TestAttachmentFeatureInstallers.reset();
    }

    @AfterEach
    void tearDown() {
        Rapunzel.shutdownAll();
    }

    @Test
    void resolvesInstallerByPlatformFromServiceLoader(@TempDir Path dir) {
        TestContext context = new TestContext(serverRuntime(PlatformId.PAPER), dir);
        Rapunzel.bootstrap(this, context);

        AttachmentSupport support = AttachmentFeatures.install();

        assertEquals(1, TestAttachmentFeatureInstallers.paperInstallCalls());
        assertEquals(0, TestAttachmentFeatureInstallers.velocityInstallCalls());
        assertSame(support, context.services().get(AttachmentSupport.class));
        assertEquals(PlatformId.PAPER, AttachmentFeatures.support().platformId());
    }

    @Test
    void rejectsUnsupportedRuntimeBeforeResolvingInstaller(@TempDir Path dir) {
        TestContext context = new TestContext(proxyRuntime(PlatformId.VELOCITY), dir);
        Rapunzel.bootstrap(this, context);

        IllegalStateException ex = assertThrows(
            IllegalStateException.class,
            AttachmentFeatures::install
        );

        assertEquals(0, TestAttachmentFeatureInstallers.paperInstallCalls());
        assertEquals(0, TestAttachmentFeatureInstallers.velocityInstallCalls());
        assertEquals(
            "RapunzelLib attachment features requires capability ATTACHMENTS but runtime VELOCITY is PROXY / PROXY",
            ex.getMessage()
        );
    }

    @Test
    void attachmentConvenienceAccessorsDelegateToInstalledSupport(@TempDir Path dir) {
        TestContext context = new TestContext(serverRuntime(PlatformId.PAPER), dir);
        Rapunzel.bootstrap(this, context);

        TestPlayer player = new TestPlayer(PlatformId.PAPER, new InMemoryPersistentAttachments());

        assertSame(player.attachments(), AttachmentFeatures.attachments(player));
        assertSame(player.attachments(), Rapunzel.attachments(player));
        assertSame(player.attachments(), context.attachments(player));
        assertTrue(AttachmentFeatures.supports(player));
        assertTrue(Rapunzel.supportsAttachments(player));
        assertTrue(context.supportsAttachments(player));
        assertSame(player, AttachmentFeatures.requireSupported(player));
        assertSame(player, Rapunzel.requireAttachmentSupport(player));
        assertSame(player, context.requireAttachmentSupport(player));
        assertEquals(1, TestAttachmentFeatureInstallers.paperInstallCalls());
    }

    @Test
    void missingInstallerHintPointsToPlatformModule(@TempDir Path dir) {
        TestContext context = new TestContext(serverRuntime(PlatformId.FABRIC), dir);
        Rapunzel.bootstrap(this, context);

        java.lang.IllegalStateException ex = assertThrows(java.lang.IllegalStateException.class, AttachmentFeatures::install);

        assertTrue(ex.getMessage().contains("rapunzellib-platform-fabric"));
        assertFalse(ex.getMessage().contains("rapunzellib-attachments-fabric"));
        assertTrue(ex.getMessage().contains("Available installer platforms: PAPER, VELOCITY."));
    }

    @Test
    void supportClassifiesTargetsAndDelegatesToWrapperAttachments(@TempDir Path dir) {
        TestContext context = new TestContext(serverRuntime(PlatformId.PAPER), dir);
        Rapunzel.bootstrap(this, context);
        AttachmentFeatures.install();

        AttachmentSupport support = AttachmentFeatures.support();
        TestPlayer persistentPlayer = new TestPlayer(PlatformId.PAPER, new InMemoryPersistentAttachments());
        TestPlayer transientPlayer = new TestPlayer(PlatformId.PAPER, RAttachmentContainer.mutable());
        TestEntity entity = new TestEntity(PlatformId.PAPER, RAttachmentContainer.mutable());
        TestWorld world = new TestWorld(PlatformId.PAPER, RAttachmentContainer.mutable());
        TestBlock block = new TestBlock(PlatformId.PAPER, world, RAttachmentContainer.mutable());
        TestBlock optionalBlock = new TestBlock(PlatformId.PAPER, world, new OptionalPersistentAttachments(true));
        UnsupportedNative other = new UnsupportedNative(PlatformId.PAPER);

        assertEquals(Optional.of(AttachmentTargetType.PLAYER), support.classify(persistentPlayer));
        assertEquals(Optional.of(AttachmentTargetType.ENTITY), support.classify(entity));
        assertEquals(Optional.of(AttachmentTargetType.WORLD), support.classify(world));
        assertEquals(Optional.of(AttachmentTargetType.BLOCK), support.classify(block));
        assertEquals(AttachmentStorageSupport.TRANSIENT_AND_OPTIONAL_PERSISTENT, support.targetSupport(AttachmentTargetType.BLOCK));
        assertEquals(AttachmentStorageSupport.TRANSIENT_AND_OPTIONAL_PERSISTENT, support.declaredSupport(block));
        assertEquals(AttachmentStorageSupport.TRANSIENT_ONLY, support.effectiveSupport(block));
        assertEquals(AttachmentStorageSupport.TRANSIENT_AND_OPTIONAL_PERSISTENT, support.effectiveSupport(optionalBlock));
        assertSame(persistentPlayer.attachments(), support.attachments(persistentPlayer));
        assertTrue(support.attachments(persistentPlayer).supports(RAttachmentScope.PERSISTENT));

        RAttachmentKey<String> transientKey = RAttachmentKey.transientKey("test:transient", String.class);
        support.attachments(persistentPlayer).put(transientKey, "value");
        assertEquals("value", persistentPlayer.attachments().get(transientKey).orElseThrow());

        RAttachmentKey<String> persistentKey = RAttachmentKey.persistent("test:persistent", String.class);
        support.attachments(persistentPlayer).put(persistentKey, "stored");
        assertEquals("stored", persistentPlayer.attachments().get(persistentKey).orElseThrow());

        assertFalse(support.attachments(transientPlayer).supports(RAttachmentScope.PERSISTENT));

        assertFalse(support.supports(other));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> support.attachments(other));
        assertEquals(
            "Unsupported attachment target " + UnsupportedNative.class.getName() + " for platform PAPER",
            ex.getMessage()
        );
    }

    private static PlatformRuntime serverRuntime(PlatformId platformId) {
        return runtime(platformId, RuntimeRole.SERVER, EngineFamily.MOJANG_SERVER, RuntimeCapability.ATTACHMENTS);
    }

    private static PlatformRuntime proxyRuntime(PlatformId platformId) {
        return runtime(platformId, RuntimeRole.PROXY, EngineFamily.PROXY);
    }

    private static PlatformRuntime runtime(
        PlatformId platformId,
        RuntimeRole role,
        EngineFamily engineFamily,
        RuntimeCapability... capabilities
    ) {
        EnumSet<RuntimeCapability> capabilitySet = EnumSet.noneOf(RuntimeCapability.class);
        for (RuntimeCapability capability : capabilities) {
            capabilitySet.add(capability);
        }
        return new PlatformRuntime(platformId, role, engineFamily, capabilitySet, new LifecycleOwner(new Object()));
    }

    private static final class TestContext implements RapunzelContext {
        private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

        private final PlatformRuntime runtime;
        private final Path dataDirectory;
        private final ServiceRegistry services = new MapServiceRegistry();
        private final Scheduler scheduler = new InlineScheduler();

        private TestContext(PlatformRuntime runtime, Path dataDirectory) {
            this.runtime = runtime;
            this.dataDirectory = dataDirectory;
        }

        @Override
        public @NotNull PlatformRuntime runtime() {
            return runtime;
        }

        @Override
        public @NotNull Logger logger() {
            return LOGGER;
        }

        @Override
        public @NotNull Path dataDirectory() {
            return dataDirectory;
        }

        @Override
        public @NotNull ResourceProvider resources() {
            return _path -> Optional.empty();
        }

        @Override
        public @NotNull Scheduler scheduler() {
            return scheduler;
        }

        @Override
        public @NotNull ServiceRegistry services() {
            return services;
        }
    }

    private static final class TestPlayer extends RNativeHandle<Object> implements RPlayer {
        private final UUID uuid = UUID.randomUUID();

        private TestPlayer(PlatformId platformId, RAttachmentContainer attachments) {
            super(platformId, new Object(), attachments);
        }

        @Override
        public @NotNull Audience audience() {
            return Audience.empty();
        }

        @Override
        public @NotNull UUID uuid() {
            return uuid;
        }

        @Override
        public @NotNull String name() {
            return "test-player";
        }

        @Override
        public boolean hasPermission(@NotNull String permission) {
            return false;
        }
    }

    private static final class TestWorld extends RNativeHandle<Object> implements RWorld {
        private TestWorld(PlatformId platformId, RAttachmentContainer attachments) {
            super(platformId, new Object(), attachments);
        }

        @Override
        public @NotNull RWorldRef ref() {
            return new RWorldRef("test-world", "test:world");
        }
    }

    private static final class TestEntity extends RNativeHandle<Object> implements REntity {
        private final UUID uuid = UUID.randomUUID();

        private TestEntity(PlatformId platformId, RAttachmentContainer attachments) {
            super(platformId, new Object(), attachments);
        }

        @Override
        public @NotNull UUID uuid() {
            return uuid;
        }

        @Override
        public @NotNull RRegistryRef<REntityType> typeRef() {
            return REntityType.ref("test:entity");
        }

        @Override
        public @NotNull Optional<RWorld> world() {
            return Optional.of(new TestWorld(PlatformId.PAPER, RAttachmentContainer.mutable()));
        }

        @Override
        public @NotNull Optional<RLocation> location() {
            return Optional.of(new RLocation(new RWorldRef("test-world", "test:world"), 1.0, 2.0, 3.0, 0.0f, 0.0f));
        }
    }

    private static final class TestBlock extends RNativeHandle<Object> implements RBlock {
        private final RWorld world;

        private TestBlock(PlatformId platformId, RWorld world, RAttachmentContainer attachments) {
            super(platformId, new Object(), attachments);
            this.world = world;
        }

        @Override
        public @NotNull RWorld world() {
            return world;
        }

        @Override
        public @NotNull de.t14d3.rapunzellib.objects.RBlockPos pos() {
            return new de.t14d3.rapunzellib.objects.RBlockPos(1, 2, 3);
        }

        @Override
        public @NotNull RRegistryRef<RBlockType> typeRef() {
            return RBlockType.ref("test:block");
        }

        @Override
        public @NotNull RBlockData data() {
            throw new UnsupportedOperationException("not needed");
        }
    }

    private static final class UnsupportedNative extends RNativeHandle<Object> {
        private UnsupportedNative(PlatformId platformId) {
            super(platformId, new Object());
        }
    }

    private static final class InMemoryPersistentAttachments extends DefaultAttachmentContainer {
        private final RNbtCompoundSession session = new RNbtCompoundSession();

        @Override
        protected @NotNull PersistentAttachmentSession openSession() {
            return session;
        }
    }

    private static final class OptionalPersistentAttachments extends DefaultAttachmentContainer {
        private final boolean sessionAvailable;
        private final RNbtCompoundSession session = new RNbtCompoundSession();

        private OptionalPersistentAttachments(boolean sessionAvailable) {
            super(AttachmentStorageSupport.TRANSIENT_AND_OPTIONAL_PERSISTENT);
            this.sessionAvailable = sessionAvailable;
        }

        @Override
        protected PersistentAttachmentSession openSession() {
            return sessionAvailable ? session : null;
        }
    }

    private static final class RNbtCompoundSession implements PersistentAttachmentSession {
        private volatile de.t14d3.rapunzellib.nbt.RNbtCompound root = de.t14d3.rapunzellib.nbt.RNbtCompound.empty();

        @Override
        public @NotNull de.t14d3.rapunzellib.nbt.RNbtCompound load() {
            return root;
        }

        @Override
        public void save(@NotNull de.t14d3.rapunzellib.nbt.RNbtCompound root) {
            this.root = root;
        }
    }

    private static final class MapServiceRegistry implements ServiceRegistry {
        private final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

        @Override
        public <T> void register(@NotNull Class<T> type, @NotNull T instance) {
            services.put(type, instance);
        }

        @Override
        public <T> @NotNull Optional<T> find(@NotNull Class<T> type) {
            Object instance = services.get(type);
            if (instance == null) {
                return Optional.empty();
            }
            return Optional.of(type.cast(instance));
        }

        @Override
        public @NotNull List<Class<?>> serviceTypes() {
            return services.keySet().stream().toList();
        }

        @Override
        public @NotNull List<Object> services() {
            return services.values().stream().toList();
        }
    }

    private static final class InlineScheduler implements Scheduler {
        @Override
        public @NotNull ScheduledTask run(@NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runAsync(@NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runLater(@NotNull Duration delay, @NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runRepeating(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }

        @Override
        public @NotNull ScheduledTask runRepeatingAsync(@NotNull Duration initialDelay, @NotNull Duration period, @NotNull Runnable task) {
            task.run();
            return new NoopTask();
        }
    }

    private static final class NoopTask implements ScheduledTask {
        @Override
        public void cancel() {
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
