package net.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public final class SpiritualEnergyDebugCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("energy")
                .executes(context -> getAllEnergy(
                        context.getSource(),
                        context.getSource().getPlayerOrException()
                ))
                .then(createGetCommand())
                .then(createElementMutationCommand("set", EnergyOperation.SET, 0.0D))
                .then(createElementMutationCommand("add", EnergyOperation.ADD, Double.MIN_VALUE))
                .then(createElementMutationCommand("remove", EnergyOperation.REMOVE, Double.MIN_VALUE))
                .then(createClearCommand())
                .then(createAllElementsCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createGetCommand() {
        return Commands.literal("get")
                .executes(context -> getAllEnergy(
                        context.getSource(),
                        context.getSource().getPlayerOrException()
                ))
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> getAllEnergy(
                                context.getSource(),
                                EntityArgument.getPlayer(context, "target")
                        ))
                        .then(Commands.argument("element", StringArgumentType.string())
                                .suggests(DebugCommandUtility::suggestElements)
                                .executes(SpiritualEnergyDebugCommand::getElementEnergy)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createElementMutationCommand(
            String commandName,
            EnergyOperation operation,
            double minimumAmount
    ) {
        return Commands.literal(commandName)
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("element", StringArgumentType.string())
                                .suggests(DebugCommandUtility::suggestElements)
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(minimumAmount))
                                        .executes(context -> mutateElementEnergy(context, operation)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createClearCommand() {
        return Commands.literal("clear")
                .executes(context -> clearAllEnergy(
                        context.getSource(),
                        List.of(context.getSource().getPlayerOrException())
                ))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> clearAllEnergy(
                                context.getSource(),
                                EntityArgument.getPlayers(context, "targets")
                        ))
                        .then(Commands.argument("element", StringArgumentType.string())
                                .suggests(DebugCommandUtility::suggestElements)
                                .executes(SpiritualEnergyDebugCommand::clearElementEnergy)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createAllElementsCommand() {
        return Commands.literal("all")
                .then(createAllElementsMutationCommand("set", EnergyOperation.SET, 0.0D))
                .then(createAllElementsMutationCommand("add", EnergyOperation.ADD, Double.MIN_VALUE))
                .then(createAllElementsMutationCommand("remove", EnergyOperation.REMOVE, Double.MIN_VALUE))
                .then(Commands.literal("clear")
                        .executes(context -> clearAllEnergy(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clearAllEnergy(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createAllElementsMutationCommand(
            String commandName,
            EnergyOperation operation,
            double minimumAmount
    ) {
        return Commands.literal(commandName)
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(minimumAmount))
                                .executes(context -> mutateAllEnergy(context, operation))));
    }

    private static int getAllEnergy(CommandSourceStack commandSource, ServerPlayer player) {
        commandSource.sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " 的全部灵力："
                        + DebugCommandUtility.formatSpiritualEnergy(player)
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int getElementEnergy(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "target");
        ElementType element = DebugCommandUtility.getElement(context, "element");
        double energy = PlayerSpiritualEnergyData.getEnergy(player, element);
        context.getSource().sendSuccess(() -> Component.literal(
                player.getGameProfile().getName() + " 的"
                        + SpiritualRootUtility.getElementName(element) + "灵力（"
                        + element.getSerializedName() + "）：" + DebugCommandUtility.formatNumber(energy)
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int mutateElementEnergy(
            CommandContext<CommandSourceStack> context,
            EnergyOperation operation
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ElementType element = DebugCommandUtility.getElement(context, "element");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        players.forEach(player -> operation.apply(player, element, amount));
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已" + operation.chineseName + SpiritualRootUtility.getElementName(element)
                        + "灵力 " + DebugCommandUtility.formatNumber(amount)
                        + "：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int mutateAllEnergy(
            CommandContext<CommandSourceStack> context,
            EnergyOperation operation
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        double amount = DoubleArgumentType.getDouble(context, "amount");
        for (ServerPlayer player : players) {
            for (ElementType element : ElementType.values()) {
                operation.apply(player, element, amount);
            }
        }
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已为全部元素" + operation.chineseName + "灵力 "
                        + DebugCommandUtility.formatNumber(amount)
                        + "：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int clearElementEnergy(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ElementType element = DebugCommandUtility.getElement(context, "element");
        players.forEach(player -> PlayerSpiritualEnergyData.clearEnergy(player, element));
        DebugCommandUtility.syncCultivationInfo(players);
        context.getSource().sendSuccess(() -> Component.literal(
                "已清除" + SpiritualRootUtility.getElementName(element)
                        + "灵力：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int clearAllEnergy(
            CommandSourceStack commandSource,
            Collection<ServerPlayer> players
    ) {
        players.forEach(PlayerSpiritualEnergyData::clearAllEnergy);
        DebugCommandUtility.syncCultivationInfo(players);
        commandSource.sendSuccess(() -> Component.literal(
                "已清除全部灵力：" + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private enum EnergyOperation {
        SET("设置") {
            @Override
            void apply(ServerPlayer player, ElementType element, double amount) {
                PlayerSpiritualEnergyData.setEnergy(player, element, amount);
            }
        },
        ADD("增加") {
            @Override
            void apply(ServerPlayer player, ElementType element, double amount) {
                PlayerSpiritualEnergyData.addEnergy(player, element, amount);
            }
        },
        REMOVE("扣除") {
            @Override
            void apply(ServerPlayer player, ElementType element, double amount) {
                PlayerSpiritualEnergyData.removeEnergy(player, element, amount);
            }
        };

        private final String chineseName;

        EnergyOperation(String chineseName) {
            this.chineseName = chineseName;
        }

        abstract void apply(ServerPlayer player, ElementType element, double amount);
    }
}
