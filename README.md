# Natural Exceptions

A systems-driven absurdity sandbox for Minecraft 1.7.10, where experimentation is the main way forward. Real
materials, continuous physical states, processes, environmental fields and absurd phenomena coexist under coherent
world rules.

**Minecraft 1.7.10 · Forge 10.13.4.1614 · Java 21**

## Install

The easy way is the ready-made [Prism Launcher](https://prismlauncher.org/) instance, which keeps itself up to date:

1. Install Prism Launcher and sign in with your Microsoft account.
2. **Add Instance → Import**, and paste `https://rimbaudia.github.io/NaturalExceptions/NaturalExceptions-Test.zip`.
3. Press **Play**. If Prism asks for Java 21, let it download it.

Every Play pulls the newest build and the mods it needs. The download page is at
[rimbaudia.github.io/NaturalExceptions](https://rimbaudia.github.io/NaturalExceptions/).

## Required mods

Natural Exceptions does not start without these; the Prism instance includes them all.

| Mod | Version | Side |
| --- | --- | --- |
| [UniMixins](https://github.com/LegacyModdingMC/UniMixins) (`+unimixins-all`) | 0.3.1 | both |
| [GTNHLib](https://github.com/GTNewHorizons/GTNHLib) and GTNHExtLib | 0.11.54, 1.0.5 | both |
| [Hodgepodge](https://github.com/GTNewHorizons/Hodgepodge) | 2.7.216 | both |
| [Lumi](https://github.com/FalsePattern/Lumi) with ChunkAPI and FalsePatternLib | 1.2.2, 0.8.4, 1.12.2 | both |
| [Angelica](https://github.com/GTNewHorizons/Angelica) | 2.2.33 | client |
| [lwjgl3ify](https://github.com/GTNewHorizons/lwjgl3ify) (Java 21) | 3.0.33 | both |

The pack that lists them is in [`tools/testpack/pack`](tools/testpack/pack). Don't add OptiFine or FastCraft: both
conflict with Angelica. On integrated graphics, a render distance of 6–8 and Fast graphics (Options → Video Settings)
help.

## Playing together

Everyone joining needs the same instance. To host a dedicated server on Linux with Java 21:

```bash
tools/testpack/make-server.sh ~/ne-server https://rimbaudia.github.io/NaturalExceptions/pack.toml
```

This installs Forge 1.7.10 with lwjgl3ify's Java 21 launcher, and writes `start.sh`, which pulls the server's side of
the pack (every required mod except the client-only Angelica) on each start. Accept Mojang's
[EULA](https://aka.ms/MinecraftEULA) by setting `eula=true` in `~/ne-server/eula.txt`, then run `~/ne-server/start.sh`.
Players add the server's address under Multiplayer. From outside the server's network, port 25565 has to be forwarded
to it.

## Building

```bash
./gradlew build          # the mod jar, in build/libs, and the tests
./gradlew runClient21    # a development client on Java 21
```

Gradle runs on Java 25 and builds Java 8 bytecode. Every push to `master` builds the mod and publishes the site.

## License

Copyright (c) 2026 Aljosa. All rights reserved: you may download and play Natural Exceptions, but not redistribute or
modify it (see [LICENSE](LICENSE)). The build files from the GTNewHorizons ExampleMod template keep their MIT license
([LICENSE-ExampleMod](LICENSE-ExampleMod)).
