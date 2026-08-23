# Ninecun Lingtai

[中文 README](README.md)

A cultivation-themed mod for **Minecraft 1.20.1 Forge**. Players awaken spiritual roots, absorb spiritual energy, comprehend techniques, learn spells, refine elixirs, and advance toward immortality while dealing with elemental environments, realm suppression, and tribulation trials.

> **Development status:** The project is actively being developed. Versions, balance, resources, data formats, and gameplay mechanics may change in incompatible ways.

## Project Overview

| Item | Information |
| --- | --- |
| Mod ID | `ninecun_lingtai` |
| Minecraft | `1.20.1` |
| Forge | `47.4.22` |
| Java | `17` |
| Current project version | `1.0.0` |
| License | [GNU GPL v3.0](LICENSE) |

## Core Gameplay

1. **Awaken a spiritual root:** Obtain or select metal, wood, water, fire, earth, yin-yang, or chaos attributes, with aptitude determined by root length.
2. **Meditate and cultivate:** Use breathing techniques to absorb spiritual energy in suitable environments, accumulate each elemental resource, and break through realms from Qi Refining and Foundation Establishment to Golden Core, Nascent Soul, and beyond.
3. **Comprehend techniques:** Read techniques through jade slips and submit your understanding for optional AI-assisted assessment. Server data packs can also define completely custom techniques and mastery systems.
4. **Learn and cast spells:** Master sword flight, escape techniques, Pushing Mountains, defensive spells, and other abilities according to your roots and cultivation.
5. **Refine elixirs:** Gather spiritual materials, learn pill recipes, and control heat and spiritual-energy consumption in the alchemy furnace to produce pills of different grades.
6. **Explore and face tribulations:** Search for spiritual stones, explore realms, challenge powerful enemies, and pay the price of cultivation through tribulations, ascension, soul wandering, and body possession.

## Implemented Systems

- **Spiritual roots and aptitude:** Root elements, root length, soul strength, spiritual-sense range, and cultivation capacity.
- **Realms and spiritual energy:** Nine major realms, realm layers, breakthrough requirements, lifespan, realm suppression, and combat growth from cultivation.
- **Elemental environments:** Metal, wood, water, fire, earth, yin-yang, and chaos attributes; dimensions, biomes, weather, terrain, and local surroundings jointly affect spiritual-energy density.
- **Techniques and jade slips:** Breathing techniques, element compatibility, prerequisites, AI comprehension scores, proficiency, mastery levels, and soul imprints.
- **Spells and movement:** Elemental attacks, sword flight, light flight/air walking, defensive spells, spiritual pressure, and Pushing Mountains.
- **Alchemy and recipes:** Alchemy furnaces, primary/secondary/auxiliary ingredient slots, heat control, pill grades, pill toxin, and recipe learning.
- **Spiritual stones and plants:** Spiritual-stone ores, human/earth/heaven-grade spiritual stones, spiritual plants, and related resources.
- **Storage rings:** Each ring owns an independent interactive inventory and supports an immersion mode.
- **Cultivating creatures:** Some creatures, villagers, and monsters can have spiritual roots, cultivate, and advance through realms.
- **Tribulation and death:** Lightning tribulations, defensive resources, ascension-related processes, soul wandering, body possession, and realm-dependent revival mechanics.
- **Three-thousand realms:** Dynamic realms, cross-realm exploration, realm notifications, and respawn-location handling.
- **UI and integrations:** Cultivation information, technique and recipe libraries, value controls, Jade tooltips, and JEI recipe display.

## Key Bindings

All key bindings can be changed in Minecraft's **Controls** menu. Defaults are:

| Function | Default key | Description |
| --- | --- | --- |
| Meditate/cultivate | `Shift + X` | Start or stop meditation |
| Release spiritual pressure | `X` | Hold to release pressure |
| Select a spell | `R` | Open the spell selection screen |
| Cast a spell | `V` | Cast the selected spell |
| Pushing Mountains | `` ` `` / `~` | Hold to use the ability |
| Storage-ring immersion mode | `O` | Toggle immersive storage-ring interaction |

## Installation and Dependencies

### Required for players

Install **Forge 47.4.22 or another compatible Forge 47 build** for **Minecraft 1.20.1**, then place the mod and its dependencies in the `mods` folder.

The main dependencies used by the project are:

- **Curios API**
- **playerAnimator**
- **GeckoLib**
- **Inventory Tabs** (client-side UI dependency)

The following mods provide optional features, information displays, recipe viewing, or development assistance and are not core gameplay dependencies:

- **Filters API:** Creative-inventory filtering compatibility.
- **Jade:** Information-display integration.
- **JEI:** Alchemy recipe viewing and recipe-screen integration.
- **Patchouli:** Development and guide-book related content.
- **WorldEdit:** Development and world-editing assistance.

For multiplayer, the server-side logic and client-side UI of Ninecun Lingtai must use matching versions. Do not update only one side. Exact dependency versions are defined by `gradle.properties`, the distributed mod pack, and the download pages of the respective dependencies.

## First Launch

1. Install Minecraft, Forge, Ninecun Lingtai, and the required dependencies.
2. Start the game or a dedicated server and enter a world.
3. On the first launch, Forge generates the following files in `config/`:
   - `ninecun_lingtai-common.toml`: Server-side and shared gameplay configuration.
   - `ninecun_lingtai-client.toml`: Client rendering configuration.
4. Use the key bindings, items, and screens to awaken a spiritual root, read jade slips, and begin cultivating.
5. After editing technique or recipe data packs, run `/reload` to reload them on the server.

## AI-Assisted Technique Comprehension

AI assessment is optional. Other cultivation, combat, and alchemy content remains available without an AI provider. To enable it, edit `config/ninecun_lingtai-common.toml` on the **server only**:

```toml
[ai]
provider = "OPENAI" # OPENAI or DEEPSEEK
apiKey = ""
model = ""
```

Important notes:

- `provider` currently supports `OPENAI` and `DEEPSEEK`.
- `apiKey` and `model` must be valid values issued by the selected provider.
- Keep the API key in the server configuration only. Never commit it to Git or expose it in screenshots or logs.
- The mod does not synchronize the API key to clients through its network packets, but server administrators are still responsible for protecting the configuration file.
- If the AI service is unavailable, configuration is incomplete, or a request fails, the related comprehension action may not complete.

## Data Packs and Extension

Techniques are registered through server data packs at:

```text
data/<namespace>/techniques/<path>.json
```

For example:

```text
data/example/techniques/sword/azure_sword.json
```

This registers the technique ID `example:sword/azure_sword`. The JSON can define the technique text, compatible roots, cultivation requirements, prerequisite techniques, AI grading rules, mastery levels, and effect handlers. Run `/reload` after editing the data pack.

See [Technique Data Pack Format](docs/功法数据包格式.md) for the complete schema and examples. Alchemy recipes are also loaded as server data; built-in examples are located in `src/main/resources/data/ninecun_lingtai/alchemy_recipes/`.

For other mods and extension developers, the project exposes APIs for cultivation, aptitude, spiritual energy, elements, spells, techniques, and alchemy. Custom technique effects can be registered through `TechniqueEffectApi.register`. These APIs are still evolving, so check the source and data formats when upgrading.

## Building

The project uses the Gradle Wrapper. On Windows, run:

```powershell
.\gradlew.bat build
```

The build artifact is generated at:

```text
build/libs/ninecun_lingtai-1.0.0.jar
```

The ForgeGradle development environment also provides tasks such as `runClient` and `runServer`. Make sure Java 17 is available and that Gradle can access the Maven repositories declared by the project before building or launching the development environment.

## Development Documentation

- [Core Concepts](docs/一些基础设定.md): Spiritual roots, spiritual energy, souls, realms, and world rules.
- [Techniques and Spells](docs/功法.md): Design notes for techniques, spells, and cultivation.
- [Technique Data Pack Format](docs/功法数据包格式.md): Complete instructions for creating technique JSON files.
- [Alchemy](docs/炼丹.md): Alchemy furnaces, recipes, heat control, and pill quality.
- [Storage Rings](docs/储物戒指.md): Storage-ring space and interaction design.
- [Five-Element Relations](docs/五行相生.md): Elemental environments, spiritual-energy density, and generation/overcoming rules.
- [Tribulation and Ascension](docs/渡劫飞升.md): Lightning, earthly, human, and ascension tribulations.
- [Three Thousand Realms](docs/三千界.md): Dynamic realms and cross-realm exploration.
- [Spiritual Stones](docs/灵石.md): Spiritual-stone resources and grades.

Development and debug commands use the `/ninecun_lingtai debug ...` prefix and should only be used in test worlds or development environments.

## License

This project is released under the [GNU General Public License v3.0](LICENSE). Follow the license terms when redistributing, modifying, or building upon the project.
