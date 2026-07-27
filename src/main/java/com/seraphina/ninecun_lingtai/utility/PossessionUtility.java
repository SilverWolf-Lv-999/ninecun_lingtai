package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSelectedSpellData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PossessionUtility {
    private PossessionUtility() {
    }

    public static PossessionResult tryPossess(ServerPlayer player, LivingEntity target) {
        PossessionResult validationResult = validateTarget(player, target);
        if (!validationResult.success()) {
            return validationResult;
        }
        CultivationStage targetStage = CreatureCultivationData.getStage(target).orElseThrow();
        SpiritualRoot targetRoot = CreatureCultivationData.getSpiritualRoot(target).orElseThrow();
        player.teleportTo(target.getX(), target.getY(), target.getZ());
        clearBodyBoundData(player);
        PlayerCultivationData.setStage(player, targetStage);
        PlayerAptitudeData.setSpiritualRoot(player, targetRoot);
        CultivationDefenseUtility.syncMaxHealthModifier(player);
        PlayerDefenseResourceData.refillToMaximum(player);
        player.setHealth(Math.min(player.getMaxHealth(), Math.max(1.0F, target.getHealth())));
        player.setYRot(target.getYRot());
        player.setXRot(target.getXRot());
        player.setYHeadRot(target.getYHeadRot());
        player.setYBodyRot(target.yBodyRot);
        target.discard();
        return PossessionResult.success(Component.translatable("message.ninecun_lingtai.soul_roaming.possessed"));
    }

    private static PossessionResult validateTarget(ServerPlayer player, LivingEntity target) {
        if (target instanceof Player) {
            return PossessionResult.failure(Component.translatable("message.ninecun_lingtai.soul_roaming.possess_player"));
        }
        if (!target.isAlive()) {
            return PossessionResult.failure(Component.translatable("message.ninecun_lingtai.soul_roaming.possess_invalid"));
        }
        if (target.getMobType() == MobType.UNDEAD) {
            return PossessionResult.failure(Component.translatable("message.ninecun_lingtai.soul_roaming.possess_undead"));
        }
        Optional<CultivationStage> playerStage = PlayerCultivationData.getStage(player);
        Optional<CultivationStage> targetStage = CreatureCultivationData.getStage(target);
        Optional<SpiritualRoot> targetRoot = CreatureCultivationData.getSpiritualRoot(target);
        if (playerStage.isEmpty()) {
            return PossessionResult.failure(Component.translatable("message.ninecun_lingtai.soul_roaming.no_soul_stage"));
        }
        if (targetStage.isEmpty() || targetRoot.isEmpty()) {
            return PossessionResult.failure(Component.translatable("message.ninecun_lingtai.soul_roaming.possess_no_cultivation"));
        }
        if (targetStage.get().realm().ordinal() >= playerStage.get().realm().ordinal()) {
            return PossessionResult.failure(Component.translatable("message.ninecun_lingtai.soul_roaming.possess_realm_mismatch"));
        }
        return PossessionResult.success(Component.empty());
    }

    private static void clearBodyBoundData(ServerPlayer player) {
        PlayerCultivationData.clearStage(player);
        PlayerAptitudeData.clearAptitude(player);
        PlayerTechniqueData.clear(player);
        PlayerTechniqueProficiencyData.clear(player);
        PlayerImprintedTechniqueData.clear(player);
        PlayerSelectedSpellData.clearSelectedSpell(player);
        PlayerSpiritualEnergyData.clearAllEnergy(player);
        PlayerDefenseResourceData.clear(player);
    }

    public record PossessionResult(boolean success, Component message) {
        private static PossessionResult success(Component message) {
            return new PossessionResult(true, message);
        }

        private static PossessionResult failure(Component message) {
            return new PossessionResult(false, message);
        }
    }
}
