package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class AptitudeDebugCommand {
    private AptitudeDebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("aptitude")
                .executes(context -> reopenAptitudeScreen(
                        context.getSource(),
                        List.of(context.getSource().getPlayerOrException())
                ))
                .then(Commands.literal("reopen")
                        .executes(context -> reopenAptitudeScreen(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> reopenAptitudeScreen(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("clear")
                        .executes(context -> clearAptitude(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clearAptitude(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("elements", StringArgumentType.string())
                                        .suggests(DebugCommandUtility::suggestRootElements)
                                        .then(Commands.argument(
                                                        "length",
                                                        FloatArgumentType.floatArg(
                                                                SpiritualRoot.MINIMUM_LENGTH_INCHES,
                                                                SpiritualRoot.MAXIMUM_LENGTH_INCHES
                                                        )
                                                )
                                                .then(Commands.argument(
                                                                "stars",
                                                                IntegerArgumentType.integer(
                                                                        SoulStrength.MINIMUM_ILLUMINATED_STARS,
                                                                        SoulStrength.MAXIMUM_ILLUMINATED_STARS
                                                                )
                                                        )
                                                        .executes(AptitudeDebugCommand::setAptitude))))))
                .then(createSpiritualRootCommand())
                .then(createSoulStrengthCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSpiritualRootCommand() {
        return Commands.literal("spiritual_root")
                .then(Commands.literal("get")
                        .executes(context -> getSpiritualRoot(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getSpiritualRoot(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("elements", StringArgumentType.string())
                                        .suggests(DebugCommandUtility::suggestRootElements)
                                        .then(Commands.argument(
                                                        "length",
                                                        FloatArgumentType.floatArg(
                                                                SpiritualRoot.MINIMUM_LENGTH_INCHES,
                                                                SpiritualRoot.MAXIMUM_LENGTH_INCHES
                                                        )
                                                )
                                                .executes(AptitudeDebugCommand::setSpiritualRoot)))))
                .then(Commands.literal("elements")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("elements", StringArgumentType.string())
                                        .suggests(DebugCommandUtility::suggestRootElements)
                                        .executes(AptitudeDebugCommand::setSpiritualRootElements))))
                .then(Commands.literal("length")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument(
                                                "length",
                                                FloatArgumentType.floatArg(
                                                        SpiritualRoot.MINIMUM_LENGTH_INCHES,
                                                        SpiritualRoot.MAXIMUM_LENGTH_INCHES
                                                )
                                        )
                                        .executes(AptitudeDebugCommand::setSpiritualRootLength))))
                .then(Commands.literal("clear")
                        .executes(context -> clearSpiritualRoot(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clearSpiritualRoot(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSoulStrengthCommand() {
        return Commands.literal("soul_strength")
                .then(Commands.literal("get")
                        .executes(context -> getSoulStrength(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getSoulStrength(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument(
                                                "stars",
                                                IntegerArgumentType.integer(
                                                        SoulStrength.MINIMUM_ILLUMINATED_STARS,
                                                        SoulStrength.MAXIMUM_ILLUMINATED_STARS
                                                )
                                        )
                                        .executes(AptitudeDebugCommand::setSoulStrength))))
                .then(Commands.literal("clear")
                        .executes(context -> clearSoulStrength(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clearSoulStrength(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static int getSpiritualRoot(CommandSourceStack commandSource, ServerPlayer player) {
        String rootDescription = PlayerAptitudeData.getSpiritualRoot(player)
                .map(DebugCommandUtility::formatSpiritualRoot)
                .orElse("未设置");
        commandSource.sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " 的灵根：" + rootDescription
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int getSoulStrength(CommandSourceStack commandSource, ServerPlayer player) {
        String soulDescription = PlayerAptitudeData.getSoulStrength(player)
                .map(DebugCommandUtility::formatSoulStrength)
                .orElse("未设置");
        commandSource.sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " 的魂魄强度：" + soulDescription
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAptitude(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        SpiritualRoot spiritualRoot = new SpiritualRoot(
                DebugCommandUtility.getSpiritualRootElements(context, "elements"),
                FloatArgumentType.getFloat(context, "length")
        );
        SoulStrength soulStrength = new SoulStrength(IntegerArgumentType.getInteger(context, "stars"));
        players.forEach(player -> PlayerAptitudeData.setAptitude(player, spiritualRoot, soulStrength));
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已设置完整资质：" + DebugCommandUtility.getPlayerNames(players)
                        + " -> " + DebugCommandUtility.formatSpiritualRoot(spiritualRoot)
                        + "，魂魄" + DebugCommandUtility.formatSoulStrength(soulStrength)
        ), false);
        return players.size();
    }

    private static int setSpiritualRoot(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        SpiritualRoot spiritualRoot = new SpiritualRoot(
                DebugCommandUtility.getSpiritualRootElements(context, "elements"),
                FloatArgumentType.getFloat(context, "length")
        );
        players.forEach(player -> PlayerAptitudeData.setSpiritualRoot(player, spiritualRoot));
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已设置灵根：" + DebugCommandUtility.getPlayerNames(players)
                        + " -> " + DebugCommandUtility.formatSpiritualRoot(spiritualRoot)
        ), false);
        return players.size();
    }

    private static int setSpiritualRootElements(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        Set<ElementType> elements = DebugCommandUtility.getSpiritualRootElements(context, "elements");
        for (ServerPlayer player : players) {
            float length = PlayerAptitudeData.getSpiritualRoot(player)
                    .map(SpiritualRoot::lengthInches)
                    .orElse(SpiritualRoot.MAXIMUM_LENGTH_INCHES);
            PlayerAptitudeData.setSpiritualRoot(player, new SpiritualRoot(elements, length));
        }
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已修改灵根元素（未设置灵根者使用九寸）：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int setSpiritualRootLength(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer player : players) {
            if (PlayerAptitudeData.getSpiritualRoot(player).isEmpty()) {
                context.getSource().sendFailure(Component.literal(
                        player.getGameProfile().getName() + " 尚未设置灵根，无法仅修改寸数。"
                ));
                return 0;
            }
        }
        float length = FloatArgumentType.getFloat(context, "length");
        for (ServerPlayer player : players) {
            SpiritualRoot oldRoot = PlayerAptitudeData.getSpiritualRoot(player).orElseThrow();
            PlayerAptitudeData.setSpiritualRoot(player, new SpiritualRoot(oldRoot.elements(), length));
        }
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已将灵根寸数设为 " + DebugCommandUtility.formatNumber(length)
                        + "：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int setSoulStrength(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        SoulStrength soulStrength = new SoulStrength(IntegerArgumentType.getInteger(context, "stars"));
        players.forEach(player -> PlayerAptitudeData.setSoulStrength(player, soulStrength));
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已将魂魄强度设为 " + DebugCommandUtility.formatSoulStrength(soulStrength)
                        + "：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int reopenAptitudeScreen(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        for (ServerPlayer player : players) {
            PlayerAptitudeData.clearAptitude(player);
            ModNetwork.sendCultivationInfo(player);
            ModNetwork.sendAptitudeScreenRequest(player);
        }
        commandSource.sendSuccess(() -> Component.literal(
                "已清除资质并重新打开选择界面：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int clearAptitude(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        players.forEach(PlayerAptitudeData::clearAptitude);
        DebugCommandUtility.syncCultivationInfo(players);
        commandSource.sendSuccess(() -> Component.literal(
                "已清除完整资质：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int clearSpiritualRoot(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        players.forEach(PlayerAptitudeData::clearSpiritualRoot);
        DebugCommandUtility.syncCultivationInfo(players);
        commandSource.sendSuccess(() -> Component.literal(
                "已清除灵根：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int clearSoulStrength(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        players.forEach(PlayerAptitudeData::clearSoulStrength);
        DebugCommandUtility.syncCultivationInfo(players);
        commandSource.sendSuccess(() -> Component.literal(
                "已清除魂魄强度：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }
}
