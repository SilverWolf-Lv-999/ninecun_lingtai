package net.seraphina.ninecun_lingtai.common.item;

public enum ItemFontStyle {
    JADE_SLIP(0x48C7A2, 0xC2FFE4, 2_900L, 0.08F, 0.68F),
    ALCHEMY_RECIPE(0xF3B13E, 0xFF6A21, 2_350L, 0.11F, 0.78F),
    DAO_SCROLL(0xE6C477, 0x78D3CD, 3_100L, 0.06F, 0.45F),
    BOOK(0xB66A32, 0xF2D69B, 3_250L, 0.05F, 0.43F),
    STORAGE_RING(0xB883FF, 0x5BF4FF, 2_150L, 0.12F, 0.75F),
    METAL(0xC7D5E5, 0xFFF0A3, 2_500L, 0.08F, 0.61F),
    WOOD(0x43BE68, 0xC6F58A, 2_750L, 0.07F, 0.55F),
    WATER(0x258BFF, 0x9BEFFF, 2_250L, 0.10F, 0.64F),
    FIRE(0xF34B32, 0xFFC05A, 1_900L, 0.12F, 0.72F),
    EARTH(0xA96D37, 0xF1C46A, 2_850L, 0.06F, 0.49F),
    YIN_YANG(0x6F6A9B, 0xF8F6FF, 3_000L, 0.05F, 0.58F),
    THUNDER(0x9A56F4, 0xFFF06B, 1_750L, 0.15F, 0.76F),
    CHAOS(0xFA318C, 0x37E8F3, 1_500L, 0.18F, 0.82F),
    FIVE_ELEMENTS(0x54C86D, 0xFF9254, 1_850L, 0.17F, 0.78F),
    MULTI_ELEMENT(0x9967EE, 0x55D9E8, 2_000L, 0.14F, 0.71F),
    ALCHEMY_TIER_ONE(0xB9C8DC, 0xFCFCFF, 3_100L, 0.04F, 0.31F),
    ALCHEMY_TIER_TWO(0x35B9FB, 0xB2F5FF, 2_650L, 0.07F, 0.42F),
    ALCHEMY_TIER_THREE(0x8371F3, 0xD4C6FF, 2_450L, 0.08F, 0.48F),
    ALCHEMY_TIER_FOUR(0xDC5EEA, 0xFFC0F4, 2_250L, 0.10F, 0.55F),
    ALCHEMY_TIER_FIVE(0xFF8144, 0xFFD48A, 2_050L, 0.11F, 0.61F),
    ALCHEMY_TIER_SIX(0xF0B731, 0xFFF0A0, 1_900L, 0.12F, 0.68F),
    ALCHEMY_TIER_SEVEN(0xF04854, 0xFFB0A3, 1_750L, 0.13F, 0.72F),
    ALCHEMY_TIER_EIGHT(0x7F35D9, 0xDB94FF, 1_650L, 0.15F, 0.77F),
    ALCHEMY_TIER_NINE(0xF8D849, 0xFFFFD4, 1_450L, 0.18F, 0.86F),
    HUMAN_SPIRITUAL_STONE(0x728B99, 0xABC1CB, 3_400L, 0.04F, 0.26F),
    EARTH_SPIRITUAL_STONE(0x36BD91, 0x70E8FF, 2_300L, 0.11F, 0.63F),
    HEAVEN_SPIRITUAL_STONE(0xFFD269, 0xFFF8D0, 1_500L, 0.17F, 0.85F),
    MORTAL_HAN_SWORD(0x8997A3, 0xD5DEE4, 3_100L, 0.05F, 0.28F),
    SPIRITUAL_HAN_SWORD(0x34C6F2, 0xB7F6FF, 2_050L, 0.12F, 0.69F),
    IMMORTAL_HAN_SWORD(0xF5C74E, 0xFFF2AE, 1_400L, 0.18F, 0.88F),
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

    public static ItemFontStyle getAlchemyTierStyle(int tier) {
        return switch (tier) {
            case 1 -> ALCHEMY_TIER_ONE;
            case 2 -> ALCHEMY_TIER_TWO;
            case 3 -> ALCHEMY_TIER_THREE;
            case 4 -> ALCHEMY_TIER_FOUR;
            case 5 -> ALCHEMY_TIER_FIVE;
            case 6 -> ALCHEMY_TIER_SIX;
            case 7 -> ALCHEMY_TIER_SEVEN;
            case 8 -> ALCHEMY_TIER_EIGHT;
            case 9 -> ALCHEMY_TIER_NINE;
            default -> ALCHEMY_TIER_ONE;
        };
    }
}
