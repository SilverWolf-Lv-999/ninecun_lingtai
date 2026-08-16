package net.seraphina.ninecun_lingtai.client.utility;

import net.seraphina.ninecun_lingtai.api.plant.ISpiritualPlant;
import net.seraphina.ninecun_lingtai.common.item.LuminousItem;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

public final class LuminousItemRenderUtility {

    public static int resolvePackedLight(ItemStack itemStack, int packedLight) {
        return isLuminous(itemStack) ? LightTexture.FULL_BRIGHT : packedLight;
    }

    private static boolean isLuminous(ItemStack itemStack) {
        if (itemStack.getItem() instanceof LuminousItem) {
            return true;
        }
        return itemStack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ISpiritualPlant;
    }
}
