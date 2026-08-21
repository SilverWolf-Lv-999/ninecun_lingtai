package net.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.common.animation.BreakthroughAnimationType;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.seraphina.ninecun_lingtai.utility.DebugCommandUtility;

import java.util.Collection;
import java.util.List;

public final class BreakthroughAnimationDebugCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("breakthrough_animation")
                .then(createAnimationCommand(
                        "mortal_to_qi",
                        BreakthroughAnimationType.MORTAL_TO_QI
                ))
                .then(createAnimationCommand(
                        "qi_to_foundation",
                        BreakthroughAnimationType.QI_TO_FOUNDATION
                ));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createAnimationCommand(
            String commandName,
            BreakthroughAnimationType animationType
    ) {
        return Commands.literal(commandName)
                .executes(context -> sendAnimation(
                        context.getSource(),
                        List.of(context.getSource().getPlayerOrException()),
                        animationType
                ))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> sendAnimation(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "targets"),
                                animationType
                        )));
    }

    private static int sendAnimation(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players,
            BreakthroughAnimationType animationType
    ) {
        players.forEach(player -> ModNetwork.sendBreakthroughAnimation(
                player,
                animationType,
                PlayerAptitudeData.getSpiritualRoot(player)
                        .map(SpiritualRoot::getElementMask)
                        .orElse(0)
        ));
        commandSource.sendSuccess(() -> Component.literal(
                "已播放突破动画 " + animationType.name().toLowerCase()
                        + "：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.isEmpty() ? Command.SINGLE_SUCCESS : players.size();
    }
}
