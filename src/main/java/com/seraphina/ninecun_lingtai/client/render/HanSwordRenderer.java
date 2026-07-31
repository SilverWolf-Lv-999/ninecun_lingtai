package com.seraphina.ninecun_lingtai.client.render;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.item.HanSwordItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public final class HanSwordRenderer extends GeoItemRenderer<HanSwordItem> {
    public HanSwordRenderer() {
        super(new DefaultedItemGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "han_sword")
        ));
    }
}
