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
import com.seraphina.ninecun_lingtai.common.player.PlayerSelectedSpellData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import com.seraphina.ninecun_lingtai.common.registry.ModSpells;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 服务端施法入口。法术是一门带 {@code spell} 块的功法：须先参悟（学会）方可择定与施展，
 * 每次施展消耗与品级、修为成正比的灵力，并像修炼一样积累这门功法的熟练度。
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
        PlayerSelectedSpellData.setSelectedSpell(player, techniqueId);
        sendActionBar(player, String.format(
                "已择定《%s》（%s）",
                technique.get().name(),
                technique.get().spell().get().grade().getChineseName()
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
        TechniqueDefinition technique = techniqueOptional.get();
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
        Optional<CultivationStage> stage = CultivationApi.getStage(player);
        if (stage.isEmpty()) {
            sendActionBar(player, "凡人之躯，无法驱动灵力施法");
            return;
        }
        double cost = getSpiritualEnergyCost(spellData, stage.get());
        if (!PlayerSpiritualEnergyData.consumeEnergy(player, spellData.costElements(), cost)) {
            sendActionBar(player, String.format(
                    "灵力不足，施展《%s》需%s",
                    technique.name(),
                    describeCost(spellData, cost)
            ));
            return;
        }
        TechniqueMasteryStage masteryStage = PlayerTechniqueData.getTechnique(player, technique.id())
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .orElse(TechniqueMasteryStage.ENTRY);
        SpellCastContext context = new SpellCastContext(
                technique,
                spellData,
                masteryStage,
                masteryStage.getEfficiency(),
                getCultivationStrength(stage.get())
        );
        if (!spell.cast(player, context)) {
            for (ElementType element : spellData.costElements()) {
                PlayerSpiritualEnergyData.addEnergy(player, element, cost);
            }
            sendActionBar(player, "施法未成，灵力已归丹田");
            return;
        }
        // 法术亦是功法，愈用愈纯熟；阶段晋升由共用逻辑负责播报。
        PlayerTechniqueManager.addProficiency(player, technique.id(), PROFICIENCY_PER_CAST);
        sendActionBar(player, String.format(
                "施展《%s》（%s），耗去%s",
                technique.name(),
                masteryStage.getChineseName(),
                describeCost(spellData, cost)
        ));
    }

    /**
     * 灵力消耗与法术品级、修为强度成正比。
     */
    public static double getSpiritualEnergyCost(TechniqueSpell spellData, CultivationStage stage) {
        return spellData.baseCost()
                * spellData.grade().getCostMultiplier()
                * getCultivationStrength(stage);
    }

    public static int getCultivationStrength(CultivationStage stage) {
        return stage.realm().ordinal() * CultivationStage.MAXIMUM_LAYER + stage.layer();
    }

    private static String describeCost(TechniqueSpell spellData, double cost) {
        String elementNames = spellData.costElements().stream()
                .map(SpiritualRootUtility::getElementName)
                .collect(Collectors.joining("、"));
        String costText = formatCost(cost) + " 点" + elementNames + "系灵力";
        return spellData.costElements().size() > 1 ? "各 " + costText : costText;
    }

    private static String formatCost(double cost) {
        long roundedCost = Math.round(cost);
        return Math.abs(cost - roundedCost) < 1.0E-6D
                ? Long.toString(roundedCost)
                : String.format("%.1f", cost);
    }

    private static void sendActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
