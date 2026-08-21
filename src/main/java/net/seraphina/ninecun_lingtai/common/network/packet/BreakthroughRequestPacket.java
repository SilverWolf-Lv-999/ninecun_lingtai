package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.common.animation.BreakthroughAnimationType;
import net.seraphina.ninecun_lingtai.common.ascension.AscensionManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationManager;
import net.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import net.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.math.BigInteger;
import java.util.Optional;
import java.util.function.Supplier;

public final class BreakthroughRequestPacket {
    public static final BreakthroughRequestPacket INSTANCE = new BreakthroughRequestPacket();

    private BreakthroughRequestPacket() {
    }

    public static void encode(BreakthroughRequestPacket packet, FriendlyByteBuf buffer) {
    }

    public static BreakthroughRequestPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void handle(
            BreakthroughRequestPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
            return;
        }
        TribulationManager.StartResult tribulationResult = TribulationManager.request(player);
        if (tribulationResult != TribulationManager.StartResult.NOT_REQUIRED) {
            handleTribulationResult(player, tribulationResult);
            return;
        }
        AscensionManager.StartResult ascensionResult = AscensionManager.request(player);
        if (ascensionResult != AscensionManager.StartResult.NOT_REQUIRED) {
            handleAscensionResult(player, ascensionResult);
            return;
        }
        Optional<CultivationStage> previousStage = PlayerCultivationData.getStage(player);
        BreakthroughResult result = previousStage.isEmpty()
                ? PlayerCultivationManager.tryBreakthrough(player)
                : PlayerCultivationManager.tryBreakthroughRealm(player);
        switch (result) {
            case SUCCESS -> {
                CultivationStage stage = PlayerCultivationData.getStage(player).orElseThrow();
                PlayerDefenseResourceData.refillToMaximum(player);
                BreakthroughAnimationType.fromTransition(previousStage, stage).ifPresent(animationType ->
                        ModNetwork.sendBreakthroughAnimation(
                                player,
                                animationType,
                                PlayerAptitudeData.getSpiritualRoot(player)
                                        .map(SpiritualRoot::getElementMask)
                                        .orElse(0)
                        )
                );
                Component message = Component.translatable(
                        "message.ninecun_lingtai.breakthrough_success",
                        CultivationUtility.getDisplayName(stage)
                );
                player.displayClientMessage(
                        Component.empty()
                                .append(player.getDisplayName())
                                .append(" 踏入" + CultivationUtility.getDisplayName(stage)),
                        false
                );
                ModNetwork.sendCultivationFeedback(player, true, message);
            }
            case NOT_AT_REALM_PEAK -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_not_peak")
            );
            case NO_SPIRITUAL_ROOT -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_no_root")
            );
            case MAXIMUM_REALM -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_maximum_realm")
            );
            default -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable(
                            "message.ninecun_lingtai.breakthrough_insufficient_energy",
                            getRequiredEnergyText(previousStage)
                    )
            );
        }
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
    }

    private static void handleTribulationResult(
            ServerPlayer player,
            TribulationManager.StartResult result
    ) {
        switch (result) {
            case STARTED -> ModNetwork.sendCultivationInfo(player);
            case ALREADY_ACTIVE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("天劫正在进行，不可再次引动。")
            );
            case NO_APTITUDE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_no_root")
            );
            case INSUFFICIENT_SPIRITUAL_ENERGY -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_insufficient_energy")
            );
            case CANNOT_MEDITATE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("当前状态无法凝神渡劫。")
            );
            case NOT_REQUIRED -> {
            }
        }
    }

    private static void handleAscensionResult(
            ServerPlayer player,
            AscensionManager.StartResult result
    ) {
        switch (result) {
            case STARTED -> {
                ModNetwork.sendCultivationInfo(player);
                ModNetwork.sendDefenseResource(player);
            }
            case ALREADY_ACTIVE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("飞升劫正在进行，不可再次引动。")
            );
            case TRIBULATION_ACTIVE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("天劫正在进行，不可再次引动。")
            );
            case INSUFFICIENT_SPIRITUAL_ENERGY -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_insufficient_energy")
            );
            case CANNOT_MEDITATE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("当前状态无法凝神渡飞升劫。")
            );
            case NOT_REQUIRED -> {
            }
        }
    }

    private static String getRequiredEnergyText(Optional<CultivationStage> currentStage) {
        CultivationStage targetStage = currentStage
                .flatMap(stage -> stage.realm().next())
                .map(nextRealm -> new CultivationStage(nextRealm, CultivationStage.MINIMUM_LAYER))
                .orElse(CultivationStage.first());
        return Optional.of(targetStage)
                .map(CultivationUtility::getRequiredSpiritualEnergy)
                .map(ChineseNumberUtility::toCompactChinese)
                .orElse(ChineseNumberUtility.toChineseInteger(BigInteger.ZERO));
    }
}
