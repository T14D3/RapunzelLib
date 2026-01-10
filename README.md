# RapunzelLib

RapunzelLib is a Java 21 library for Minecraft plugins/mods that share code across **Paper**, **Velocity**, **Fabric**, **NeoForge**, and **Sponge (Vanilla)**. It provides a small, platform-neutral API plus platform-specific bootstraps and implementations.

## Modules

- `api` - platform-neutral interfaces and value types
- `bom` - Gradle/Maven BOM to keep module versions aligned
- `common` - default context + common implementations (e.g. YAML MiniMessage messages)
- `commands` - Brigadier helpers (e.g. list-argument parsing) + platform-neutral `RCommandSource`
- `commands-paper` - optional Paper adapters (e.g. Bukkit sender → `RCommandSource`)
- `commands-fabric` - optional Fabric adapters (e.g. `CommandSourceStack` → `RCommandSource`)
- `commands-neoforge` - optional NeoForge adapters
- `commands-sponge` - optional Sponge adapters (e.g. Sponge command source → `RCommandSource`)
- `events` - platform-neutral game action events (sync cancellable + sync/async observers)
- `events-paper` - Paper bridge (Bukkit/Paper listeners → Rapunzel events)
- `events-fabric` - Fabric bridge (Fabric callbacks → Rapunzel events)
- `events-neoforge` - NeoForge bridge
- `events-sponge` - Sponge bridge
- `platform-paper` - Paper bootstrap + wrappers + scheduler + plugin-messaging transport
- `platform-velocity` - Velocity bootstrap + wrappers + scheduler + plugin-messaging transport + proxy-side responders
- `platform-fabric` - Fabric bootstrap + wrappers + scheduler (network defaults to in-memory)
- `platform-neoforge` - NeoForge bootstrap + wrappers + scheduler (network defaults to in-memory)
- `platform-sponge` - Sponge bootstrap + wrappers + scheduler (network defaults to in-memory)
- `network` - transport abstraction (`Messenger`), typed event bus, plugin-messaging payloads, Redis Pub/Sub transport, file sync, network info
- `database-spool` - wrapper around `de.t14d3:spool` for simple DB usage + DB-backed network outbox (`DbQueuedMessenger`)
- `gradle-plugin` - `de.t14d3.rapunzellib` Gradle plugin (templates, message validation, multi-server runner integration)
- `tool-server-runner` - CLI used by the Gradle plugin

## Artifacts / Dependencies

This project is published under:

- **Group**: `de.t14d3.rapunzellib`
- **BOM**: `bom`
- **ArtifactIds** (match module names): `api`, `common`, `commands`, `commands-paper`, `commands-fabric`, `commands-neoforge`, `commands-sponge`, `events`, `events-paper`, `events-fabric`, `events-neoforge`, `events-sponge`, `network`, `database-spool`, `platform-paper`, `platform-velocity`, `platform-fabric`, `platform-neoforge`, `platform-sponge`, `tool-server-runner`

Note: jar file names are prefixed `rapunzellib-...`, but Maven artifactIds are the plain module names above.

Repository (as used by this build):

```kotlin
repositories {
  maven("https://maven.t14d3.de/releases")
  maven("https://maven.t14d3.de/snapshots") // for *-SNAPSHOT versions
}
```

Pick the platform module you run on:

```kotlin
dependencies {
  implementation(platform("de.t14d3.rapunzellib:bom:<version>"))
  implementation("de.t14d3.rapunzellib:platform-paper")
  // or: platform-velocity / platform-fabric / platform-neoforge / platform-sponge
}
```

Optional add-ons:

```kotlin
dependencies {
  implementation("de.t14d3.rapunzellib:network")
  implementation("de.t14d3.rapunzellib:database-spool")
}
```

### Shaded artifacts

Most non-`api` modules also publish a **`shaded` classifier** (e.g. `...:<version>:shaded`) which relocates common libraries (notably SnakeYAML and/or Gson) to reduce dependency conflicts.

Fabric also publishes an unremapped dev jar as `:dev-shaded`.

## Gradle plugin (`de.t14d3.rapunzellib`)

The `gradle-plugin` module publishes a Gradle plugin with developer tools:

- `rapunzellibValidateMessages`: validates message key usage in compiled bytecode against your `messages.yml`.
- `rapunzellibRunServers` / `rapunzellibRunPerfServers`: runs a local Velocity + multiple Paper backends using `tool-server-runner`.
- `rapunzellibInitTemplate`: generates a small starter template (messages/config/Example.java).

Example:

```kotlin
plugins {
  id("de.t14d3.rapunzellib") version "<version>"
}

rapunzellib {
  messagesFile.set(layout.projectDirectory.file("src/main/resources/messages.yml"))
  failOnUnusedKeys.set(true)
}
```

More details: `gradle-plugin/README.md`.

## Core API

### Bootstrapping (`RapunzelContext`)

`Rapunzel` is a global holder for exactly one `RapunzelContext`.

The platform bootstraps create a context (`DefaultRapunzelContext`), register default services, and call `Rapunzel.bootstrap(owner, ctx)`.

In shared runtimes where multiple components may call a platform bootstrap, the bootstraps use `Rapunzel.bootstrapOrAcquire(owner, ...)` so only the first call creates the global context; later calls simply acquire a lease for the existing context.

If multiple plugins/mods shade RapunzelLib, which exact version provides the shared classes depends on the platform's classloading behavior and load order; keep your consumers reasonably version-aligned.

**Paper**

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.platform.paper.PaperRapunzelBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
  @Override public void onEnable() {
    PaperRapunzelBootstrap.bootstrap(this);
  }

  @Override public void onDisable() {
    Rapunzel.shutdown(this);
  }
}
```

**Velocity**

```java
import com.velocitypowered.api.proxy.ProxyServer;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.platform.velocity.VelocityRapunzelBootstrap;
import org.slf4j.Logger;

import java.nio.file.Path;

public final class MyPlugin {
  public MyPlugin(ProxyServer proxy, Logger logger, Path dataDir) {
    VelocityRapunzelBootstrap.bootstrap(this, proxy, logger, dataDir);
  }

  public void shutdown() {
    Rapunzel.shutdown(this);
  }
}
```

**Fabric**

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.platform.fabric.FabricRapunzelBootstrap;
import net.minecraft.server.MinecraftServer;

public final class MyModBootstrap {
  private static final String MOD_ID = "my_mod";

  public static void init(MinecraftServer server) {
    FabricRapunzelBootstrap.bootstrap(MOD_ID, server, MyModBootstrap.class);
  }

  public static void shutdown() {
    Rapunzel.shutdown(MOD_ID);
  }
}
```

**Sponge (Vanilla)**

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.platform.sponge.SpongeRapunzelBootstrap;
import org.spongepowered.api.Server;
import org.spongepowered.plugin.PluginContainer;

import java.nio.file.Path;

public final class MySpongePlugin {
  public void startup(PluginContainer container, Server server, Path dataDir) {
    SpongeRapunzelBootstrap.bootstrap(container, dataDir, server);
  }

  public void shutdown() {
    Rapunzel.shutdown(this);
  }
}
```

After bootstrapping:

- `Rapunzel.context()` returns the current `RapunzelContext` (throws if not bootstrapped)
- `Rapunzel.players()`, `Rapunzel.worlds()`, `Rapunzel.blocks()` are convenience shortcuts
- If multiple components share the same runtime, use `Rapunzel.acquire(owner)` / `Rapunzel.shutdown(owner)` to keep the context alive until the last owner releases.

### Service registry

`RapunzelContext.services()` is a minimal `ServiceRegistry` (type → instance).

The provided `DefaultRapunzelContext` supports:

- registering instances (and auto-closing `AutoCloseable` services)
- registering additional `AutoCloseable`s
- closing all closeables in reverse registration order

### Configuration (`ConfigService`, `YamlConfig`)

The default config implementation is `SnakeYamlConfigService` / `SnakeYamlConfig`.

Key properties:

- Loads YAML from disk.
- Optional default resource merge: missing keys **and comments** are merged from a classpath YAML resource.
- Typed getters with coercion for common types (e.g. primitives, `UUID`, `Duration`, enums) plus:
  - `MessageKey`
  - `RBlockPos` from `{x,y,z}` or `"x y z"` / `"x,y,z"`
  - `RWorldRef` from `{name,key}` or `"minecraft:overworld"`
  - `RLocation` from a map (`world`, `x`, `y`, `z`, optional `yaw`/`pitch`)
  - Java records backed by YAML maps
- Reads/writes simple YAML comments (per dotted path).

Example:

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.objects.RBlockPos;

import java.time.Duration;

YamlConfig cfg = Rapunzel.context().configs().load(
  Rapunzel.context().dataDirectory().resolve("config.yml"),
  "config.yml"
);

Duration cooldown = cfg.getDuration("cooldown", Duration.ofSeconds(5));
RBlockPos spawn = cfg.getBlockPos("spawn", new RBlockPos(0, 64, 0));

cfg.set("cooldown", Duration.ofMinutes(2));
cfg.setComment("cooldown", "Accepts 10s/5m/2h/1d or ISO-8601 like PT5M");
cfg.save();
```

### Messages (`MessageFormatService`)

The default message implementation is `YamlMessageFormatService` (module `common`).

- Backed by `messages.yml` (any depth; keys are flattened using dotted paths).
- Values are Adventure **MiniMessage** templates.
- Unknown MiniMessage tags become placeholders (e.g. `<name>`), replaced via `Placeholders`.
- If the key `prefix` exists, it is automatically prepended to all messages.

`messages.yml`:

```yaml
prefix: "<gray>[MyPlugin]</gray> "
example.welcome: "<green>Hello <name>!</green>"
```

Usage:

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.message.Placeholders;
import net.kyori.adventure.text.Component;

Component msg = Rapunzel.context().messages().component(
  "example.welcome",
  Placeholders.builder().string("name", player.name()).build()
);
player.sendMessage(msg);
```

### Wrappers (`RPlayer`, `RWorld`, `RBlock`, …)

Wrapper interfaces live in `api` and provide:

- `platformId()` and `handle()` to access the native platform object
- `extras()` (`RExtras`) as a small per-wrapper key/value store for extensions
- Adventure `Audience` via `RAudience` (players)
- `RPlayer` includes `hasPermission(String)` and optional server-side methods (`world`, `location`, `teleport`)

Support varies by platform:

- **Paper/Fabric**: players, worlds, blocks, location + teleport.
- **Velocity**: players; worlds/blocks are not available.

Note: `RProxyPlayer` and `RServerPlayer` are deprecated in favor of `RPlayer` and its optional methods.

### Scheduling (`Scheduler`)

`Scheduler` exposes:

- `run`, `runAsync`
- `runLater(Duration, ...)`
- `runRepeating(Duration initialDelay, Duration period, ...)`

The platform modules provide implementations that map to the underlying platform scheduler.

## Network APIs

### Transport (`Messenger`)

`Messenger` is the low-level abstraction used by other network features.

Provided implementations include:

- `PaperPluginMessenger` / `VelocityPluginMessenger` (plugin messaging over `rapunzellib:bridge`)
- `RedisPubSubMessenger` (Redis Pub/Sub transport, no external Redis client dependency)
- `InMemoryMessenger` (single-process/testing)

#### Switching to Redis via config (`MessengerTransportBootstrap`)

`network/bootstrap/MessengerTransportBootstrap.java` can replace the currently registered `Messenger` with `RedisPubSubMessenger` based on config (`network.transport=redis`).

```yaml
network:
  transport: redis
  serverName: backend-1           # or env RAPUNZEL_SERVER_NAME
  proxyServerName: velocity       # or env RAPUNZEL_PROXY_SERVER_NAME
  redis:
    host: 127.0.0.1
    port: 6379
    ssl: false
    username: ""
    password: ""
    transportChannel: "rapunzellib:bridge"
```

Call it after platform bootstrap:

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.bootstrap.MessengerTransportBootstrap;

var cfg = Rapunzel.context().configs().load(
  Rapunzel.context().dataDirectory().resolve("config.yml"),
  "config.yml"
);
var result = MessengerTransportBootstrap.bootstrap(cfg, Rapunzel.context().platformId(), Rapunzel.context().logger());

// IMPORTANT: result.closeable() is NOT auto-registered for shutdown.
// Close it yourself, or register it via DefaultRapunzelContext#registerCloseable.
```

### Typed messaging (`NetworkEventBus`)

`NetworkEventBus` builds typed JSON messages (Gson) on top of `Messenger`.

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.NetworkEventBus;

record Ping(String message) {}

Messenger messenger = Rapunzel.context().services().get(Messenger.class);
NetworkEventBus bus = new NetworkEventBus(messenger);

try (NetworkEventBus.Subscription sub =
         bus.register("myplugin:ping", Ping.class, (payload, from) -> {
           Rapunzel.context().logger().info("Ping from {}: {}", from, payload.message());
         })) {
  bus.sendToProxy("myplugin:ping", new Ping("hello"));
}
```

### RPC (`RpcClient`)

`RpcClient` is a small request/response helper on top of `Messenger` (correlation IDs + timeouts + typed results).

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.rpc.RpcClient;

Messenger messenger = Rapunzel.context().services().get(Messenger.class);
RpcClient rpc = new RpcClient(messenger, Rapunzel.context().scheduler(), Rapunzel.context().logger());

rpc.callProxy("myplugin:service", "ping", null, String.class)
   .thenAccept(result -> Rapunzel.context().logger().info("Result: {}", result));
```

### Network info (proxy queries)

`NetworkInfoClient` (backend) can ask the proxy for:

- backend name (`networkServerName()`)
- server list (`servers()`)
- global player list (`players()`)

On Velocity, `VelocityNetworkInfoResponder` is the proxy-side handler.

### File sync (file-level diff over `Messenger`)

`FileSyncEndpoint` and `FileSyncSpec` provide a chunked file sync protocol intended for transports with small message limits (plugin messaging).

Typical use case:

- **Authority** (often proxy) owns the canonical files and broadcasts invalidations.
- **Followers** (backend servers) request sync and apply changed/new files.

### DB-backed outbox (optional)

Plugin messaging has delivery constraints (e.g. requires a player connection). The `database-spool` module provides:

- `DbQueuedMessenger` - wraps a `Messenger`, persists allowlisted channels to a shared DB and retries periodically
- `NetworkQueueConfig` - reads outbox settings from `YamlConfig` (`network.queue.*`)

Platform bootstraps that use plugin-messaging transports (Paper/Fabric/NeoForge/Velocity) call `NetworkQueueBootstrap.wrapIfEnabled(...)` automatically when `network.queue.enabled=true`. To persist queued messages, configure `network.queue.jdbc` (or `database.jdbc`) to point at a JDBC URL (e.g. SQLite).

Config keys (with defaults):

- `network.queue.enabled` (`true`)
- `network.queue.allowlist` (defaults to `["rapunzellib:filesync:invalidate", "db.cache_event"]`)
- `network.queue.flushPeriodSeconds` (`2`)
- `network.queue.maxBatchSize` (`200`)
- `network.queue.maxAgeSeconds` (`300`)

Minimal setup example:

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.database.SpoolDatabase;
import de.t14d3.rapunzellib.network.Messenger;
import de.t14d3.rapunzellib.network.queue.DbQueuedMessenger;
import de.t14d3.rapunzellib.network.queue.NetworkOutboxMessage;
import de.t14d3.rapunzellib.network.queue.NetworkQueueConfig;

var cfg = Rapunzel.context().configs().load(
  Rapunzel.context().dataDirectory().resolve("config.yml"),
  "config.yml"
);

NetworkQueueConfig q = NetworkQueueConfig.read(cfg);
if (q.enabled()) {
  SpoolDatabase db = SpoolDatabase.open(
    "jdbc:sqlite:" + Rapunzel.context().dataDirectory().resolve("outbox.db"),
    Rapunzel.context().logger(),
    NetworkOutboxMessage.class
  );
  Messenger base = Rapunzel.context().services().get(Messenger.class);

  DbQueuedMessenger queued = new DbQueuedMessenger(
    db,
    base,
    Rapunzel.context().scheduler(),
    Rapunzel.context().logger(),
    NetworkQueueConfig.defaultOwnerId(),
    q.channelAllowlist(),
    q.flushPeriod(),
    q.maxBatchSize(),
    q.maxAge()
  );

  // Replace the Messenger service with the queued wrapper.
  Rapunzel.context().services().register(Messenger.class, queued);

  // DbQueuedMessenger only stops its flush task on close; you own the DB lifecycle.
  // Close queued/db on shutdown (or register them as closeables if you use DefaultRapunzelContext).
}
```

## Database

`SpoolDatabase` wraps `de.t14d3.spool.core.EntityManager` and provides:

- a simple synchronized access pattern (`locked`, `transactional`)
- `flush()` and `flushAsync()`
- If you use `DbQueuedMessenger`, register `NetworkOutboxMessage` as an entity.
- You need a JDBC driver on the classpath for your chosen `jdbc:` URL.

## Caveats / Gotchas

- `Rapunzel.context()` throws until you bootstrap; only one global context is supported.
- Plugin messaging transport needs a player connection:
  - Paper: `Messenger.isConnected()` is false with zero online players; sends are dropped.
  - Velocity: forwarding to a backend server requires at least one player currently on that backend (queueing can be enabled via `network.queue.*`).
- Platform differences are explicit:
  - Velocity has no worlds/blocks; some operations throw `UnsupportedOperationException`.
- `YamlMessageFormatService` turns unknown MiniMessage tags into placeholders; avoid naming placeholders after built-in MiniMessage tags.


## Commands helpers (optional)

The `commands` module provides small Brigadier helpers intended for reuse across projects.

- `ListArgumentType<T>` - CommandAPI-style delimiter-based list parsing + suggestions (validated via `getList(...)`)
- `commands-paper` / `commands-fabric` - optional source adapters for Bukkit/Fabric command sources

## Game events (optional)

The `events` modules provide a small, semantic action-event layer (not a Bukkit event mirror).

Install the platform bridge after bootstrapping the context:

**Paper**

```java
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.rapunzellib.platform.paper.PaperRapunzelBootstrap;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {
  @Override public void onEnable() {
    PaperRapunzelBootstrap.bootstrap(this);
    GameEvents.install(this);
  }

  @Override public void onDisable() {
    Rapunzel.shutdown(this);
  }
}
```

Then subscribe to cancellable pre-events and/or async snapshots:

```java
import de.t14d3.rapunzellib.events.GameEvents;
import de.t14d3.rapunzellib.events.block.BlockBreakPre;

GameEvents.bus().onPre(BlockBreakPre.class, e -> {
  if (!e.player().hasPermission("myplugin.break")) e.deny();
});
```

## Gradle plugin (optional)

Plugin id: `de.t14d3.rapunzellib`.

If you consume it from `https://maven.t14d3.de`, add the repository to `settings.gradle(.kts)`:

```kotlin
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven("https://maven.t14d3.de/snapshots")
  }
}
```

Apply:

```kotlin
plugins {
  id("de.t14d3.rapunzellib") version "<version>"
}
```

Provides tasks:

- `rapunzellibValidateMessages` - scans compiled classes for used message keys and compares them to `messages.yml`
- `rapunzellibInitTemplate` - generates a starter project template
- `rapunzellibRunServers` - runs Velocity + multiple Paper backends via the included runner
- `rapunzellibRunPerfServers` - runs Velocity + multiple Paper backends with JFR enabled (and MySQL enabled by default)

Notes:

- `rapunzellibValidateMessages` only detects string-constant keys and is configurable via the `rapunzellib` extension (`messageKeyCallOwners`, `messageKeyCallMethods`, …).
- `rapunzellibRunServers` / `rapunzellibRunPerfServers` download jars via PaperMC Fill v3 and write temporary instances to `run/server-runner/` (MySQL requires Docker).

## Building this repo

```bash
./gradlew build
```

On Windows:

```powershell
./gradlew.bat build
```
