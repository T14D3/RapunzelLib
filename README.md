# RapunzelLib

RapunzelLib is a Java 25 library for building Minecraft plugins and mods that run across **Paper, Fabric, NeoForge, Sponge, and Velocity** from a single shared codebase. It wraps platform-native APIs behind a unified data model - RPlayer, REntity, RBlock, RWorld, and registries - so your business logic never touches platform-specific code.

A companion mod or plugin (shipped with each `platform-*` artifact) owns the global context. Your plugin borrows a consumer view from it, gaining access to config, messages, logging, scheduling, attachments, and whatever feature families you opt into. The runtime layer keeps wrapper identity and attachment storage shared and consistent regardless of which plugin requests a player - same UUID always resolves to the same RPlayer instance.

## Quick start

Published under `de.t14d3.rapunzellib`. Use the BOM for version alignment:

```kotlin
// build.gradle.kts
repositories {
    mavenCentral()
    maven("https://maven.t14d3.de/releases")
}

dependencies {
    implementation(platform("de.t14d3.rapunzellib:bom:<version>"))
    implementation("de.t14d3.rapunzellib:platform-paper")
}
```

Substitute `platform-paper` with the platform you need: `platform-fabric`, `platform-neoforge`, `platform-sponge`, or `platform-velocity`. Add the snapshots repository if you are using a `-SNAPSHOT` version.

## Bootstrap

Each `platform-*` artifact bundles a standalone companion mod or plugin. Install that on your server first - it owns the single global `RapunzelContext`. Your plugin then borrows a consumer view:

**Paper** - install `RapunzelLibPaper.jar` on your server, then call `acquire()`:

```java
public final class MyPlugin extends JavaPlugin {
    private BootstrapHandle handle;

    @Override
    public void onEnable() {
        handle = PaperRapunzelBootstrap.acquire(this);
    }

    @Override
    public void onDisable() {
        handle.close();
    }
}
```

**Fabric** - the companion mod is auto-bundled:

```java
BootstrapHandle handle = FabricRapunzelBootstrap.acquire("my-mod", server, MyMod.class);
```

**NeoForge, Sponge, Velocity** - same pattern with platform-specific parameters:

```java
NeoForgeRapunzelBootstrap.acquire("my-mod", server, logger, dataDir, MyMod.class);
SpongeRapunzelBootstrap.acquire(container, dataDir, server);
VelocityRapunzelBootstrap.acquire(plugin, proxy, logger, dataDir);
```

The `Class<?>` argument is a resource anchor - RapunzelLib loads `config.yml` and `messages.yml` from that class's jar.

## Using the context

After bootstrap, everything flows through `Rapunzel.context()`:

```java
RapunzelContext ctx = Rapunzel.context();
RPlayer player = ctx.players().require(nativePlayer);
String prefix = ctx.messages().raw("prefix");
ctx.scheduler().runAsync(() -> doWork(player));
```

The context provides: `players()`, `entities()`, `worlds()`, `blocks()`, `registries()`, `configs()`, `messages()`, `scheduler()`, `attachments()`, `nativeInterop()`, `services()`, and capability checks via `supports()` / `requireCapability()`.

### Generated project wrapper (recommended)

The Gradle plugin generates a `<ProjectName>Rapunzel` class that wraps all context access in static calls - no manual context threading needed across your shared codebase:

```java
// The plugin rewrites bootstrap calls to init the wrapper automatically
PaperRapunzelBootstrap.acquire(this);

// Anywhere in your code:
RPlayer player = MyProjectRapunzel.players().require(nativePlayer);
```

See the [Gradle plugin docs](gradle-plugin/README.md) for the build configuration.

## Feature families

Each feature family is an optional add-on. Depend on the platform-specific artifact and call the installer. Features transitively pull in the base module:

```kotlin
implementation("de.t14d3.rapunzellib:commands-paper")
implementation("de.t14d3.rapunzellib:events-paper")
implementation("de.t14d3.rapunzellib:gui-paper")
implementation("de.t14d3.rapunzellib:inventory-paper")
implementation("de.t14d3.rapunzellib:nbt-paper")
implementation("de.t14d3.rapunzellib:visuals-paper")
implementation("de.t14d3.rapunzellib:livetest-paper")
```

| Feature | Description | Installer |
|---|---|---|
| **Commands** | Cross-platform Brigadier dispatcher | `CommandFeatures.commands().registerRoot("cmd", commandNode)` |
| **Events** | Unified game event bus | `GameEvents.bus().onPre(BlockBreakPre.class, handler)` |
| **GUI** | Inventory-based UI framework (buttons, sliders, dialogs, pagination) | `GuiFeatures.renderer()` |
| **Inventory** | Cross-platform inventory wrapping | `InventoryFeatures.inventories().wrap(nativeInventory)` |
| **NBT** | NBT codec, serialization, item-stack adapters | `NbtFeatures.itemStacks().find(stackHandle)` |
| **Visuals** | Beacon beams, block displays, particles, glow outlines | `VisualFeatures.visuals().manager()` |
| **LiveTest** | Bot-based integration testing via MCProtocolLib | `LiveTestFeatures.install().host().runTest(new MyTest())` |

GUI depends on events, inventory, and NBT - installing it auto-installs those three. Visuals is independent.

**Event consumption convention:** one event family per semantic action (`Pre` = deny-able, `Post` = informational; snapshots are async-fire-and-forget). Payloads are the discriminated union of consumer needs - shared RLib types only, never platform handles. The catalog in the events module is the single source of truth; extend the catalog instead of mirroring Bukkit/Fabric/Forge events one-to-one. Each platform manifest declares its parity for every catalog entry.

**Network and database-spool** work differently. The platform bootstrap registers an in-memory `Messenger` into the service registry. For Redis-backed multi-node messaging, add:

```kotlin
implementation("de.t14d3.rapunzellib:network")
implementation("de.t14d3.rapunzellib:database-spool") // message queues via de.t14d3:spool
```

Then fetch them from the service registry: `ctx.services().get(Messenger.class)`.

## Capability checks

Write runtime-aware code with capability introspection:

```java
if (ctx.supports(RuntimeCapability.WORLDS)) {
    RWorld world = ctx.worlds().get(/* world key */);
} else {
    // Velocity - proxy only, no worlds
}
```

## Platform support

| | Paper | Fabric | NeoForge | Sponge | Velocity |
|---|---|---|---|---|---|
| Commands | ✅ | ✅ | ✅ | ✅ | ✅ |
| Events | ✅ | ✅ | ✅ | ✅ | ✅ |
| GUI | ✅ | ✅ | ✅ | ✅ | - |
| Inventory | ✅ | ✅ | ✅ | ✅ | - |
| NBT | ✅ | ✅ | ✅ | ✅ | ✅ |
| Visuals | ✅ | ✅ | ✅ | ✅ | ✅ |

Velocity is a proxy - worlds, blocks, GUI, and inventory are unavailable.

## Architecture at a glance

```
api/                          Public API contracts + static facade
common/                       Shared implementations (config, apps, attachments)
platform-{shared,*}/          Platform adapters and companion mods/plugins
{feature}/*                    Feature base API
{feature}-shared/             Shared feature logic
{feature}-{platform}/         Platform-specific feature wiring
gradle-plugin/                Build software (code gen, dev runner, sidebar-validation)
bom/                          BOM
```

The `*_shared` modules (`platform-shared`, `commands-shared`, `events-shared`, etc.) are internal. Do not depend on them directly - depend on `platform-*` or feature-platform modules.

## Gradle Plugin

```kotlin
plugins {
    id("de.t14d3.rapunzellib") version "<version>"
}
```

Provides message validation, generated sources (registry catalogs, NBT schema, BlockDisplay metadata), a multi-server dev runner, and project scaffold templates. Details in the [gradle-plugin README](gradle-plugin/README.md).

## Building from source

Requires Java 25 and Gradle 9.4+.

```bash
git clone https://github.com/t14d3/RapunzelLib.git
cd RapunzelLib

# Build all configured Minecraft targets:
./gradlew build

# Or build for a specific target:
./gradlew build -Prapunzellib.minecraftTarget=26.1.2

# Run tests:
./gradlew test

# Generate documentation:
./gradlew javadoc
```

Target versions are configured in `gradle/minecraft-targets.properties`. Build artifacts land in `build/libs/<version>/`.

### Versioned artifacts

RapunzelLib publishes fabric-style multi-version artifacts. **Every** Minecraft target appends a `+mc<target>` suffix so all targets can coexist in the same Maven repository. There is no plain version:

| Minecraft target | Artifact version |
|---|---|
| 26.2 (core/default) | `0.3.1+mc26.2` / `0.3.1-SNAPSHOT+mc26.2` |
| 26.1.2 | `0.3.1+mc26.1.2` |
| 1.21.11 | `0.3.1+mc1.21.11` |
| 1.21.10 | `0.3.1+mc1.21.10` |

The MC-independent Gradle plugin artifact (`de.t14d3.rapunzellib.gradle:gradle-plugin`) keeps its own plain version. Publishing a specific target requires an explicit target: `./gradlew build publishToCentralPortal -Prapunzellib.minecraftTarget=1.21.11`. The default build produces the core (26.2) versioned artifacts.

## Trade-offs

- There is exactly one global context, bootstrapped by the companion plugin. Consumers borrow a view - do not call `bootstrap()` unless you are the companion.
- Velocity is a proxy ∼ no worlds, blocks, GUI, or inventory. Use capability cheats at Runtime.
- Some ecosystem dependencies are snapshots because the Minecraft tooling stack still depends on new snapshot options.
- The `api/` module includes generated sources listing vanilla item types, block types, and entity types. These are written at build time and checked in.