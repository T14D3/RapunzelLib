# RapunzelLib Gradle plugin (`de.t14d3.rapunzellib`)

This module publishes a Gradle plugin that provides developer tools for RapunzelLib consumers:

- `rapunzellibValidateMessages`: validates that message keys used in compiled bytecode exist in your `messages.yml`.
- `rapunzellibRunServers` / `rapunzellibRunPerfServers`: runs a local Velocity + multiple Paper backends via the plugin's built-in server runner.
- `rapunzellibGenerateKeyCatalog`: generates Java constant catalogs from explicit namespaced-key inputs.
- `rapunzellibGenerateRegistryCatalogs`: generates typed Java registry catalogs from configured native registry sources.
- `rapunzellibVerifyRegistryCatalogParity`: explicitly verifies configured registry catalogs against parity sources.
- `rapunzellibInitTemplate`: generates a multi-module starter template (`template/`).
- `rapunzellibGeneratePlatformAdapterScaffold`: generates scaffold modules for new platform adapters.
- `rapunzellibVerifyInstallerWiring`: verifies `META-INF/services` installer descriptors for detected RapunzelLib feature/platform modules.
- `rapunzellibVerifySharedParity`: validates Fabric/NeoForge Minecraft + mappings parity assumptions.

When the consuming project also applies Fabric Loom or NeoForge ModDev and depends on `platform-fabric` or `platform-neoforge`, the plugin also auto-wires RapunzelLib's startup companion mods:

- Fabric: adds `platform-fabric` and `platform-fabric:<version>:companion` to Loom `include`, and keeps the companion on `localRuntime` for dev runs.
- NeoForge: adds `platform-neoforge` and `platform-neoforge:<version>:companion` to the main `jarJar` configuration, and keeps the companion on a private dev-runtime classpath extension.

## Apply the plugin

The plugin is resolved through Gradle's plugin management repositories, so put the repository setup in `settings.gradle.kts`.

Stable versions:

```kotlin
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.t14d3.de/releases")
  }
}
```

If you use a `-SNAPSHOT` plugin version, keep `releases` and add `snapshots`:

```kotlin
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.t14d3.de/releases")
    maven("https://maven.t14d3.de/snapshots")
  }
}
```

Then apply the plugin in the consuming build script:

```kotlin
plugins {
  id("de.t14d3.rapunzellib") version "<version>"
}
```

If the same project also consumes RapunzelLib libraries, add the matching `releases` or `releases` + `snapshots` repository under your normal dependency repositories too. `pluginManagement.repositories` only resolves the Gradle plugin itself.

## `rapunzellib { ... }` extension

```kotlin
rapunzellib {
  // Message validation
  messagesFile.set(layout.projectDirectory.file("src/main/resources/messages.yml"))
  additionalMessagesFiles.add(layout.projectDirectory.file("src/main/resources/messages-extra.yml"))
  failOnUnusedKeys.set(true)
  alwaysUsedKeys.add("prefix")

  // Bytecode scan settings (advanced)
  messageKeyCallOwners.add("com/example/MyMessageKeyHolder")
  messageKeyCallMethods.addAll("getMessage", "getRaw")
  messageKeyPrefix.set("")

  // Template generator
  templateOutputDir.set(layout.projectDirectory.dir("template"))
  templateBasePackage.set("com.example")
  templateProjectName.set(project.name)

  // Platform adapter scaffold generator
  scaffoldOutputDir.set(layout.projectDirectory.dir("platform-adapter-scaffold"))
  scaffoldBasePackage.set("de.t14d3.rapunzellib")
  scaffoldPlatformKey.set("custom")
  scaffoldFeatures.set(setOf("commands", "events", "gui", "nbt"))

  // Key catalog generator
  keyCatalog {
    inputFiles.from(layout.projectDirectory.file("src/main/rapunzellib/keys.txt"))
    packageName.set("com.example.keys")
    className.set("BlockKeys")
    domainName.set("blocks")
    outputDir.set(layout.buildDirectory.dir("generated/sources/rapunzellib/keyCatalog/main/java"))
  }
}
```

### Bytecode scan notes

`rapunzellibValidateMessages` scans `classesDirs` for invocations of configured call sites:

- Default method names: `getMessage`, `getRaw`
- Default prefix: `""`
- Optional extra owners/methods can be added if your project wraps RapunzelLib's message API.

## Tasks

### `rapunzellibValidateMessages`

Common usage:

- `./gradlew rapunzellibValidateMessages`
- `./gradlew check` (you can wire it in via `check.dependsOn("rapunzellibValidateMessages")`)

Failure modes:

- Invalid YAML: task fails with a `GradleException` pointing at the file.
- No message files configured: task fails with a message listing `rapunzellib.messagesFile` and `rapunzellib.additionalMessagesFiles`.

### `rapunzellibRunServers` / `rapunzellibRunPerfServers`

Both tasks use the same `RunServersTask` implementation, but they are optimized for different feedback loops:

- `rapunzellibRunServers`: everyday manual debugging; starts the stack and keeps it running until one server exits.
- `rapunzellibRunPerfServers`: same stack, but always adds `--jfr` so each server writes recordings under `instances/<name>/jfr/`.

Shared Gradle properties for `rapunzellibRunServers` and `rapunzellibRunPerfServers` (easy to override per-machine / CI):

- `multiPaperVersion` (default: `1.21.10`)
- `multiPaperCount` (default: `2`)
- `multiPaperBasePort` (default: `25566`)
- `multiVelocityEnabled` (default: `true`)
- `multiVelocityVersion` (default: `latest`)
- `multiVelocityPort` (default: `25565`)
- `multiRunnerJava` (default: `""` -> use runner default)
- `multiRunnerJvmArgs` (CSV, e.g. `-Xmx2G,-XX:+AlwaysPreTouch`)

Perf task additions:

- `multiJfr=true` or use `rapunzellibRunPerfServers` (enables JFR)
- `multiJfrSettings=<name>` (optional)
- `rapunzellibRunPerfServers` also defaults `multiMysql=true` unless you override it.

Optional MySQL (Docker):

- `multiMysql=true|false`
- `multiMysqlPort` (default: `3307`)
- `multiMysqlDatabase` (default: `rapunzellib`)
- `multiMysqlRootPassword` (default: `root`)
- `multiMysqlImage` (default: `mysql:latest`)
- `multiMysqlContainerName` (optional)

Notes:

- The runner downloads Paper/Velocity builds via the Fill v3 service and writes instances under `run/server-runner/` by default.
- Extra plugins can be added via `multiPaperExtraPlugins` / `multiVelocityExtraPlugins` (CSV of local jar paths).

### `rapunzellibGenerateKeyCatalog`

Generates a Java constant class with nested namespace-specific constant classes from explicit key-list inputs.

- Input format: UTF-8 text files with one `namespace:path` entry per line.
- Blank lines and lines starting with `#` or `//` are ignored.
- Output model: constant classes only; no enums or closed runtime registry assumptions.
- When the Java plugin is present, the generated output directory is added to `main` automatically and `compileJava` depends on the generation task.

Run via `./gradlew rapunzellibGenerateKeyCatalog`.

Example input file:

```text
# src/main/rapunzellib/keys.txt
minecraft:stone
minecraft:oak_log
rapunzellib:bridge
```

Example config:

```kotlin
rapunzellib {
  keyCatalog {
    inputFiles.from(layout.projectDirectory.file("src/main/rapunzellib/keys.txt"))
    packageName.set("com.example.keys")
    className.set("NetworkKeys")
    domainName.set("network")
  }
}
```

### `rapunzellibGenerateRegistryCatalogs` / `rapunzellibVerifyRegistryCatalogParity`

Registry catalog generation stays wired into `compileJava`, but full registry parity verification is opt-in.

- `compileJava` and the default `check` lifecycle do not run registry parity verification.
- Run `./gradlew rapunzellibVerifyRegistryCatalogParity` to execute all configured registry parity checks for the current project.
- Run the generated per-catalog tasks such as `./gradlew rapunzellibVerifyVanillaItemTypesParity` for targeted/manual verification.
- To re-add registry parity to `check` explicitly, use `./gradlew check -Prapunzellib.registryParityInCheck=true`.

### `rapunzellibInitTemplate`

Generates a multi-module starter template into `template/`:

- `common` module (shared feature install + config/messages)
- `platform-paper` module (Paper bootstrap entry)
- `platform-velocity` module (Velocity bootstrap entry)
- root `settings.gradle.kts` + `build.gradle.kts`

Run via `./gradlew rapunzellibInitTemplate`.

### `rapunzellibGeneratePlatformAdapterScaffold`

Generates deterministic text scaffolding for a new platform key:

- `platform-<key>` bootstrap module stub (`<Platform>RapunzelBootstrap`)
- selected feature installer modules (`commands/gui/nbt/events`)
- `META-INF/services` descriptor stubs for generated installers
- shared-core aware Gradle snippets at `snippets/settings.gradle.kts` and `snippets/build.gradle.kts`
- `snippets/architecture-notes.md` with thin-shim/shared-core guidance

Run via `./gradlew rapunzellibGeneratePlatformAdapterScaffold`.

Example:

```kotlin
rapunzellib {
  scaffoldPlatformKey.set("minestom")
  scaffoldFeatures.set(setOf("commands", "events", "gui", "nbt"))
  scaffoldSharedCoreFamily.set("none") // or "auto", "shared", or a custom <family>-shared suffix
  scaffoldSharedCoreFeatures.set(setOf("events", "gui"))
  scaffoldOutputDir.set(layout.projectDirectory.dir("stage9-scaffold"))
}
```

Notes:

- `scaffoldSharedCoreFamily` defaults to `auto`.
- `auto` maps Paper/Fabric/NeoForge scaffolds onto the current `*-shared` layout where applicable.
- `scaffoldSharedCoreFeatures` defaults to the features that already use shared-core modules for the selected platform family.

### `rapunzellibVerifyInstallerWiring`

Verifies installer/service registration coverage for RapunzelLib feature modules present in the current multi-project build.

- Detects supported installer-bearing modules from the current project matrix instead of a fixed hard-coded list.
- Requires descriptor file presence at `src/main/resources/META-INF/services/<InstallerType>`.
- Requires each descriptor to contain at least one non-comment, non-blank line.

Run via `./gradlew rapunzellibVerifyInstallerWiring`.

### `rapunzellibVerifySharedParity`

Validates Fabric + NeoForge parity assumptions without resolving dependencies/network access.

- Fails when a managed `*-fabric` module exists without its `*-neoforge` counterpart, or vice versa.
- For each detected parity-managed `*-fabric` module: requires `minecraft` dependency declaration and non-empty `mappings` configuration.
- Ensures all detected Fabric modules agree on a single Minecraft version.
- If NeoForge modules are present: checks NeoForge version prefix alignment with the Minecraft version (same convention as root parity checks).

Run via `./gradlew rapunzellibVerifySharedParity`.
