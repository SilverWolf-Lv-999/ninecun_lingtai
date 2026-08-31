package net.seraphina.ninecun_lingtai.common.aura;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.seraphina.ninecun_lingtai.common.time.MonotonicCalendarUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 区域灵气调试命令（M2 切片的 debug 可查出口，独立根避免动现有命令树）：
 * <pre>
 * /lingtai_aura                       查看脚下灵域（质量档/存量比例/惩罚/记录数）
 * /lingtai_aura drain &lt;units&gt;    扣减脚下灵域存量
 * /lingtai_aura fill                  回满脚下灵域（记录随之删除，验证稀疏存储）
 * /lingtai_aura penalty &lt;tiers&gt; &lt;days&gt;  施加质量降档惩罚
 * </pre>
 */
public final class AuraDebugCommand {
    private static final int PERMISSION_LEVEL = 2;

    private AuraDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lingtai_aura")
                .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                .executes(context -> showCell(context.getSource()))
                .then(Commands.literal("drain")
                        .then(Commands.argument("units", DoubleArgumentType.doubleArg(0.0D))
                                .executes(context -> drainCell(
                                        context.getSource(),
                                        DoubleArgumentType.getDouble(context, "units")
                                ))))
                .then(Commands.literal("fill")
                        .executes(context -> fillCell(context.getSource())))
                .then(Commands.literal("penalty")
                        .then(Commands.argument("tiers", IntegerArgumentType.integer(0, 3))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                        .executes(context -> applyPenalty(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "tiers"),
                                                IntegerArgumentType.getInteger(context, "days")
                                        ))))));
    }

    private static int showCell(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        BlockPos position = player.blockPosition();
        AuraQuality quality = SpiritualAuraManager.getQuality(level, position);
        double reserveRatio = SpiritualAuraManager.getReserveRatio(level, position);
        long cellKey = SpiritualAuraManager.getCellKey(position);
        SpiritualAuraSavedData.AuraCell cell = SpiritualAuraSavedData.get(level).peekCell(cellKey);
        String cellDetail = cell == null
                ? "无记录（视为满额）"
                : String.format(
                        "存量 %.1f 单位，惩罚 %d 档，当日采收 %d 次",
                        cell.getReserveUnits(),
                        cell.getPenaltyTiers(),
                        cell.getHarvestCount()
                );
        source.sendSuccess(() -> Component.literal(String.format(
                "§6灵域 %d：§r质量 %s（×%.2f），存量比例 %.1f%%；%s；本维度记录 %d 条",
                cellKey,
                quality.getChineseName(),
                quality.getBreakthroughCoefficient(),
                reserveRatio * 100.0D,
                cellDetail,
                SpiritualAuraManager.getTrackedCellCount(level)
        )), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int drainCell(CommandSourceStack source, double units) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        long currentDay = MonotonicCalendarUtility.getCalendarDay(level.getServer());
        SpiritualAuraSavedData savedData = SpiritualAuraSavedData.get(level);
        SpiritualAuraSavedData.AuraCell cell = savedData.getOrCreateCell(
                SpiritualAuraManager.getCellKey(player.blockPosition()),
                currentDay
        );
        cell.setReserveUnits(Math.max(0.0D, cell.getReserveUnits() - units));
        savedData.setDirty();
        source.sendSuccess(() -> Component.literal(String.format(
                "已扣减 %.1f 单位，剩余 %.1f 单位",
                units,
                cell.getReserveUnits()
        )), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int fillCell(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        SpiritualAuraManager.addReserve(
                level,
                player.blockPosition(),
                SpiritualAuraManager.getReserveCapUnits()
        );
        source.sendSuccess(() -> Component.literal(
                "已回满脚下灵域（若无惩罚则记录已删除）"
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int applyPenalty(
            CommandSourceStack source,
            int tiers,
            int days
    ) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerLevel level = player.serverLevel();
        long currentDay = MonotonicCalendarUtility.getCalendarDay(level.getServer());
        SpiritualAuraSavedData savedData = SpiritualAuraSavedData.get(level);
        SpiritualAuraSavedData.AuraCell cell = savedData.getOrCreateCell(
                SpiritualAuraManager.getCellKey(player.blockPosition()),
                currentDay
        );
        cell.setPenalty(tiers, currentDay + days);
        savedData.setDirty();
        source.sendSuccess(() -> Component.literal(String.format(
                "已施加质量降档：%d 档，持续 %d 修真日",
                tiers,
                days
        )), false);
        return Command.SINGLE_SUCCESS;
    }
}
