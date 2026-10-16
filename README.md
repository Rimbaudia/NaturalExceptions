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

## Performance

`./gradlew runClient` loads [Angelica](https://github.com/GTNewHorizons/Angelica) (the 1.7.10 port of Sodium), GTNHLib and Hodgepodge. They are dev-runtime only and never become NE dependencies. Players on weak machines should install the same three mods next to NE.

Don't use OptiFine or FastCraft. Both conflict with Angelica, and OptiFine can't be redistributed or pulled from Maven.

Angelica's video settings (Options → Video Settings) are where the FPS is. On integrated graphics, try a render distance of 6–8 and Fast graphics.

`/ne_perf` shows what NE's own work costs: server tick time, then calls, average and worst ms for each timed operation, including every worldgen stage. `/ne_perf gen` generates the nearest ungenerated Citadel and times it, and works from a server console. `/ne_perf reset` clears the figures.
