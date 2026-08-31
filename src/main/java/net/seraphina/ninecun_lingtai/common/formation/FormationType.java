package net.seraphina.ninecun_lingtai.common.formation;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 阵法类别（阵禁篇 6.4 首期七阵，本期只实装前两种；
 * 其余五种仅占位序列化名，加载到未实装类型时拒绝激活）。
 */
public enum FormationType {
    SPIRIT_GATHERING("spirit_gathering"),
    SHIELD("shield"),
    /** M5：困灵阵。未实装。 */
    TRAP("trap"),
    /** M5：杀伐阵。未实装。 */
    KILLING("killing"),
    /** M5：幻阵。未实装。 */
    ILLUSION("illusion"),
    /** M5：遮蔽阵。未实装。 */
    VEIL("veil"),
    /** M5：引灵阵。未实装。 */
    CHANNELING("channeling");

    private static final Map<String, FormationType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(FormationType::getSerializedName, Function.identity()));

    private final String serializedName;

    FormationType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    /** 本期可激活的两种。 */
    public boolean isImplemented() {
        return this == SPIRIT_GATHERING || this == SHIELD;
    }

    public static Optional<FormationType> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
