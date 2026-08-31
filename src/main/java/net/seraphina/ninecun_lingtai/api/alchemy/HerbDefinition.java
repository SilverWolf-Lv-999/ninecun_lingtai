package net.seraphina.ninecun_lingtai.api.alchemy;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 灵药属性（炼丹 M4-c，数据驱动 {@code alchemy_herbs/*.json}）。
 * 可给原版物品（如恶魂之泪）追加属性，无需接管物品注册。
 *
 * @param id      定义文件的资源位置
 * @param item    对应物品注册名（按物品查表的键）
 * @param tier    药材阶别 1-9
 * @param element 药性主元素（null = 无性平和药）
 * @param nature  药性寒温（"cold" / "neutral" / "warm" / "scorching"，暂只作展示与内容标注）
 */
public record HerbDefinition(
        ResourceLocation id,
        ResourceLocation item,
        int tier,
        @Nullable ElementType element,
        String nature
) {
    public HerbDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(nature, "nature");
        if (tier < 1 || tier > 9) {
            throw new IllegalArgumentException("herb tier must be within [1, 9]: " + tier);
        }
    }
}
