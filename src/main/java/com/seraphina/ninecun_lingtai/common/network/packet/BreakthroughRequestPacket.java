package com.seraphina.ninecun_lingtai.common.network.packet;

import com.seraphina.ninecun_lingtai.api.cultivation.BreakthroughResult;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import com.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
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
        Optional<CultivationStage> previousStage = PlayerCultivationData.getStage(player);
        BreakthroughResult result = PlayerCultivationManager.tryBreakthroughRealm(player);
        switch (result) {
            case SUCCESS -> {
                CultivationStage stage = PlayerCultivationData.getStage(player).orElseThrow();
                PlayerDefenseResourceData.refillToMaximum(player);
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

    private static String getRequiredEnergyText(Optional<CultivationStage> currentStage) {
        return currentStage
                .flatMap(stage -> stage.realm().next())
                .map(nextRealm -> CultivationUtility.getRequiredSpiritualEnergy(
                        new CultivationStage(nextRealm, CultivationStage.MINIMUM_LAYER)
                ))
                .map(ChineseNumberUtility::toCompactChinese)
                .orElse(ChineseNumberUtility.toChineseInteger(BigInteger.ZERO));
    }
}
