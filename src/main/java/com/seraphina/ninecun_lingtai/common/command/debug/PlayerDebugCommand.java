package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class PlayerDebugCommand {
    private PlayerDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("player")
                .executes(context -> inspectPlayer(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.literal("inspect")
                        .executes(context -> inspectPlayer(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> inspectPlayer(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("reset")
                        .executes(context -> resetPlayers(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> resetPlayers(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("sync")
                        .executes(context -> syncPlayers(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> syncPlayers(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static int inspectPlayer(CommandSourceStack commandSource, ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        commandSource.sendSuccess(() -> Component.literal("§6[玩家调试] §f" + playerName), false);
        commandSource.sendSuccess(() -> Component.literal(
                "灵根：" + PlayerAptitudeData.getSpiritualRoot(player)
                        .map(DebugCommandUtility::formatSpiritualRoot)
                        .orElse("未设置")
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "魂魄：" + PlayerAptitudeData.getSoulStrength(player)
                        .map(DebugCommandUtility::formatSoulStrength)
                        .orElse("未设置")
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "境界：" + PlayerCultivationData.getStage(player)
                        .map(DebugCommandUtility::formatCultivationStage)
                        .orElse("未入道")
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "灵力：" + DebugCommandUtility.formatSpiritualEnergy(player)
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "打坐：" + (PlayerMeditationManager.isMeditating(player) ? "是" : "否")
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetPlayers(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        for (ServerPlayer player : players) {
            PlayerMeditationManager.stopMeditating(player);
            PlayerAptitudeData.clearAptitude(player);
            PlayerCultivationData.clearStage(player);
            PlayerSpiritualEnergyData.clearAllEnergy(player);
            ModNetwork.sendCultivationInfo(player);
            ModNetwork.sendAptitudeScreenRequest(player);
        }
        commandSource.sendSuccess(() -> Component.literal(
                "已完全重置玩家数据并重新打开资质界面：" + DebugCommandUtility.getPlayerNames(players)
        ), true);
        return players.size();
    }

    private static int syncPlayers(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        DebugCommandUtility.syncCultivationInfo(players);
        for (ServerPlayer player : players) {
            PlayerMeditationManager.syncState(player, player);
        }
        commandSource.sendSuccess(() -> Component.literal(
                "已向客户端重新同步玩家数据：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }
}
