package com.seraphina.ninecun_lingtai.api.alchemy;

public record AlchemyGrade(int tier, int rank) {
    private static final String[] CHINESE_NUMERALS = {
            "零", "一", "二", "三", "四", "五", "六", "七", "八", "九"
    };

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
}
