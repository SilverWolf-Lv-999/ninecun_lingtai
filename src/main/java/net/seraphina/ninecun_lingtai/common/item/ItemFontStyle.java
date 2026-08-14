package net.seraphina.ninecun_lingtai.common.item;

public enum ItemFontStyle {
    JADE_SLIP(0x48C7A2, 0xC2FFE4, 2_900L, 0.08F, 0.68F),
    ALCHEMY_RECIPE(0xF3B13E, 0xFF6A21, 2_350L, 0.11F, 0.78F),
    SPIRITUAL_STONE(0x4EDEFF, 0xACF4FF, 2_650L, 0.09F, 0.73F),
    DAO_SCROLL(0xE6C477, 0x78D3CD, 3_100L, 0.06F, 0.45F),
    HAN_SWORD(0xD5E8F0, 0xFD5252, 1_950L, 0.13F, 0.63F),
    STORAGE_RING(0xB883FF, 0x5BF4FF, 2_150L, 0.12F, 0.75F),
    GRASS_RESTORATION_PILL(0x7FD45B, 0xD1FF9C, 2_900L, 0.07F, 0.48F),
    QI_GATHERING_POWDER(0xA5D86B, 0xF4FFD6, 3_200L, 0.05F, 0.34F),
    BREATH_CONDENSING_PILL(0xC8DD58, 0xFFFFB5, 2_450L, 0.08F, 0.43F),
    CLEANSING_DUST_PILL(0xFBFFF9, 0xA1D3DA, 3_600L, 0.04F, 0.31F),
    GRAIN_AVOIDANCE_PILL(0xE5C45F, 0xFFEBC1, 3_050L, 0.05F, 0.39F),
    JADE_MARROW_PILL(0xA6FFD3, 0xD2A9FF, 2_750L, 0.10F, 0.58F),
    FOUNDATION_PILL(0xAAB9C5, 0xC9E3F5, 3_400L, 0.04F, 0.29F),
    SPIRIT_DEW_PILL(0x5DB6FF, 0xB8F5FF, 2_500L, 0.09F, 0.52F),
    CALM_MIND_PILL(0xF3A561, 0xFFE59B, 3_750L, 0.03F, 0.26F),
    VEIN_EXTENSION_PILL(0xFFE454, 0xFFF6A0, 1_700L, 0.16F, 0.67F),
    GOLDEN_WOUND_PILL(0xFFCF4A, 0xFFFFC5, 2_200L, 0.10F, 0.57F),
    CORE_FORMATION_PILL(0x68F4E1, 0xFFD95A, 1_850L, 0.14F, 0.72F),
    ORIGIN_RETURN_PILL(0xF07147, 0xFFA36C, 2_100L, 0.11F, 0.61F),
    BODY_TEMPERING_PILL(0x499F93, 0x9FCF7C, 2_800L, 0.07F, 0.42F),
    SPIRIT_CONDENSING_PILL(0x3A7ABB, 0x9CCBFF, 2_350L, 0.10F, 0.49F),
    DREGS_PILL(0x7B6A5A, 0x4A433D, 4_600L, 0.02F, 0.08F),
    SPIRITUAL_STONE_ORE(0x4BCBFF, 0x91F6FF, 2_400L, 0.08F, 0.64F),
    DEEPSLATE_SPIRITUAL_STONE_ORE(0x42699B, 0x7FA6D9, 3_100L, 0.06F, 0.37F),
    ALCHEMY_FURNACE(0xFF9D34, 0xFFE271, 1_800L, 0.15F, 0.75F),
    FIRE_LINGZHI(0xFB5030, 0xFFA14C, 1_650L, 0.17F, 0.71F);

    private final int primaryColor;
    private final int secondaryColor;
    private final long cycleDuration;
    private final float characterPhase;
    private final float glowStrength;

    ItemFontStyle(
            int primaryColor,
            int secondaryColor,
            long cycleDuration,
            float characterPhase,
            float glowStrength
    ) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.cycleDuration = cycleDuration;
        this.characterPhase = characterPhase;
        this.glowStrength = glowStrength;
    }

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getSecondaryColor() {
        return secondaryColor;
    }

    public long getCycleDuration() {
        return cycleDuration;
    }

    public float getCharacterPhase() {
        return characterPhase;
    }

    public float getGlowStrength() {
        return glowStrength;
    }
}
