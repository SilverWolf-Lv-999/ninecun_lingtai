package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import com.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class MeditationDebugCommand {
    private MeditationDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("meditation")
                .executes(context -> getMeditationState(
                        context.getSource(),
                        context.getSource().getPlayerOrException()
                ))
                .then(Commands.literal("get")
                        .executes(context -> getMeditationState(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getMeditationState(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("start")
                        .executes(context -> startMeditation(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> startMeditation(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("stop")
                        .executes(context -> stopMeditation(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> stopMeditation(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("toggle")
                        .executes(context -> toggleMeditation(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> toggleMeditation(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static int getMeditationState(CommandSourceStack commandSource, ServerPlayer player) {
        commandSource.sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " 的打坐状态："
                        + (PlayerMeditationManager.isMeditating(player) ? "正在打坐" : "未打坐")
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int startMeditation(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        List<ServerPlayer> startedPlayers = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (PlayerMeditationManager.startMeditating(player)) {
                startedPlayers.add(player);
            }
        }
        if (!startedPlayers.isEmpty()) {
            commandSource.sendSuccess(() -> Component.literal(
                    "已开始打坐：" + DebugCommandUtility.getPlayerNames(startedPlayers)
            ), false);
        }
        if (startedPlayers.size() < players.size()) {
            commandSource.sendFailure(Component.literal(
                    "部分玩家因死亡、旁观、乘坐、睡眠或离地等状态无法开始打坐。"
            ));
        }
        return startedPlayers.size();
    }

    private static int stopMeditation(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        players.forEach(PlayerMeditationManager::stopMeditating);
        commandSource.sendSuccess(() -> Component.literal(
                "已停止打坐：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int toggleMeditation(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        int changedPlayerCount = 0;
        for (ServerPlayer player : players) {
            if (PlayerMeditationManager.isMeditating(player)) {
                PlayerMeditationManager.stopMeditating(player);
                changedPlayerCount++;
            } else if (PlayerMeditationManager.startMeditating(player)) {
                changedPlayerCount++;
            }
        }
        int finalChangedPlayerCount = changedPlayerCount;
        commandSource.sendSuccess(() -> Component.literal(
                "已切换打坐状态：" + DebugCommandUtility.getPlayerNames(players)
                        + "（成功 " + finalChangedPlayerCount + " 人）"
        ), false);
        return changedPlayerCount;
    }
}
