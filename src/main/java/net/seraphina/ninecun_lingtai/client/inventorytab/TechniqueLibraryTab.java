package net.seraphina.ninecun_lingtai.client.inventorytab;

import com.kqp.inventorytabs.tabs.tab.Tab;
import net.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import net.seraphina.ninecun_lingtai.client.screen.TechniqueLibraryScreen;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class TechniqueLibraryTab extends Tab {
    public TechniqueLibraryTab() {
        super(new ItemStack(ModItems.JADE_SLIP.get()));
    }

    @Override
    public void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        ClientTechniqueManager.beginOpenLibrary();
        minecraft.setScreen(new TechniqueLibraryScreen());
    }

    @Override
    public boolean shouldBeRemoved() {
        return Minecraft.getInstance().player == null;
    }

    @Override
    public Component getHoverText() {
        return Component.translatable("inventory_tab.ninecun_lingtai.techniques");
    }

    @Override
    public int getPriority() {
        return 89;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TechniqueLibraryTab;
    }

    @Override
    public int hashCode() {
        return TechniqueLibraryTab.class.hashCode();
    }
}
