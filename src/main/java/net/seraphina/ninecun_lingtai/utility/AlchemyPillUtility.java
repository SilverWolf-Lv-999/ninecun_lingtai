package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Optional;

public final class AlchemyPillUtility {
    private static final String TIER_KEY = ModMain.MOD_ID + ":alchemy_pill_tier";
    private static final String RANK_KEY = ModMain.MOD_ID + ":alchemy_pill_rank";
    private static final String PRIMARY_ELEMENT_KEY = ModMain.MOD_ID + ":alchemy_pill_primary_element";

    public static ItemStack createStack(ItemLike itemLike, AlchemyGrade grade) {
        return createStack(itemLike, 1, grade);
    }

    public static ItemStack createStack(ItemLike itemLike, int count, AlchemyGrade grade) {
        return createStack(itemLike, count, grade, null);
    }

    public static ItemStack createStack(
            ItemLike itemLike,
            int count,
            AlchemyGrade grade,
            ElementType primaryElement
    ) {
        ItemStack itemStack = new ItemStack(itemLike, count);
        setGrade(itemStack, grade);
        setPrimaryElement(itemStack, primaryElement);
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

    public static Optional<ElementType> getPrimaryElement(ItemStack itemStack) {
        if (!itemStack.hasTag()) {
            return Optional.empty();
        }
        return ElementType.fromSerializedName(itemStack.getTag().getString(PRIMARY_ELEMENT_KEY));
    }

    public static void setPrimaryElement(ItemStack itemStack, ElementType primaryElement) {
        itemStack.getOrCreateTag().putString(
                PRIMARY_ELEMENT_KEY,
                primaryElement == null ? "" : primaryElement.getSerializedName()
        );
    }
}
