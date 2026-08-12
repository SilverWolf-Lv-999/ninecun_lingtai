package net.seraphina.ninecun_lingtai.api.item;

import net.minecraft.world.item.ItemStack;

/**
 * 可以有灵气的物品的接口
 * */
public interface ISpiritualItem {
    SpiritualItemGrade getGrade(ItemStack itemStack);
}
