package net.seraphina.ninecun_lingtai.common.myminecraft;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LogoRenderer;

public class MyLogoRenderer extends LogoRenderer {
    public MyLogoRenderer(boolean pKeepLogoThroughFade) {
        super(pKeepLogoThroughFade);
    }

    public void renderLogo(GuiGraphics pGuiGraphics, int pScreenWidth, float pTransparency, int pHeight) {
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, this.keepLogoThroughFade ? 1.0F : pTransparency);
        int $$4 = pScreenWidth / 2 - 128;
        pGuiGraphics.blit(EASTER_EGG_LOGO, $$4, pHeight, 0.0F, 0.0F, 256, 44, 256, 64);
        int $$5 = pScreenWidth / 2 - 64;
        int $$6 = pHeight + 44 - 7;
        pGuiGraphics.blit(MINECRAFT_EDITION, $$5, $$6, 0.0F, 0.0F, 128, 14, 128, 16);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
