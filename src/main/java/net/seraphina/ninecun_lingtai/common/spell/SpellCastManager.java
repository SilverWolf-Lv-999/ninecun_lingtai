package net.seraphina.ninecun_lingtai.common.spell;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.spell.Spell;
import net.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueSpell;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerSelectedSpellData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.seraphina.ninecun_lingtai.common.registry.ModSpells;
import net.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import net.seraphina.ninecun_lingtai.utility.SpellCostUtility;
import net.seraphina.ninecun_lingtai.utility.SpellPowerUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        castSelectedSpell(player, null);
    }

    public static void castSelectedSpell(ServerPlayer player, UUID targetPlayerId) {
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
        castTechnique(player, techniqueOptional.get(), false, targetPlayerId);
    }

    public static void castTechnique(ServerPlayer player, TechniqueDefinition technique) {
        castTechnique(player, technique, false);
    }

    public static void castTechniqueFromInteraction(ServerPlayer player, TechniqueDefinition technique) {
        castTechnique(player, technique, true);
    }

    private static void castTechnique(
            ServerPlayer player,
            TechniqueDefinition technique,
            boolean interactionTriggered
    ) {
        castTechnique(player, technique, interactionTriggered, null);
    }

    private static void castTechnique(
            ServerPlayer player,
            TechniqueDefinition technique,
            boolean interactionTriggered,
            UUID targetPlayerId
    ) {
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
        Set<ElementType> costElements = SpellCostUtility.getEffectiveCostElements(
                spellData,
                PlayerAptitudeData.getSpiritualRoot(player)
        );
        Map<ElementType, Double> anyElementConsumption = null;
        if (costElements.isEmpty()) {
            anyElementConsumption = PlayerSpiritualEnergyData.consumeEnergyAny(player, cost);
            if (anyElementConsumption == null) {
                sendActionBar(player, String.format(
                        "灵力不足，施展《%s》需%s",
                        technique.name(),
                        describeCost(costElements, cost)
                ));
                return;
            }
        } else if (!PlayerSpiritualEnergyData.consumeEnergy(player, costElements, cost)) {
            sendActionBar(player, String.format(
                    "灵力不足，施展《%s》需%s",
                    technique.name(),
                    describeCost(costElements, cost)
            ));
            return;
        }
        SpellCastContext context = new SpellCastContext(
                technique,
                spellData,
                masteryStage,
                powerEfficiency,
                cultivationStrength,
                interactionTriggered,
                targetPlayerId
        );
        if (!spell.cast(player, context)) {
            if (anyElementConsumption != null) {
                for (Map.Entry<ElementType, Double> entry : anyElementConsumption.entrySet()) {
                    PlayerSpiritualEnergyData.addEnergy(player, entry.getKey(), entry.getValue());
                }
            } else {
                for (ElementType element : costElements) {
                    PlayerSpiritualEnergyData.addEnergy(player, element, cost);
                }
            }
            ModNetwork.sendCultivationInfo(player);
            sendActionBar(player, "施法未成，灵力已归丹田");
            return;
        }
        PlayerTechniqueManager.addProficiency(player, technique.id(), PROFICIENCY_PER_CAST);
        ModNetwork.sendCultivationInfo(player);
        sendActionBar(player, String.format(
                "施展《%s》（%s），耗去%s",
                technique.name(),
                masteryStage.getChineseName(),
                describeCost(costElements, cost)
        ));
    }

    public static int getCultivationStrength(CultivationStage stage) {
        return stage.realm().ordinal() * CultivationStage.MAXIMUM_LAYER + stage.layer();
    }

    private static String describeCost(Set<ElementType> costElements, double cost) {
        if (costElements.isEmpty()) {
            return formatCost(cost) + " 点灵力（不拘属性）";
        }
        if (costElements.size() == 1 && costElements.contains(ElementType.CHAOS)) {
            return formatCost(cost) + " 点混沌灵力";
        }
        String elementNames = costElements.stream()
                .map(SpiritualRootUtility::getElementName)
                .collect(Collectors.joining("、"));
        String costText = formatCost(cost) + " 点" + elementNames + "系灵力";
        return costElements.size() > 1 ? "各 " + costText : costText;
    }

    private static String formatCost(double cost) {
        return ChineseNumberUtility.toCompactChinese(cost, 1);
    }

    private static void sendActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
