package com.seraphina.ninecun_lingtai.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;

public abstract class BaseModel extends BlockEntityWithoutLevelRenderer {
    public BaseModel() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }
}
