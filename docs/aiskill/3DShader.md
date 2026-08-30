# Forge 1.20.1 3D Shader 实现技能（唯一参考：ArcaneVortex 0.6.8）

> **适用范围**：Minecraft Forge 1.20.1 客户端的实体、方块实体、世界阶段特效、天空/球体、轨迹、程序水、半透明护盾、体积效果，以及依赖场景颜色/深度的屏幕空间 3D 效果。  
> **唯一技术参考**：`C:/Users/Administrator/Downloads/ArcaneVortex [Alpha]-0.6.8-1.20.1-Forge.jar`。文中的分类、资源布局、风险和设计规律均从其中的 shader、renderer、PostChain/RenderTarget 使用方式抽取；不以任何项目源码、其他 Mod 或网络样例为参考。  
> **默认 GLSL**：`#version 150`。ArcaneVortex 的普通 Minecraft Shader 资源采用该版本；除非目标环境已实际验证，禁止默认引入高版本 GLSL、`layout` 限定、compute shader 或不受 Minecraft loader 支持的内建。

---

## 0. 先判定你正在做哪一种 3D 效果

“3D Shader”至少有四种本质不同的任务，错误地把它们混成一个渲染入口会造成坐标、深度或排序问题：

| 类型 | 输入几何 | 常见入口 | Shader 的关键职责 | ArcaneVortex 观察对象 |
|---|---|---|---|---|
| **材质型** | 方块/实体已有网格 | 自定义 `RenderType`、实体或方块实体 renderer | UV、光照、雾、法线、材质颜色 | `water_block`、`cosmic`、`fantasy_block` |
| **几何特效型** | 自行生成的 quad、球、带状 trail | `RenderLevelStageEvent` 或 renderer | camera-relative 顶点、billboard、透明排序 | `trail`、`melee_trail`、`rendertype_glass_sphere` |
| **程序体积型** | 简单代理体（quad/box/sphere） | 世界阶段渲染 | view ray、ray marching、early exit、tone mapping | `volumetric_shader` |
| **屏幕空间场景读取型** | 全屏 quad / NDC quad | 特定世界最终阶段 | scene color/depth copy、坐标重建、深度输出 | `black_hole`、`black_hole_pro` |

**先选最小正确类别，不是最少代码类别。** 例如：

- 一个随实体位置移动的能量球应是几何特效型或实体 renderer，而不是在 HUD 画“看似在世界中”的圆；
- 想吸收/折射身后的世界画面，必须走场景读取型，普通实体 shader 无法凭空读取刚写入主 framebuffer 的颜色；
- 有真实方块模型、lightmap 和雾需求时，应选材质型，并把模型顶点与 Minecraft 的 fog/light 契约保留；
- 光柱/雾体若用几十层透明 quad 伪造，需要先决定是否真的必须 ray marching；性能预算低时优先几何近似。

---

## 1. ArcaneVortex JAR 中可学习的 3D Shader 结构

JAR 内 `assets/arcanevortex/shaders/core` 包含大量 core shader。与 3D 最相关、且各自代表不同问题的资源包括：

| 资源 | 可学习主题 | 不应机械复制的部分 |
|---|---|---|
| `starfield` | 球面映射、方向向量、星点 hash/noise、星空滚动 | 大量星循环应有严格上限与质量模式 |
| `water_block` | Position/Color/UV0/UV2/Normal、ChunkOffset、view-space 距离、末尾 fog | 法线不能简单用投影矩阵变换，应使用 normal matrix 思路 |
| `cosmic` | `#moj_import <light.glsl>`、`<fog.glsl>`、Sampler0 + Sampler2、FogShape | JSON/GLSL 名称与类型须自己严格验证，不能继承任何潜在不一致 |
| `rendertype_glass_sphere` | view position、normal、Fresnel、rim glow、半透明球 | 观察方向正负必须按视图坐标实测，不可照抄符号 |
| `volumetric_shader` | 屏幕坐标、yaw/pitch、固定上限 ray marching、ACES/gamma | 固定 80 步只代表参考实现，不能无条件用于所有硬件 |
| `black_hole` / `black_hole_pro` | 主场景颜色/深度采样、副本 target、投影状态备份恢复、`gl_FragDepth` | 深度重建、矩阵逆和深度写入需要更严格的空间验证 |

JAR 中还有 `sky_block`、`star_sky`、`nebula_cube`、`sun`、`warp_world` 等资源。它们进一步说明：游戏内“3D 效果”可能是模型材质、天空方向采样、程序空间图案或屏幕空间折射；必须根据实际输入数据做架构选择。

---

## 2. 资源布局、注册与三方接口契约

### 2.1 Core 3D Shader 的资源文件

一个 3D core shader 的最小资源组三件套：

```text
src/main/resources/assets/<modid>/shaders/core/aura_shield.json
src/main/resources/assets/<modid>/shaders/core/aura_shield.vsh
src/main/resources/assets/<modid>/shaders/core/aura_shield.fsh
```

逻辑名 `new ResourceLocation(MOD_ID, "aura_shield")` 对应 core 下同名 JSON；JSON 内 `vertex` 和 `fragment` 再解析到同名 `.vsh`/`.fsh`。文件名、modid、大小写和 uniform 名称都是硬接口，不允许“看起来差不多”。

### 2.2 `VertexFormat ↔ JSON attributes ↔ GLSL in` 必须逐项相同

ArcaneVortex 的不同 3D 效果使用的输入组合不一样：

| JAR Shader | JSON attributes | 适用情形 |
|---|---|---|
| `starfield` | `Position`、`UV0` | 简单天空/贴图几何 |
| `water_block` | `Position`、`Color`、`UV0`、`UV2`、`Normal` | 方块材质、光照/雾、程序水 |
| `cosmic` | `Position`、`Color`、`UV0`、`UV2`、`Normal` | 纹理 + lightmap + fog 的复杂材质 |
| `rendertype_glass_sphere` | `Position`、`UV0`、`Color`、`Normal` | 自定义半透明球体 |
| `volumetric_shader` | `Position`、`UV0`、`Color` | 代理几何上的程序体积效果 |
| `black_hole` | `Position` | 全屏 NDC 代理 quad |

在自己的实现中，先写接口表，再写任何 GLSL：

| 数据 | Java 顶点格式/写入 | JSON attribute | vertex shader | 片元是否需要 |
|---|---|---|---|---|
| 顶点位置 | `POSITION_COLOR_TEX_NORMAL` 等 | `Position` | `in vec3 Position` | 间接：经 varying |
| 顶点色 | 同上 | `Color` | `in vec4 Color` | `out vec4 vertexColor` |
| 主纹理 UV | 同上 | `UV0` | `in vec2 UV0` | `out vec2 texCoord0` |
| lightmap UV | 含 UV2 的格式 | `UV2` | `in ivec2 UV2`（依选用格式） | 光照采样/封装函数 |
| 法线 | 含 Normal 的格式 | `Normal` | `in vec3 Normal` | `out vec3 normalView` |

禁止将 JAR JSON 中声明但某一 vertex shader 未实际使用的属性视为标准。例如 `water_block` 声明 `UV2`，其可观察 vertex 逻辑重点仍在程序水与 fog；自己的 shader 不应白白传递未使用属性。**但如果你确实用 Minecraft lightmap，就必须从顶点格式到 sampler/GLSL 完整打通。**

### 2.3 原创 `aura_shield.json` 接口样板

以下为“半透明灵气护盾”选择 `Position + Color + UV0 + Normal` 的原创样板。其顶点格式必须选择与之对应的格式，不能用 `POSITION_TEX` 代替：

```json
{
  "blend": {
    "func": "add",
    "srcrgb": "srcalpha",
    "dstrgb": "one_minus_src_alpha"
  },
  "vertex": "examplemod:aura_shield",
  "fragment": "examplemod:aura_shield",
  "attributes": ["Position", "Color", "UV0", "Normal"],
  "samplers": [
    { "name": "Sampler0" }
  ],
  "uniforms": [
    { "name": "ModelViewMat", "type": "matrix4x4", "count": 16, "values": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1] },
    { "name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1] },
    { "name": "GameTime", "type": "float", "count": 1, "values": [0.0] },
    { "name": "ShieldColor", "type": "float", "count": 4, "values": [0.35, 0.75, 1.0, 0.65] },
    { "name": "Strength", "type": "float", "count": 1, "values": [1.0] }
  ]
}
```

### 2.4 客户端持有器与注册

`ShaderInstance` 仅属于客户端。将它放在 `client/shader/ClientShaderHolder`，将反复设置 uniform/恢复状态的逻辑放在 `client/utility/ShaderRenderUtility`。不在服务端类加载路径、公共 common init 或网络服务端分支引用它。

```java
@OnlyIn(Dist.CLIENT)
public final class ClientShaderHolder {
    public static final ResourceLocation AURA_SHIELD_ID =
            new ResourceLocation(ExampleMod.MOD_ID, "aura_shield");

    @Nullable
    private static ShaderInstance auraShieldShader;

    private ClientShaderHolder() {
    }

    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(event.getResourceProvider(), AURA_SHIELD_ID,
                        DefaultVertexFormat.POSITION_COLOR_TEX_NORMAL),
                shaderInstance -> auraShieldShader = shaderInstance
        );
    }

    @Nullable
    public static ShaderInstance getAuraShieldShader() {
        return auraShieldShader;
    }
}
```

资源重载会使旧实例失效；不能跨重载保存旧 `Uniform` 或 program id。shader 不可用时 renderer 必须降级（例如绘制普通半透明模型/粒子或直接跳过可选视觉层），不得令实体或世界渲染崩溃。

---

## 3. 坐标空间：3D Shader 正确性的核心

### 3.1 完整空间链

必须能回答每个 `vec3` 处于什么空间；变量名不能只叫 `pos`、`normal`：

```text
模型空间（model/local）
  Position：模型顶点相对模型原点的位置
       │  可选：实体/方块的局部 PoseStack 变换
       ▼
世界相对空间（world-relative / chunk-relative）
  对方块网格常通过 ChunkOffset 补足其区块相对位置
       │  ModelViewMat
       ▼
视图空间（view space）
  相机通常位于原点；可直接求到相机距离、viewDir
       │  ProjMat
       ▼
裁剪空间（clip space） gl_Position
       │  除以 w
       ▼
NDC（[-1,1]） -> 屏幕 UV（[0,1]） -> framebuffer 像素
```

ArcaneVortex `water_block.vsh` 的核心可观察流程是：`Position + ChunkOffset` 后乘 `ModelViewMat`，再乘 `ProjMat`；并以 view-space 位置计算距离和传递给 fragment。`rendertype_glass_sphere` 也先构造 view position，再得到 clip position。这是最应保留的顺序。

### 3.2 位置的 `w = 1`，方向/法线的 `w = 0`

位置需要平移：

```glsl
vec3 localPosition = Position + ChunkOffset;
vec4 viewPosition4 = ModelViewMat * vec4(localPosition, 1.0);
gl_Position = ProjMat * viewPosition4;
```

方向、切线和法线不应受平移影响：

```glsl
vec3 viewDirection = (ModelViewMat * vec4(localDirection, 0.0)).xyz;
```

`water_block` 对 Normal 使用 `w = 0` 的观察正确在于“法线不是位置”。但它将 normal 乘以 projection 的形态不能作为严格法线变换范本。对包含非均匀缩放的模型，正确方法是使用 model-view 的 inverse-transpose 3×3 normal matrix，再 `normalize`。如果当前渲染路径只能获得 ModelViewMat，则至少不要把投影矩阵混进用于 `dot(normal, lightDir)` 的法线；投影会破坏光照几何关系。

### 3.3 `ChunkOffset` 只应用一次

区块/方块渲染常见大错是：CPU 顶点已经写入 world-relative 坐标，又在 shader 中再加 `ChunkOffset`，导致模型随区块倍移或远离原点。实施前明确：

- CPU 送入的 `Position` 是 block-local、chunk-local 还是 camera-relative；
- `ChunkOffset` 由哪一层提供；
- `ModelViewMat` 是否已包含实体/方块的局部 PoseStack 平移；
- 最终 `viewPosition` 是否与相机原点一致。

建立只应用一次的公式并写在 renderer 注释中。不要根据“看起来位置不对”不断加/减相机坐标；那会在区块边界、远距离和第三人称下再次出错。

### 3.4 相机相对渲染

在 `RenderLevelStageEvent` 自行提交顶点时，世界坐标通常很大，直接以绝对世界坐标送 GPU 会降低远距离精度。通用结构是：

```java
Vec3 cameraPosition = event.getCamera().getPosition();
Vec3 effectPosition = this.getInterpolatedEffectPosition(partialTick);
double relativeX = effectPosition.x - cameraPosition.x;
double relativeY = effectPosition.y - cameraPosition.y;
double relativeZ = effectPosition.z - cameraPosition.z;

poseStack.pushPose();
poseStack.translate(relativeX, relativeY, relativeZ);
// 在局部原点提交球体、quad 或 trail 顶点。
poseStack.popPose();
```

不要同时：

1. CPU 已经减了 cameraPosition；
2. PoseStack 又减一次；
3. shader 再额外加/减 cameraPos uniform。

三者只能有一个层次承担相机相对转换。需要把相机绝对位置传入 shader 的情况（例如世界坐标噪声/稳定星空）是另一回事；那时要明示它只用于程序计算，不能再参与几何平移。


---

## 4. 矩阵、法线、光照、雾与顶点颜色

### 4.1 正确的顶点输出骨架

以“会被雾影响的半透明材质”为例，vertex shader 的职责是把每种数据变换到 fragment 真正需要的空间：

```glsl
#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ChunkOffset;

out vec4 vertexColor;
out vec2 texCoord0;
out vec3 viewPos;
out vec3 normalView;
out float vertexDistance;

void main() {
    vec3 modelOrChunkPosition = Position + ChunkOffset;
    vec4 viewPosition4 = ModelViewMat * vec4(modelOrChunkPosition, 1.0);

    gl_Position = ProjMat * viewPosition4;
    vertexColor = Color;
    texCoord0 = UV0;
    viewPos = viewPosition4.xyz;
    normalView = normalize((ModelViewMat * vec4(Normal, 0.0)).xyz);
    vertexDistance = length(viewPosition4.xyz);
}
```

此处 `normalView` 仅在没有非均匀缩放时可作为实用近似。若模型可缩放、骨骼/姿态矩阵非均匀，须将 normal matrix 从 Java 传入或使用可用的 Minecraft 矩阵契约。无论哪一种，fragment 中都应再次 `normalize(normalView)`，因为插值会改变长度。

### 4.2 观察方向的符号必须实测

在常见 view space 中，相机位于 `(0,0,0)`，片元位置为 `viewPos`。从片元指向相机的方向通常是：

```glsl
vec3 viewDirection = normalize(-viewPos);
```

ArcaneVortex `rendertype_glass_sphere` 以 view position 和 normal 构造 Fresnel/rim light，是正确的设计方向；但其片元中的具体符号不能不验证地照搬。用以下诊断检查：

- 输出 `0.5 + 0.5 * viewDirection`，围绕对象转镜头时方向色应平滑变化；
- 输出 `abs(dot(normalize(normalView), viewDirection))`，正面和边缘应呈符合直觉的连续分布；
- 若背面亮、正面暗或镜头靠近时反向，先检查 viewDirection 符号，再检查 cull 和 normal 方向。

### 4.3 Fresnel / rim glow 的可维护写法

`rendertype_glass_sphere` 的透明球实现展示了典型 Fresnel 思路：使用法线与视线夹角，边缘更亮。原创模板：

```glsl
float fresnel = pow(1.0 - clamp(abs(dot(normalize(normalView), viewDirection)), 0.0, 1.0), 2.5);
vec3 rimColor = ShieldColor.rgb * (0.25 + fresnel * 1.25);
float alpha = clamp((0.12 + fresnel * 0.55) * Strength, 0.0, 1.0);
```

规则：

- 先确保 normal 和 viewDirection 在同一空间；
- `abs` 是否应该使用取决于是否双面显示。单面外壳通常不希望背面进入计算；
- power 越高，亮边越窄；应命名为 `FresnelPower`，不是散落常数；
- shader alpha、RenderType blend、depth write 必须一起设计，详见第 5 节。

### 4.4 Minecraft 雾：与游戏环境一致地合成

ArcaneVortex `water_block` 通过 `vertexDistance` 和 `smoothstep(FogStart, FogEnd, ...)` 把最终颜色向 `FogColor` 混合；`cosmic` 引入 `#moj_import <fog.glsl>` 和 `#moj_import <light.glsl>`，并使用 `fog_distance(ModelViewMat, Position, FogShape)`。这体现了两个层级：

1. **简单可控雾**：自己把 view-space 距离传到 fragment，末尾 mix；
2. **尽量与 vanilla 一致的雾**：使用 Minecraft 提供的 fog include 和对应 uniform/函数签名。

简单雾的稳健模板：

```glsl
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

vec4 applySimpleFog(vec4 color, float distanceToCamera) {
    float fogFactor = smoothstep(FogStart, FogEnd, distanceToCamera);
    return vec4(mix(color.rgb, FogColor.rgb, fogFactor), color.a);
}
```

必须在最终材质/光晕/纹理颜色组成后再雾化。若先雾化再叠加明亮 rim，护盾边会穿透浓雾，视觉上与原版场景断层。半透明 alpha 的处理需按设计决定：一般 fog 混 RGB，保持此物体原本 alpha；若你同时修改 alpha，必须验证远距离排序与颜色累积。

若采用 `#moj_import <fog.glsl>` / `<light.glsl>`：

- 必须查看当前 Minecraft 版本实际提供的函数签名和要求的 uniform，不能只写 import；
- `FogShape`、`FogStart`、`FogEnd`、`FogColor` 应与 JSON/Java 提供路径对应；
- 使用内建函数时不要再对同一颜色叠加第二套雾，除非这是刻意的魔法雾效果且命名清楚；
- 需要 `Sampler2` lightmap 时，在 JSON、纹理绑定、vertex UV2 和 GLSL sample 四处保持一致。

### 4.5 UV2/lightmap 与多纹理

ArcaneVortex `cosmic` 的 JSON 包含 `Sampler0` 与 `Sampler2`，以及 `UV2` 和 light/fog import。这代表一种典型方块/实体材质接口：

- `Sampler0`：主材质 / atlas；
- `Sampler2`：lightmap；
- `UV0`：主纹理坐标；
- `UV2`：lightmap 坐标；
- `Color`：模型顶点 tint 或光照颜色。

实现时不要只“声明 `Sampler2`”却从未绑定或从未使用。也不要为了几张小贴图在 fragment 中添加十个独立 sampler；优先使用 atlas、图集 UV、单张 mask 或明确层数。每增加一个 sampler 都增加绑定复杂度、资源重载风险和驱动限制压力。

---

## 5. RenderType / CompositeState：决定透明、深度、剔除和排序

Shader 计算正确而 RenderType 状态错误，画面仍会完全错误。对于每个 3D 效果，必须明确下列状态：

| 状态 | 需要决定的问题 | 常见选择 |
|---|---|---|
| ShaderState | 使用哪一个 `ShaderInstance` | 客户端 holder 里的当前实例 |
| TextureState | 哪张主纹理绑定给 sampler | 主 atlas、护盾 mask、噪声图 |
| TransparencyState | RGB/alpha 怎样累积 | 标准 translucent 或预乘 alpha 专用配置 |
| DepthTestState | 是否被方块/实体遮挡 | 通常保持 LEQUAL；穿墙效果才显式禁深度 |
| WriteMaskState | 是否写深度 | 半透明护盾通常颜色写、深度不写 |
| CullState | 背面是否剔除 | 闭合球外表面可 cull；双面薄片/内部可 no-cull |
| OutputState | 写入哪个 target | 常规世界主 target；场景复制/特殊 target 必须明确 |
| sortOnUpload | 半透明顶点是否按需要排序 | 依几何和渲染批次决定，不能想当然 |

### 5.1 半透明护盾的推荐状态思路

以 ArcaneVortex `rendertype_glass_sphere` 的透明球方向为例：

- **深度测试开启**：护盾应被墙遮挡；
- **深度写入关闭**：防止透明外壳先写深度，错误遮住后面同一对象或其他透明层；
- **标准 alpha blend**：fragment 输出非预乘 RGB + alpha；
- **cull 的选择由网格决定**：只看外侧且法线正确可 cull；玩家可能进入球内或要双面可 no-cull，但要处理内外面亮度；
- **透明排序不能被忽略**：多个大护盾交叠时，提交顺序决定视觉。若需要稳定效果，按相机距离排序你的实例，或降低相互透明重叠。

不要为了“玻璃看起来更亮”直接启用 additive blend 而忽略 alpha：additive 不会正常遮挡，多个实例会迅速过曝。可以为 rim glow 另做 additive pass，但底壳仍以透明 pass 渲染，并明确两个 pass 顺序。

### 5.2 自定义 RenderType 模板

```java
@OnlyIn(Dist.CLIENT)
public final class ClientRenderTypes {
    private static final RenderType AURA_SHIELD = RenderType.create(
            "examplemod_aura_shield",
            DefaultVertexFormat.POSITION_COLOR_TEX_NORMAL,
            VertexFormat.Mode.QUADS,
            512,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            ClientShaderHolder::getAuraShieldShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(
                            AURA_MASK_TEXTURE, false, false))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(true)
    );

    private ClientRenderTypes() {
    }

    public static RenderType auraShield() {
        return AURA_SHIELD;
    }
}
```

这里 `createCompositeState(true)` 中的布尔语义以及不同 mappings/API 细节必须在你的 Forge 1.20.1 环境编译确认；本文的重点是状态组合本身。绝对不要把“能显示”当作透明状态验证完成：从墙后、球内、两球重叠、与水/云/粒子同屏等场景检查。

### 5.3 状态恢复与异常边界

Renderer 不应让下一个 Mod 或原版渲染继承自己的 shader、纹理、blend、cull、color 或 framebuffer。使用 `PoseStack.pushPose()/popPose()` 管理几何矩阵；手动使用 RenderSystem 修改状态时，以 `try/finally` 恢复。Buffer 提交与状态切换之间不要插入无关的 draw。

对于全屏场景读取效果，除普通 RenderType 状态外还必须备份/恢复 projection。ArcaneVortex `BlackHoleRenderer` 明确调用 projection backup 和 restore；这是因为它会在渲染中设置与特效匹配的投影，遗漏 restore 会污染后续世界/UI 渲染。

---

## 6. 选择渲染入口：实体、方块实体、世界阶段

### 6.1 EntityRenderer

适合视觉严格附着某个实体的场景：姿态、插值位置、实体可见性、实体模型的同步都较自然。适用于护盾球、跟随物、投射物外壳、复杂模型材质。注意：实体 renderer 被调用不表示你可以忽略距离裁剪和透明排序；特效对象仍需决定是否应渲染。

### 6.2 BlockEntityRenderer

适合与方块/方块实体生命周期绑定的祭坛、传送门、方块内部水面。必须明确 block local 坐标、block entity 世界坐标和 chunk offset 的分工。模型若来自 chunk render 管线，`ChunkOffset` 契约与手动 renderer 不同；不要把两套方式混在一个 shader。

### 6.3 `RenderLevelStageEvent`

适合不从属于单一模型的世界特效：多点 trail、范围法阵、天空覆盖层、调试几何、场景读取特效。关键是选择正确 stage：

- 太早：需要的深度/颜色尚未完成；
- 太晚：UI 或其他后续层已开始，可能无法正确遮挡；
- 对依赖 main color/depth copy 的效果，必须保证 scene copy 的时机在读取之前、主目标尚未被错误重绑。

为一个 effect 选择一个固定 stage 和一个固定 manager；不要在多个事件回调都重复渲染“以防没画上”。

### 6.4 `VertexConsumer` 与立即 BufferBuilder

- **VertexConsumer**：适合与 RenderType 批处理结合，材质/实体/常规四边形和模型最稳定；
- **BufferBuilder / immediate buffer**：适合一次性程序几何、需要精确控制顶点提交的场景；但必须选择与 shader 匹配的 VertexFormat，并在正确时机 end/draw；
- 两者都必须在写顶点前确认字段顺序和格式。没有 Normal 的 buffer 不能让 shader 假装读取 Normal。

先选择一条顶点提交路径，调通 Position/UV/Color/Normal 后再加入复杂 shader。不要通过“手动 OpenGL attribute”绕过 Minecraft 的格式管理；这会与 RenderType/ShaderInstance 的既有契约冲突。


---

## 7. 每帧参数：时间、相机、yaw/pitch、尺度和世界位置

ArcaneVortex 的 `starfield`、`water_block`、`cosmic`、`volumetric_shader` 都传入时间和/或相机角度；`black_hole` 还传入相机位置、实体位置、投影矩阵、缩放和密度参数。这些 uniform 不是“多传一些备用数据”，而是每个算法的空间协议。

### 7.1 统一参数单位表

建议把所有 3D shader 参数列成如下表并写在 renderer/manager 附近：

| uniform | 类型 | 空间/单位 | 生产者 | 使用限制 |
|---|---|---|---|---|
| `GameTime` | float | 秒 | 客户端时钟/partial tick | 不传毫秒；必要时 `fract` 防精度丢失 |
| `CameraPos` | vec3 | 绝对世界坐标 | 当前 camera | 仅用于 world-anchored 算法，不重复平移几何 |
| `YawRadians` | float | 弧度 | camera yaw 转换 | `sin/cos` 只接受弧度 |
| `PitchRadians` | float | 弧度 | camera pitch 转换 | 与 yaw 旋转顺序固定 |
| `EntityPos` | vec3 | 必须注明 world/local/view | effect owner | 与 ray origin 同一空间 |
| `Scale` | float/vec3 | 模型单位 | renderer 状态 | 先 clamp，避免 0/负值异常 |
| `ScreenSize` | vec2 | framebuffer 像素 | window/render target | 不与 GUI 逻辑尺寸混用 |
| `Strength` | float | `[0,1]` | client state | 作为透明/混合上限 |

### 7.2 yaw/pitch 的弧度与旋转顺序

ArcaneVortex `starfield` 和 `volumetric_shader` 使用 camera yaw/pitch 来改变方向/射线。Java 角度往往是 degree，GLSL 三角函数输入是 radian：

```java
float yawRadians = (float) Math.toRadians(camera.getYRot());
float pitchRadians = (float) Math.toRadians(camera.getXRot());
```

在 shader 内，先定义单独旋转函数，再固定顺序：

```glsl
mat2 rotate2D(float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return mat2(c, -s, s, c);
}

vec3 rotateYawPitch(vec3 direction, float yawRadians, float pitchRadians) {
    direction.xz = rotate2D(yawRadians) * direction.xz;
    direction.yz = rotate2D(pitchRadians) * direction.yz;
    return direction;
}
```

不能只“调到看起来能转”。记录 yaw 正方向、pitch 正方向和 yaw/pitch 的先后，第一人称、第三人称与世界坐标噪声中都使用同一约定。若角度从 Java 已转为弧度，变量名必须带 `Radians`，避免二次转换。

### 7.3 partial tick 与稳定动画

渲染帧率不等于游戏 tick。需要连续运动时使用部分 tick 插值实体位置/旋转，并让时间 uniform 使用连续秒。不要在 shader 中只依赖整数 tick 造成 20 Hz 跳动；也不要把每帧 delta 累加到多个对象各自的非同步计时器，除非刻意需要独立相位。

为世界固定纹理（例如程序水、地面噪声）使用世界位置或稳定 seed，而不是只用模型 local Position；否则每个方块/实体都有相同局部重复图案。为实体局部魔法纹路则反过来使用 local Position，令纹路随实体移动。

---

## 8. 球面映射与星空：从 `starfield` 学习方向场

ArcaneVortex `starfield` 的 attributes 是 `Position`、`UV0`，uniform 包括 `CameraYaw`、`CameraPitch`、`StarScale`、`EnableSphericalMapping`、`EnableAdvancedEffects`、`MoveSpeed`、`StarCount`、`StarMask`、`StarMSpeed`、`StarMScale`、`MaxStarCount`、`Opacity`、`ColorModulator`。其 vertex shader 将 Position、UV 和位置相关值传下去；fragment shader 用方向向量转球面 UV，再通过 hash/noise 生成星点、运动、闪烁。

### 8.1 方向向量到球面 UV

在使用球、天空盒或任何“从中心向外看”的代理几何时，先规范化方向：

```glsl
vec2 directionToSphericalUv(vec3 direction) {
    direction = normalize(direction);
    float longitude = atan(direction.z, direction.x);
    float latitude = acos(clamp(direction.y, -1.0, 1.0));
    return vec2(longitude / (2.0 * 3.14159265) + 0.5, latitude / 3.14159265);
}
```

- `atan(z, x)` 给出横向角度；
- `acos(y)` 给出从极点到极点的纬向进度；
- `clamp` 防止浮点误差使 `acos` 输入超出 `[-1,1]`；
- 横向 U 是环状的，通常要 `fract` 包裹；
- 不应直接用模型 `UV0` 代替方向映射，除非网格 UV 已明确是等距矩形球面展开。

### 8.2 U seam 的距离必须环绕

星点/星云在 U=0/1 接缝附近比较距离时，普通 `abs(a-b)` 会错误地产生“跨整张图的远距离”。JAR 的星空逻辑考虑了 U 环绕。原创辅助函数：

```glsl
vec2 wrappedUvDelta(vec2 a, vec2 b) {
    vec2 delta = abs(a - b);
    delta.x = min(delta.x, 1.0 - delta.x);
    return delta;
}
```

对 V 不要环绕；纬度两端是极点而不是无缝左右边界。

### 8.3 程序星点的固定上限

复杂星点生成常会遍历邻居 cell 或星列表。JAR 含 `MaxStarCount`，说明需要显式限制复杂度。安全结构：

```glsl
const int MAX_STARS = 32;

for (int starIndex = 0; starIndex < MAX_STARS; starIndex++) {
    if (starIndex >= StarCount) {
        break;
    }
    // 用 starIndex 推导稳定 seed、位置、颜色、闪烁。
}
```

`StarCount` 必须在 Java 侧 clamp 到 `[0, MAX_STARS]`。禁止直接写 `for (int i = 0; i < StarCount; i++)`，因为部分 GLSL 编译器/驱动对动态循环优化和性能表现不可预测。分级质量可以采用 8/16/32 的固定上限不同 shader 变体，或统一 MAX 但低质量提前 break。

### 8.4 相机角度与天空移动

`starfield` 同时有球面映射与 camera yaw/pitch 参数。实现星空滚动时先决定视觉语义：

- **天空应随镜头自然旋转**：方向来自 view ray/球体 direction，通常无需再把 camera rotation 二次叠加；
- **魔法穹顶纹理独立缓慢旋转**：在 spherical UV 上额外 `uv.x += time * MoveSpeed`；
- **固定在世界方向**：必须使用 world/view 关系和相机绝对旋转，确保玩家移动不会让星空“贴在屏幕上”。

每一种只能有一套角度变换。画面“转得比镜头快一倍”几乎总是 shader 与 PoseStack/ModelView 两边都旋转了。

---

## 9. 程序水与方块材质：从 `water_block` 学习

ArcaneVortex `water_block` 声明 `Position`、`Color`、`UV0`、`UV2`、`Normal`，并传入 `Sampler0`、`ColorModulator`、`FogStart`、`FogEnd`、`FogColor`、`time`、`screenSize`、`yaw`、`pitch`。其 vertex 逻辑把 `Position + ChunkOffset` 变换到 view/clip，传递 vertexDistance、Color、UV、normal 和 view-space `fPos`；fragment 使用视线与多层循环构造程序水纹，再与原材质混合，末尾处理 fog。

### 9.1 材质水的合成顺序

推荐以下顺序，避免程序颜色把原方块材质、透明度或 fog 完全破坏：

1. 采样 `Sampler0`，取得原材质颜色与 alpha；
2. 计算程序波纹/法线扰动/高光强度；
3. 使用 `mix(base.rgb, waterRgb, WaterBlend)` 合成，而非无条件覆盖；
4. 乘入 `vertexColor` / `ColorModulator` 的 tint 约定；
5. 处理透明度；
6. 末尾应用 fog；
7. 输出与 RenderType blend 一致的非预乘或预乘结果。

```glsl
vec4 baseSample = texture(Sampler0, texCoord0);
float wave = 0.5 + 0.5 * sin(texCoord0.x * 18.0 + GameTime * 2.0);
vec3 proceduralWater = mix(vec3(0.03, 0.18, 0.32), vec3(0.12, 0.55, 0.78), wave);
vec3 materialRgb = mix(baseSample.rgb, proceduralWater, WaterBlend);
vec4 color = vec4(materialRgb * vertexColor.rgb, baseSample.a * vertexColor.a);
fragColor = applySimpleFog(color, vertexDistance);
```

### 9.2 多层波纹循环必须有预算

JAR 的水使用固定 `MAX_ITER = 5` 风格的多层程序循环。新的效果应把每层含义写明：波长、速度、振幅、旋转/偏移和累计方式。示例：

```glsl
const int WAVE_LAYERS = 4;

float layeredWave(vec2 uv, float time) {
    float sum = 0.0;
    float amplitude = 0.5;
    for (int layer = 0; layer < WAVE_LAYERS; layer++) {
        float layerScale = 4.0 + float(layer) * 3.0;
        vec2 direction = normalize(vec2(1.0, 0.35 + float(layer) * 0.2));
        sum += sin(dot(uv * layerScale, direction) + time * (1.2 + float(layer) * 0.3)) * amplitude;
        amplitude *= 0.55;
    }
    return sum;
}
```

避免把连续时间和高频 UV 直接相乘到巨大数值；长时间运行后可能出现精度/闪烁。可以让 `GameTime` 只取周期范围（如 `mod` 或 `fract`）或在 Java 传可控循环时间。

### 9.3 法线与伪法线

程序水若仅改变颜色可不改 normal；若要高光，应从波函数导数或邻域采样构造扰动法线。用 UV 空间梯度时，要明确它仍是近似，并将其转换到与光/视线同一空间。不要拿 screen-space 法线与 view-space lightDir 相点乘。

### 9.4 Fog 是材质的一部分，不是最后补丁

JAR 在 `water_block` 末尾处理 fog，正确原因是 shader 生成的水色仍应融入世界雾。任何材质型 3D 特效若跳过雾，远处会像“贴在屏幕前”；若重复雾化，又会比原版暗得多。雾方案只选一个主路径并在多天气、下界/末地、夜视等环境测试。

---

## 10. 灵气护盾球：从 `rendertype_glass_sphere` 到完整原创算法

`rendertype_glass_sphere` 使用 `Position`、`UV0`、`Color`、`Normal`，vertex 输出 viewPos、normal、screenPos、distanceFromCenter 等，fragment 以 Fresnel 和 rim light 生成半透明玻璃球。它是最适合作为“自定义 3D core shader”模板的 JAR 资源之一。

### 10.1 vertex shader（原创模板）

```glsl
#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
out vec3 viewPos;
out vec3 normalView;
out float localRadius;

void main() {
    vec4 viewPosition4 = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPosition4;

    vertexColor = Color;
    texCoord0 = UV0;
    viewPos = viewPosition4.xyz;
    normalView = normalize((ModelViewMat * vec4(Normal, 0.0)).xyz);
    localRadius = length(Position);
}
```

如果 Java 已通过 `PoseStack.translate/scale` 把球移动和缩放，shader 不再另传/另加实体位置。若 shader 需要稳定 world noise，单独传 `WorldAnchor` 并只用于计算噪声坐标。

### 10.2 fragment shader（原创模板）

```glsl
#version 150

uniform sampler2D Sampler0;
uniform float GameTime;
uniform vec4 ShieldColor;
uniform float Strength;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 viewPos;
in vec3 normalView;
in float localRadius;

out vec4 fragColor;

void main() {
    vec3 n = normalize(normalView);
    vec3 v = normalize(-viewPos);
    float fresnel = pow(1.0 - clamp(abs(dot(n, v)), 0.0, 1.0), 2.4);

    float scan = 0.5 + 0.5 * sin(texCoord0.y * 28.0 - GameTime * 3.0);
    float outerPulse = 0.5 + 0.5 * sin(GameTime * 2.0 - localRadius * 10.0);
    float mask = texture(Sampler0, texCoord0).a;

    vec3 rgb = ShieldColor.rgb * vertexColor.rgb;
    rgb += ShieldColor.rgb * fresnel * (0.45 + scan * 0.35);
    float alpha = clamp(mask * Strength * (0.10 + fresnel * 0.60 + outerPulse * 0.10), 0.0, 1.0);
    fragColor = vec4(rgb, alpha * vertexColor.a);
}
```

测试从四个角度进行：远处、贴近、墙后、球内。若球内不应可见，启用正确 cull 或在 shader 丢弃背面；若球内也要可见，确认 normal/Fresnel 的 `abs` 语义是特意双面。

---

## 11. 体积 ray marching：从 `volumetric_shader` 学习“代理几何 + 固定步数”

ArcaneVortex `volumetric_shader` 的 attributes 是 `Position`、`UV0`、`Color`，vertex 将 clip/NDC 推导为 `fragCoord`，fragment 使用 yaw/pitch 改变 ray direction，并以固定约 80 次循环进行程序体积采样，最后做 ACES tone mapping 与 gamma。它揭示的是完整流程，而不是建议所有项目默认跑 80 步。

### 11.1 代理体与 ray 的空间必须统一

Ray marching 至少要有：

- `rayOrigin`：从哪里射线开始；
- `rayDirection`：朝哪里，且 normalized；
- `t`：沿 ray 的距离；
- `samplePosition = rayOrigin + rayDirection * t`：与 density/SDF 使用同一空间；
- 代理几何决定 ray 可见的区域，但不自动解决世界坐标问题。

若 density 函数使用 entity local space，ray origin/direction 也要转换到该 local space；若 density 使用 world space，就用 camera/world aligned ray。最危险的错误是 rayOrigin 在 world space，density 却以 local Position 计算——画面可能“动”，但会随相机/实体产生错误滑动。

### 11.2 安全积分结构

```glsl
const int MAX_STEPS = 32;
const float MAX_DISTANCE = 8.0;

vec3 marchVolume(vec3 rayOrigin, vec3 rayDirection, float time) {
    float stepLength = MAX_DISTANCE / float(MAX_STEPS);
    float transmittance = 1.0;
    vec3 accumulatedLight = vec3(0.0);

    for (int stepIndex = 0; stepIndex < MAX_STEPS; stepIndex++) {
        float distanceAlongRay = (float(stepIndex) + 0.5) * stepLength;
        vec3 samplePosition = rayOrigin + rayDirection * distanceAlongRay;
        float density = clamp(sampleDensity(samplePosition, time), 0.0, 1.0);

        vec3 emission = density * vec3(0.15, 0.55, 1.0);
        accumulatedLight += transmittance * emission * stepLength;
        transmittance *= exp(-density * stepLength * 2.0);

        if (transmittance < 0.02) {
            break;
        }
    }
    return accumulatedLight;
}
```

生产要求：

- `MAX_STEPS` 为编译期上限；
- `MAX_DISTANCE`、步长与体积实际尺寸相关，不能使用任意巨大数；
- 用 `transmittance` 或 alpha 做 early exit；
- 低质量模式降低步数、最大距离或 density 噪声 octave；
- 避免每一步进行高成本 3D FBM + 多次 texture fetch；
- 若效果覆盖很大屏幕，考虑降低渲染分辨率/缩小代理几何，而不是只继续加步数。

### 11.3 tone mapping 与 gamma 的边界

JAR 在体积效果末尾使用 ACES tone mapping 和 gamma，说明体积累积可能超过显示范围。规则是：一条渲染链中必须明确由谁负责 tone mapping/gamma。若 Minecraft 主渲染已经做了相关颜色处理，又在自定义 effect 重复做，可能整体过暗/过饱和。先以线性、clamp 的调试输出验证 density，再决定是否使用 tone mapping；这不是装饰性“一键电影感”。


---

## 12. 场景颜色/深度读取：从 `black_hole` 学习正确的黑洞、折射和引力透镜架构

ArcaneVortex `black_hole` 的 JSON 仅以 `Position` 作为 attribute，却声明了 `MainDepthSampler`、`MainColorSampler`、`TextureSampler`、`ColorSampler`，以及 `ProjMat`、`ModelViewMat`、`time`、`screenSize`、`projectionMatrix`、`modelViewMatrix`、`cameraPos`、`yaw`、`pitch`、`entityPos`、`scale`、`accretionDiskDensity`、`tiltAngle`、`intensity` 等 uniform。其 vertex 直接输出 NDC，fragment 读取场景颜色/深度、重建空间信息、计算偏折画面，并可能写 `gl_FragDepth`。

JAR 的 `BlackHoleRenderer` 还可观察到以下关键步骤：

1. 复制主颜色 target（`SceneCopyTarget.copyMainColor()`）；
2. 备份 projection matrix；
3. 把主 depth、主颜色副本、特效纹理、色图分别绑定给相应 sampler；
4. 设置时间、投影/模型视图矩阵、相机位置、实体位置、scale、吸积盘密度、倾角、强度；
5. 绘制全屏/代理几何；
6. 恢复 projection matrix。

这就是屏幕空间 3D 特效和普通透明实体 shader 的决定性区别。

### 12.1 **禁止**采样正在写入的主颜色纹理

以下错误结构绝不能出现：

```text
从 minecraft:main 的 color texture 采样
                │
                └── 同一个 draw 同时写回 minecraft:main
```

这是 texture feedback loop。OpenGL 对结果不做有用保证，常见表现为闪烁、上一帧残影、不同显卡不同结果或黑屏。

正确结构必须与 JAR 的“先 copy 再采样”方向一致：

```text
Main RenderTarget（当前已完成的场景）
        │ copy color
        ▼
Scene Copy RenderTarget ──> MainColorSampler
        │
        └── 黑洞/折射 shader 读取 scene copy + depth
                                      │
                                      ▼
                         写入当前 main target 或明确的输出 target
```

若同一帧有多个场景读取特效，决定它们各自读取“原始主画面”还是“上一个 effect 的输出”。不能各自随意 copy/绑定，否则排序与性能不可控。建立一个 `client/render/SceneCopyTarget` 或 manager 统一维护复制时机和一帧内的有效性。

### 12.2 Scene copy 的生命周期与尺寸

scene copy 不是一张永久尺寸的纹理：

- framebuffer 变大/变小时必须 resize；
- 资源重载、离开世界和客户端关闭时必须释放；
- 复制时机必须在你要折射的内容已经写入主 target 后；
- 每帧只在需要时 copy 一次，而不是每个黑洞实例复制一次；
- 复制后声明“本帧版本号/有效阶段”，避免另一个 renderer 在主 target 已发生变化后误把旧 copy 当新场景。

对于远距离、屏幕占比很小的黑洞，可以先做屏幕裁剪/距离裁剪，再决定是否进行场景 copy。若一个 effect 不在视锥内或强度为 0，不应该触发全屏资源工作。

### 12.3 深度读取和空间重建

深度 buffer 中的值不是直接的 world-space 距离。严谨流程是：

1. 用屏幕 UV 采样 depth；
2. 将 UV 与 depth 还原到 NDC：`ndc = vec3(uv * 2 - 1, depth * 2 - 1)`；
3. 用 **inverse projection matrix** 从 clip/NDC 还原到 view space；
4. 除以 `w`；
5. 若需要 world space，再用 inverse view/model-view 变换；
6. 与黑洞中心、ray 或遮挡关系使用同一空间比较。

原创伪 GLSL：

```glsl
vec3 reconstructViewPosition(vec2 screenUv, float depthValue, mat4 inverseProjection) {
    vec4 clipPosition = vec4(screenUv * 2.0 - 1.0, depthValue * 2.0 - 1.0, 1.0);
    vec4 viewPosition = inverseProjection * clipPosition;
    return viewPosition.xyz / max(viewPosition.w, 0.00001);
}
```

注意：不同深度约定、projection 方向、RenderTarget 格式和当前矩阵状态都必须实测。JAR 的复杂片元中出现的矩阵变量/命名只应作为“需要 projection/model-view/camera 数据”的观察依据，**不能替代你自己的 inverse matrix 验证**。

调试步骤：

1. 先输出深度灰度，确认 sampler 有数据；
2. 输出重建的 view position 归一化色；
3. 用一个已知平面/方块距离验证数值变化方向；
4. 再引入黑洞中心距离和遮挡逻辑；
5. 最后再做镜头偏折、吸积盘和最终 color grading。

### 12.4 写 `gl_FragDepth` 的风险

ArcaneVortex 黑洞片元会修改 `gl_FragDepth`，这可以让屏幕空间效果与深度遮挡更紧密，但风险非常高：

- 输出值必须处于当前 depth 空间的合法范围，通常 clamp 到 `[0,1]`；
- 不能把线性 view distance 直接写进去；
- 写深度会影响此 draw 之后所有透明/粒子/世界元素；
- 深度测试、depth mask、shader 内 discard 和 RenderType 状态必须作为一个整体测试；
- 若特效只需视觉折射、不应遮挡后续物体，宁可不写 `gl_FragDepth`。

实现策略：先完成“只改颜色、不写深度”的稳定版本；仅在确实需要遮挡一致性时新增可独立开关的 depth-write 分支，并用不同距离、边缘、物体交叠、FOV 和窗口尺寸测试。

### 12.5 全屏 NDC quad 不是普通世界 quad

`black_hole.vsh` 的可观察结构是：

```glsl
gl_Position = vec4(Position, 1.0);
texCoord = Position.xy * 0.5 + 0.5;
```

这表示 `Position` 已是 NDC/clip 风格的全屏顶点，而不是实体模型坐标。此类 shader 的 Java 顶点格式、值范围和 RenderType/投影处理必须专门设计：

- 输入 quad 常为 `(-1,-1)、`(1,-1)、`(1,1)、`(-1,1)`；
- `texCoord` 由 NDC 映射到 `[0,1]`；
- 不能将相机 relative world position 传给该 `Position`；
- world 中黑洞的位置不是顶点位置，而是一个 uniform（例如 `BlackHoleCenterWorld`/`EntityPos`）并通过深度重建参与比较；
- 若临时修改 projection，必须如 JAR renderer 一样 backup/restore。

---

## 13. 场景读取 Renderer 的完整职责拆分

不要把 copy、矩阵、sampler 和实体遍历写在一个巨型事件回调中。建议按客户端职责拆分：

```text
client/render/SceneCopyTarget
  - 创建、resize、copyMainColor、close

client/shader/ClientShaderHolder
  - 注册/提供 black_hole ShaderInstance

client/utility/ShaderUniformUtility
  - 以安全方式设置 float/vec/matrix；开发期记录缺失接口

client/utility/CameraRenderUtility
  - 集中转换 camera yaw/pitch、获取/验证矩阵与 framebuffer 尺寸

client/render/BlackHoleEffectRenderer
  - 可见性筛选、scene copy 请求、sampler 绑定、uniform 更新、全屏 quad 提交、状态备份恢复
```

工具类仅包含重复可复用的方法时，不写无意义构造逻辑；若需要阻止实例化，可有私有构造方法。字段使用 `lowerCamelCase`，常量使用 `UPPER_SNAKE_CASE`。若未来增加客户端配置，可将显示质量/开关纳入 `common/config/ClientConfig`；涉及共同游戏规则的参数与视觉质量参数分开，不能让服务端强行加载客户端 shader 类。

一个 effect render 的逻辑顺序应当是：

1. 从管理器取得活跃黑洞/折射体列表；
2. 依据距离、视锥、强度、尺寸筛除不可见对象；
3. 若没有对象，立即返回，不 copy、不绑定 shader；
4. 确保 scene copy target 已创建且尺寸匹配；
5. 在本帧正确 stage 复制主颜色一次；
6. 备份 projection 和你将修改的 RenderSystem 状态；
7. 绑定 current shader、scene copy color、主 depth、附加纹理/色图；
8. 对每一个可见对象设置一组严格单位化的 uniform；
9. 绘制 NDC quad 或经定义的代理几何；
10. 在 `finally` 恢复 projection、主 target 绑定和其他状态；
11. 资源重载/世界退出时 close target 和清空临时引用。

不要把“同屏有三个黑洞”实现为复制三次主场景。复制一次后，对每个对象多画一次 pass；若实例数量过大，进一步限制数量、合并实例数据或退化为较便宜的 core sphere effect。

---

## 14. 3D Shader 的性能、精度与兼容性预算

### 14.1 优先裁剪，再运行 fragment

从便宜到昂贵的顺序：

1. 服务端/客户端状态判断：effect 是否激活；
2. 距离阈值、frustum 可见性、最小屏幕尺寸；
3. 选择质量级别；
4. 再创建/复制 target、绑定 shader；
5. 最后才执行全屏采样、ray marching 或多层 noise。

黑洞/体积效果成本主要在片元阶段。即使几何只有四个顶点，也可能覆盖数百万像素；不能因“quad 很少”误认为免费。

### 14.2 固定上限与质量档

所有复杂循环都必须有编译期最大值：

- 星空：`MAX_STARS`；
- 水纹：`WAVE_LAYERS`；
- Voronoi：固定 3×3 邻域；
- Ray marching：`MAX_STEPS`；
- 吸积盘/镜头采样：固定最大采样数。

将低/中/高质量对应到清楚的数值范围，并在 Java 配置读取后 clamp。不得让服务器数据包或任意配置值直接变成 shader 循环上限。超出预算时的回退顺序应是：降低程序层数 -> 降低渲染范围/分辨率 -> 关闭场景读取高阶特效 -> 保留简单透明核心效果。

### 14.3 精度风险

- 大世界坐标直接在 shader 做高频 noise 会损失精度：使用 camera-relative 位置、局部坐标或周期化坐标；
- 绝对时间无限增大时 `sin/cos` 和 `fract` 精度会下降：传循环秒值或拆分为相位；
- 归一化零向量、除以零、`acos` 输入越界、inverse matrix 不可用都会产生 NaN；每一个都须 clamp/guard；
- 窗口尺寸为 0（最小化/初始化瞬间）时不得相除；
- 深度/颜色 target 的 resize 后旧纹理 id 不可继续使用。

### 14.4 兼容性与失败回退

- shader 注册失败：不崩溃，退回普通 RenderType 或跳过纯视觉层；
- scene copy 分配失败：禁用黑洞折射，但仍可绘制简单 emissive 球；
- 某些光影/渲染改造环境可能改变 framebuffer/阶段：将场景读取效果限制为可关闭客户端增强功能，并记录明确的诊断信息；
- 不使用未经验证的高版本 GLSL 和 vendor 特性；
- 不让专用服务端加载任何 `ShaderInstance`、`RenderSystem`、`Minecraft` 或 renderer 类。

---

## 15. 常见错误与诊断序列

| 现象 | 高概率原因 | 先做什么 |
|---|---|---|
| 物体跟相机漂移/翻倍移动 | camera-relative 变换应用两次 | 写出 CPU/PoseStack/shader 三层坐标公式，只保留一次 |
| 模型完全不可见 | stage/RenderType 错、顶点格式不匹配、cull/depth 错 | 用纯色 shader、关闭 cull 诊断、确认 Position 输入 |
| 只有远处/近处可见 | depth test 或 projection 空间错 | 输出 view-space 距离，检查 RenderType 深度状态 |
| 护盾边缘黑/透明怪异 | alpha 模型与 blend 不匹配 | 检查预乘/非预乘约定与纹理边缘 RGB |
| 球内部反光翻转 | viewDirection/normal 符号或双面语义错 | 可视化 normal 与 dot 值，分别测试内外面 |
| 方块材质远处像贴纸 | 未 fog 或 fog 空间错误 | 使用 view distance，末尾合成 fog |
| 水纹闪烁 | 高密度/高频噪声、硬 step、时间精度差 | 降低频率，增加 smoothstep，周期化时间 |
| 黑洞黑屏/闪烁 | 读写同一 main color target | 实现 scene copy 并验证 copy 在 draw 前完成 |
| 黑洞遮挡全错 | 把 depth 当距离、错误 inverse matrix、错误 `gl_FragDepth` | 先不写 frag depth，单独验证重建空间 |
| 资源重载后崩溃 | 缓存旧 ShaderInstance/Uniform/target | 重载后重新获取，world exit/resize close |

### 15.1 每个 3D Shader 的最小诊断输出

按顺序实施，不要直接调复杂效果：

1. fragment 输出常量品红，验证 shader/RenderType 已运行；
2. 输出 `vertexColor`，验证 Color attribute；
3. 输出 `vec3(texCoord0, 0)`，验证 UV；
4. 输出 `normalize(normalView) * 0.5 + 0.5`，验证法线与空间；
5. 输出 `clamp(length(viewPos) / range, 0, 1)`，验证 view position；
6. 输出 texture sampler，验证纹理绑定；
7. 输出 fog factor，验证雾；
8. 对场景读取特效，依次输出 scene color、depth、重建坐标、mask；
9. 最后才加入 Fresnel、noise、水纹、ray marching、扭曲、`gl_FragDepth` 和多 pass 合成。

---

## 16. 给 AI 的完整实施顺序与验收清单

### 16.1 实施顺序

1. **分类**：材质、几何、体积或场景读取；选一个 renderer 入口与 stage。
2. **定义接口表**：每个 attribute/uniform/sampler 标明类型、单位、空间、生产者、消费者。
3. **定义顶点空间链**：Position 从 local 到 view 到 clip 的唯一公式；决定 ChunkOffset 和 camera-relative 的唯一承担者。
4. **创建资源**：core JSON、`.vsh`、`.fsh`；默认 `#version 150`；大小写逐字匹配。
5. **注册客户端 ShaderInstance**：资源重载安全；空实例有可选效果回退。
6. **创建并缓存 RenderType**：精确设定 shader、texture、blend、depth、write mask、cull 和排序语义。
7. **先做诊断绘制**：常量色 -> attribute -> UV -> normal -> view position -> sampler -> fog。
8. **加入算法**：Fresnel、水纹、球面映射或 ray marching；每个循环有固定上限、参数有单位。
9. **如要读取场景**：先建立 SceneCopyTarget、尺寸管理和一次/帧 copy，再接 depth 重建；禁止 feedback loop。
10. **恢复状态并释放**：`try/finally` 恢复 projection/target/RenderSystem；窗口 resize、资源重载和世界退出 close 所有 target。
11. **测试矩阵**：不同 FOV、第一/第三人称、远近距离、区块边界、窗口 resize、资源重载、切世界、透明物体交叠、低质量模式和 shader 失败回退。

### 16.2 验收清单

- [ ] 本次 3D Shader 资源和 Java 客户端逻辑以 ArcaneVortex JAR 的同类结构为唯一参考，没有混入无关实现。
- [ ] 所有 3D client 类只在客户端路径加载；服务端状态与客户端视觉状态分离。
- [ ] VertexFormat、JSON attributes、GLSL `in` 声明和顶点写入顺序逐项一致。
- [ ] 每个 `vec3` 的空间明确，位置使用 `w=1`、方向/法线使用 `w=0`，`ChunkOffset`/相机相对转换各仅执行一次。
- [ ] normal、viewDir、lightDir 均处于同一空间；复杂缩放时采用正确 normal matrix 策略。
- [ ] 雾、lightmap、多纹理和顶点颜色接口完整闭合，没有声明未绑定/未使用 sampler。
- [ ] RenderType 的透明、depth test、depth write、cull 和排序均经过内外/遮挡/交叠测试。
- [ ] 所有复杂循环有编译期上限，所有用户/网络输入在 Java 侧 clamp，存在质量降级策略。
- [ ] 场景读取效果复制主颜色后再采样，绝不采样当前写入 target；scene copy 有 resize/close 生命周期。
- [ ] 深度重建和 `gl_FragDepth` 通过独立诊断验证；不需要深度写入时未开启它。
- [ ] renderer 在异常和正常路径都恢复 PoseStack、projection、target 和 RenderSystem 状态。
- [ ] 完成不同窗口尺寸、资源重载、切换世界、无 shader/低配置回退等测试。
