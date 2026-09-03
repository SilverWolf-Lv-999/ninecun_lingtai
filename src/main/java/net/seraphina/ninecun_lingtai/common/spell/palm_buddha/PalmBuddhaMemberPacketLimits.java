package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

/** 掌中佛国成员分页的协议硬上限，防止客户端请求或服务端响应无界增长。 */
public final class PalmBuddhaMemberPacketLimits {
    public static final int MAXIMUM_PAGE_SIZE = 12;
    public static final int MAXIMUM_PAGE_COUNT = 1024;
    public static final int MAXIMUM_TOTAL_MEMBER_COUNT = MAXIMUM_PAGE_SIZE * MAXIMUM_PAGE_COUNT;

    private PalmBuddhaMemberPacketLimits() {
    }
}
