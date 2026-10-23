# Dev

本文面向参与模组开发的开发者，说明如何启动项目、定位代码、扩展内容，以及在提交前完成验证。
项目是 **Minecraft 1.20.1 Forge** 修仙主题模组，模组 ID 为 `ninecun_lingtai`。

> `docs/` 是设计和数据格式的权威来源。功能设计尚未等同于已实现功能；开发前应先确认相应代码、
> 数据文件和测试入口是否已经存在。

## 1. 快速启动

### 环境

- JDK `17`。项目通过 Gradle toolchain 固定使用 Java 17。
- Windows 下使用仓库自带的 `gradlew.bat`；其他系统使用 `./gradlew`。
- 首次执行 Gradle 会下载 Forge、映射和开发期依赖，需要网络连接。
- 开发环境依赖由 Gradle 自动解析，无需手工把前置模组复制到 `run/mods/`。修改运行时依赖时，
  同步检查 `build.gradle` 与 `src/main/resources/META-INF/mods.toml`。

当前版本定义在 `gradle.properties`：

| 项目             | 版本                            |
|----------------|-------------------------------|
| Minecraft      | `1.20.1`                      |
| Forge          | `47.4.22`                     |
| Java           | `17`                          |
| Gradle Wrapper | `8.8`                         |
| 映射             | Parchment `2023.09.03-1.20.1` |

在 PowerShell 中执行：

```powershell
.\gradlew.bat runClient
```

开发客户端的工作目录是 `run/`。第一次成功启动后，开发环境配置会生成在：

```text
run/config/ninecun_lingtai-common.toml
```

常用任务：

```powershell
# 编译、测试、重混淆并打包
.\gradlew.bat build

# 仅编译主代码
.\gradlew.bat classes

# 启动客户端
.\gradlew.bat runClient

# 启动无界面专用服务器
.\gradlew.bat runServer

# 执行 JUnit 测试
.\gradlew.bat test

# 运行 Forge GameTest Server（有 GameTest 时使用）
.\gradlew.bat runGameTestServer

# 运行数据生成，输出到 src/generated/resources/
.\gradlew.bat runData
```

正式构建产物位于 `build/libs/ninecun_lingtai-1.0.0.jar`。不要提交 `build/`、`run/`、`run-data/`、
`.gradle/` 或 IDE 工作区文件。

### IDE

以 Gradle 项目导入仓库根目录即可。运行和调试时优先使用 Gradle 的 `runClient`、`runServer`、
`runData` 任务，确保 ForgeGradle 的类路径、Mixin refmap 和 transformation service 均被正确配置。

`build.gradle` 将 `transform/` 与 `utility/launch/` 放进独立的 `transformationService` 源集，并在运行
任务中自动生成、挂载开发用 jar。不要把这些类移回普通 `main` 源集，也不要手工修改运行类路径。

## 2. 先读什么

按开发目标阅读相应文档，避免只依据名称猜测规则：

| 目标                        | 文档                                             |
|---------------------------|------------------------------------------------|
| 世界观、灵根、灵气、境界与寿元           | [docs/一些基础设定.md](一些基础设定.md)               |
| 五行相生相克与环境灵气               | [docs/五行相生.md](五行相生.md)                   |
| 功法、法术和防御法术规则              | [docs/功法.md](功法.md)                       |
| 功法数据包字段                   | [docs/功法数据包格式.md](功法数据包格式.md)             |
| 伤害元素和环境规则数据包              | [docs/伤害元素JSON说明.md](伤害元素JSON说明.md)       |
| 炼丹设计                      | [docs/炼丹.md](炼丹.md)                       |
| 储物戒指设计                    | [docs/储物戒指.md](储物戒指.md)                   |
| 灵石生成和品级                   | [docs/灵石.md](灵石.md)                       |
| Filters API 的复杂 NBT 创意栏兼容 | [docs/Fitter API 修复.md](Fitter%20API%20修复.md) |
| 前置和开发辅助模组                 | [docs/必须(前置内容)：.md](docs/必须(前置内容)：.md)         |

`docs/修仙_by at(或许可以参考一下？)/` 下的资料是世界观和灵感参考，不应在没有明确设计确认的
情况下直接视为当前实现规范。

## 3. 工程导航

主入口是 `src/main/java/com/seraphina/ninecun_lingtai/ModMain.java`。它负责注册内容、通用配置和
网络通道。

```text
src/main/java/com/seraphina/ninecun_lingtai/
  api/          对外可复用的领域 API 与数据类型
  client/       仅客户端：界面、输入、渲染、动画、客户端缓存和兼容
  common/       双端共享及服务端权威逻辑
    ability/     威压、移山等能力
    block/       方块和方块实体
    command/     /ninecun_lingtai 调试命令
    config/      Forge 配置定义
    data/        服务端数据包重载监听器
    entity/      自定义实体
    event/       Forge 事件订阅
    item/        物品逻辑
    menu/        服务端菜单
    network/     ModNetwork 和 packet
    player/      玩家持久化/同步数据与管理器
    registry/    DeferredRegister 集中注册
    spell/       法术实现
    tribulation/ 渡劫状态与流程
  integration/  可选模组集成，例如 Jade
  mixin/        对原版和第三方模组的 Mixin
  transform/    ModLauncher transformation service
  utility/      可复用、无状态的工具方法

src/main/resources/
  assets/ninecun_lingtai/  客户端资源：模型、纹理、语言、shader、音效、动画
  data/ninecun_lingtai/    服务端数据：功法、丹方、伤害元素、环境、世界生成等
  META-INF/                mods.toml 和 access transformer
```

### 模块边界

- 除客户端定向网络包通过 `DistExecutor` 进入客户端缓存的现有模式外，`common/` 不得依赖
  `net.minecraft.client.*`、Forge client event 或屏幕/渲染类。
- 客户端界面、渲染、按键和客户端缓存放在 `client/`；`ClientModEventHandler` 统一注册菜单屏幕、
  键位、渲染器和 shader。
- 游戏规则、物品状态、方块实体、玩家数据、伤害结算和权限判断以服务端为权威，放在 `common/`。
- 可供其他模块或未来附属模组使用的数据类型、查询入口和扩展点放在 `api/`，不要让 API 反向依赖
  具体 screen、packet 或 manager 实现。
- 多处需要的纯逻辑放在 `utility/`，例如 `TechniqueJsonUtility`、`AlchemyRecipeJsonUtility`。
  工具类保持无状态，命名使用 `XxxUtility`。
- 只为第三方模组存在的实现放在 `integration/` 或 `client/compat/`，并确保目标模组不存在时不会
  加载相关类。

## 4. 代码约定

本项目使用包拆分，不把所有类堆在一个包中。按领域和运行侧创建子包，例如
`common/entity/ability`、`common/network/packet`、`client/render/layer`。

- 字段使用 lower camel case，例如 `thisIsAField`。
- 常量使用 upper snake case，例如 `THIS_IS_A_CONSTANT`。
- 类、枚举、record 使用 PascalCase；方法使用 lower camel case。
- 配置类位于 `common/config/`，通用配置命名为 `CommonConfig`，仅客户端配置命名为
  `ClientConfig`。当前项目已有 `CommonConfig`。
- 配置项使用 `ForgeConfigSpec` 定义，按功能通过 `BUILDER.push(...)` 分组；不要在客户端同步
  API Key 等服务端私密配置。
- 需要在多个位置复用的逻辑先评估是否应抽到 `utility/`，不要复制粘贴。
- 新增文字必须同时检查 `assets/ninecun_lingtai/lang/zh_cn.json` 和 `en_us.json`。
- 代码文件为 UTF-8；Java 编译已在 Gradle 中显式设置 UTF-8。

## 5. 内容注册与客户端配套

注册类集中在 `common/registry/`，由 `ModMain` 在 mod event bus 上注册：

- `ModBlocks`、`ModItems`
- `ModBlockEntityTypes`、`ModMenuTypes`
- `ModEntityTypes`
- `ModSpells`
- `ModSounds`
- `ModCreativeModeTabs`

新增内容时遵循现有注册类，不要在业务类的静态初始化中直接注册对象。

### 新增方块、物品或实体

除 Java 实现和 registry 外，通常还需要同步检查：

- `assets/ninecun_lingtai/lang/` 中的中英文名称；
- 方块状态、模型、物品模型、纹理和音效资源；
- 方块掉落表、标签、配方、世界生成等服务端数据；
- 方块实体的 `ModBlockEntityTypes`、菜单的 `ModMenuTypes`；
- 实体的属性注册、客户端 renderer/model layer；
- 创意栏展示。对于同一 `Item` 的复杂 NBT 变体，使用现有缓存和 Filters API 兼容机制，不能只
  添加一个空 `ItemStack`。

客户端注册入口是 `client/event/ClientModEventHandler.java`。服务端逻辑不应通过该类或其他客户端
类间接加载。

## 6. 网络和状态同步

所有包在 `common/network/ModNetwork.java` 的单一 `SimpleChannel` 中注册。新增包应：

1. 放在 `common/network/packet/`。
2. 定义 `encode`、`decode` 和 `handle`，并在 `ModNetwork.register()` 中以唯一递增 ID 注册。
3. 明确方向：客户端请求使用 `PLAY_TO_SERVER`，同步和打开界面使用 `PLAY_TO_CLIENT`。
4. 在服务端处理客户端包时，从 `context.getSender()` 获取玩家；检查玩家状态、手持物、目标、
   距离、权限和资源，不信任客户端传来的结论。
5. 用 `PacketDistributor.PLAYER` 或 `TRACKING_ENTITY_AND_SELF` 精确发送状态，避免无差别广播。
6. 客户端包处理只更新客户端缓存、界面或表现，不在客户端决定修为、背包、伤害或世界状态。

现有示例可参考：

- 客户端请求、服务端校验：`SetMeditationStatePacket`
- 服务端玩家数据快照：`SyncCultivationInfoPacket`
- 跟踪者同步：`ModNetwork.sendDefenseSpellState`

修改编码字段、顺序、方向或语义时必须同步修改协议版本 `PROTOCOL_VERSION`，并用客户端连接
专用服务器验证。

## 7. 数据包与热重载

功法、丹方、伤害元素和元素环境由服务端数据包加载。默认内容位于：

```text
src/main/resources/data/ninecun_lingtai/
  techniques/
  alchemy_recipes/
  damage_elements/
  element_environments/
```

对应加载器是 `common/data/` 中的 `TechniqueManager`、`AlchemyRecipeManager`、
`DamageElementManager` 和 `ElementEnvironmentManager`，通过 Forge 的 reload listener 注册。
在运行中的世界修改 JSON 后执行：

```mcfunction
/reload
```

然后查看日志中 `Loaded ...` 和 `Failed to load ...`。单个 JSON 解析失败会记录错误并跳过该定义，
因此不能只凭 `/reload` 没有中断来判断数据有效。

数据包开发规则：

- 将可配置内容优先做成 JSON，而不是把数值和内容写死在 Java 中。
- ID 由文件路径决定；`data/example/techniques/sword/azure_sword.json` 的 ID 是
  `example:sword/azure_sword`。
- 功法格式以 [docs/功法数据包格式.md](docs/功法数据包格式.md) 为准。
- 丹方必须包含一个主药、两个次药、两个辅药；同一个输出物品只能由一个已加载丹方定义。
- 伤害元素、环境维度/群系/天气倍率以
  [docs/伤害元素JSON说明.md](docs/伤害元素JSON说明.md) 为准。
- 服务器数据变更后，`OnDatapackSyncEvent` 会重新向玩家发送功法和丹方库；验证时仍应重新打开
  对应界面，确认实际客户端显示。

`runData` 的输出目录是 `src/generated/resources/`，已被加入主资源集。运行后先审查生成差异，只提交
确有必要的资源文件。

## 8. 功法、法术与扩展点

功法定义是数据驱动的；功法效果通过 `TechniqueEffectApi.register(...)` 注册处理器。新增效果类型时，
在公共初始化路径注册唯一的 `ResourceLocation`，并让处理器只处理与自己类型对应的参数。

法术实现位于 `common/spell/`，注册位于 `ModSpells`。增加新的可施放法术通常需要同时完成：

1. 实现 `Spell` 接口或复用现有法术实现模式。
2. 在 `ModSpells` 注册。
3. 在相应功法 JSON 中声明法术数据。
4. 添加选中、施放、灵力消耗、冷却、服务端校验和必要的同步。
5. 如有视觉效果，在 `client/` 中添加 renderer、动画、粒子或 shader，而不是污染公共实现。

功法、丹方、玉简和丹方卷轴都以服务器当前加载的数据为准。物品 NBT 只保存用于定位和展示的
标识信息；任何决定学习、烙印、使用或产出的操作必须由服务端再次查询当前数据并校验。

## 9. 配置和 AI

通用配置在 `common/config/CommonConfig.java` 定义。开发客户端启动后使用：

```text
run/config/ninecun_lingtai-common.toml
```

关键分组包括 `elementDamage`、`defenseSpells`、`meditation`、`tribulation`、`lifespan` 和 `ai`。

AI 功法参悟的配置只应保存在服务端：

```toml
[ai]
provider = "OPENAI"
apiKey = ""
model = ""
```

当前支持 `OPENAI` 与 `DEEPSEEK`。不得将实际 API Key 写入源码、示例数据、截图、提交记录或
共享客户端配置。空 API Key 或模型 ID 会在实际请求时失败，这是预期的保护行为。

## 10. 调试与验证

拥有权限等级 `2` 的玩家可使用：

```mcfunction
/ninecun_lingtai debug help
```

调试子命令覆盖玩家数据、资质、境界、灵力、打坐、寿元、生物、功法和通知。常见入口：

```mcfunction
/ninecun_lingtai debug aptitude ...
/ninecun_lingtai debug cultivation ...
/ninecun_lingtai debug energy ...
/ninecun_lingtai debug meditation ...
/ninecun_lingtai debug technique ...
```

以游戏内自动补全和 `/ninecun_lingtai debug help` 的当前输出为准。调试命令仅用于开发世界，
不要把依赖它们的流程当成正式玩法。

提交前最低验证：

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

按改动范围补充人工验证：

| 改动 | 必做验证 |
| --- | --- |
| 数据包 JSON | `/reload`、日志无解析错误、界面与实际效果均正确 |
| 注册/资源 | `runClient` 启动，创造栏、语言、模型和掉落符合预期 |
| 客户端界面或渲染 | 独立客户端启动，重进世界后确认状态恢复 |
| 网络包或玩家数据 | `runServer` 加 `runClient` 联机，验证服务端拒绝非法请求并正确同步 |
| 方块实体/容器 | 放置、打开、断开重连、破坏、掉落和数据保存 |
| Mixin 或 transformation service | 运行客户端和专用服务器，检查启动日志、目标模组兼容和崩溃报告 |

项目当前没有可运行的 JUnit 测试类或 Forge GameTest。新增跨模块计算、JSON 解析、网络数据编解码
或容易回归的规则时，应在 `src/test/java/` 添加 JUnit 测试；涉及游戏内行为时补充 GameTest。

## 11. 提交检查

- 只提交与本次功能有关的 Java、资源、数据和文档。
- 不提交 `run/config/` 中的本地 API Key、存档、日志、构建产物或 IDE 元数据。
- 新增功能时同步更新相关 `docs/` 设计/格式说明；修改现有 JSON 字段或游戏规则时必须更新其格式文档。
- 任何客户端请求都必须有服务端校验；任何服务端状态变化都必须考虑客户端同步、重连和数据包重载。
- 对两个或更多位置复用的逻辑，优先抽为语义清晰的 `XxxUtility` 或既有 manager 的方法。
- 提交前运行 `.\gradlew.bat build`，并检查 `git status` 确认没有误带本地文件。
