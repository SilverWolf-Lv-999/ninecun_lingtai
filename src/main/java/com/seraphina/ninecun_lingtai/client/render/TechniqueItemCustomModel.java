package com.seraphina.ninecun_lingtai.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;

public class TechniqueItemCustomModel extends BlockEntityWithoutLevelRenderer {
    public static final TechniqueItemCustomModel INSTANCE = new TechniqueItemCustomModel();

    public TechniqueItemCustomModel() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }
}
