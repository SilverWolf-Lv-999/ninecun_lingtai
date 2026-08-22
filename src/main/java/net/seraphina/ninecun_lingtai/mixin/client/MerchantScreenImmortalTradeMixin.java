package net.seraphina.ninecun_lingtai.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.seraphina.ninecun_lingtai.utility.ImmortalTradeUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MerchantScreen.class)
public abstract class MerchantScreenImmortalTradeMixin extends AbstractContainerScreen<MerchantMenu> {
    private static final Component LEVEL_SEPARATOR = Component.literal(" - ");
    private static final Component TRADES_LABEL = Component.translatable("merchant.trades");

    protected MerchantScreenImmortalTradeMixin(
            MerchantMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void ninecunLingtai$renderImmortalPathTitle(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            CallbackInfo callbackInfo
    ) {
        if (!ImmortalTradeUtility.hasImmortalTrade(this.menu.getOffers())) {
            return;
        }

        Component component = this.title.copy()
                .append(LEVEL_SEPARATOR)
                .append(Component.translatable("merchant.level.immortal"));
        int titleWidth = this.font.width(component);
        int titleX = 49 + this.imageWidth / 2 - titleWidth / 2;
        guiGraphics.drawString(this.font, component, titleX, 6, 4210752, false);
        guiGraphics.drawString(
                this.font,
                this.playerInventoryTitle,
                this.inventoryLabelX,
                this.inventoryLabelY,
                4210752,
                false
        );
        int tradesWidth = this.font.width(TRADES_LABEL);
        guiGraphics.drawString(this.font, TRADES_LABEL, 5 - tradesWidth / 2 + 48, 6, 4210752, false);
        callbackInfo.cancel();
    }
}
