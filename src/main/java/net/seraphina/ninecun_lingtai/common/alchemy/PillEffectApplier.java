package net.seraphina.ninecun_lingtai.common.alchemy;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyPillEffectData;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughBonusData;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughDiscountData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerPillToxinData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritInfusionData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.seraphina.ninecun_lingtai.common.qi_deviation.QiDeviationManager;
import net.seraphina.ninecun_lingtai.common.karma.KarmaUtility;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.seraphina.ninecun_lingtai.utility.SoulTemperingUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PillEffectApplier {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public static void apply(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        switch (effect.type()) {
            case "heal" -> applyHeal(player, effect, multiplier);
            case "restore_spirit" -> applyRestoreSpirit(player, effect, multiplier);
            case "spirit_infusion" -> applySpiritInfusion(player, effect, multiplier);
            case "breakthrough_energy_discount" -> applyBreakthroughDiscount(player, effect);
            case "breakthrough_success_bonus" -> applyBreakthroughSuccessBonus(player, effect);
            case "proficiency_gain" -> applyProficiencyGain(player, effect, multiplier);
            case "lifespan_add" -> applyLifespanAdd(player, effect, multiplier);
            case "toxin_cleanse" -> applyToxinCleanse(player, effect, multiplier);
            case "defense_repair" -> applyDefenseRepair(player, effect, multiplier);
            case "food" -> applyFood(player, effect, multiplier);
            case "soul_tempering" -> applySoulTempering(player, effect, multiplier);
            case "soul_injury_heal" -> applySoulInjuryHeal(player, effect, multiplier);
            case "resolve_qi_deviation" -> QiDeviationManager.resolveByPill(player);
            case "karma_redeem" -> applyKarmaRedeem(player, effect, multiplier);
            default -> LOGGER.warn("Unknown alchemy pill effect type: {}", effect.type());
        }
    }

    private static void applyHeal(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        float healing = (float) ((player.getMaxHealth() * effect.getNumber("max_health_fraction", 0.0D)
                + effect.getNumber("flat_healing", 0.0D)) * multiplier);
        if (healing > 0.0F) {
            player.heal(healing);
        }
        int regenerationDuration = effect.getInt("regeneration_duration", 0);
        if (regenerationDuration > 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    scaleDuration(regenerationDuration, multiplier),
                    effect.getInt("regeneration_amplifier", 0)
            ));
        }
    }

    private static void applyRestoreSpirit(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (spiritualRoot.isEmpty()) {
            return;
        }
        double amount = effect.getNumber("amount", 0.0D) * multiplier;
        if (amount <= 0.0D) {
            return;
        }
        double maximumEnergy = CultivationUtility.getMaximumAbsorbableSpiritualEnergy(
                PlayerCultivationData.getStage(player)
        ).doubleValue();
        for (ElementType element : SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get())) {
            double remainingCapacity = maximumEnergy - PlayerSpiritualEnergyData.getEnergy(player, element);
            if (remainingCapacity > 0.0D) {
                PlayerSpiritualEnergyData.addEnergy(player, element, Math.min(amount, remainingCapacity));
            }
        }
        ModNetwork.sendCultivationInfo(player);
    }

    private static void applySpiritInfusion(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        Optional<CultivationStage> currentStage = PlayerCultivationData.getStage(player);
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        if (currentStage.isEmpty() || spiritualRoot.isEmpty()) {
            return;
        }
        Optional<CultivationStage> nextStage = currentStage.get().next();
        if (nextStage.isEmpty()) {
            return;
        }
        double requirement = CultivationUtility.getRequiredSpiritualEnergy(nextStage.get()).doubleValue();
        double requestedAmount = requirement
                * effect.getNumber("fraction_of_next_layer", 0.0D)
                * multiplier;
        double cap = requirement * PlayerSpiritInfusionData.INFUSION_CAP_FRACTION;
        double used = PlayerSpiritInfusionData.getUsed(player, currentStage.get());
        double allowed = Math.max(0.0D, cap - used);
        double injected = Math.min(requestedAmount, allowed);
        if (injected <= 0.0D) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.pill_infusion_capped"),
                    true
            );
            return;
        }
        for (ElementType element : SpiritualRootUtility.getIndependentRootElements(spiritualRoot.get())) {
            PlayerSpiritualEnergyData.addEnergy(player, element, injected);
        }
        PlayerSpiritInfusionData.addUsed(player, currentStage.get(), injected);
        ModNetwork.sendCultivationInfo(player);
    }

    private static void applyBreakthroughSuccessBonus(ServerPlayer player, AlchemyPillEffectData effect) {
        double bonus = effect.getNumber("bonus", 0.0D);
        int durationTicks = effect.getInt("duration_ticks", 72000);
        if (bonus <= 0.0D) {
            return;
        }
        boolean applied = PlayerBreakthroughBonusData.applyBonus(
                player,
                bonus,
                player.level().getGameTime() + durationTicks
        );
        player.displayClientMessage(
                Component.translatableWithFallback(
                        applied
                                ? "message.ninecun_lingtai.pill_breakthrough_bonus"
                                : "message.ninecun_lingtai.pill_breakthrough_bonus_wasted",
                        applied ? "破境把握提升 %s%%（三日内冲关有效）" : "已有同档或更强的破境加成，此丹白费了",
                        Math.round(bonus * 100.0D)
                ),
                true
        );
    }

    private static void applyBreakthroughDiscount(ServerPlayer player, AlchemyPillEffectData effect) {
        double discount = effect.getNumber("discount", 0.0D);
        int durationTicks = effect.getInt("duration_ticks", 72000);
        if (discount <= 0.0D) {
            return;
        }
        PlayerBreakthroughDiscountData.applyDiscount(
                player,
                discount,
                player.level().getGameTime() + durationTicks
        );
        player.displayClientMessage(
                Component.translatable(
                        "message.ninecun_lingtai.pill_breakthrough_discount",
                        Math.round(discount * 100.0D)
                ),
                true
        );
    }

    private static void applyProficiencyGain(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        long gain = Math.round(effect.getNumber("technique_proficiency", 0.0D) * multiplier);
        if (gain <= 0L) {
            return;
        }
        Optional<ResourceLocation> techniqueId = PlayerMeditationManager.getCultivatedTechnique(player)
                .or(() -> findMostPracticedTechnique(player));
        if (techniqueId.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.pill_no_technique"),
                    true
            );
            return;
        }
        PlayerTechniqueManager.addProficiency(player, techniqueId.get(), gain);
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.pill_proficiency_gain", gain),
                true
        );
    }

    private static Optional<ResourceLocation> findMostPracticedTechnique(ServerPlayer player) {
        Set<ResourceLocation> learnedTechniques = PlayerTechniqueData.getTechniques(player).keySet();
        Map<ResourceLocation, Long> proficiencies = PlayerTechniqueProficiencyData.getAll(player);
        return learnedTechniques.stream()
                .max(Comparator.comparingLong(techniqueId -> proficiencies.getOrDefault(techniqueId, 0L)));
    }

    private static void applyLifespanAdd(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        int years = (int) Math.round(effect.getNumber("years", 0.0D) * multiplier);
        if (years <= 0) {
            return;
        }
        EntityLifespanData.addAgeTicks(player, -LifespanUtility.ticksForYears(years));
        player.displayClientMessage(
                Component.translatable("message.ninecun_lingtai.pill_lifespan_add", years),
                true
        );
    }

    private static void applyToxinCleanse(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        PlayerPillToxinData.reduceToxin(player, effect.getNumber("amount", 0.0D) * multiplier);
        player.displayClientMessage(
                Component.translatable(
                        "message.ninecun_lingtai.pill_toxin_cleansed",
                        Math.round(PlayerPillToxinData.getToxin(player))
                ),
                true
        );
    }

    private static void applyDefenseRepair(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        int maximum = PlayerDefenseResourceData.capture(player).maximum();
        int repairAmount = (int) Math.round(maximum * effect.getNumber("fraction", 0.0D) * multiplier);
        if (repairAmount > 0 && PlayerDefenseResourceData.repair(player, repairAmount) > 0) {
            ModNetwork.sendDefenseResource(player);
        }
    }

    private static void applySoulTempering(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        double amount = effect.getNumber("amount", 0.0D) * multiplier;
        if (amount <= 0.0D) {
            return;
        }
        SoulTemperingUtility.addProgress(player, amount);
    }

    private static void applySoulInjuryHeal(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        double amount = effect.getNumber("amount", 0.0D) * multiplier;
        if (amount <= 0.0D) {
            return;
        }
        SoulInjuryUtility.heal(player, amount, SoulInjuryUtility.HealSource.PILL);
    }

    /** 忏心丹（道心杀孽）：消解杀孽，redeemed 记账走 KarmaUtility 统一口径。 */
    private static void applyKarmaRedeem(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        double amount = effect.getNumber("amount", 0.0D) * multiplier;
        if (amount <= 0.0D) {
            return;
        }
        KarmaUtility.redeem(player, amount);
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.pill_karma_redeem",
                        "诵经忏悔，手上血腥渐淡，心口滞涩稍平"
                ),
                true
        );
    }

    private static void applyFood(ServerPlayer player, AlchemyPillEffectData effect, double multiplier) {
        int nutrition = (int) Math.round(effect.getNumber("nutrition", 0.0D) * multiplier);
        double saturation = effect.getNumber("saturation", 0.0D) * multiplier;
        if (nutrition <= 0) {
            return;
        }
        float saturationModifier = (float) (saturation / (nutrition * 2.0D));
        player.getFoodData().eat(nutrition, saturationModifier);
    }

    private static int scaleDuration(int duration, double multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(duration * multiplier));
    }
}
