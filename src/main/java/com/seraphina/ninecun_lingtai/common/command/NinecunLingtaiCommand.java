package com.seraphina.ninecun_lingtai.common.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class NinecunLingtaiCommand {
    private static final int DEBUG_PERMISSION_LEVEL = 2;

    private NinecunLingtaiCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
                Commands.literal(ModMain.MOD_ID)
                        .then(Commands.literal("debug")
                                .requires(source -> source.hasPermission(DEBUG_PERMISSION_LEVEL))
                                .then(Commands.literal("aptitude")
                                        .executes(context -> reopenAptitudeScreen(context.getSource()))))
        );
    }

    private static int reopenAptitudeScreen(CommandSourceStack commandSource) throws CommandSyntaxException {
        ServerPlayer player = commandSource.getPlayerOrException();
        PlayerAptitudeData.clearAptitude(player);
        ModNetwork.sendAptitudeScreenRequest(player);
        commandSource.sendSuccess(
                () -> Component.literal("已清除当前资质并重新打开选择界面。"),
                false
        );
        return Command.SINGLE_SUCCESS;
    }
}
