package com.seraphina.ninecun_lingtai.api.alchemy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public record AlchemyGrade(int tier, int rank) {
    private static final String[] CHINESE_NUMERALS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
    };
    private static final List<AlchemyGrade> ALL_GRADES = createAllGrades();

    public static final AlchemyGrade LOWEST = new AlchemyGrade(1, 1);

    public AlchemyGrade {
        if (tier < 1 || tier > 9) {
            throw new IllegalArgumentException("Alchemy tier must be between one and nine");
        }
        if (rank < 1 || rank > 9) {
            throw new IllegalArgumentException("Alchemy rank must be between one and nine");
        }
    }

    public String getChineseName() {
        return CHINESE_NUMERALS[this.tier] + "阶" + CHINESE_NUMERALS[this.rank] + "品";
    }

    public Component getDisplayName() {
        return Component.literal(getChineseName()).withStyle(getFormatting());
    }

    public ChatFormatting getFormatting() {
        return switch (this.tier) {
            case 1 -> ChatFormatting.WHITE;
            case 2 -> ChatFormatting.GREEN;
            case 3 -> ChatFormatting.AQUA;
            case 4 -> ChatFormatting.BLUE;
            case 5 -> ChatFormatting.LIGHT_PURPLE;
            case 6 -> ChatFormatting.GOLD;
            case 7 -> ChatFormatting.RED;
            case 8 -> ChatFormatting.DARK_PURPLE;
            case 9 -> ChatFormatting.YELLOW;
            default -> throw new IllegalStateException("Unexpected alchemy tier: " + this.tier);
        };
    }

    public int getQualityLevel() {
        return (this.tier - 1) * 9 + this.rank;
    }

    public double getEffectMultiplier() {
        return 1.0D + (getQualityLevel() - 1) * 0.05D;
    }

    public static List<AlchemyGrade> getAllGrades() {
        return ALL_GRADES;
    }

    private static List<AlchemyGrade> createAllGrades() {
        List<AlchemyGrade> grades = new ArrayList<>(81);
        for (int tier = 1; tier <= 9; tier++) {
            for (int rank = 1; rank <= 9; rank++) {
                grades.add(new AlchemyGrade(tier, rank));
            }
        }
        return List.copyOf(grades);
    }
}
