# RapunzelLib Gradle plugin (`de.t14d3.rapunzellib`)

This module publishes a Gradle plugin that provides developer tools for RapunzelLib consumers:

- `rapunzellibValidateMessages`: validates that message keys used in compiled bytecode exist in your `messages.yml`.
- `rapunzellibRunServers` / `rapunzellibRunPerfServers`: runs a local Velocity + multiple Paper backends via `tool-server-runner`.
- `rapunzellibInitTemplate`: generates a small starter template (`template/`).

## Apply the plugin

```kotlin
plugins {
  id("de.t14d3.rapunzellib") version "<version>"
}
```

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

These tasks drive `tool-server-runner` and accept most settings via Gradle properties (easy to override per-machine / CI):

- `multiPaperVersion` (default: `1.21.10`)
- `multiPaperCount` (default: `2`)
- `multiPaperBasePort` (default: `25566`)
- `multiVelocityEnabled` (default: `true`)
- `multiVelocityVersion` (default: `latest`)
- `multiVelocityPort` (default: `25565`)
- `multiRunnerJava` (default: `""` → use runner default)
- `multiRunnerJvmArgs` (CSV, e.g. `-Xmx2G,-XX:+AlwaysPreTouch`)

Perf task additions:

- `multiJfr=true` or use `rapunzellibRunPerfServers` (enables JFR)
- `multiJfrSettings=<name>` (optional)

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

### `rapunzellibInitTemplate`

Generates a minimal starter template into `template/`:

- `messages.yml`
- `config.yml`
- `Example.java`

Run via `./gradlew rapunzellibInitTemplate`.

