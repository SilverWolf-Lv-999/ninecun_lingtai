package net.seraphina.ninecun_lingtai.common.death;

/**
 * 死亡路线（灵气生态与死亡复活日志 §6.2，M3 首期三条 + 夺舍占位）。
 */
public enum DeathRoute {
    /** 正常重生：练气掉 1 小层；筑基+掉大境界 30% 进度 + 魂伤。 */
    RESPAWN,
    /** 魂魄归体：魂形飘回尸身原地复活，限时 = 魂强 × 2 分钟，超时按重罚重生。 */
    SOUL_RETURN,
    /** 燃元婴：原地半血复活，跌至金丹 + 折寿 20%（元婴+）。 */
    BURN_NASCENT,
    /** 夺舍生物：M4 属性移植方案（灵气生态日志 §6.3），本期仅界面占位。 */
    POSSESS_MOB;

    public static DeathRoute fromOrdinal(int ordinal) {
        DeathRoute[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : RESPAWN;
    }
}
