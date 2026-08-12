package net.seraphina.ninecun_lingtai.api.spell;

import java.util.Optional;

public enum SpellGrade {
    FIRST(1, "一品"),
    SECOND(2, "二品"),
    THIRD(3, "三品"),
    FOURTH(4, "四品"),
    FIFTH(5, "五品"),
    SIXTH(6, "六品"),
    SEVENTH(7, "七品"),
    EIGHTH(8, "八品"),
    NINTH(9, "九品");

    private final int rank;
    private final String chineseName;

    SpellGrade(int rank, String chineseName) {
        this.rank = rank;
        this.chineseName = chineseName;
    }

    public int getRank() {
        return this.rank;
    }

    public double getCostMultiplier() {
        return this.rank;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    public static Optional<SpellGrade> fromRank(int rank) {
        return rank >= 1 && rank <= values().length
                ? Optional.of(values()[rank - 1])
                : Optional.empty();
    }
}
