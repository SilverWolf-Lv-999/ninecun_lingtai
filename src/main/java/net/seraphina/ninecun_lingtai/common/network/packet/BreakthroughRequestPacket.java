package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.common.animation.BreakthroughAnimationType;
import net.seraphina.ninecun_lingtai.common.breakthrough.BreakthroughRitualManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerBreakthroughFailureData;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
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
        Optional<CultivationStage> previousStage = PlayerCultivationData.getStage(player);
        // P4 路由（§5.2）：巅峰大境界一律先过引灵仪式掷骰；掷骰成功后由仪式分派——
        // 目标 ∈ [金丹, ∞) 起天劫、目标 = 化神起飞升劫、目标 = 筑基直接晋升。
        // 天劫/飞升不再由本包直连（防止跳过掷骰）。
        if (previousStage.isPresent()) {
            BreakthroughRitualManager.RequestResult ritualResult =
                    BreakthroughRitualManager.request(player);
            if (ritualResult != BreakthroughRitualManager.RequestResult.NOT_MAJOR_TARGET) {
                handleRitualResult(player, ritualResult);
                return;
            }
        }
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
                        Component.translatableWithFallback(
                                "message.ninecun_lingtai.breakthrough_success_broadcast",
                                "%s 踏入%s",
                                player.getDisplayName(),
                                CultivationUtility.getDisplayName(stage)
                        ),
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

    private static void handleRitualResult(
            ServerPlayer player,
            BreakthroughRitualManager.RequestResult result
    ) {
        switch (result) {
            case STARTED -> {
                ModNetwork.sendCultivationInfo(player);
                ModNetwork.sendDefenseResource(player);
            }
            case ALREADY_IN_RITUAL -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.breakthrough_ritual_active",
                            "引灵仪式正在进行，不可再次引动。"
                    )
            );
            case BLOCKED_BY_TRIBULATION -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("天劫正在进行，不可再次引动。")
            );
            case NO_APTITUDE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.breakthrough_no_root")
            );
            case IN_BOTTLENECK -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.breakthrough_bottleneck",
                            "道基未稳，%s 日后方可再度冲关",
                            getBottleneckRemainingDaysText(player)
                    )
            );
            case INSUFFICIENT_SPIRITUAL_ENERGY -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable(
                            "message.ninecun_lingtai.breakthrough_insufficient_energy",
                            getRequiredEnergyText(PlayerCultivationData.getStage(player))
                    )
            );
            case CANNOT_MEDITATE -> ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.literal("当前状态无法凝神引灵。")
            );
            case NOT_MAJOR_TARGET -> {
            }
        }
    }

    private static String getBottleneckRemainingDaysText(ServerPlayer player) {
        long remainingTicks = PlayerBreakthroughFailureData.getBottleneckRemainingTicks(
                player,
                player.serverLevel().getGameTime()
        );
        return Long.toString(Math.max(1L, (remainingTicks + 23999L) / 24000L));
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
