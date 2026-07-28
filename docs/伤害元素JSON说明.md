# 伤害元素 JSON

伤害元素映射文件放在：

`data/<命名空间>/damage_elements/*.json`

固定元素伤害示例：

```json
{
  "damage_types": [
    "minecraft:in_fire",
    "minecraft:on_fire"
  ],
  "element": "fire",
  "strength": 1.0
}
```

使用攻击者灵根的伤害示例：

```json
{
  "damage_types": [
    "minecraft:player_attack"
  ],
  "element": "attacker_spiritual_root",
  "strength": 1.0,
  "fallback_element": "metal",
  "fallback_strength": 1.0
}
```

- `damage_types`：需要映射的原版或模组伤害类型 ID。
- `element`：`metal`、`wood`、`water`、`fire`、`earth`、`yin_yang`、`thunder`、`chaos`，或动态值 `attacker_spiritual_root`。
- `strength`：固定元素的旺盛度；使用攻击者灵根时，此值作为攻击者灵根旺盛度的倍率。
- `fallback_element`、`fallback_strength`：动态元素无法从攻击者身上取得时使用的后备值。

默认规则位于：

`src/main/resources/data/ninecun_lingtai/damage_elements/`

平衡倍率位于通用配置的 `elementDamage` 节点。相生方向的攻击固定削弱；相克双方会比较旺盛度，先天相克方向拥有额外优势，但旺盛度不足时仍会被反压制。

默认命中特效：金为暴击碎芒、木为自然生机、水为水花、火为火焰、土为泥土碎屑、阴阳为附魔流光、雷为电弧、混沌为逆向传送门粒子。

## 维度与群系环境倍率

环境规则文件放在：

`data/<命名空间>/element_environments/*.json`

单条规则示例：

```json
{
  "dimensions": [
    "minecraft:the_nether"
  ],
  "multipliers": {
    "fire": 1.75,
    "water": 0.45
  }
}
```

一个文件也可以通过 `rules` 放置多条规则：

```json
{
  "rules": [
    {
      "biomes": [
        "minecraft:ocean",
        "minecraft:deep_ocean"
      ],
      "multipliers": {
        "water": 1.75,
        "fire": 0.5
      }
    },
    {
      "biome_tags": [
        "minecraft:is_forest"
      ],
      "multipliers": {
        "wood": 1.4
      }
    }
  ]
}
```

- `dimensions`：规则生效的维度 ID，可省略。
- `biomes`：规则生效的群系 ID，可省略。
- `biome_tags`：规则生效的群系标签 ID，可省略。
- 同一规则同时填写维度与群系条件时，两者必须同时满足；同类条件中的多个值满足任意一个即可。
- `multipliers`：各元素的环境伤害倍率，未填写的元素按 `1.0` 处理。
- 模组自带的默认规则会显式列出金、木、水、火、土、阴阳、雷、混沌全部八种元素；数据包自定义规则仍可省略中性元素。
- 同时命中的维度、群系和数据包附加规则会相乘。
- 混沌属性的最终环境倍率始终不会低于 `1.0`，即使数据包配置了削弱倍率也不会被环境削弱。

默认规则覆盖原版三个维度与 1.20.1 的全部 64 个原版群系，位于：

`src/main/resources/data/ninecun_lingtai/element_environments/`
