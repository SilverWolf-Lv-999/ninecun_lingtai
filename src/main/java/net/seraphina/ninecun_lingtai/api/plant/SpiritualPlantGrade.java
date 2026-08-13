package net.seraphina.ninecun_lingtai.api.plant;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public record SpiritualPlantGrade(int tier, int rank) {
    public static final int MINIMUM_TIER = 1;
    public static final int MAXIMUM_TIER = 9;
    public static final int MINIMUM_RANK = 1;
    public static final int MAXIMUM_RANK = 9;
    public static final SpiritualPlantGrade LOWEST = new SpiritualPlantGrade(MINIMUM_TIER, MINIMUM_RANK);
    public static final SpiritualPlantGrade HIGHEST = new SpiritualPlantGrade(MAXIMUM_TIER, MAXIMUM_RANK);

    private static final String[] CHINESE_NUMERALS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
    };

    public SpiritualPlantGrade {
        if (tier < MINIMUM_TIER || tier > MAXIMUM_TIER) {
            throw new IllegalArgumentException("Spiritual plant tier must be between one and nine");
        }
        if (rank < MINIMUM_RANK || rank > MAXIMUM_RANK) {
            throw new IllegalArgumentException("Spiritual plant rank must be between one and nine");
        }
    }

    public int getQualityLevel() {
        return (this.tier - 1) * MAXIMUM_RANK + this.rank;
    }

    public String getChineseName() {
        return CHINESE_NUMERALS[this.tier] + "阶" + CHINESE_NUMERALS[this.rank] + "品";
    }

    public Component getDisplayName() {
        return Component.literal(getChineseName()).withStyle(getFormatting());
    }

    public SpiritualPlantGrade offset(int qualityOffset) {
        int qualityLevel = Math.max(
                LOWEST.getQualityLevel(),
                Math.min(HIGHEST.getQualityLevel(), getQualityLevel() + qualityOffset)
        );
        return fromQualityLevel(qualityLevel);
    }

    public static SpiritualPlantGrade fromQualityLevel(int qualityLevel) {
        if (qualityLevel < LOWEST.getQualityLevel() || qualityLevel > HIGHEST.getQualityLevel()) {
            throw new IllegalArgumentException("Spiritual plant quality level must be between one and eighty-one");
        }
        int tier = (qualityLevel - 1) / MAXIMUM_RANK + 1;
        int rank = (qualityLevel - 1) % MAXIMUM_RANK + 1;
        return new SpiritualPlantGrade(tier, rank);
    }

    private ChatFormatting getFormatting() {
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
            default -> throw new IllegalStateException("Unexpected spiritual plant tier: " + this.tier);
        };
    }
}
