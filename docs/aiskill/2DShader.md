# Forge 1.20.1 2D Shader 实现技能（唯一参考：ArcaneVortex 0.6.8）

> **适用范围**：Minecraft Forge 1.20.1 客户端的 GUI、HUD、全屏、屏幕后处理（PostChain）与 2D 程序化特效。  
> **唯一技术参考**：`C:/Users/Administrator/Downloads/ArcaneVortex [Alpha]-0.6.8-1.20.1-Forge.jar`。本文只从该 JAR 已包含的 shader 资源、后处理链和客户端管理类抽取设计规律；不依赖或假定任何其他 Mod、项目源码或网络资料。  
> **默认 GLSL**：`#version 150`。ArcaneVortex 的常规 Shader 使用这一版本；除非已经在目标运行环境验证过加载器与显卡上下文，禁止为方便而改用 `#version 330`、`#version 400`、`layout(...)`、compute shader 或其他高版本特性。

---

## 0. 目标、边界与执行原则

2D Shader 不是“把一段 GLSL 塞进 GUI”的同义词。完整实现至少由五个相互严格匹配的部分构成：

1. **资源声明**：`assets/<modid>/shaders/core/*.json` 或 `shaders/program/*.json`；
2. **顶点阶段**：顶点格式、坐标空间、UV 与 varying 输出；
3. **片元阶段**：采样、颜色、时间、遮罩、混合和透明度；
4. **Java 客户端生命周期**：注册、缓存、uniform 更新、窗口缩放、资源重载、世界退出；
5. **渲染状态恢复**：批处理 flush、纹理绑定、blend/depth/cull/颜色状态，以及异常时的 `finally` 恢复。

任何一个环节漏掉，常见结果都是黑屏、纯白四边形、shader 找不到、uniform 永远为默认值、HUD 在不同 GUI 缩放下错位、后处理只显示一帧，或进入世界数次后显存泄漏。

### 0.1 本技能的两条实现路线

| 路线 | 资源位置 | 驱动对象 | 适合的效果 | ArcaneVortex 参考 |
|---|---|---|---|---|
| **Core 2D Shader** | `shaders/core/<name>.json/.vsh/.fsh` | `ShaderInstance` + 自定义 `RenderType` / 立即绘制 | HUD 图标、法阵、按钮、进度条、局部遮罩、文字/精灵变色 | `flowing_gradient`、`text`、`iap` 等 |
| **PostChain** | `shaders/post/<chain>.json` 与 `shaders/program/<program>.*` | `PostChain` | 主画面反色、扭曲、扫描、局部全屏过渡、多 pass 效果 | `invert_colors`、`warp` 和 `InvertShaderManager` |

**选择标准**：

- 只需要画在某块 GUI 区域、需要与组件层级和裁剪区域协作：优先 Core 2D Shader。
- 必须读取已经渲染好的世界/界面画面，作用于整个主 RenderTarget：使用 PostChain。
- 需要局部全屏特效时，不要先假定 PostChain 可以“天然裁剪”；应明确 mask、stencil、额外 target 或在 Core shader 中绘制覆盖层的方案。
- 不得每帧在两条路线之间来回创建对象。Shader、RenderType、PostChain 都应该有明确的缓存与释放边界。

### 0.2 目录与 ResourceLocation 映射

ArcaneVortex JAR 的结构明确区分为：

```text
assets/arcanevortex/shaders/
├── core/       # ShaderInstance / RenderType 直接使用的 shader
├── post/       # 后处理链（target 与 pass 的编排）
└── program/    # 后处理 pass 的 JSON、vertex shader、fragment shader
```

在你的 Mod 中，把 `arcanevortex` 替换成自己的 `modid`：

```text
src/main/resources/assets/<modid>/shaders/core/aura_panel.json
src/main/resources/assets/<modid>/shaders/core/aura_panel.vsh
src/main/resources/assets/<modid>/shaders/core/aura_panel.fsh

src/main/resources/assets/<modid>/shaders/post/screen_pulse.json
src/main/resources/assets/<modid>/shaders/program/screen_pulse.json
src/main/resources/assets/<modid>/shaders/program/screen_pulse.vsh
src/main/resources/assets/<modid>/shaders/program/screen_pulse.fsh
```

名称解析必须一一对应：

- `new ResourceLocation(MOD_ID, "aura_panel")` 对应 `shaders/core/aura_panel.json`；
- post chain 的 `new ResourceLocation(MOD_ID, "shaders/post/screen_pulse.json")` 对应这个确切资源文件；
- post pass 的 `"name": "<modid>:screen_pulse"` 对应 `shaders/program/screen_pulse.json`；
- program JSON 内 `"vertex": "<modid>:screen_pulse"` 与 `"fragment": "<modid>:screen_pulse"` 对应 `program/screen_pulse.vsh`、`program/screen_pulse.fsh`。

大小写、下划线、命名空间均为契约的一部分。不要仅因为 JAR 中个别复杂 shader 可观察到 uniform 命名风格不统一，就让自己的新资源出现 `screenSize`/`ScreenSize` 或 `time`/`Time` 混用。**JSON、GLSL 与 Java 查询的名称必须字符级一致。**

---

## 1. ArcaneVortex 的 2D 设计可直接学习的内容

### 1.1 `flowing_gradient`：遮罩驱动的滚动多色渐变

JAR 中的 `core/flowing_gradient` 使用：

- attributes：`Position`、`UV0`；
- sampler：`Sampler0`；
- 参数：`time`、`FlowSpeed`、`colorCount`、`alpha`、`gradientSpan`；
- 多个整型颜色 uniform（最多 16 个颜色停靠点）；
- 纹理采样的 RGB 平均亮度作为遮罩强度；
- 以 `fract(texCoord.x * gradientSpan - time * FlowSpeed)` 得到循环渐变进度。

这给出一个非常重要的结构：**纹理决定“哪里可以画”，Shader 算法决定“那里是什么颜色”。** 它适合给文字、图标、SVG 栅格化结果、法阵 alpha 图、进度条填充图加流光，而不是把所有细节烘焙成纹理。

实现时必须先决定遮罩的语义：

| 遮罩来源 | 适用素材 | 推荐写法 | 风险 |
|---|---|---|---|
| `sample.a` | PNG/字体 alpha 已正确 | `mask = sample.a` | 最直观，透明边缘不会被 RGB 污染 |
| `dot(sample.rgb, vec3(...))` | 黑白亮度图 | `mask = luminance(sample.rgb)` | 必须说明黑白方向，避免“白色才显示”成为隐含规则 |
| 独立 sampler | 正常纹理 + 单独 mask | `mask = texture(MaskSampler, uv).r` | sampler 增加，须显式绑定 |

JAR 的 16 个 `int` 颜色方式值得理解其目的（Java 易传 `0xRRGGBB`），但新实现不应不加约束地复制 16 段分支。颜色很多时，优先考虑：小型色带纹理、明确数量的 2/3/4 个 `vec4` uniform，或固定上限且所有下标都在 GLSL 允许范围内的方案。资源声明能够加载，比“理论上更灵活”更重要。

### 1.2 `invert_colors` / `warp`：将效果拆成“链”和“程序”

ArcaneVortex 的 `post/invert_colors.json` 定义链路，核心结构是：

```json
{
  "targets": ["swap", "temp"],
  "passes": [
    {
      "name": "arcanevortex:invert_colors",
      "intarget": "minecraft:main",
      "outtarget": "swap"
    },
    {
      "name": "minecraft:blit",
      "intarget": "swap",
      "outtarget": "minecraft:main"
    }
  ]
}
```

应从中学到以下规则：

- `minecraft:main` 是主画面的 RenderTarget；
- `swap` 是本链声明的中间 target；
- `minecraft:blit` 将中间结果写回主 target；
- JAR 虽声明 `temp`，但这个单效果链没有用到它。新链只能声明实际需要的 target；若要两个/多个 pass 的乒乓读写，才声明 `swap`、`temp` 等明确用途的 target；
- **同一个 pass 不能从某 target 采样又写回同一 target**。这会形成 feedback loop，结果未定义，通常表现为闪烁、黑屏或驱动相关错误。

其 `program/invert_colors` 的 vertex shader 只有 `Position`，再用 `ProjMat`、`InSize`、`OutSize` 构造全屏坐标。这和普通 GUI Core shader 的 `Position + UV0` 输入模式不同：PostChain 的全屏网格及 target 尺寸由后处理框架驱动，不能把 GUI shader 的属性定义随意挪过来。

---

## 2. Core Shader 的三方契约：VertexFormat、JSON、GLSL

### 2.1 必须同时匹配的三处

以最常用的 `Position + UV0` 为例，三处必须一致：

1. Java 选用的 `DefaultVertexFormat.POSITION_TEX`；
2. `aura_panel.json` 的 `"attributes": ["Position", "UV0"]`；
3. vertex shader 的 `in vec3 Position; in vec2 UV0;`（或匹配的分量数）。

```json
{
  "blend": {
    "func": "add",
    "srcrgb": "srcalpha",
    "dstrgb": "one_minus_src_alpha"
  },
  "vertex": "examplemod:aura_panel",
  "fragment": "examplemod:aura_panel",
  "attributes": ["Position", "UV0"],
  "samplers": [
    { "name": "Sampler0" }
  ],
  "uniforms": [
    { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1] },
    { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1] },
    { "name": "GameTime", "type": "float", "count": 1, "values": [0.0] },
    { "name": "ScreenSize", "type": "float", "count": 2, "values": [1.0, 1.0] },
    { "name": "Strength", "type": "float", "count": 1, "values": [1.0] }
  ]
}
```

这段 JSON 是可改名的原创模板，重点不是字段数量，而是每个字段都有一个明确生产者：

- `ModelViewMat`/`ProjMat` 由渲染框架或当前矩阵状态提供；
- `Sampler0` 由 Java 绑定纹理；
- `GameTime`、`ScreenSize`、`Strength` 由每次绘制前的 Java 逻辑写入；
- JSON 默认值只是加载和未写入时的安全值，**不是动画更新机制**。

### 2.2 属性不匹配时的表现与修复

| 错误 | 现象 | 根因 | 修复 |
|---|---|---|---|
| Java 发送 `POSITION_TEX`，JSON 只有 `Position` | UV 随机/全零/链接失败 | 顶点布局不一致 | 三处全部改为 Position+UV0，或 Java 改为 POSITION |
| GLSL 写 `in vec4 Color`，但 JSON/VertexFormat 没有 `Color` | shader 加载失败或颜色异常 | attribute 找不到 | 加 `Color` 并用 `POSITION_COLOR_TEX` 等正确格式 |
| JSON 是 `UV0`，GLSL 写 `UV` | 链接失败 | 名称不匹配 | 使用 Minecraft 约定属性名 `UV0` |
| position 分量混乱 | 四边形被裁掉/变成线 | `vec2/vec3/vec4` 契约错误 | GUI 顶点的常规输入使用 `vec3 Position` |

禁止“试几个名称看哪个能显示”。先从 Java 顶点格式反推 JSON，再从 JSON 精确写 GLSL 输入；新增数据时同步改完三处后再启动游戏验证。

---

## 3. GUI 坐标、像素尺寸与 UV 的正确协议

### 3.1 不要混用三种尺寸

2D 渲染最容易出现缩放错位，因为以下尺寸并不等价：

- **GUI 逻辑尺寸**：组件布局使用的宽高，受 GUI Scale 影响；
- **窗口 framebuffer 尺寸**：真实像素宽高，`gl_FragCoord` 和后处理 target 使用它；
- **纹理像素尺寸**：贴图或 mask 的原始宽高。

统一原则：

- GUI quad 的 `Position` 使用 GUI 当前矩阵定义的坐标；
- PostChain 的 `InSize`/`OutSize`/`ScreenSize` 使用 RenderTarget 或窗口 framebuffer 的实际像素尺寸；
- fragment 中用 `gl_FragCoord / ScreenSize` 得到屏幕 UV 时，`ScreenSize` 必须与 framebuffer 同源；
- 如果只是普通 GUI 纹理，传入插值 `UV0`，不要强行依赖 `gl_FragCoord`。

### 3.2 UV、像素与 aspect ratio

UV 的常用范围是 `[0, 1]`。如果要从中心计算圆形、波纹或径向扭曲，必须在计算距离前修正横纵比：

```glsl
vec2 centeredUv = uv * 2.0 - 1.0;
centeredUv.x *= ScreenSize.x / max(ScreenSize.y, 1.0);
float radius = length(centeredUv);
```

若不做修正，正方形 framebuffer 以外的屏幕上，数学圆会变成椭圆。ArcaneVortex 的反色程序中包含带 aspect 校正的椭圆/暗角模式，说明这不是装饰性细节，而是实际效果一致性的必要条件。

### 3.3 GUI 与 PostChain 不能共用错误的 UV 假设

- GUI shader：`UV0` 通常来自调用者提供的纹理坐标；
- PostChain：JAR 的 `invert_colors.vsh` 使用 `Position.xy / OutSize` 生成 `texCoord`；
- 全屏 clip/NDC quad：`Position` 可能是 `[-1,1]`，也可能由程序的 projection 转为 clip 坐标。必须以该 program vertex shader 的输入约定为准。

不要看见 `texCoord` 变量就假设一定是 GUI 贴图 UV；它只是一对被 vertex shader 传给 fragment shader 的插值数据。


---

## 4. Core 2D Shader 的客户端注册、持有与资源重载

### 4.1 只在客户端创建并保存 `ShaderInstance`

`ShaderInstance` 是 OpenGL 资源相关的客户端对象。公共代码、服务端初始化、数据包 handler 的服务端分支都不应触碰它。推荐把持有器放在客户端包，例如 `client/shader/ClientShaderHolder`；重复的 uniform 设置、状态恢复则封装在 `client/utility/ShaderRenderUtility`。字段遵守普通字段 `lowerCamelCase`、常量 `UPPER_SNAKE_CASE` 的命名约定。

下面是“资源重载时注册、平时只读取”的完整模式。事件总线具体归属应按你的 Forge 1.20.1 客户端注册位置接入，但禁止在 `render` 方法中 `new ShaderInstance(...)`：

```java
@OnlyIn(Dist.CLIENT)
public final class ClientShaderHolder {
    public static final ResourceLocation AURA_PANEL_ID =
            new ResourceLocation(ExampleMod.MOD_ID, "aura_panel");

    @Nullable
    private static ShaderInstance auraPanelShader;

    private ClientShaderHolder() {
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), AURA_PANEL_ID,
                        DefaultVertexFormat.POSITION_TEX),
                shaderInstance -> auraPanelShader = shaderInstance
        );
    }

    @Nullable
    public static ShaderInstance getAuraPanelShader() {
        return auraPanelShader;
    }
}
```

注意事项：

1. `ResourceLocation` 传的是逻辑名 `aura_panel`，不是 `shaders/core/aura_panel.json` 的文件路径；框架按 core shader 规则解析。
2. `DefaultVertexFormat.POSITION_TEX` 必须与 JSON 的 `Position`、`UV0` 以及 GLSL `in` 声明匹配。
3. 资源重载可能替换 shader 实例；不要缓存旧实例、旧 uniform 对象或旧 program id。
4. 取得 null 时的降级策略应是画普通纹理/纯色，而不是抛异常中断整个 HUD。
5. 不应给只包含静态方法的 utility 编写无意义构造方法；持有器因需阻止实例化可有私有构造方法。

### 4.2 RenderType 只描述状态，不存每帧参数

`RenderType` 适合缓存，因为它描述 shader、顶点格式、纹理、blend、深度和 cull 等稳定状态。`Strength`、`GameTime`、矩阵和屏幕尺寸是每次 draw 的数据，不能写进一个“每帧新建 RenderType”的错误模型。

概念性模板：

```java
@OnlyIn(Dist.CLIENT)
public final class ClientRenderTypes {
    private static final RenderType AURA_PANEL = RenderType.create(
            "examplemod_aura_panel",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            ClientShaderHolder::getAuraPanelShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            AURA_TEXTURE, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    private ClientRenderTypes() {
    }

    public static RenderType auraPanel() {
        return AURA_PANEL;
    }
}
```

这段模板的核心要求：

- GUI/HUD 一般使用无深度测试、颜色写入、透明混合、无背面剔除；
- 若叠加顺序由 GUI 决定，就不要让深度写入把后续 HUD 遮住；
- 选择 `TRANSLUCENT_TRANSPARENCY` 后 fragment 输出的 alpha 必须真的有意义；
- texture state 绑定的纹理必须和 GLSL 的 sampler 名/采样单元逻辑一致；
- 如果同一 shader 要画多种纹理，不能假设 RenderType 的单个 texture state 会自动解决所有 sampler。应在受控的 draw 前逐个绑定，并设计明确的 sampler 约定。

### 4.3 `GuiGraphics.flush()` 是状态边界

GUI 通常通过批处理收集顶点。你要切换到自定义 shader、手动绑定 sampler 或调整 RenderSystem 状态时，先让之前的 GUI 批次落地；绘制完后也应恢复预期状态，避免后面的原版组件“继承”你的 shader/blend。

建议封装为可复用工具方法：

```java
@OnlyIn(Dist.CLIENT)
public final class ShaderRenderUtility {
    private ShaderRenderUtility() {
    }

    public static void renderWithGuiShader(
            GuiGraphics guiGraphics,
            ShaderInstance shaderInstance,
            Runnable drawCall
    ) {
        guiGraphics.flush();
        try {
            RenderSystem.setShader(() -> shaderInstance);
            drawCall.run();
        } finally {
            guiGraphics.flush();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
```

这是“状态隔离”的骨架，而不是可以不经检查直接复制的万能方法。使用前要确认：

- 调用者是否已经安排了纹理绑定；
- 使用的顶点格式是否确为 PositionTex；
- 之后的 GUI 是否依赖特定 blend、depth 或 scissor 状态；
- drawCall 抛异常时，`finally` 是否恢复了所有由本次绘制改变的状态。

**不要在不理解当前批处理状态时混用 `GuiGraphics`、`BufferBuilder`、直接 GL 调用。** 一次 bug 修复必须包括“顶点提交在哪里发生”和“下一个绘制将看到什么状态”。

---

## 5. Sampler、uniform 与每帧参数生命周期

### 5.1 Sampler 是纹理接口，不是纹理文件名

在 core JSON 中：

```json
"samplers": [
  { "name": "Sampler0" }
]
```

在 fragment shader 中：

```glsl
uniform sampler2D Sampler0;
```

二者只是“shader 期待一个 2D 采样器”的接口。Java/RenderType 负责让正确纹理在绘制时绑定到它。`Sampler0` 是 Minecraft shader 生态常用名字，但不是魔法词；若定义 `MaskSampler`，JSON、GLSL 和绑定逻辑都必须共同使用 `MaskSampler`。

采样前先确定过滤和边界：

- HUD 图标、像素画：通常需要 nearest 过滤；
- 柔和 mask、渐变：通常适合 linear 过滤；
- 扭曲靠近边缘：决定 clamp 还是 repeat，不能让 UV 越界行为偶然决定画面；
- atlas 中采样：要避开相邻 sprite 边缘，必要时 inward clamp 或使用正确插值。

### 5.2 Uniform 查找和设置必须安全且有节制

`ShaderInstance#getUniform(name)` 可能得到 null：资源文件缺 uniform、名字拼错、资源重载过或加载失败时都可能出现。包装一个小工具，令调用点不必散落空指针判断：

```java
@OnlyIn(Dist.CLIENT)
public final class ShaderUniformUtility {
    private ShaderUniformUtility() {
    }

    public static void setFloat(ShaderInstance shaderInstance, String uniformName, float value) {
        Uniform uniform = shaderInstance.getUniform(uniformName);
        if (uniform != null) {
            uniform.set(value);
        }
    }

    public static void setVec2(ShaderInstance shaderInstance, String uniformName, float x, float y) {
        Uniform uniform = shaderInstance.getUniform(uniformName);
        if (uniform != null) {
            uniform.set(x, y);
        }
    }
}
```

使用要求：

- 只用这个工具掩盖“可选效果/资源暂不可用”的情况；开发期间仍要记录一次缺失名称，不能让拼写错误永久静默；
- 不要跨资源重载缓存 `Uniform` 引用；每次通过当前 `ShaderInstance` 获取，或者在重载后重新建立缓存；
- 每帧只写确实会变化的 uniform。恒定颜色、固定比例或不变采样参数可在状态变化时设置；
- 一个参数只能有一种单位协议，例如 `GameTime` 明确为秒，`Strength` 明确是 `[0,1]`，角度明确是弧度。

### 5.3 时间、进度与平滑变化

ArcaneVortex 的 `InvertShaderManager` 维护 `currentStrength`、`targetStrength` 和 `TRANSITION_DURATION = 0.5F`，用真实时间 delta 平滑逼近目标值。它传达的原则是：**shader 负责基于当前参数插值，不负责猜测何时开始、何时结束。**

推荐把动画状态写在客户端管理器：

```java
public final class ScreenEffectState {
    private float currentStrength;
    private float targetStrength;

    public void setEnabled(boolean enabled) {
        this.targetStrength = enabled ? 1.0F : 0.0F;
    }

    public void tick(float deltaSeconds) {
        float speed = 1.0F / 0.5F;
        this.currentStrength = Mth.approach(
                this.currentStrength,
                this.targetStrength,
                speed * Math.max(0.0F, deltaSeconds)
        );
    }

    public float getCurrentStrength() {
        return this.currentStrength;
    }
}
```

实现时禁止把 `System.currentTimeMillis()` 的毫秒数直接作为 GLSL `sin` 参数。应转为秒，控制上限，并在暂停、切换世界、窗口失焦等状态下决定动画是否继续。对所有频率参数给出单位：例如 `pulseFrequencyHz` 是每秒周期数，shader 内角频率为 `2π * Hz`。

### 5.4 Alpha、预乘与混合公式要配套

ArcaneVortex 的 program JSON 使用源 alpha / one-minus-source-alpha 的常见透明混合。默认非预乘 alpha 的 fragment 输出约定是：

```glsl
vec4 outputColor = vec4(rgb, alpha);
// rgb 不预先乘 alpha；混合使用 srcAlpha, oneMinusSrcAlpha。
```

若你选择预乘 alpha，则 fragment 必须输出 `rgb * alpha`，且 blend 因子改为适合预乘的组合。两种模型不能混用；混用会导致边缘发黑、光晕变暗或透明色溢出。一个 RenderType 内确定一种约定，并让所有素材与 shader 坚持它。

---

## 6. Core 2D 原创模板：滚动法阵遮罩

下面模板借鉴 `flowing_gradient` 的“mask + 动态颜色”结构，但算法和命名为原创简化版本。它示范怎样让一张透明法阵 PNG 决定形状，同时由 shader 生成动态流光。

### 6.1 `aura_panel.vsh`

```glsl
#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = UV0;
}
```

### 6.2 `aura_panel.fsh`

```glsl
#version 150

uniform sampler2D Sampler0;
uniform float GameTime;
uniform float Strength;
uniform vec2 ScreenSize;
uniform vec4 InnerColor;
uniform vec4 OuterColor;

in vec2 texCoord;
out vec4 fragColor;

float smoothPulse(float value) {
    return value * value * (3.0 - 2.0 * value);
}

void main() {
    vec4 maskSample = texture(Sampler0, texCoord);
    float mask = maskSample.a;

    vec2 centeredUv = texCoord * 2.0 - 1.0;
    centeredUv.x *= ScreenSize.x / max(ScreenSize.y, 1.0);

    float radial = clamp(length(centeredUv), 0.0, 1.0);
    float flow = 0.5 + 0.5 * sin(GameTime * 4.0 - radial * 18.0);
    float colorProgress = smoothPulse(flow);
    vec3 rgb = mix(InnerColor.rgb, OuterColor.rgb, colorProgress);

    float edgeGlow = smoothstep(0.85, 0.25, radial);
    float alpha = mask * Strength * mix(0.45, 1.0, edgeGlow);
    fragColor = vec4(rgb, alpha);
}
```

### 6.3 调用前检查

- 纹理是否真的有 alpha；若素材仅是黑白 RGB，替换 `maskSample.a` 为明确的亮度函数；
- `ScreenSize` 是否用 framebuffer 尺寸或此法阵 quad 的局部宽高。上例用于“圆形不被屏幕比例拉伸”时，可传 effect 区域尺寸；不要不经思考总传窗口尺寸；
- `Strength` 是否已 clamp 到 `[0,1]`；
- 传颜色时是否与非预乘 alpha 混合规则一致；
- 法阵纹理坐标若上下颠倒，应在 Java 顶点 UV 或 `texCoord.y = 1.0 - UV0.y` 中选**一个**位置修正，不要两边都翻转。


---

## 7. 2D 程序化效果：从 ArcaneVortex 的反色模式提炼可维护算法

ArcaneVortex 的 `invert_colors.fsh` 不只做全局 `1.0 - color`。它以 `UseType` 选择多种强度掩码和 UV 操作：全屏、圆形扩散、纵向扫描、FBM 噪声、Voronoi、裂缝感组合噪声、马赛克 hash、径向扭曲和带宽高比修正的椭圆/暗角。这说明一个“屏幕效果”应拆为两步：

1. **计算 effectMask / finalStrength**：效果在哪些像素、以多强作用；
2. **计算 effectColor 或 distortedUv**：该像素怎样变色或从哪里取样。

不要把十几种模式堆到一个无限膨胀的 fragment shader。对于可切换模式，固定少量 enum 方案；对于完全不同的效果，独立 program，避免所有分支每像素参与。

### 7.1 可复用 hash、noise 和 FBM

下方是原创的基础噪声工具。目的是解释结构，而非要求逐字符复制：

```glsl
float hash21(vec2 point) {
    point = fract(point * vec2(123.34, 456.21));
    point += dot(point, point + 45.32);
    return fract(point.x * point.y);
}

float valueNoise(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    vec2 eased = local * local * (3.0 - 2.0 * local);

    float a = hash21(cell);
    float b = hash21(cell + vec2(1.0, 0.0));
    float c = hash21(cell + vec2(0.0, 1.0));
    float d = hash21(cell + vec2(1.0, 1.0));

    return mix(mix(a, b, eased.x), mix(c, d, eased.x), eased.y);
}

float fbm(vec2 point) {
    float total = 0.0;
    float amplitude = 0.5;
    for (int octave = 0; octave < 5; octave++) {
        total += amplitude * valueNoise(point);
        point = point * 2.02 + vec2(17.1, 9.2);
        amplitude *= 0.5;
    }
    return total;
}
```

实施规则：

- 循环上限使用编译期常量，例如 `for (int octave = 0; octave < 5; octave++)`；不要让不受约束的 uniform 决定循环次数；
- `point` 的坐标尺度是视觉参数。`uv * 3` 与 `uv * 120` 的纹理密度相差巨大，应暴露为有名字的 `NoiseScale`；
- 噪声驱动时间时，通常使用 `fbm(uv * scale + vec2(time * speed, 0.0))`，并说明 speed 的单位是 UV 单位/秒；
- hash/value noise 不是物理随机数，目标是相邻片元之间连续或可控地不连续；
- 在全屏 post shader 中，5~6 octave 已经不轻。先用 2 octave 验证画面和性能，再增加层数。

### 7.2 Voronoi、马赛克和裂缝感

ArcaneVortex 使用 3×3 邻域的 Voronoi 思路：对当前 cell 及相邻 cell 的随机 feature point 求最小距离。这适合魔法碎片、细胞、结晶和裂缝底图。

```glsl
float voronoiDistance(vec2 point) {
    vec2 cell = floor(point);
    vec2 local = fract(point);
    float nearestDistance = 10.0;

    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 offset = vec2(float(x), float(y));
            vec2 feature = vec2(
                hash21(cell + offset),
                hash21(cell + offset + vec2(37.0, 11.0))
            );
            nearestDistance = min(nearestDistance, length(offset + feature - local));
        }
    }
    return nearestDistance;
}
```

衍生方式：

- `smoothstep(borderA, borderB, voronoiDistance(...))`：生成细胞边缘；
- `floor(uv * MosaicGrid)` 后 hash：生成马赛克随机块；
- `abs(fbm(a) - fbm(b))` 或 noise 阈值：生成裂缝带；
- 所有阈值要按分辨率与 `NoiseScale` 测试。屏幕高分辨率时一像素细线可能会闪烁，应使用 `fwidth`（若环境验证可用）或软 `smoothstep` 过渡，而不是硬 `step`。

### 7.3 SDF、圆形扩散与扫描线

圆心扩散的基础不是“直接比较 UV”，而是先定义中心、比例和 feather：

```glsl
float circularMask(vec2 uv, vec2 aspect, vec2 center, float radius, float feather) {
    vec2 delta = uv - center;
    delta.x *= aspect.x / max(aspect.y, 1.0);
    float distanceToCenter = length(delta);
    return 1.0 - smoothstep(radius - feather, radius + feather, distanceToCenter);
}
```

- `radius` 应为 0~1 的归一化半径；
- `feather` 是边缘软化宽度，不能为 0；
- 扩散动画可用 `radius = mix(startRadius, endRadius, progress)`；
- `progress` 必须来自 Java 状态，不要在 shader 中根据“某个绝对时间”猜测开始时间。

纵向扫描可写为：

```glsl
float scanBand = 1.0 - smoothstep(
        bandHalfWidth,
        bandHalfWidth + softEdge,
        abs(uv.y - scanProgress)
);
```

这样扫描条的中心由 `scanProgress` 控制；若要让扫描后的区域永久受影响，使用 `smoothstep(scanProgress - feather, scanProgress + feather, uv.y)` 形成累计区域，再与带状高亮分开合成。

### 7.4 极坐标、径向扭曲与安全 UV

ArcaneVortex 的某个模式会先计算扭曲 UV，再采样主画面。实现这种效果时必须保证采样坐标和强度边界可控：

```glsl
vec2 radialDistort(vec2 uv, vec2 screenSize, float strength, float time) {
    vec2 centered = uv * 2.0 - 1.0;
    centered.x *= screenSize.x / max(screenSize.y, 1.0);

    float radius = length(centered);
    vec2 direction = radius > 0.0001 ? centered / radius : vec2(0.0);
    float wave = sin(radius * 28.0 - time * 5.0);
    centered += direction * wave * strength * 0.025;

    centered.x *= screenSize.y / max(screenSize.x, 1.0);
    return clamp(centered * 0.5 + 0.5, vec2(0.001), vec2(0.999));
}
```

关键点：

- 除以半径前检查接近 0 的情况，避免中心 NaN；
- `strength` 应先 clamp；
- 采样 UV 是否 clamp 取决于美术目标，但必须主动规定；
- 以 `ScreenSize` 矫正后，再反向取消 x 比例；
- 扭曲强度应随 mask 变化，例如 `mix(uv, distortedUv, effectMask)`，而非整个画面无差别扭曲。

---

## 8. PostChain：从 JSON 到 process 的完整管线

### 8.1 链文件只负责编排 target 与 pass

一个单 pass 全屏脉冲的链可写为：

```json
{
  "targets": ["swap"],
  "passes": [
    {
      "name": "examplemod:screen_pulse",
      "intarget": "minecraft:main",
      "outtarget": "swap"
    },
    {
      "name": "minecraft:blit",
      "intarget": "swap",
      "outtarget": "minecraft:main"
    }
  ]
}
```

JAR 的 `invert_colors`、`warp` 采用的正是“主画面 -> 中间 target -> blit 回主画面”结构。链 JSON 中不能在同 pass 中把 `minecraft:main` 同时作为 `intarget` 和 `outtarget`。

多 pass 时才做 ping-pong：

```text
minecraft:main -> swap  (pass A)
swap           -> temp  (pass B)
temp           -> swap  (pass C)
swap           -> minecraft:main (blit)
```

每一步清楚写出读者和写者；不使用的 target 不声明。若 pass B 既需要 A 的颜色又需要另一张静态纹理，静态纹理由 program sampler 绑定，A 的颜色仍是 `intarget`，不要冒充成“第二个主 target”。

### 8.2 program JSON 是全屏 pass 的接口声明

```json
{
  "blend": {
    "func": "add",
    "srcrgb": "srcalpha",
    "dstrgb": "one_minus_src_alpha"
  },
  "vertex": "examplemod:screen_pulse",
  "fragment": "examplemod:screen_pulse",
  "attributes": ["Position"],
  "samplers": [
    { "name": "DiffuseSampler" }
  ],
  "uniforms": [
    { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1] },
    { "name": "InSize", "type": "float", "count": 2, "values": [1.0, 1.0] },
    { "name": "OutSize", "type": "float", "count": 2, "values": [1.0, 1.0] },
    { "name": "ScreenSize", "type": "float", "count": 2, "values": [1.0, 1.0] },
    { "name": "Strength", "type": "float", "count": 1, "values": [0.0] },
    { "name": "GameTime", "type": "float", "count": 1, "values": [0.0] }
  ]
}
```

JAR 的 `program/invert_colors.json` 同样包含 `Position`、`DiffuseSampler`、`ProjMat`、`InSize`、`OutSize`，并扩展了 `InvertStrength`、`ScreenSize`、`UseType`。它展示的是 **PostChain program 的属性契约**：不要把普通 core shader 的 `UV0` 强行加入，除非你的全屏顶点生成与 program JSON 都为此设计。

### 8.3 最小而完整的全屏 vertex shader

```glsl
#version 150

in vec4 Position;

uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;

out vec2 texCoord;

void main() {
    vec4 projected = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(projected.xy, 0.2, 1.0);
    texCoord = Position.xy / max(OutSize, vec2(1.0));
}
```

这是沿着 JAR `invert_colors.vsh` 的接口形状写出的模板。重点：

- `Position` 的真实数值范围由 PostChain 全屏网格定义，不能假定和 GUI quad 一样；
- `OutSize` 为零必须防守性处理；
- `InSize` 适合推导源纹理一个 texel 的偏移：`vec2 oneTexel = 1.0 / max(InSize, vec2(1.0));`；
- `gl_Position.z` 和 `w` 应保持适用于该全屏 pass 的稳定值；若改投影逻辑必须测试深度/排序影响。

### 8.4 原创 screen pulse fragment shader

```glsl
#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 ScreenSize;
uniform float Strength;
uniform float GameTime;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 safeScreenSize = max(ScreenSize, vec2(1.0));
    vec2 centered = texCoord * 2.0 - 1.0;
    centered.x *= safeScreenSize.x / safeScreenSize.y;

    float radius = length(centered);
    float ring = 1.0 - smoothstep(0.08, 0.18, abs(radius - fract(GameTime * 0.35)));
    float vignette = smoothstep(1.25, 0.20, radius);
    float mask = clamp((ring * 0.65 + vignette * 0.20) * Strength, 0.0, 1.0);

    vec2 oneTexel = 1.0 / max(InSize, vec2(1.0));
    vec2 shiftedUv = clamp(texCoord + centered * oneTexel * 8.0 * mask, 0.001, 0.999);
    vec4 source = texture(DiffuseSampler, shiftedUv);
    vec3 tint = vec3(0.30, 0.65, 1.00);

    fragColor = vec4(mix(source.rgb, source.rgb + tint * 0.18, mask), source.a);
}
```

这个模板刻意不做真正反色，以展示正确的 PostChain 组织方式：先采样已经完成的场景，再按 target 尺寸生成 texel 偏移，最后输出到**另一个** target。不要把 `DiffuseSampler` 当普通 GUI 图标；它代表当前 pass 的输入画面。


---

## 9. PostChain 管理器：必须实现完整生命周期

ArcaneVortex 的 `com.erchien.arcanevortex.TimeStop.InvertShaderManager` 展示的不是“加载一个 chain 后调用 process”这么简单。JAR 内可观察到的状态包括：

- `isInverting`：逻辑开关；
- `currentStrength`、`targetStrength`：当前视觉强度和目标强度；
- `TRANSITION_DURATION = 0.5F`：淡入淡出时长；
- `invertShader`：缓存的 `PostChain`；
- `lastWidth`、`lastHeight`：上次 target 尺寸；
- `shaderLoadFailed`：加载失败熔断；
- `lastUpdateTime`：真实时间 delta 的基准。

它还会在窗口尺寸变更时 resize，在处理完后重新绑定主 RenderTarget，在客户端世界不再存在时 cleanup。新实现必须保持这一完整性，而不是“每帧 new PostChain”或“加载一次永不释放”。

### 9.1 管理器的状态机

建议将状态明确建模：

```text
Disabled
  └─ 请求开启 → FadingIn / Active
FadingIn
  ├─ strength 达 1 → Active
  └─ 请求关闭 → FadingOut
Active
  └─ 请求关闭 → FadingOut
FadingOut
  ├─ strength 达 0 → Disabled（可以保留短暂缓存，也可以关闭 chain）
  └─ 请求开启 → FadingIn
LoadFailed
  └─ 资源重载、明确重试或下次客户端会话才允许重新尝试
```

网络同步与视觉状态必须分开：服务端同步“技能是否生效/剩余时间/效果类型”等权威游戏状态；客户端管理器根据同步状态调整 `targetStrength` 并渲染。JAR 的逻辑会在本地有 client level 时经由请求/状态路径驱动，而非将视觉 bool 误认为唯一游戏真相。不要让 shader toggle 成为服务端判定的依据。

### 9.2 结构化伪代码

以下为 API 名称可能因 Forge mappings 有差异的结构化模板；实现时应以你的 1.20.1 编译环境方法签名为准，但不能删掉任一生命周期职责：

```java
@OnlyIn(Dist.CLIENT)
public final class ScreenPulseManager {
    private static final ResourceLocation SCREEN_PULSE_CHAIN =
            new ResourceLocation(ExampleMod.MOD_ID, "shaders/post/screen_pulse.json");
    private static final float TRANSITION_DURATION_SECONDS = 0.50F;

    @Nullable
    private PostChain screenPulseChain;
    private boolean enabled;
    private boolean shaderLoadFailed;
    private int lastWidth = -1;
    private int lastHeight = -1;
    private long lastUpdateNanos;
    private float currentStrength;
    private float targetStrength;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.targetStrength = enabled ? 1.0F : 0.0F;
    }

    public void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || this.shaderLoadFailed) {
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds = this.lastUpdateNanos == 0L
                ? 0.0F
                : Math.min((now - this.lastUpdateNanos) / 1_000_000_000.0F, 0.10F);
        this.lastUpdateNanos = now;

        float approachSpeed = 1.0F / TRANSITION_DURATION_SECONDS;
        this.currentStrength = Mth.approach(
                this.currentStrength,
                this.targetStrength,
                approachSpeed * deltaSeconds
        );

        if (this.currentStrength <= 0.0001F && !this.enabled) {
            return;
        }

        int framebufferWidth = minecraft.getWindow().getWidth();
        int framebufferHeight = minecraft.getWindow().getHeight();
        if (!this.ensureLoadedAndResized(minecraft, framebufferWidth, framebufferHeight)) {
            return;
        }

        this.applyUniforms(framebufferWidth, framebufferHeight);
        try {
            this.screenPulseChain.process(partialTick);
        } finally {
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private boolean ensureLoadedAndResized(Minecraft minecraft, int width, int height) {
        try {
            if (this.screenPulseChain == null) {
                this.screenPulseChain = new PostChain(
                        minecraft.getTextureManager(),
                        minecraft.getResourceManager(),
                        minecraft.getMainRenderTarget(),
                        SCREEN_PULSE_CHAIN
                );
            }
            if (width != this.lastWidth || height != this.lastHeight) {
                this.screenPulseChain.resize(width, height);
                this.lastWidth = width;
                this.lastHeight = height;
            }
            return true;
        } catch (Exception exception) {
            this.shaderLoadFailed = true;
            this.cleanup();
            // 记录一次包含 ResourceLocation 的错误日志，避免每帧刷屏。
            return false;
        }
    }

    private void applyUniforms(int width, int height) {
        // 遍历本 chain 的 pass，安全地设置 Strength/GameTime/ScreenSize 等。
        // uniform 名称必须与 program JSON 和 GLSL 完全一致。
    }

    public void cleanup() {
        if (this.screenPulseChain != null) {
            this.screenPulseChain.close();
            this.screenPulseChain = null;
        }
        this.enabled = false;
        this.currentStrength = 0.0F;
        this.targetStrength = 0.0F;
        this.lastWidth = -1;
        this.lastHeight = -1;
        this.lastUpdateNanos = 0L;
    }
}
```

说明：

- `deltaSeconds` 要限幅。窗口切换、调试器暂停后首帧可能很大，若不限制，视觉强度会瞬间跳变；
- `shaderLoadFailed` 是熔断而非永久吞错。资源重载或新会话应允许重置后重试；
- `PostChain#resize` 必须发生在窗口/target 尺寸变化时，不是只在启动时；
- `process` 后必须把主 RenderTarget 重新 `bindWrite(true)`，否则后续渲染可能写到错误 framebuffer；
- `cleanup` 不只是赋 null，必须 `close()`；
- 在客户端 tick END 阶段或世界/服务器连接结束的确定位置检测 `level == null` 并 cleanup，正是 JAR 管理器体现的清理策略。

### 9.3 uniform 更新的正确粒度

后处理 program 可能有一个或多个 pass。更新时：

1. 先取得当前 chain 的 pass 列表；
2. 对每个确实使用 `Strength` 的 pass 设置平滑强度；
3. 对需要像素坐标的 pass 设置同源 `ScreenSize`；
4. 对需要时间的 pass 传秒值；
5. 缺失 uniform 只跳过并在开发期记录，不因一个可选 uniform 终止渲染；
6. 不要修改 `DiffuseSampler` 来试图输入主画面——它由 PostChain 的 `intarget` 注入。

如果 chain 有 pass A、B、C，别假定每一 pass 都有相同 uniform。可以按统一名称更新共有参数，但每个 shader 的独有参数需要精确匹配其 program JSON。

### 9.4 何时调用 render

后处理必须发生在“要处理的主画面已被渲染”和“结果还可写回主目标”的正确阶段。ArcaneVortex 的 GUI post 渲染路径调用管理器更新，说明它把该效果定位为屏幕最终展示层的一部分。你的选择必须写清：

- 想影响世界但不影响特定 GUI：在 GUI 前、世界结束后的合适阶段处理；
- 想影响 HUD/GUI：在其对应的最终 GUI 阶段处理；
- 多个 PostChain 效果：定义顺序，例如先颜色分级、后扭曲、最后暗角，并明确每个 effect 是覆盖还是叠加；
- 不要在同一帧的多个事件各调用一次 `process`，否则强度、target 内容和性能都会错。

将“唯一调用点”封装到 `client/utility/ScreenEffectRenderUtility` 或专门 manager 中，其他系统只提交状态请求。

---

## 10. 常见错误、验证顺序与性能预算

### 10.1 故障定位表

| 现象 | 优先检查 | 典型修复 |
|---|---|---|
| 资源加载报找不到 shader | ResourceLocation、文件目录、core/program/post 混用 | 逐段检查 modid、名称与文件位置；不要传错误扩展名 |
| 整块纯黑 | fragment 未写正确颜色、sampler 未绑定、PostChain feedback loop | 先输出 `vec4(1,0,1,1)` 验证 shader 运行，再验证采样和 target |
| 整块纯白 | mask 语义错、alpha 未使用、纹理坐标全落在白区 | 直接可视化 `mask`，检查 alpha/RGB 约定 |
| GUI 缩放后变形 | GUI 尺寸和 framebuffer 尺寸混用 | 明确每个 uniform 的尺寸来源；圆形计算加入 aspect correction |
| 后处理仅首次有效 | 未 resize、未 bindWrite、chain 被错误 close | 记录窗口尺寸与调用顺序，确保 process 后绑定主 target |
| 切世界后崩溃/显存上涨 | PostChain 未 close、旧 shader/uniform 被保留 | 在 level 为 null 和资源重载边界 cleanup/reacquire |
| 后续 UI 颜色/纹理错 | RenderSystem 状态泄漏、未 flush | 自定义状态前后 flush，`finally` 恢复 shader/color/blend 等 |
| 强度闪跳 | 毫秒直接传 GLSL、delta 未限幅、每帧重置当前值 | 使用秒、累计状态、平滑 approach |
| 高分辨率严重掉帧 | 全屏每像素多 octave/多 pass/多采样 | 降低 pass 数和 octave，添加 quality 模式或降低 effect 更新频率 |

### 10.2 开发时的可视化诊断方法

不要直接调一堆数学常量。按以下顺序替换输出：

1. `fragColor = vec4(1, 0, 1, 1)`：确认 shader 已被使用；
2. `fragColor = texture(Sampler0, texCoord)` 或 `DiffuseSampler`：确认 sampler 和 UV；
3. `fragColor = vec4(vec3(texCoord, 0), 1)`：确认 UV 朝向与范围；
4. `fragColor = vec4(vec3(mask), 1)`：确认遮罩；
5. `fragColor = vec4(vec3(Strength), 1)`：确认 Java uniform 更新；
6. `fragColor = vec4(vec3(fract(GameTime)), 1)`：确认时间单位和刷新；
7. 最后才叠加反色、噪声、扭曲、混色与 alpha。

每次只引入一个变量，才可以从画面症状准确回溯到契约哪一层断了。

### 10.3 性能与兼容性要求

- PostChain 的代价与 framebuffer 像素数直接相关。4K 下一个 6-octave、3×3 Voronoi、多个 texture fetch 的全屏 pass 可能非常昂贵；
- 每新增 pass 都意味着一次全屏读写和中间 RenderTarget；优先合并真正可合并的计算，但不要为了“少 pass”制造 feedback loop；
- 固定循环上限，避免依赖用户可配置数值的无限工作量。配置中若暴露质量，必须 clamp 到安全上限；
- 低端模式可关闭扭曲、从 6 octave 降为 2~3 octave、去掉 Voronoi 或降低 effect 强度；
- shader 加载失败必须回退到普通渲染，并避免每帧反复构造/记录异常；
- 所有客户端 shader 功能必须可在专用服务端安全缺席。服务端绝不加载客户端类。

---

## 11. 给 AI 的实施顺序（必须逐项完成）

1. **仅在客户端确定效果类型**：Core 局部渲染，还是 PostChain 全屏处理；不得混淆两种 JSON 格式。
2. **先制定接口表**：列出 VertexFormat、JSON attributes、GLSL inputs、samplers、uniform 名称、类型、单位、默认值和写入位置。
3. **创建 core 或 post/program 资源**：所有名称一次确定；使用 `#version 150`。
4. **先画诊断色**：确认 shader 实际被加载与调用。
5. **再接纹理/主画面**：确认 UV、mask 与 sampler。
6. **实现 Java 持有器与 utility**：注册时创建，渲染时读取；静态通用方法进 `client/utility/`，不写无意义构造方法。
7. **明确渲染状态**：在 GUI 批处理边界 flush；设置 blend/depth/cull；用 `try/finally` 恢复。
8. **若使用 PostChain，完整实现 manager**：惰性加载、平滑 strength、resize、pass uniform、`process` 后 `bindWrite`、world exit cleanup、失败熔断与回退。
9. **测试矩阵**：至少测试不同 GUI Scale、窗口尺寸变化、全屏切换、资源重载、切换世界/断开连接、shader 不可用时的降级和低质量模式。
10. **最后才做美术参数调优**：把所有魔数命名化，按秒、像素、UV 或归一化范围记录单位；避免将 JAR 中观察到的任意数值当作通用答案。

## 12. 完成清单

- [ ] 本次仅修改目标 shader 资源、客户端渲染代码或文档；没有把 client shader 类引入服务端路径。
- [ ] `core`、`post`、`program` 文件放在正确资源目录，ResourceLocation 一致。
- [ ] VertexFormat / JSON attributes / GLSL inputs 三方逐项一致。
- [ ] 所有 sampler/uniform 名称大小写一致，JSON 类型与 GLSL 类型一致。
- [ ] 时间、strength、角度和尺寸都标有单位与来源。
- [ ] GUI 和 framebuffer 尺寸没有混用；径向效果已校正 aspect ratio。
- [ ] 自定义 GUI 渲染前后处理 flush 和状态恢复。
- [ ] PostChain 没有读写同一 target；有 resize、bindWrite、close 和加载失败回退。
- [ ] 全屏算法的循环/采样次数有固定上限，并有质量降级方案。
- [ ] 已按诊断输出验证 shader、UV、sampler、mask、uniform 和最终混合。
