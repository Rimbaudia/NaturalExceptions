# Natural Exceptions

Natural Exceptions is a systems-driven absurdity sandbox where experimentation is the primary form of progression.

**Target:** Minecraft Java 1.7.10 / Forge 10.13.4.1614  
**Status:** N0.01.0 is under active development.

## Premise

Real materials, continuous physical states, processes, environmental fields, and absurd phenomena coexist under coherent world rules. Minecraft is the first host, but Natural Exceptions owns its canonical physical truth.

## Development & Build

This project uses the GTNewHorizons ExampleMod / RetroFuturaGradle toolchain.
Gradle is executed using a Java 25 daemon, producing Java-8-compatible mod bytecode via Jabel.

### Build Commands

```bash
# Build the mod JAR
./gradlew build

# Run unit and integration tests
./gradlew test

# Launch the Minecraft development client
./gradlew runClient
```

> [!NOTE]
> P0's bootstrap copper scatter has been replaced by the P6 staged Overworld materializer. See [the P6 generation boundary](docs/architecture/p6-staged-overworld-generation-boundary.md) for its deterministic fields, geology-gated copper, host-terrain compromise, and chunk-history rules.
