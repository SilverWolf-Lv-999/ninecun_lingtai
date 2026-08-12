# Ninecun Lingtai

A cultivation-themed mod for **Minecraft 1.20.1 Forge**. Players awaken spiritual roots, absorb spiritual energy, cultivate techniques, refine elixirs, cast spells, and advance along the immortal path while facing elemental environments and realm-based suppression.

> This project is still under development. Values, content, and mechanics may change.

## Implemented Features

- **Cultivation system**: Spiritual root aptitude, soul, spiritual energy, lifespan, nine major realms, and breakthroughs.
- **Five-element environment**: Metal, wood, water, fire, earth, yin-yang, and chaos attributes, plus the effects of dimensions, biomes, and weather on spiritual energy.
- **Techniques and spells**: Jade-slip imprints, proficiency, breathing techniques, five-element cultivation methods, sword flight, escape techniques, defensive spells, and more.
- **Technique comprehension**: Supports evaluating players' written understanding of cultivation techniques through OpenAI or DeepSeek.
- **Alchemy and resources**: Alchemy furnaces, pill recipes, elixirs, spirit-stone ores, and spirit-stone grades.
- **Storage rings**: Every ring has its own independent, interactive storage inventory.
- **Combat and survival**: Elemental damage, realm-based suppression, spiritual pressure, tribulations, soul wandering, body possession, and related mechanics.

## Requirements

- Minecraft `1.20.1`
- Forge `47.4.22`
- Java `17`
- Curios API
- playerAnimator
- Inventory Tabs
- Filters API

Jade, JEI, Patchouli, WorldEdit, and similar mods are development or auxiliary dependencies and are not required for normal gameplay.

## Build

```powershell
.\gradlew.bat build
```

The build artifact is located at `build/libs/ninecun_lingtai-1.0.0.jar`.

## Configuration and Extension

- The common configuration file is generated at `config/ninecun_lingtai-common.toml` after the first launch.
- To enable AI-assisted technique comprehension, set the provider, API key, and model ID in the `[ai]` section of the configuration file. The API key must be stored on the server only.
- Cultivation techniques and pill recipes are loaded through server data packs and take effect after running `/reload`.

See [Cultivation Technique Data Pack Format](docs/功法数据包格式.md) for the full data format.

## Design Documents

Development design notes and rule descriptions are available in [`docs`](docs):

- [Core Concepts](docs/一些基础设定.md)
- [Cultivation Techniques and Spells](docs/功法.md)
- [Alchemy](docs/炼丹.md)
- [Storage Rings](docs/储物戒指.md)
- [Five-Element Environment](docs/五行相生.md)

## License

GNU GPL v3.0
