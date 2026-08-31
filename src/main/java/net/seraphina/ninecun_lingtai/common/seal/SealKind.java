package net.seraphina.ninecun_lingtai.common.seal;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 器禁类型（阵禁篇 4.1）。M3 落地认主 / 传讯 / 自毁，
 * M4 补齐隐匿 / 反噬 / 遗念——六种全部可刻。
 */
public enum SealKind {
    OWNERSHIP("ownership", 1),
    MESSAGE("message", 1),
    DETONATE("detonate", 3),
    /** 对神识探测隐形（藏最值钱的那件）：物品展示快照抹平 + 满隐容器不被神识标记。 */
    CONCEAL("conceal", 2),
    /** 破解失败瞬间反击破解者魂伤 +15 × 层品阶（防盗刺）。 */
    BACKLASH("backlash", 3),
    /** 主人预录遗言，层破时以主人口吻发给破解者并即时落神念指纹（复仇线索 + 叙事）。 */
    LEGACY("legacy", 4);

    private static final Map<String, SealKind> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SealKind::getSerializedName, Function.identity()));

    private final String serializedName;
    private final int grade;

    SealKind(String serializedName, int grade) {
        this.serializedName = serializedName;
        this.grade = grade;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    /** 禁制品阶（决定刻印消耗与锁纹盘面规格）。 */
    public int getGrade() {
        return this.grade;
    }

    /** M4 起六种全实装。 */
    public boolean isImplemented() {
        return true;
    }

    public static Optional<SealKind> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
