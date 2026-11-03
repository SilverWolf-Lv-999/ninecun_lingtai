package net.seraphina.ninecun_lingtai.client.imc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import org.jetbrains.annotations.Nullable;

public class IMinecraft extends Minecraft {
    public IMinecraft(GameConfig pGameConfig) {
        super(pGameConfig);
    }

    @Override
    public void setScreen(@Nullable Screen pGuiScreen) {
        if (pGuiScreen instanceof DeathScreen) {
            EntityUtility.protectEntity(player);
            return;
        }
        super.setScreen(pGuiScreen);
    }

    @Override
    public void tick() {
        super.tick();
        if (CultivationDefenseUtility.hasActiveDefense(player))
            return;
        EntityUtility.protectEntity(player);
    }
}
