# RapunzelLib

RapunzelLib is a Java 21 library for Minecraft plugins and mods that want one shared codebase across Paper, Velocity, Fabric, NeoForge, and Sponge. It gives each consumer its own context for config, messages, logging, scheduling, and lifecycle, while keeping shared runtime state for wrapper identity, attachments, registries, and messaging. Optional feature families cover commands, events, GUI, inventory, NBT, and networking.

## Why use it instead of platform APIs directly?

- Use one mental model for bootstrap, config, messages, scheduling, wrappers, and registries across multiple runtimes.
- Keep consumer-owned state isolated while sharing the state that must be global, such as player wrapper caches and attachment storage.
- Keep cross-platform code in shared modules, while thin platform adapters stay in `platform-*` and `*-<platform>` artifacts.
- Add only the feature families you need instead of committing to a giant all-in-one framework.
- Still drop to native APIs when needed through wrapper handles and native interop.

Direct platform APIs are still the simpler choice when you only target one runtime and want deep platform-specific behavior everywhere. RapunzelLib helps most when you are trying to keep shared code honest across more than one platform.


## Minimal dependency setup

RapunzelLib is published under group `de.t14d3.rapunzellib`.

For stable releases, the releases repository is enough:

```kotlin
repositories {
    mavenCentral()
    maven("https://maven.t14d3.de/releases")
}

dependencies {
    implementation(platform("de.t14d3.rapunzellib:bom:<version>"))
    implementation("de.t14d3.rapunzellib:platform-paper")
    // or: platform-velocity / platform-fabric / platform-neoforge / platform-sponge
}
```

If you use a `-SNAPSHOT` version, also add `maven("https://maven.t14d3.de/snapshots")`.

That gets you the core bootstrap path. `platform-*` modules create a consumer-owned `RapunzelContext`, register config/messages loading, scheduler, wrappers, registries, attachments, and default networking for that runtime. Shared wrapper and attachment identity is backed by the process-wide Rapunzel runtime rather than by sharing one consumer context. Internally that base wiring flows through `PlatformFeatures.install()`, so the platform service registration path is consistent with the optional feature families.

Add optional modules only when you compile against those APIs directly. For most applications, prefer the single platform-specific coordinate because it already brings the base feature module transitively:

- Commands: `commands-paper`, `commands-fabric`, `commands-neoforge`, or `commands-sponge`
- Events: `events-paper`, `events-fabric`, `events-neoforge`, or `events-sponge`
- GUI: `gui-paper`, `gui-fabric`, `gui-neoforge`, or `gui-sponge`
- Inventory: `inventory-paper`, `inventory-fabric`, `inventory-neoforge`, or `inventory-sponge`
- NBT: `nbt-paper`, `nbt-fabric`, `nbt-neoforge`, or `nbt-sponge`
- Networking APIs: `network`
- DB-backed queueing and simple DB wrapper: `database-spool`

Add the base feature modules (`commands`, `events`, `gui`, `inventory`, `nbt`) separately only when you have advanced shared-code modules that compile against the public API without also owning a concrete platform adapter dependency.

In most consumer projects you do not need to depend on `common`, `platform-shared`, or any `*-shared` module directly.

## Bootstrap from the native runtime entrypoint

Call the platform bootstrap once your runtime gives you its real startup objects. Store the returned `RapunzelContext` on your plugin/mod instance and use that object as your primary entrypoint:

```java
public final class MyPlugin extends JavaPlugin {
    private RapunzelContext rapunzel;

    @Override
    public void onEnable() {
        this.rapunzel = PaperRapunzelBootstrap.bootstrap(this);
    }

    public RapunzelContext rapunzel() {
        return rapunzel;
    }

    @Override
    public void onDisable() {
        Rapunzel.shutdown(this);
    }
}
```

Then pass the context into your own services, or access it through your plugin instance:

```java
public final class MyFeature {
    private final RapunzelContext rapunzel;

    public MyFeature(RapunzelContext rapunzel) {
        this.rapunzel = rapunzel;
    }

    public void reloadText() {
        rapunzel.messages().reload();
    }
}
```

Platform entrypoints:

- Paper: call `PaperRapunzelBootstrap.bootstrap(plugin)` in `onEnable()` and `Rapunzel.shutdown(plugin)` in `onDisable()`.
- Velocity: call `VelocityRapunzelBootstrap.bootstrap(plugin, proxy, logger, dataDirectory)` once those injected objects are available.
- Fabric: call `FabricRapunzelBootstrap.bootstrap(modId, server, resourceAnchor)` from a server lifecycle hook, not from the early mod initializer before a `MinecraftServer` exists. If the project also applies the `de.t14d3.rapunzellib` Gradle plugin with Loom, RapunzelLib auto-bundles its Fabric startup companion mod.
- NeoForge: call `NeoForgeRapunzelBootstrap.bootstrap(modId, server, logger, dataDirectory, resourceAnchor)` once you have the running server and data directory. If the project also applies the `de.t14d3.rapunzellib` Gradle plugin with NeoForge ModDev, RapunzelLib auto-bundles its NeoForge startup companion mod.
- Sponge: call `SpongeRapunzelBootstrap.bootstrap(container, dataDirectory, server)` from plugin startup wiring once the container and server are available.

`resourceAnchor` should be a class from your own jar so RapunzelLib can load bundled defaults such as `config.yml` and `messages.yml`.

If you want an explicit lifecycle object, every platform bootstrap also exposes `bootstrapHandle(...)`; keep the `BootstrapHandle` and call `handle.close()` during shutdown.

## Contexts, runtime, and static access

RapunzelLib separates consumer-owned context state from shared runtime state:

- `RapunzelContext` is per consumer. It owns `dataDirectory()`, `logger()`, `configs()`, `messages()`, `scheduler()`, and the consumer service registry.
- `RapunzelRuntime` is shared by the running RapunzelLib instance. It owns shared wrapper/accessor services, registry bridges, native interop, attachment stores/services, and the in-memory messenger.

This means Plugin A and Plugin B get separate contexts, but the same player UUID can still resolve to the same shared wrapper instance and attachment container underneath.

Prefer explicit context access in application code:

```java
RapunzelContext ctx = plugin.rapunzel();
RPlayer player = ctx.players().require(nativePlayer);
String prefix = ctx.messages().raw("prefix");
```

Static access still exists, but it is intentionally conservative:

- If exactly one context exists, `Rapunzel.context()` and helpers such as `Rapunzel.players()` use it for compatibility.
- If multiple contexts exist, static access requires a scoped context; otherwise it throws instead of guessing the wrong plugin.
- Scheduled tasks submitted through `context.scheduler()` are scoped automatically.

Use `Rapunzel.withContext(...)` only when you are adapting a callback that cannot easily receive your context directly:

```java
Rapunzel.withContext(rapunzel, () -> {
    Rapunzel.players().require(nativePlayer);
});
```

For new code, `ctx.players()` is preferred over `Rapunzel.players()`.

## What the platform module buys you

- Bootstraps a `RapunzelContext`
- Registers YAML config and MiniMessage-backed messages
- Registers scheduler, wrapper accessors, registries, and attachment support
- Sets up the default network transport for that runtime

After bootstrap you can use your stored `RapunzelContext` for `players()`, `entities()`, `worlds()`, `blocks()`, `registries()`, and `attachments()`.

Services that do not have dedicated top-level accessors are available through `context.services()`, for example `context.services().get(Messenger.class)`.

Optional feature families expose their own lazy-installing entrypoints after bootstrap:

- `CommandFeatures.commands()`
- `EventFeatures.bus()` (or `GameEvents.bus()` for the legacy alias)
- `GuiFeatures.renderer()`
- `InventoryFeatures.inventories()`
- `NbtFeatures.itemStacks()`

`NbtFeatures.install()` also registers the platform item-stack adapters in the service registry, and the inventory / GUI platform adapters resolve those shared item services instead of constructing their own private adapter paths.

## Honest tradeoffs

- There is no single global consumer context; consumers should store/pass their own `RapunzelContext`.
- Static `Rapunzel.context()` is a compatibility and scoped-callback convenience, not the primary ownership model.
- Platform differences are not hidden completely; for example Velocity is a proxy runtime and does not expose worlds, blocks, GUI, inventory, or game events.
- Plugin messaging is convenient but delivery can depend on connected players; Redis is optional when you need a stronger transport.
- Some ecosystem dependencies are snapshots or betas because the Minecraft tooling stack still depends on them.
