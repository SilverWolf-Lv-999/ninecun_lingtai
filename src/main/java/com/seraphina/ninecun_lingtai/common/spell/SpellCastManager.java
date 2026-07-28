package com.seraphina.ninecun_lingtai.common.spell;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.spell.Spell;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueSpell;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerSelectedSpellData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import com.seraphina.ninecun_lingtai.common.registry.ModSpells;
import com.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import com.seraphina.ninecun_lingtai.utility.SpellCostUtility;
import com.seraphina.ninecun_lingtai.utility.SpellPowerUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 服务端施法入口。法术是一门带 {@code spell} 块的功法：须先参悟（学会）方可择定与施展，
 * 每次施展消耗随品级与本次实际威力共同增长，并像修炼一样积累这门功法的熟练度。
 */
public final class SpellCastManager {
    private static final long PROFICIENCY_PER_CAST = 1L;

    private SpellCastManager() {
    }

    public static void selectSpell(ServerPlayer player, ResourceLocation techniqueId) {
        Optional<TechniqueDefinition> technique = TechniqueManager.INSTANCE.getTechnique(techniqueId);
        if (technique.isEmpty() || technique.get().spell().isEmpty()) {
            sendActionBar(player, "此法术不存在于天地之间");
            return;
        }
        if (!PlayerTechniqueData.hasTechnique(player, techniqueId)) {
            sendActionBar(player, "尚未参悟《" + technique.get().name() + "》，无法施展");
            return;
        }
        TechniqueSpell spellData = technique.get().spell().get();
        PlayerSelectedSpellData.setSelectedSpell(player, techniqueId);
        sendActionBar(player, String.format(
                "已择定《%s》（%s·%s）",
                technique.get().name(),
                spellData.grade().getChineseName(),
                spellData.category().getChineseName()
        ));
    }

    public static void castSelectedSpell(ServerPlayer player) {
        Optional<ResourceLocation> selectedTechniqueId = PlayerSelectedSpellData.getSelectedSpell(player);
        if (selectedTechniqueId.isEmpty()) {
            sendActionBar(player, "尚未择定任何法术");
            return;
        }
        Optional<TechniqueDefinition> techniqueOptional =
                TechniqueManager.INSTANCE.getTechnique(selectedTechniqueId.get());
        if (techniqueOptional.isEmpty() || techniqueOptional.get().spell().isEmpty()) {
            PlayerSelectedSpellData.clearSelectedSpell(player);
            sendActionBar(player, "所择法术已失传，请重新择法");
            return;
        }
        castTechnique(player, techniqueOptional.get());
    }

    /** 施展指定的法术类功法（御剑等法术亦可绕过择法，由特定操作直接触发）。 */
    public static void castTechnique(ServerPlayer player, TechniqueDefinition technique) {
        if (technique.spell().isEmpty()) {
            sendActionBar(player, "此功并非法术，无从施展");
            return;
        }
        TechniqueSpell spellData = technique.spell().get();
        if (!PlayerTechniqueData.hasTechnique(player, technique.id())) {
            sendActionBar(player, "尚未参悟《" + technique.name() + "》，无法施展");
            return;
        }
        Spell spell = ModSpells.REGISTRY.get().getValue(spellData.spellId());
        if (spell == null) {
            sendActionBar(player, "《" + technique.name() + "》玄机晦涩，此方天地暂无法回应");
            return;
        }
        if (spell.cancelIfActive(player)) {
            sendActionBar(player, "已解除《" + technique.name() + "》");
            return;
        }
        Optional<CultivationStage> stage = CultivationApi.getStage(player);
        if (stage.isEmpty()) {
            sendActionBar(player, "凡人之躯，无法驱动灵力施法");
            return;
        }
        TechniqueMasteryStage masteryStage = PlayerTechniqueData.getTechnique(player, technique.id())
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .orElse(TechniqueMasteryStage.ENTRY);
        long proficiency = PlayerTechniqueProficiencyData.get(player, technique.id());
        double powerEfficiency = SpellPowerUtility.getPowerEfficiency(
                masteryStage,
                proficiency,
                PlayerAptitudeData.getSpiritualRoot(player)
        );
        int cultivationStrength = getCultivationStrength(stage.get());
        double cost = SpellCostUtility.getSpiritualEnergyCost(
                spellData,
                cultivationStrength,
                powerEfficiency
        );
        Map<ElementType, Double> anyElementConsumption = null;
        if (spellData.consumesAnyElement()) {
            anyElementConsumption = PlayerSpiritualEnergyData.consumeEnergyAny(player, cost);
            if (anyElementConsumption == null) {
                sendActionBar(player, String.format(
                        "灵力不足，施展《%s》需%s",
                        technique.name(),
                        describeCost(spellData, cost)
                ));
                return;
            }
        } else if (!PlayerSpiritualEnergyData.consumeEnergy(player, spellData.costElements(), cost)) {
            sendActionBar(player, String.format(
                    "灵力不足，施展《%s》需%s",
                    technique.name(),
                    describeCost(spellData, cost)
            ));
            return;
        }
        SpellCastContext context = new SpellCastContext(
                technique,
                spellData,
                masteryStage,
                powerEfficiency,
                cultivationStrength
        );
        if (!spell.cast(player, context)) {
            if (anyElementConsumption != null) {
                for (Map.Entry<ElementType, Double> entry : anyElementConsumption.entrySet()) {
                    PlayerSpiritualEnergyData.addEnergy(player, entry.getKey(), entry.getValue());
                }
            } else {
                for (ElementType element : spellData.costElements()) {
                    PlayerSpiritualEnergyData.addEnergy(player, element, cost);
                }
            }
            ModNetwork.sendCultivationInfo(player);
            sendActionBar(player, "施法未成，灵力已归丹田");
            return;
        }
        // 法术亦是功法，愈用愈纯熟；阶段晋升由共用逻辑负责播报。
        PlayerTechniqueManager.addProficiency(player, technique.id(), PROFICIENCY_PER_CAST);
        ModNetwork.sendCultivationInfo(player);
        sendActionBar(player, String.format(
                "施展《%s》（%s），耗去%s",
                technique.name(),
                masteryStage.getChineseName(),
                describeCost(spellData, cost)
        ));
    }

    public static int getCultivationStrength(CultivationStage stage) {
        return stage.realm().ordinal() * CultivationStage.MAXIMUM_LAYER + stage.layer();
    }

    private static String describeCost(TechniqueSpell spellData, double cost) {
        if (spellData.consumesAnyElement()) {
            return formatCost(cost) + " 点灵力（不拘属性）";
        }
        String elementNames = spellData.costElements().stream()
                .map(SpiritualRootUtility::getElementName)
                .collect(Collectors.joining("、"));
        String costText = formatCost(cost) + " 点" + elementNames + "系灵力";
        return spellData.costElements().size() > 1 ? "各 " + costText : costText;
    }

    private static String formatCost(double cost) {
        return ChineseNumberUtility.toCompactChinese(cost, 1);
    }

    private static void sendActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
