package net.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import net.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import net.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class LifespanDebugCommand {
    private static final int MAXIMUM_SET_YEARS = 1_000_000;
    private static final int MAXIMUM_ADJUST_YEARS = 1_000_000;

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("lifespan")
                .executes(context -> getLifespan(
                        context.getSource(),
                        context.getSource().getPlayerOrException()
                ))
                .then(Commands.literal("get")
                        .executes(context -> getLifespan(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getLifespan(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("years", IntegerArgumentType.integer(0, MAXIMUM_SET_YEARS))
                                        .executes(context -> setAge(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                IntegerArgumentType.getInteger(context, "years"),
                                                0
                                        ))
                                        .then(Commands.argument(
                                                        "days",
                                                        IntegerArgumentType.integer(0, LifespanUtility.DAYS_PER_YEAR - 1)
                                                )
                                                .executes(context -> setAge(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "years"),
                                                        IntegerArgumentType.getInteger(context, "days")
                                                ))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument(
                                                "years",
                                                IntegerArgumentType.integer(-MAXIMUM_ADJUST_YEARS, MAXIMUM_ADJUST_YEARS)
                                        )
                                        .executes(context -> addYears(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                IntegerArgumentType.getInteger(context, "years")
                                        )))))
                .then(Commands.literal("expire")
                        .executes(context -> expire(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> expire(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))))
                .then(Commands.literal("reset")
                        .executes(context -> reset(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> reset(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static int getLifespan(CommandSourceStack commandSource, ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        if (!LifespanUtility.hasLifespan(player)) {
            commandSource.sendSuccess(() -> Component.literal(
                    "§6" + playerName + " 不受寿元系统影响"
            ), false);
            return Command.SINGLE_SUCCESS;
        }

        LifespanSnapshot lifespan = EntityLifespanData.capture(player);
        commandSource.sendSuccess(() -> Component.literal("§6" + playerName + " 的寿元"), false);
        commandSource.sendSuccess(() -> Component.literal(
                "年龄：" + LifespanUtility.formatAge(lifespan.ageTicks())
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "寿元上限：" + LifespanUtility.formatMaximum(lifespan)
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "剩余寿元：" + LifespanUtility.formatRemaining(lifespan)
        ), false);
        commandSource.sendSuccess(() -> Component.literal("状态：" + describeStatus(lifespan)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAge(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players,
            int years,
            int days
    ) {
        long ageTicks = LifespanUtility.ticksForYearsAndDays(years, days);
        players.forEach(player -> EntityLifespanData.setAgeTicks(player, ageTicks));
        commandSource.sendSuccess(() -> Component.literal(
                "已设置年龄为 " + LifespanUtility.formatAge(ageTicks) + "："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int addYears(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players,
            int years
    ) {
        long deltaTicks = years >= 0
                ? LifespanUtility.ticksForYears(years)
                : -LifespanUtility.ticksForYears(-years);
        players.forEach(player -> EntityLifespanData.addAgeTicks(player, deltaTicks));
        commandSource.sendSuccess(() -> Component.literal(
                "已将年龄调整 " + (years >= 0 ? "+" : "") + years + " 年（自动限制在 0 岁以上）："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int expire(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        List<ServerPlayer> expiredPlayers = new ArrayList<>();
        List<ServerPlayer> immortalPlayers = new ArrayList<>();
        for (ServerPlayer player : players) {
            LifespanSnapshot lifespan = EntityLifespanData.capture(player);
            if (lifespan.immortal()) {
                immortalPlayers.add(player);
                continue;
            }
            EntityLifespanData.setAgeTicks(player, lifespan.maximumTicks());
            expiredPlayers.add(player);
        }
        if (!expiredPlayers.isEmpty()) {
            commandSource.sendSuccess(() -> Component.literal(
                    "已令以下玩家到达寿元大限（下一刻将触发寿终）："
                            + DebugCommandUtility.getPlayerNames(expiredPlayers)
            ), true);
        }
        if (!immortalPlayers.isEmpty()) {
            commandSource.sendFailure(Component.literal(
                    "以下玩家已入长生之境，无法触发寿终：" + DebugCommandUtility.getPlayerNames(immortalPlayers)
            ));
        }
        return expiredPlayers.size();
    }

    private static int reset(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        players.forEach(player -> EntityLifespanData.setAgeTicks(player, 0L));
        commandSource.sendSuccess(() -> Component.literal(
                "已重置寿元（年龄归零）：" + DebugCommandUtility.getPlayerNames(players)
                        + "§7（若玩家因寿终转为旁观模式，请用 /gamemode 手动恢复）"
        ), false);
        return players.size();
    }

    private static String describeStatus(LifespanSnapshot lifespan) {
        if (lifespan.immortal()) {
            return "§b长生（与天地齐寿）";
        }
        if (lifespan.expired()) {
            return "§c已至大限";
        }
        return "§a正常";
    }
}
