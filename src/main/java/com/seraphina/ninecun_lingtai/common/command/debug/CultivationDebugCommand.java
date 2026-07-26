package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class CultivationDebugCommand {
    private static final int TOTAL_STAGE_COUNT = CultivationRealm.values().length * CultivationStage.MAXIMUM_LAYER;

    private CultivationDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("cultivation")
                .executes(context -> getCultivation(
                        context.getSource(),
                        context.getSource().getPlayerOrException()
                ))
                .then(Commands.literal("get")
                        .executes(context -> getCultivation(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getCultivation(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("realm", StringArgumentType.string())
                                        .suggests(DebugCommandUtility::suggestRealms)
                                        .then(Commands.argument(
                                                        "layer",
                                                        IntegerArgumentType.integer(
                                                                CultivationStage.MINIMUM_LAYER,
                                                                CultivationStage.MAXIMUM_LAYER
                                                        )
                                                )
                                                .executes(CultivationDebugCommand::setCultivation)))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument(
                                                "stages",
                                                IntegerArgumentType.integer(-TOTAL_STAGE_COUNT, TOTAL_STAGE_COUNT)
                                        )
                                        .executes(CultivationDebugCommand::addCultivationStages))))
                .then(Commands.literal("max")
                        .executes(context -> maximizeCultivation(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> maximizeCultivation(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("clear")
                        .executes(context -> clearCultivation(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clearCultivation(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static int getCultivation(CommandSourceStack commandSource, ServerPlayer player) {
        String stageDescription = PlayerCultivationData.getStage(player)
                .map(DebugCommandUtility::formatCultivationStage)
                .orElse("未入道");
        commandSource.sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " 的境界：" + stageDescription
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setCultivation(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        CultivationStage stage = new CultivationStage(
                DebugCommandUtility.getRealm(context, "realm"),
                IntegerArgumentType.getInteger(context, "layer")
        );
        players.forEach(player -> PlayerCultivationData.setStage(player, stage));
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已设置境界：" + DebugCommandUtility.getPlayerNames(players)
                        + " -> " + DebugCommandUtility.formatCultivationStage(stage)
        ), false);
        return players.size();
    }

    private static int addCultivationStages(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        int offset = IntegerArgumentType.getInteger(context, "stages");
        for (ServerPlayer player : players) {
            int currentIndex = PlayerCultivationData.getStage(player)
                    .map(CultivationUtility::getProgressionIndex)
                    .orElse(-1);
            int targetIndex = Math.max(-1, Math.min(TOTAL_STAGE_COUNT - 1, currentIndex + offset));
            if (targetIndex < 0) {
                PlayerCultivationData.clearStage(player);
            } else {
                PlayerCultivationData.setStage(player, getStageByProgressionIndex(targetIndex));
            }
        }
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已将境界进度调整 " + (offset >= 0 ? "+" : "") + offset
                        + " 阶（自动限制在未入道至最高境界）："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int maximizeCultivation(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        CultivationStage maximumStage = getStageByProgressionIndex(TOTAL_STAGE_COUNT - 1);
        players.forEach(player -> PlayerCultivationData.setStage(player, maximumStage));
        DebugCommandUtility.syncCultivationInfo(players);
        commandSource.sendSuccess(() -> Component.literal(
                "已将境界设为最高：" + DebugCommandUtility.getPlayerNames(players)
                        + " -> " + DebugCommandUtility.formatCultivationStage(maximumStage)
        ), false);
        return players.size();
    }

    private static int clearCultivation(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        players.forEach(PlayerCultivationData::clearStage);
        DebugCommandUtility.syncCultivationInfo(players);
        commandSource.sendSuccess(() -> Component.literal(
                "已清除境界：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static CultivationStage getStageByProgressionIndex(int progressionIndex) {
        CultivationRealm realm = CultivationRealm.values()[progressionIndex / CultivationStage.MAXIMUM_LAYER];
        int layer = progressionIndex % CultivationStage.MAXIMUM_LAYER + CultivationStage.MINIMUM_LAYER;
        return new CultivationStage(realm, layer);
    }
}
