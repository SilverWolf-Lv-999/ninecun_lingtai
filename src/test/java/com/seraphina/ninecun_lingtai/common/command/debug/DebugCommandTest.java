package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class DebugCommandTest {
    @Test
    void registersAllPlayerDebugCategories() {
        CommandNode<CommandSourceStack> debugCommand = DebugCommand.create().build();

        assertNotNull(debugCommand.getChild("help"));
        assertNotNull(debugCommand.getChild("player"));
        assertNotNull(debugCommand.getChild("aptitude"));
        assertNotNull(debugCommand.getChild("cultivation"));
        assertNotNull(debugCommand.getChild("energy"));
        assertNotNull(debugCommand.getChild("meditation"));
    }

    @Test
    void registersDetailedMutationCommands() {
        CommandNode<CommandSourceStack> debugCommand = DebugCommand.create().build();

        assertNotNull(debugCommand.getChild("player").getChild("reset"));
        assertNotNull(debugCommand.getChild("aptitude").getChild("spiritual_root"));
        assertNotNull(debugCommand.getChild("aptitude").getChild("soul_strength"));
        assertNotNull(debugCommand.getChild("cultivation").getChild("add"));
        assertNotNull(debugCommand.getChild("cultivation").getChild("max"));
        assertNotNull(debugCommand.getChild("energy").getChild("all"));
        assertNotNull(debugCommand.getChild("energy").getChild("remove"));
        assertNotNull(debugCommand.getChild("meditation").getChild("toggle"));
    }
}
