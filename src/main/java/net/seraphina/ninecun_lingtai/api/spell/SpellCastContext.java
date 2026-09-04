package net.seraphina.ninecun_lingtai.api.spell;

import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueSpell;

import java.util.Objects;
import java.util.UUID;

/**
 * Everything a spell implementation may scale its behaviour by.
 *
 * @param technique           施展所依据的功法定义
 * @param spellData           功法中的法术数据（品级、基础消耗等）
 * @param masteryStage        施法者对此功法的掌握阶段（入门／小成／大成／圆满）
 * @param powerEfficiency     熟练度发挥率与灵根资质相乘后的威力比例
 * @param cultivationStrength 修为强度（境界序号 × 9 + 层数，练气一层为 1）
 * @param interactionTriggered 是否由客户端交互请求触发
 * @param targetEntityId       本次施法锁定的生物目标，可为空
 */
public record SpellCastContext(
        TechniqueDefinition technique,
        TechniqueSpell spellData,
        TechniqueMasteryStage masteryStage,
        double powerEfficiency,
        int cultivationStrength,
        boolean interactionTriggered,
        UUID targetEntityId
) {
    public SpellCastContext(
            TechniqueDefinition technique,
            TechniqueSpell spellData,
            TechniqueMasteryStage masteryStage,
            double powerEfficiency,
            int cultivationStrength
    ) {
        this(technique, spellData, masteryStage, powerEfficiency, cultivationStrength, false, null);
    }

    public SpellCastContext {
        Objects.requireNonNull(technique, "technique");
        Objects.requireNonNull(spellData, "spellData");
        Objects.requireNonNull(masteryStage, "masteryStage");
    }
}
