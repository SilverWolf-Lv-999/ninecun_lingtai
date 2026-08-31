package net.seraphina.ninecun_lingtai.common.inner_demon;

/**
 * 心魔反制招（心魔即己 §6.3）：从锁定账本推 2–3 个，REFLECT 阶段解锁。
 * 枚举名用于会话序列化，勿改名。参数为类内常量（配置化见接线清单）。
 */
public enum MindDemonCounterMove {
    /**
     * 破盾蓄力：前摇 {@value #SHIELD_BREAK_WINDUP_TICKS} tick 蓝光粒子聚集，
     * 命中清空护体点 30%；前摇内对心魔造成 ≥1 伤害或位移 8 格可取消。
     * 触发条件：防御法术使用率 > 40%。
     */
    SHIELD_BREAK("破妄一击"),
    /**
     * 封空圈：以玩家为心 r=10 圆环，8s 内禁用位移法术
     * （{@code CastSpellPacket} 校验 trial 状态拒绝）；生成前 1s 预警粒子。
     * 触发条件：位移法术使用率 > 30%。
     */
    AIR_LOCK("困灵封空"),
    /**
     * 药念幻象：玩家服丹时 30% 概率触发"虚假冷却"提示（仅 UI 干扰，不真扣）。
     * 触发条件：soulPill 服用频次高（> 5/近 7 修真日）。
     */
    PILL_HAZE("药念幻象"),
    /**
     * 反震甲：受近战/strike 类伤害反弹 20% 为魂伤计。
     * 触发条件：近战命中占比高（top spell 为 strike 类）。
     */
    THORN_MAIL("荆棘心甲"),
    /**
     * 锁灵猎傀：优先攻击 {@code DefenseSpellGuardianEntity}。
     * 触发条件：召唤物依赖（guardian 使用率高）。
     */
    FOCUS_SUMMON("噬傀锁灵");

    public static final int SHIELD_BREAK_WINDUP_TICKS = 24;
    public static final double SHIELD_BREAK_DEFENSE_DRAIN_RATIO = 0.30D;
    public static final double SHIELD_BREAK_CANCEL_DISTANCE = 8.0D;
    public static final int AIR_LOCK_DURATION_TICKS = 160;
    public static final int AIR_LOCK_WARNING_TICKS = 20;
    public static final double AIR_LOCK_RADIUS = 10.0D;
    public static final double PILL_HAZE_TRIGGER_CHANCE = 0.30D;
    public static final double THORN_MAIL_REFLECT_RATIO = 0.20D;

    private final String chineseName;

    MindDemonCounterMove(String chineseName) {
        this.chineseName = chineseName;
    }

    public String chineseName() {
        return this.chineseName;
    }

    public static MindDemonCounterMove byName(String name) {
        for (MindDemonCounterMove move : values()) {
            if (move.name().equals(name)) {
                return move;
            }
        }
        return null;
    }
}
