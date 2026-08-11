package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;

public final class AlchemyPillUtility {
    private static final String TIER_KEY = ModMain.MOD_ID + ":alchemy_pill_tier";
    private static final String RANK_KEY = ModMain.MOD_ID + ":alchemy_pill_rank";

    public static ItemStack createStack(ItemLike itemLike, AlchemyGrade grade) {
        return createStack(itemLike, 1, grade);
    }

    public static ItemStack createStack(ItemLike itemLike, int count, AlchemyGrade grade) {
        ItemStack itemStack = new ItemStack(itemLike, count);
        setGrade(itemStack, grade);
        return itemStack;
    }

    public static List<ItemStack> getCreativeItemStacks(ItemLike itemLike) {
        return AlchemyGrade.getAllGrades().stream()
                .map(grade -> createStack(itemLike, grade))
                .toList();
    }

    public static AlchemyGrade getGrade(ItemStack itemStack) {
        if (!itemStack.hasTag()) {
            return AlchemyGrade.LOWEST;
        }
        CompoundTag itemData = itemStack.getTag();
        try {
            return new AlchemyGrade(
                    itemData.getInt(TIER_KEY),
                    itemData.getInt(RANK_KEY)
            );
        } catch (IllegalArgumentException exception) {
            return AlchemyGrade.LOWEST;
        }
    }

    public static void setGrade(ItemStack itemStack, AlchemyGrade grade) {
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putInt(TIER_KEY, grade.tier());
        itemData.putInt(RANK_KEY, grade.rank());
    }
}
