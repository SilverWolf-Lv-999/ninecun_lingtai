package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import com.seraphina.ninecun_lingtai.utility.NotificationUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class NotificationDebugCommand {
    private NotificationDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("notification")
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("title", StringArgumentType.string())
                                .then(Commands.argument("content", StringArgumentType.greedyString())
                                        .executes(context -> sendNotification(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                StringArgumentType.getString(context, "title"),
                                                StringArgumentType.getString(context, "content")
                                        )))));
    }

    private static int sendNotification(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players,
            String title,
            String content
    ) {
        players.forEach(player -> NotificationUtility.send(
                player,
                Component.literal(title),
                Component.literal(content)
        ));
        commandSource.sendSuccess(() -> Component.literal(
                "已向指定玩家发送竹卷通知：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.isEmpty() ? Command.SINGLE_SUCCESS : players.size();
    }
}
