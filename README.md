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

## Requirements

NE needs these mods next to it, and Forge refuses to start without them. 1.7.10 is not playable without them.

| Mod | Version | Side |
| --- | --- | --- |
| [UniMixins](https://github.com/LegacyModdingMC/UniMixins) (`+unimixins-all`) | 0.3.1 | both |
| [GTNHLib](https://github.com/GTNewHorizons/GTNHLib) and its GTNHExtLib | 0.11.54, 1.0.5 | both |
| [Hodgepodge](https://github.com/GTNewHorizons/Hodgepodge) | 2.7.216 | both |
| [Lumi](https://github.com/FalsePattern/Lumi) with ChunkAPI and FalsePatternLib | 1.2.2, 0.8.4, 1.12.2 | both |
| [Angelica](https://github.com/GTNewHorizons/Angelica) | 2.2.33 | client |
| [lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify), to run on Java 21 | 3.0.33 | both |

The test pack (`tools/testpack`) has all of them. To play the current build on another machine, see
[Testing on the Mac](docs/testing-on-mac.md).

NE's coremod (`naturalexceptions.coremod`) makes them start together. On a dedicated server, which has no Angelica,
Hodgepodge would apply a lighting fix (`optimizeWorldUpdateLight`) that collides with Lumi and stops the server at
startup. NE leaves that fix out whenever Lumi is present, as Hodgepodge itself does when Angelica is loaded.
`ServerSideLinkageTest` keeps NE's common code off anything Minecraft has only on the client, which would work in
single player and crash a server.

## Performance

`./gradlew runClient` loads [Angelica](https://github.com/GTNewHorizons/Angelica) (the 1.7.10 port of Sodium), GTNHLib and Hodgepodge, plus [Lumi](https://github.com/FalsePattern/Lumi) with ChunkAPI and FalsePatternLib. Lumi replaces vanilla's lighting engine, which chokes on the open air under sky islands: without it, a Citadel ran at about 4 FPS on a T440p, and with it at about 30. They are required: NE will not load without them (see Requirements below).

Don't use OptiFine or FastCraft. Both conflict with Angelica, and OptiFine can't be redistributed or pulled from Maven.

On a weak machine, use `./gradlew runClient21` instead of `runClient`. It runs the same client on Java 21 with lwjgl3ify, and a modern JIT and garbage collector usually mean steadier frames and fewer GC hitches than Java 8.

Angelica's video settings (Options → Video Settings) are where the FPS is. On integrated graphics, try a render distance of 6–8 and Fast graphics.

`/ne_perf` shows what NE's own work costs: server tick time, then calls, average and worst ms for each timed operation, including every worldgen stage. `/ne_perf gen` generates the nearest ungenerated Citadel and times it, and works from a server console. `/ne_perf reset` clears the figures.

## License

Copyright (c) 2026 Aljosa. All rights reserved: you may download and play Natural Exceptions, but not redistribute or
modify it (see [LICENSE](LICENSE)). The build files from the GTNewHorizons ExampleMod template keep their MIT license
([LICENSE-ExampleMod](LICENSE-ExampleMod)).
