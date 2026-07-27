# 复杂 NBT 创意栏物品 Utility

`ComplexNbtCreativeTabUtility` 用于在创意栏列表中补回带有复杂 NBT 的 `ItemStack` 变体。

Filters API 的分类数据只保存 `Item`，刷新创意栏时会用 `new ItemStack(item)` 重建列表。这样会丢掉同一个物品上的 NBT 信息，例如功法玉简的功法 id、名称和描述。因此 `ninecun_lingtai` 使用 mixin 接入 Filters API 的创意栏刷新流程，并在刷新完成后调用这个 utility。

当前用途：

- 基础物品：`ninecun_lingtai:jade_slip`
- 复杂 NBT 变体来源：`TechniqueCreativeTabCache.getItemStacks()`
- 结果：当“功法”分类启用并显示空白玉简时，自动补入所有带功法 NBT 的玉简。

行为约束：

- 只有基础物品已经出现在当前创意栏列表中时，才补入 NBT 变体，因此会尊重 Filters API 分类按钮的启用/禁用状态。
- 只补入与基础物品相同并且带 NBT 的 `ItemStack`，避免重复添加空白玉简。
- 使用 `ItemStack.isSameItemSameTags` 去重，避免每次刷新重复累加同一个 NBT 物品。
- 补入后按物品注册 id 排序，并由 mixin 重新应用当前滚动位置，让可见槽位和列表内容保持一致。

以后如果有其他“同一个物品、不同 NBT”的创意栏变体，可以在 `COMPLEX_NBT_ITEM_PROVIDERS` 中添加新的基础物品和变体列表来源。
