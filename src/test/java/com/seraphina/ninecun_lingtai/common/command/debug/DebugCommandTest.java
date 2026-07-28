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
        assertNotNull(debugCommand.getChild("lifespan"));
        assertNotNull(debugCommand.getChild("creature"));
        assertNotNull(debugCommand.getChild("technique"));
        assertNotNull(debugCommand.getChild("notification"));
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
        assertNotNull(debugCommand.getChild("lifespan").getChild("set"));
        assertNotNull(debugCommand.getChild("lifespan").getChild("add"));
        assertNotNull(debugCommand.getChild("lifespan").getChild("expire"));
        assertNotNull(debugCommand.getChild("lifespan").getChild("reset"));
        assertNotNull(debugCommand.getChild("creature").getChild("get"));
        assertNotNull(debugCommand.getChild("creature").getChild("cultivation").getChild("set"));
        assertNotNull(debugCommand.getChild("creature").getChild("cultivation").getChild("add"));
        assertNotNull(debugCommand.getChild("creature").getChild("spiritual_root").getChild("set"));
        assertNotNull(debugCommand.getChild("creature").getChild("lifespan").getChild("expire"));
        assertNotNull(debugCommand.getChild("creature").getChild("defense").getChild("refill"));
        assertNotNull(debugCommand.getChild("technique").getChild("learn"));
        assertNotNull(debugCommand.getChild("technique").getChild("mastery"));
        assertNotNull(debugCommand.getChild("technique").getChild("proficiency"));
        assertNotNull(debugCommand.getChild("technique").getChild("forget"));
        assertNotNull(debugCommand.getChild("notification").getChild("targets"));
    }
}
