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
> The bootstrap copper generator included in P0 is a temporary development bridge to verify registration, drops, assets, and runtime integration. It will be replaced in P6 by the staged Natural Exceptions world-generation pipeline.
