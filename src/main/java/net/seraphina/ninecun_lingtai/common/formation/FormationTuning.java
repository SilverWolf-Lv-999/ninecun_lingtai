package net.seraphina.ninecun_lingtai.common.formation;

/**
 * 阵法调参常量（M4 施工约束：不动 CommonConfig，数值先以类内常量落地）。
 * 每个常量注释里给出建议的 CommonConfig 键名（formation 组），
 * 主控接线时可整组迁移，迁移后本类只留转发。
 */
public final class FormationTuning {

    // ==================== 几何校验（施工图 6.4，任务口径否决 0.5 格亚方块档） ====================

    /** 建议键 formationDeviationGood：偏差 ≤ 该值（格）→ 效率 100%。 */
    public static final double DEVIATION_GOOD = 1.0D;
    /** 建议键 formationDeviationLimit：偏差 ≤ 该值（格）→ 效率 80%；再远即溃并反冲。 */
    public static final double DEVIATION_LIMIT = 2.0D;
    /** 建议键 formationDeviationGoodEfficiency。 */
    public static final double EFFICIENCY_GOOD = 1.0D;
    /** 建议键 formationDeviationCoarseEfficiency。 */
    public static final double EFFICIENCY_COARSE = 0.8D;
    /** 建议键 flagSequenceShengBonus：顺位相邻旗全部相生 → 效率 +20%。 */
    public static final double FLAG_SEQUENCE_SHENG_BONUS = 0.2D;
    /** 建议键 flagSequenceKePenalty：出现相邻相克 → 效率 −30%。 */
    public static final double FLAG_SEQUENCE_KE_PENALTY = 0.3D;
    /** 每个旗位以期望点为中心的立方搜索半径（格），覆盖 DEVIATION_LIMIT + 冗余。 */
    public static final int FLAG_SEARCH_RADIUS = 3;

    /** 建议键 formationBacklashEnergyPerGrade：布阵失败反冲抽取激活者灵力 = 该值 × 品阶。 */
    public static final double ACTIVATION_BACKLASH_ENERGY_PER_GRADE = 20.0D;
    /** 建议键 formationBacklashDamage：布阵失败反冲伤害（魔法伤害，半心 ×4）。 */
    public static final float ACTIVATION_BACKLASH_DAMAGE = 4.0F;

    // ==================== 阵盘储能（储能式） ====================

    /** 建议键 formationDiscEnergyCapacity：阵盘储能上限（灵力单位）。 */
    public static final double DISC_ENERGY_CAPACITY = 4000.0D;
    /** 建议键 formationSpiritStoneEnergyUnits：一枚灵石充入的储能。 */
    public static final double SPIRIT_STONE_ENERGY_UNITS = 400.0D;
    /** 建议键 formationUpkeepPerSecondPerGrade：运转维持耗能/秒 = 该值 × 品阶。 */
    public static final double UPKEEP_PER_SECOND_PER_GRADE = 0.2D;

    // ==================== 聚灵阵（位势模型，走灵气账本公开 API） ====================

    /** 建议键 formationGatherReservePerSecondPerGrade：每秒注入灵域存量 = 该值 × 品阶 × 效率 × gather_rate。 */
    public static final double GATHER_RESERVE_PER_SECOND_PER_GRADE = 1.0D;
    /** 建议键 formationGatherEnergyPerSecondPerGrade：聚灵注入的额外耗能/秒 = 该值 × 品阶。 */
    public static final double GATHER_ENERGY_PER_SECOND_PER_GRADE = 0.5D;
    /** 聚灵注入的结算间隔（tick）；20 = 每秒一账，与打坐扣账同频。 */
    public static final int GATHER_INTERVAL_TICKS = 20;

    // ==================== 护府阵（储能式半球护罩） ====================

    /** 建议键 shieldEnergyPerDamage：吸收 1 点伤害折算储能 = 该值 ÷ 效率 ÷ shield_efficiency。 */
    public static final double SHIELD_ENERGY_PER_DAMAGE = 3.0D;

    // ==================== 点睛 / 灵阵（地禁，锚位口径阵禁篇 3.1） ====================

    /** 建议键 formationEnlightenAnchors13：1-3 阶点睛占锚位。 */
    public static final int ENLIGHTEN_ANCHORS_GRADE_1_3 = 2;
    /** 建议键 formationEnlightenAnchors46：4-6 阶点睛占锚位。 */
    public static final int ENLIGHTEN_ANCHORS_GRADE_4_6 = 3;
    /** 建议键 formationEnlightenAnchors7Plus：7 阶以上点睛占锚位。 */
    public static final int ENLIGHTEN_ANCHORS_GRADE_7_PLUS = 4;
    /** 点睛确认窗口（tick）：首次潜行右键提示消耗，窗口内再次点击确认。 */
    public static final int ENLIGHTEN_CONFIRM_WINDOW_TICKS = 100;
    /** 警报扫描间隔（tick）。 */
    public static final int ALARM_SCAN_INTERVAL_TICKS = 20;

    // ==================== 生门（阵禁篇 7.2） ====================

    /** 建议键 formationLifeGateParalysisTicks：拔除生门旗全阵瘫痪时长（60 秒）。 */
    public static final int LIFE_GATE_PARALYSIS_TICKS = 1200;
    /** 建议键 formationWrongFlagBacklashDamage：拔错旗灵力反冲伤害。 */
    public static final float WRONG_FLAG_BACKLASH_DAMAGE = 4.0F;
    /** 建议键 formationWrongFlagKnockback：拔错旗击退强度。 */
    public static final double WRONG_FLAG_KNOCKBACK = 1.2D;
    /** 建议键 formationDisturbLockDays：惊扰满 3 次后灵力流隐藏（巧破关闭）的修真日数。 */
    public static final int DISTURB_HIDE_FLOW_DAYS = 1;
    /** 建议键 formationLifeGateMinStars：神识里辨认生门逆流所需的最低魂强星数。 */
    public static final int LIFE_GATE_MIN_SOUL_STARS = 3;

    // ==================== 驱阵（阵禁篇 6.3） ====================

    /** 建议键 formationSurgeEnergyCost：驱阵一次抽取阵主自身灵力。 */
    public static final double SURGE_PLAYER_ENERGY_COST = 60.0D;
    /** 建议键 formationSurgeCooldownTicks：驱阵冷却。 */
    public static final int SURGE_COOLDOWN_TICKS = 1200;
    /** 建议键 formationSurgeGatherReserveUnits：聚灵阵驱阵瞬时注入的灵域存量（× 效率）。 */
    public static final double SURGE_GATHER_RESERVE_UNITS = 300.0D;
    /** 建议键 formationSurgeShieldEnergyUnits：护府阵驱阵瞬时补充的阵盘储能（× 效率）。 */
    public static final double SURGE_SHIELD_ENERGY_UNITS = 200.0D;

    // ==================== 指引与灵力流下发 ====================

    /** 幻影指引下发间隔（tick）与判绿阈值（格）。 */
    public static final int GUIDE_SEND_INTERVAL_TICKS = 10;
    public static final double GUIDE_ALIGNED_DISTANCE = 1.0D;
    /** 指引生效距离：持阵盘潜行的玩家离阵盘多远内出图。 */
    public static final double GUIDE_MAX_DISTANCE = 32.0D;
    /** 灵力流下发间隔（tick）与观察距离补偿（radius + 该值内的开神识玩家收包）。 */
    public static final int FLOW_SEND_INTERVAL_TICKS = 20;
    public static final double FLOW_EXTRA_VIEW_DISTANCE = 16.0D;

    private FormationTuning() {
    }
}
