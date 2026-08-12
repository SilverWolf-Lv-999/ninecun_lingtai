package net.seraphina.ninecun_lingtai.client.render;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.block.entity.AlchemyFurnaceBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class AlchemyFurnaceBlockRenderer extends GeoBlockRenderer<AlchemyFurnaceBlockEntity> {
    public AlchemyFurnaceBlockRenderer() {
        super(new DefaultedBlockGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "alchemy_furnace")
        ));
    }
}
