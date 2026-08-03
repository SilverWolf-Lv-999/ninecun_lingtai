package com.seraphina.ninecun_lingtai.common.item;

import net.minecraft.world.item.Item;

public class ColorItem extends Item {
    private final int color;

    public ColorItem(Properties properties, int color) {
        super(properties);
        this.color = color & 0xFFFFFF;
    }

    public final int getColor() {
        return this.color;
    }
}
