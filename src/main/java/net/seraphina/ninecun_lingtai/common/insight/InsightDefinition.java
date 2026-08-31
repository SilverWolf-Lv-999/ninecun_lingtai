package net.seraphina.ninecun_lingtai.common.insight;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;

/**
 * 单枚悟道印记的静态定义（T14，M1 代码内注册，datapack 化留 M5）。
 *
 * @param id              印记 id，语言键为 insight.ninecun_lingtai.&lt;path 以 . 连接&gt;.name / .desc
 * @param realm           抽取门槛：破入该大境界时进入候选池
 * @param category        类别（加权与展示用）
 * @param tier            档位
 * @param affinityElement 亲和元素：玩家灵根含该元素时抽取权重翻倍，可为 null（无偏向）
 * @param effects         效果表：effectKey -&gt; 数值，经 {@link InsightEffectUtility#query} 单点读取（R9）
 */
public record InsightDefinition(
        ResourceLocation id,
        CultivationRealm realm,
        Category category,
        Tier tier,
        @Nullable ElementType affinityElement,
        Map<String, Double> effects
) {
    public enum Category {
        COMBAT,
        ALCHEMY,
        FORMATION,
        SURVIVAL
    }

    public enum Tier {
        ENTRY,
        ADEPT,
        MASTER
    }

    public InsightDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(realm, "realm");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(tier, "tier");
        effects = Map.copyOf(effects);
    }

    public double getEffect(String effectKey) {
        return this.effects.getOrDefault(effectKey, 0.0D);
    }

    /** 语言键主干：insight.ninecun_lingtai.golden_core.stable_as_bedrock 之类（path 的 / 换 .）。 */
    public String getTranslationKeyBase() {
        return "insight." + ModMain.MOD_ID + "." + this.id.getPath().replace('/', '.');
    }
}
