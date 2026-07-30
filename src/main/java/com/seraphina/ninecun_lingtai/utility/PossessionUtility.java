package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerPossessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class PossessionUtility {
    private PossessionUtility() {
    }

    public static PossessionResult tryPossess(ServerPlayer player, ServerPlayer target) {
        PossessionResult validationResult = validatePossession(player, target);
        if (!validationResult.success()) {
            return validationResult;
        }
        return possess(player, target);
    }

    public static PossessionResult validatePossession(ServerPlayer player, ServerPlayer target) {
        return validateTarget(player, target);
    }

    public static PossessionResult possess(ServerPlayer player, ServerPlayer target) {
        PossessionResult validationResult = validateTarget(player, target);
        if (!validationResult.success()) {
            return validationResult;
        }
        if (!PlayerPossessionManager.startPossessing(player, target)) {
            return PossessionResult.failure(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.possess_invalid")
            );
        }
        return PossessionResult.success(
                Component.translatable("message.ninecun_lingtai.soul_roaming.possessed")
        );
    }

    private static PossessionResult validateTarget(ServerPlayer player, ServerPlayer target) {
        if (player == target
                || !target.isAlive()
                || target.isSpectator()
                || PlayerPossessionManager.isPossessing(target)
                || PlayerPossessionManager.isPossessedTarget(target)) {
            return PossessionResult.failure(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.possess_invalid")
            );
        }

        CultivationStage playerStage = PlayerCultivationData.getStage(player).orElse(null);
        CultivationStage targetStage = PlayerCultivationData.getStage(target).orElse(null);
        SpiritualRoot targetRoot = PlayerAptitudeData.getSpiritualRoot(target).orElse(null);
        if (playerStage == null) {
            return PossessionResult.failure(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.no_soul_stage")
            );
        }
        if (targetStage == null || targetRoot == null) {
            return PossessionResult.failure(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.possess_no_cultivation")
            );
        }
        if (targetStage.realm().ordinal() >= playerStage.realm().ordinal()) {
            return PossessionResult.failure(
                    Component.translatable("message.ninecun_lingtai.soul_roaming.possess_realm_mismatch")
            );
        }
        return PossessionResult.success(Component.empty());
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
