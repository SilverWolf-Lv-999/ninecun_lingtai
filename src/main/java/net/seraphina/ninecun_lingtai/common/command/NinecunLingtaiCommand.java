package net.seraphina.ninecun_lingtai.common.command;

import com.mojang.brigadier.CommandDispatcher;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.command.debug.DebugCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class NinecunLingtaiCommand {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(
                Commands.literal(ModMain.MOD_ID)
                        .then(DebugCommand.create())
        );
    }
}
