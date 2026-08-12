# AI Agent Guide: Ninecun Lingtai

This file is the entry point for coding agents working in this repository. Read it before searching the codebase.
Its purpose is to let an agent identify the relevant implementation surface first, then inspect only the files
needed for the requested change.

## Project Identity

- Mod: `ninecun_lingtai` (Ninecun Lingtai / 九寸灵台)
- Platform: Minecraft `1.20.1`, Forge `47.4.22`, Java `17`
- Build system: Gradle Wrapper `8.8`
- Mappings: Parchment `2023.09.03-1.20.1`
- Main source package currently being used: `net.seraphina.ninecun_lingtai`
- Mod entry point: `src/main/java/net/seraphina/ninecun_lingtai/ModMain.java`
- Resource namespace: `ninecun_lingtai`
- Main resource roots:
  - Client assets: `src/main/resources/assets/ninecun_lingtai/`
  - Server data: `src/main/resources/data/ninecun_lingtai/`

The mod is a full client-and-server mod, not a client-only mod. The server owns gameplay state, validation,
inventory changes, world changes, combat, and persistence. The client owns input, screens, rendering, animation,
and display caches.

## Mandatory Working Rules

1. Do not do a full repository scan by default. Start with the task routing table below, then inspect the named
   entry point and its direct dependencies. Use targeted `rg` searches only after that.
2. Treat the current source tree as authoritative over old paths in documents, comments, or Gradle configuration.
3. Keep a change within its existing domain package. Do not put unrelated classes into one package.
4. Fields use `thisIsAField`; constants use `THIS_IS_A_CONSTANT`; types use `PascalCase`; methods use
   `lowerCamelCase`.
5. Put reusable stateless helpers in `utility/` or `client/utility/`, named `XxxUtility`. Do not duplicate
   non-trivial rules in multiple event handlers, packets, or screens.
6. Config definitions belong in `common/config/`. Use `CommonConfig` for common/server-authoritative settings and
   `ClientConfig` for client-only settings if one is introduced.
7. New player-facing strings require both:
   - `src/main/resources/assets/ninecun_lingtai/lang/zh_cn.json`
   - `src/main/resources/assets/ninecun_lingtai/lang/en_us.json`
8. Preserve existing user changes. This repository currently has uncommitted package-migration work; do not reset,
   revert, or broadly reformat it.

## Current Repository State: Package Migration

The codebase is actively being migrated from `com.seraphina.ninecun_lingtai` to
`net.seraphina.ninecun_lingtai`.

- Java files have been moved under `src/main/java/net/seraphina/ninecun_lingtai/`.
- The actual Java package declarations are predominantly `net.seraphina.ninecun_lingtai`.
- Some metadata and build configuration still contain `com.seraphina` references, including transformation-service
  source-set includes/excludes, `gradle.properties`, Mixin configuration/plugin string constants, and at least one
  stale Java import.
- `Dev.md` predates or overlaps this migration and includes old `com.seraphina` paths. Use this file and the current
  source tree for navigation; update `Dev.md` only when the requested work includes documentation maintenance.

When a task touches build startup, Mixins, the transformation service, or package names, first run:

```powershell
rg -n "com\.seraphina|com/seraphina" build.gradle gradle.properties src
.\gradlew.bat classes
```

Fix only references required by the task or by a directly observed compile/startup failure. Do not restart the
entire migration unless explicitly asked.

## Fast Start And Verification

Use the repository wrapper on Windows:

```powershell
.\gradlew.bat classes
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat runData
```

- Development client and server both use `run/` as their working directory.
- Development common config is generated at `run/config/ninecun_lingtai-common.toml`.
- `runData` writes generated resources to `src/generated/resources/`, which is already a main resource root.
- Packaged output is `build/libs/ninecun_lingtai-1.0.0.jar`.
- There are currently no checked-in JUnit test sources or Forge GameTests. Add focused JUnit tests under
  `src/test/java/` for pure calculations, parsing, or packet codecs when changing such behavior.

For a documentation-only change, validate Markdown paths and review `git diff -- AI_AGENT.md`; a Gradle build is
not required unless the task also changes code or build files.

## Minimal Task Routing

Read the files in the matching row first. Do not read every class in the listed directories.

| Requested change | Read first | Then inspect only as needed |
| --- | --- | --- |
| Register an item, block, entity, sound, menu, or spell | `ModMain.java`, the matching `common/registry/Mod*.java` | Matching `common/item`, `common/block`, `common/entity`, `common/menu`, or `common/spell` class; matching resources |
| Add or change a client screen, keybind, HUD, renderer, shader, animation | `client/event/ClientModEventHandler.java` or `client/event/ClientForgeEventHandler.java` | Matching `client/screen`, `client/input`, `client/render`, `client/player`, `client/utility`; referenced asset |
| Change player cultivation, aptitude, energy, meditation, lifespan, possession, or learned data | Matching `common/player/Player*Data.java` and manager | Related `common/event/*EventHandler.java`, `common/network/ModNetwork.java`, client cache/packet |
| Add or alter a network action or sync | `common/network/ModNetwork.java` | Matching packet in `common/network/packet/`, authoritative manager/event, receiving client manager |
| Add a technique, recipe, damage element, or elemental environment | Matching file in `src/main/resources/data/ninecun_lingtai/` | Corresponding manager under `common/data/`, parser/network utility, relevant document in `docs/` |
| Change spell behavior | `common/registry/ModSpells.java`, `api/spell/Spell.java`, `common/spell/SpellCastManager.java` | Concrete spell, technique JSON, packets, client visuals |
| Change cultivation technique behavior or AI comprehension | `api/technique/`, `common/data/TechniqueManager.java` | `common/ai/TechniqueComprehensionService.java`, `utility/Technique*.java`, `utility/Ai*.java`, technique JSON |
| Change alchemy or alchemy furnace behavior | `common/block/entity/AlchemyFurnaceBlockEntity.java`, `common/data/AlchemyRecipeManager.java` | `common/menu/AlchemyFurnaceMenu.java`, `common/network/packet/AlchemyFurnaceActionPacket.java`, client screen/renderer, `utility/Alchemy*.java` |
| Change storage-ring behavior | `common/ring/RingStorageManager.java`, `common/ring/RingStorageSavedData.java` | `common/menu/StorageRingMenu.java`, ring packets, `client/ring/`, ring Mixins |
| Change damage elements, defense, five-element rules | `api/element/`, `utility/ElementDamageUtility.java` | `common/event/ElementDamageEventHandler.java`, `common/defense/`, `common/spell/defense/`, JSON and `CommonConfig` |
| Change tribulation, pressure, light flight, soul roaming, or moving mountains | Matching manager in `common/tribulation`, `common/ability`, `common/flight`, or `common/player` | Matching event handler, packet, client manager, renderer/utility |
| Change commands or debug tools | `common/command/NinecunLingtaiCommand.java` | Matching class in `common/command/debug/`, related manager/utility |
| Change a Mixin or health transformation | `ninecun_lingtai.mixins.json`, matching Mixin, `mixin/IMixinPlugin.java` | `transform/`, `utility/launch/`, `build.gradle`, access transformer; launch both client and server |
| Change an optional integration | Files under `integration/` or `client/compat/` | `mods.toml`, `build.gradle`, compatibility Mixins; test with and without the target mod |

## Code Map

```text
src/main/java/net/seraphina/ninecun_lingtai/
  ModMain.java                  Forge @Mod entry point; registry, config, network bootstrap
  api/                          Domain-facing data types and extension APIs
    ai/                         AI provider enum
    alchemy/                    Recipe and pill definition types
    aptitude/                   Spiritual root and soul types
    cultivation/                Realm, stage, breakthrough APIs
    element/                    Elements, relations, elemental power
    item/                       Spiritual-item abstraction
    spell/                      Spell interface, category, cast context
    technique/                  Technique definitions, requirements, effects, progress
  common/                       Server-authoritative/shared gameplay
    ability/                    Pressure and pushing-mountains mechanics
    ai/                         Technique-comprehension service
    block/                      Blocks and block entities
    command/                    Root debug command and subcommands
    config/                     Forge configuration
    creature/                   Cultivator creature data and goals
    data/                       Datapack reload managers
    defense/                    Entity defense resource state
    entity/                     Custom entity implementations
    event/                      Forge event subscribers
    flight/                     Light-flight server state
    item/                       Item use/NBT/gameplay logic
    lifespan/                   Entity lifespan data
    menu/                       Server menus
    network/                    SimpleChannel plus packets
    player/                     Player persistent data and managers
    registry/                   DeferredRegister definitions
    ring/                       Storage-ring persistence and server behavior
    spell/                      Spell implementations and cast managers
    tribulation/                Tribulation lifecycle
  client/                       Client-only input, state, UI, rendering, compat
    event/                      Client MOD-bus and Forge-bus registration
    input/                      Key mappings
    player/                     Client-side synced state managers
    screen/                     GUI screens
    render/                     Entity/block/shader renderer classes
    utility/                    Client render and UI helpers
    ring/                       Storage-ring client projection/immersion
    compat/                     JEI and optional client integrations
  integration/                  Optional cross-mod integrations, currently Jade
  mixin/                        Vanilla and optional-mod Mixins
  transform/                    ModLauncher health transformation service
  utility/                      Shared reusable utility classes
```

Important central files:

- `ModMain.java`: register all Forge content and common config here through existing registry classes.
- `common/registry/`: the only normal location for mod-content registration.
- `common/network/ModNetwork.java`: one `SimpleChannel`; packet IDs come from its sequential `getId()` calls.
- `common/data/TechniqueManager.java`, `AlchemyRecipeManager.java`, `DamageElementManager.java`,
  `ElementEnvironmentManager.java`: datapack loading state.
- `common/event/TechniqueEventHandler.java`, `AlchemyRecipeEventHandler.java`,
  `ElementDamageEventHandler.java`: reload listeners and technique/recipe client resync.
- `client/event/ClientModEventHandler.java`: client setup, menu screens, key mappings, renderers, layers, shaders.
- `client/event/ClientForgeEventHandler.java`: client ticks, input interception, world/HUD rendering, cleanup.
- `common/config/CommonConfig.java`: all existing tunable gameplay and AI settings.

## Gameplay Data And Persistence

Most gameplay state is stored with Forge capabilities attached to players or entities. Existing `*Data` classes provide
the state holder; `*Manager` classes coordinate gameplay and synchronization. Before adding another capability, check
the relevant existing data class first.

Common examples:

- Cultivation, energy, aptitude, techniques, and player value settings: `common/player/`
- Defense resource: `common/defense/EntityDefenseResourceData.java` and `common/player/`
- Creature cultivation: `common/creature/CreatureCultivationData.java`
- Entity lifespan: `common/lifespan/`
- Storage ring persistence: `common/ring/RingStorageSavedData.java`, backed by `SavedData`

Rules:

- Mutate authoritative state on the logical server.
- Copy or preserve state on player clone/reconnect wherever the relevant existing event handler does so.
- Send a deliberate sync after an authoritative mutation, and update only client-side caches from clientbound packets.
- Do not make client cache state the source of truth.

## Networking Contract

Packets live in `common/network/packet/` and are registered in `common/network/ModNetwork.java`.

For a new packet:

1. Add a packet class with `encode`, `decode`, and `handle`.
2. Register it in `ModNetwork.register()` with the proper `NetworkDirection`.
3. Client request packets are `PLAY_TO_SERVER`; the handler must obtain the player from
   `context.getSender()` and validate all relevant state, item, target, distance, permissions, and resource costs.
4. Sync/open/display packets are `PLAY_TO_CLIENT`; they update client cache, screens, or rendering state only.
5. Use the narrowest existing send helper or `PacketDistributor` target: player, tracking entity plus self, or a
   specific group. Do not broadcast by default.
6. If wire fields, their order, direction, or semantics change, update `PROTOCOL_VERSION` in `ModNetwork`.

Several existing clientbound packet classes directly reference client managers. Preserve the established loading
pattern when editing them, but do not introduce `net.minecraft.client.*` dependencies into ordinary server gameplay
classes or into code that can be initialized on a dedicated server.

## Datapack Content

Default server data is under:

```text
src/main/resources/data/ninecun_lingtai/
  techniques/             Cultivation technique definitions
  alchemy_recipes/        Alchemy recipe definitions
  damage_elements/        Damage type -> elemental power mappings
  element_environments/   Dimension, biome, weather elemental modifiers
  damage_type/            Custom Forge damage types
  dimension/              Storage-ring dimension definition
  dimension_type/         Storage-ring dimension type
  worldgen/               Spiritual-stone ore features
```

Datapack ID is derived from namespace and path. For example:

```text
data/example/techniques/sword/azure_sword.json
-> example:sword/azure_sword
```

After changing a datapack in a running development world, run `/reload` and inspect the logs. A failed JSON
definition can be logged and skipped without stopping the reload, so successful command completion alone is not
enough.

Read the corresponding document before changing data formats:

- Techniques: `docs/功法数据包格式.md`
- Damage elements and environments: `docs/伤害元素JSON说明.md`
- Alchemy design: `docs/炼丹.md`
- Storage rings: `docs/储物戒指.md`
- Element relations: `docs/五行相生.md`
- World/cultivation concepts: `docs/一些基础设定.md`

Design documents are normative for intended rules, but implementation and existing data define what is currently
shipped. Do not silently implement speculative features from the world-building reference directory
`docs/修仙_by at(或许可以参考一下？)/`.

## Content Registration And Resources

Use the existing registry class by content type:

- Blocks: `common/registry/ModBlocks.java`
- Block entities: `common/registry/ModBlockEntityTypes.java`
- Items: `common/registry/ModItems.java`
- Entity types: `common/registry/ModEntityTypes.java`
- Menus: `common/registry/ModMenuTypes.java`
- Spells: `common/registry/ModSpells.java`
- Sounds: `common/registry/ModSounds.java`
- Creative tab: `common/registry/ModCreativeModeTabs.java`

Do not register content from an item, block, utility, or event-handler static initializer.

For each new content type, inspect a nearest existing sibling and add all applicable companion files:

- `assets/ninecun_lingtai/lang/*.json`
- item/block models, blockstates, textures, sounds, animations, shader files
- loot tables, tags, recipes, worldgen data, damage types, or other server data
- block entity type, menu type, client screen/renderer, entity attributes, renderer/model layer
- creative-tab behavior, including complex NBT variants where applicable

## Client/Server Boundaries

- `common/` and `api/` must not directly import `net.minecraft.client.*`, client event classes, screens, or
  renderers.
- `client/` may consume synced common/API data but must not decide authoritative game outcomes.
- Client registrations belong in `client/event/ClientModEventHandler.java`.
- Gameplay input interception and render hooks belong in `client/event/ClientForgeEventHandler.java`.
- For client-specific classes that are only valid when an optional mod is loaded, use the existing
  `client/compat/` or `integration/` pattern and ensure their classes are not loaded without the dependency.

## Configuration And AI Providers

`CommonConfig` currently provides game balance groups such as element damage, defense spells, meditation,
tribulation, light flight, shrinking earth, pushing mountains, pressure, lifespan, creature cultivation, and `ai`.

Generated development config:

```text
run/config/ninecun_lingtai-common.toml
```

AI technique-comprehension configuration is server sensitive:

```toml
[ai]
provider = "OPENAI"
apiKey = ""
model = ""
```

- Supported providers are `OPENAI` and `DEEPSEEK`.
- API keys must never be committed, sent through mod packets, placed in sample data, or copied into client-only
  configuration.
- Provider/model details are intentionally configurable because the available models can change.

## Mixins And Transformation Service

This project has two advanced startup mechanisms. Treat changes here as high risk.

### Mixins

- Config: `src/main/resources/ninecun_lingtai.mixins.json`
- Plugin: `src/main/java/net/seraphina/ninecun_lingtai/mixin/IMixinPlugin.java`
- Access transformer: `src/main/resources/META-INF/accesstransformer.cfg`

The Mixin plugin conditionally handles Embeddium, Bad Optimizations, JEI, and Inventory Tabs compatibility.
When modifying a Mixin, verify target method/mapping names, refmap configuration, optional-mod gating, and both
client and dedicated-server startup when applicable.

### Health Transformation Service

- Service class: `transform/HealthTransformationService.java`
- Launch plugin: `transform/HealthLaunchPluginService.java`
- Utility: `utility/launch/LaunchPluginUtility.java`
- Service declaration:
  `src/main/transformationServiceResources/META-INF/services/cpw.mods.modlauncher.api.ITransformationService`
- Gradle source-set/jar wiring: `build.gradle`

`build.gradle` builds this code in a separate `transformationService` source set and attaches a development jar to
run tasks. Its package include/exclude paths are affected by the active `com` -> `net` migration. Do not move this
code to normal `main`, bypass the generated dev jar, or modify class-loading logic without validating `runClient`,
`runServer`, and a packaged `build`.

## Required Validation By Change Type

| Change | Minimum verification |
| --- | --- |
| Java-only shared logic | `.\gradlew.bat classes` and focused JUnit test if practical |
| Any code submitted for integration | `.\gradlew.bat test`, then `.\gradlew.bat build` |
| Datapack JSON | `/reload`, log inspection, target screen/effect in game |
| Item/block/entity/resource | `runClient`; language, model, creative tab, placement/use/drop behavior |
| Screen, render, shader, keybind | `runClient`; load world, use feature, leave/re-enter world to confirm cache cleanup |
| Network or player/entity data | `runServer` plus a client connection; verify invalid requests are rejected and valid state syncs |
| Block entity/menu/ring | Place, open, interact, disconnect/reconnect, break/drop, and persistence |
| Mixin/transformation/build startup | `runClient`, `runServer`, and `build`; inspect startup logs and crash reports |
| Optional integration | Test with and without the optional target mod present |

For build failures, first distinguish:

1. Failures introduced by the requested change.
2. Existing failures caused by the in-progress package migration or unrelated user work.

Report existing blockers precisely; do not hide them with unrelated broad edits.

## Compact Discovery Commands

Use targeted commands after selecting a task route:

```powershell
# Find the implementation of a feature without scanning generated/run output.
rg -n -S "TechniqueComprehensionService|ClassOrMethodName" src/main/java

# Locate resource and data files for a registered id.
rg --files src/main/resources | rg "technique_id|item_id|block_id"

# Find every sender/receiver of a specific packet or manager.
rg -n -S "PacketClassName|ManagerClassName" src/main/java

# Find stale package migration references only when relevant.
rg -n "com\.seraphina|com/seraphina" build.gradle gradle.properties src

# Review only the files changed for the current task.
git diff --stat
git diff -- path/to/changed/file
```

Avoid scanning `build/`, `.gradle/`, `run/`, `run-data/`, `logs/`, or `resources/` unless the request explicitly
concerns generated output, local runtime state, or source art assets.

## Completion Checklist

- The implementation uses the current `net.seraphina.ninecun_lingtai` package unless the change specifically repairs
  a stale migration reference.
- Server-authoritative actions are validated on the server and synchronized deliberately.
- New content has all required registration, resource, data, and translation companions.
- New config follows the `common/config/` convention and does not expose secrets to clients.
- Modified JSON has a matching parser/document check and has been tested with `/reload`.
- Reusable logic is in an appropriate `XxxUtility` or established manager rather than duplicated.
- Relevant Gradle and in-game verification has completed, or an exact blocker has been reported.
