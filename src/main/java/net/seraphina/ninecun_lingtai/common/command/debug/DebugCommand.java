package net.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DebugCommand {
    private static final int DEBUG_PERMISSION_LEVEL = 2;

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("debug")
                .requires(source -> source.hasPermission(DEBUG_PERMISSION_LEVEL))
                .executes(context -> showHelp(context.getSource()))
                .then(Commands.literal("help").executes(context -> showHelp(context.getSource())))
                .then(PlayerDebugCommand.create())
                .then(AptitudeDebugCommand.create())
                .then(CultivationDebugCommand.create())
                .then(SpiritualEnergyDebugCommand.create())
                .then(MeditationDebugCommand.create())
                .then(LifespanDebugCommand.create())
                .then(CreatureDebugCommand.create())
                .then(TechniqueDebugCommand.create())
                .then(NotificationDebugCommand.create());
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
                "§elifespan§r：查询、设置、增减年龄，或触发寿终、重置寿元"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§ecreature§r：查看并调整非玩家生物的境界、灵根、寿元与境界防御"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§etechnique§r：查询、列出、授予、遗忘功法，或调整理解度、境界、熟练度"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "§enotification§r：向指定玩家发送竹卷通知"
        ), false);
        commandSource.sendSuccess(() -> Component.literal(
                "玩家参数支持 @s、玩家名、@a；生物参数支持 @e 选择器；元素与境界支持英文 ID 或中文名。"
        ), false);
        return Command.SINGLE_SUCCESS;
    }
}
