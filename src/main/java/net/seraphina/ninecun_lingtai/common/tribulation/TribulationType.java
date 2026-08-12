package net.seraphina.ninecun_lingtai.common.tribulation;

public enum TribulationType {
    THREE_NINE(
            "三九雷劫",
            27,
            1.0D,
            "三九之数，天道初试；雷分三转，劫炼九重，守心明性者方可筑金丹。"
    ),
    FOUR_NINE(
            "四九雷劫",
            36,
            1.32D,
            "四九天劫，四象同鸣；三十六雷落尽，方显金丹真火，逆天而行者当慎守本心。"
    );

    private final String chineseName;
    private final int strikeCount;
    private final double strengthMultiplier;
    private final String ancientDescription;

    TribulationType(
            String chineseName,
            int strikeCount,
            double strengthMultiplier,
            String ancientDescription
    ) {
        this.chineseName = chineseName;
        this.strikeCount = strikeCount;
        this.strengthMultiplier = strengthMultiplier;
        this.ancientDescription = ancientDescription;
    }

    public String chineseName() {
        return this.chineseName;
    }

    public int strikeCount() {
        return this.strikeCount;
    }

    public double strengthMultiplier() {
        return this.strengthMultiplier;
    }

    public String ancientDescription() {
        return this.ancientDescription;
    }
}
