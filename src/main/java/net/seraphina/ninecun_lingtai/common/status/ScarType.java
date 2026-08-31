package net.seraphina.ninecun_lingtai.common.status;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 伤痕（长期状态）类型及其元数据。
 *
 * <p>元数据只描述类型本身的规则（是否会到期、是否可被行为化解、显示名与化解提示的语言键、
 * 默认持续时长），不含任何玩法数值——数值影响（如"有瓶颈期则不可破境"）由各玩法系统
 * 通过 {@link ScarManager} 查询后自行决定。</p>
 *
 * <p>语言键约定（待集中接线补入 lang 文件）：显示名 {@code scar.ninecun_lingtai.<name>}，
 * 默认化解提示 {@code scar.ninecun_lingtai.<name>.resolve_hint}。</p>
 */
public enum ScarType {
    /** 道基受损：破境失败的长期后遗症。无期限，可靠丹药/机缘化解。 */
    DAO_BASE_INJURY("dao_base_injury", "道基受损", false, true, -1L),
    /** 魂伤：渡劫失败、身死等对神魂的长期创伤。无期限，可靠养魂类丹药化解。 */
    SOUL_INJURY("soul_injury", "魂伤", false, true, -1L),
    /** 心障：走火入魔等留下的长期心魔阴影。无期限，可靠心境行为化解。 */
    HEART_DEMON("heart_demon", "心障", false, true, -1L),
    /** 劫后虚弱：渡劫后的短期虚弱。有到期时间，只能自然恢复，不可行为化解。 */
    POST_TRIBULATION_WEAKNESS("post_tribulation_weakness", "劫后虚弱", true, false, 24000L),
    /** 走火入魔（发作期）：短期失控状态。有到期时间，也可被行为提前化解。 */
    QI_DEVIATION("qi_deviation", "走火入魔", true, true, 48000L),
    /** 瓶颈期：短期内不可再尝试破境。有到期时间，也可被顿悟/丹药提前化解。 */
    BOTTLENECK("bottleneck", "瓶颈期", true, true, 72000L);

    private static final Map<String, ScarType> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(ScarType::getSerializedName, Function.identity()));

    private final String serializedName;
    private final String chineseName;
    private final boolean expirable;
    private final boolean resolvableByAction;
    private final long defaultDurationTicks;
    private final String displayNameKey;
    private final String resolveHintKey;

    ScarType(
            String serializedName,
            String chineseName,
            boolean expirable,
            boolean resolvableByAction,
            long defaultDurationTicks
    ) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
        this.expirable = expirable;
        this.resolvableByAction = resolvableByAction;
        this.defaultDurationTicks = defaultDurationTicks;
        this.displayNameKey = "scar." + ModMain.MOD_ID + "." + serializedName;
        this.resolveHintKey = this.displayNameKey + ".resolve_hint";
    }

    /** NBT 序列化名。 */
    public String getSerializedName() {
        return this.serializedName;
    }

    /** 中文名，仅用于日志等内部输出；玩家可见文本一律走语言键。 */
    public String getChineseName() {
        return this.chineseName;
    }

    /** 是否有到期时间（false 表示默认无期限，只能被移除/化解）。 */
    public boolean isExpirable() {
        return this.expirable;
    }

    /** 是否可被玩家行为（丹药、闭关、顿悟等）化解。 */
    public boolean isResolvableByAction() {
        return this.resolvableByAction;
    }

    /** 默认持续时长（tick），-1 表示无期限。建议后续提为 CommonConfig 配置项。 */
    public long getDefaultDurationTicks() {
        return this.defaultDurationTicks;
    }

    /** 显示名语言键。 */
    public String getDisplayNameKey() {
        return this.displayNameKey;
    }

    /** 默认化解提示语言键。 */
    public String getResolveHintKey() {
        return this.resolveHintKey;
    }

    public Component getDisplayName() {
        return Component.translatable(this.displayNameKey);
    }

    public static Optional<ScarType> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
