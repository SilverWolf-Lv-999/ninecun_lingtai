package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.item.SpiritualStoneGrade;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public final class SpiritualStoneUtility {
    private static final String GRADE_KEY = ModMain.MOD_ID + ":spiritual_stone_grade";
    private static final String ENERGY_KEY = ModMain.MOD_ID + ":spiritual_stone_energy";
    private static final double DEPLETED_EPSILON = 1.0E-9D;

    private SpiritualStoneUtility() {
    }

    public static ItemStack createStack(SpiritualStoneGrade grade) {
        ItemStack itemStack = new ItemStack(ModItems.SPIRITUAL_STONE.get());
        writeSpiritualStoneData(itemStack, grade, grade.getEnergyCapacity());
        return itemStack;
    }

    public static List<ItemStack> getCreativeItemStacks() {
        return Arrays.stream(SpiritualStoneGrade.values())
                .map(SpiritualStoneUtility::createStack)
                .toList();
    }

    public static boolean isSpiritualStone(ItemStack itemStack) {
        return itemStack.is(ModItems.SPIRITUAL_STONE.get());
    }

    public static boolean hasRemainingEnergy(ItemStack itemStack) {
        return isSpiritualStone(itemStack) && getRemainingEnergy(itemStack) > DEPLETED_EPSILON;
    }

    public static boolean isUsableForCultivation(ItemStack itemStack) {
        return itemStack.getCount() == 1 && hasRemainingEnergy(itemStack);
    }

    public static SpiritualStoneGrade getGrade(ItemStack itemStack) {
        if (!isSpiritualStone(itemStack) || !itemStack.hasTag()) {
            return SpiritualStoneGrade.HUMAN;
        }
        return SpiritualStoneGrade.fromSerializedName(itemStack.getTag().getString(GRADE_KEY))
                .orElse(SpiritualStoneGrade.HUMAN);
    }

    public static double getRemainingEnergy(ItemStack itemStack) {
        if (!isSpiritualStone(itemStack)) {
            return 0.0D;
        }

        SpiritualStoneGrade grade = getGrade(itemStack);
        if (!itemStack.hasTag() || !itemStack.getTag().contains(ENERGY_KEY, Tag.TAG_ANY_NUMERIC)) {
            return grade.getEnergyCapacity();
        }

        double remainingEnergy = itemStack.getTag().getDouble(ENERGY_KEY);
        if (!Double.isFinite(remainingEnergy) || remainingEnergy <= 0.0D) {
            return 0.0D;
        }
        return Math.min(remainingEnergy, grade.getEnergyCapacity());
    }

    public static double getAbsorptionMultiplier(ItemStack itemStack) {
        return isSpiritualStone(itemStack)
                ? getGrade(itemStack).getAbsorptionMultiplier()
                : 0.0D;
    }

    public static double drainEnergy(ItemStack itemStack, double amount) {
        if (!hasRemainingEnergy(itemStack) || !Double.isFinite(amount) || amount <= 0.0D) {
            return 0.0D;
        }

        SpiritualStoneGrade grade = getGrade(itemStack);
        double remainingEnergy = getRemainingEnergy(itemStack);
        double drainedEnergy = Math.min(remainingEnergy, amount);
        double updatedEnergy = remainingEnergy - drainedEnergy;
        if (updatedEnergy <= DEPLETED_EPSILON) {
            itemStack.shrink(1);
            if (!itemStack.isEmpty()) {
                writeSpiritualStoneData(itemStack, grade, grade.getEnergyCapacity());
            }
            return drainedEnergy;
        }

        writeSpiritualStoneData(itemStack, grade, updatedEnergy);
        return drainedEnergy;
    }

    private static void writeSpiritualStoneData(
            ItemStack itemStack,
            SpiritualStoneGrade grade,
            double remainingEnergy
    ) {
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putString(GRADE_KEY, grade.getSerializedName());
        itemData.putDouble(ENERGY_KEY, Math.max(0.0D, Math.min(remainingEnergy, grade.getEnergyCapacity())));
    }
}
