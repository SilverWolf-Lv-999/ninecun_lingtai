package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.item.SpiritualItemGrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.Arrays;
import java.util.List;

public final class SpiritualItemUtility {
    private static final String GRADE_KEY = ModMain.MOD_ID + ":spiritual_item_grade";

    public static ItemStack createStack(ItemLike itemLike, SpiritualItemGrade grade) {
        ItemStack itemStack = new ItemStack(itemLike);
        setGrade(itemStack, grade);
        return itemStack;
    }

    public static List<ItemStack> getCreativeItemStacks(ItemLike itemLike) {
        return Arrays.stream(SpiritualItemGrade.values())
                .map(grade -> createStack(itemLike, grade))
                .toList();
    }

    public static SpiritualItemGrade getGrade(ItemStack itemStack) {
        if (!itemStack.hasTag()) {
            return SpiritualItemGrade.MORTAL;
        }

        return SpiritualItemGrade.fromSerializedName(itemStack.getTag().getString(GRADE_KEY))
                .orElse(SpiritualItemGrade.MORTAL);
    }

    public static void setGrade(ItemStack itemStack, SpiritualItemGrade grade) {
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putString(GRADE_KEY, grade.getSerializedName());
    }
}
