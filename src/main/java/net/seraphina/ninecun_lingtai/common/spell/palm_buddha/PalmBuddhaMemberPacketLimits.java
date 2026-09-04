package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

public final class PalmBuddhaMemberPacketLimits {
    public static final int MAXIMUM_PAGE_SIZE = 12;
    public static final int MAXIMUM_PAGE_COUNT = 1024;
    public static final int MAXIMUM_TOTAL_MEMBER_COUNT = MAXIMUM_PAGE_SIZE * MAXIMUM_PAGE_COUNT;
    public static final int MAXIMUM_CANDIDATE_COUNT = 48;

    private PalmBuddhaMemberPacketLimits() {
    }
}
