package net.seraphina.ninecun_lingtai.mixin.client;

import net.seraphina.ninecun_lingtai.client.utility.ComplexNbtCreativeTabUtility;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(value = CreativeModeInventoryScreen.class, priority = 900)
public abstract class CreativeModeInventoryScreenFiltersMixin extends EffectRenderingInventoryScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    protected CreativeModeInventoryScreenFiltersMixin(
            CreativeModeInventoryScreen.ItemPickerMenu menu,
            Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
    }

    @Dynamic("Added by Filters API's CreativeModeInventoryScreenMixin.")
    @Inject(method = "filtersApi$updateItems", at = @At("TAIL"), remap = false, require = 0)
    private void ninecunLingtai$appendComplexNbtItemStacks(CallbackInfo callbackInfo) {
        if (ComplexNbtCreativeTabUtility.appendKnownComplexNbtStacks(selectedTab, this.menu.items)) {
            this.menu.scrollTo(this.scrollOffs);
        }
    }
}
