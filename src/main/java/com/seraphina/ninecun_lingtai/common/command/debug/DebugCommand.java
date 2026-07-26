package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DebugCommand {
    private static final int DEBUG_PERMISSION_LEVEL = 2;

    private DebugCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("debug")
                .requires(source -> source.hasPermission(DEBUG_PERMISSION_LEVEL))
                .executes(context -> showHelp(context.getSource()))
                .then(Commands.literal("help")
                        .executes(context -> showHelp(context.getSource())))
                .then(PlayerDebugCommand.create())
                .then(AptitudeDebugCommand.create())
                .then(CultivationDebugCommand.create())
                .then(SpiritualEnergyDebugCommand.create())
                .then(MeditationDebugCommand.create());
    }

    private static int showHelp(CommandSourceStack commandSource) {
        commandSource.sendSuccess(() -> Component.literal("§6九寸灵台玩家调试指令"), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§eplayer§r：查看、重置或同步玩家的全部修炼数据"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§eaptitude§r：调整灵根元素/寸数、魂魄星数，或重开资质界面"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§ecultivation§r：查询、设置、增减或清除境界与层数"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§eenergy§r：查询、设置、增加、扣除或清除各元素灵力"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§emeditation§r：查询、开始、停止或切换打坐状态"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "参数支持 @s、玩家名、@a；元素与境界支持英文 ID 或中文名（中文候选会自动加引号）。"
        ), false);
        return Command.SINGLE_SUCCESS;
    }
}
