package net.seraphina.ninecun_lingtai.common.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;
import java.util.Optional;

/**
 * 一条具体的伤痕记录。
 *
 * @param type           伤痕类型
 * @param severity       严重度，取值 [{@value #MIN_SEVERITY}, {@value #MAX_SEVERITY}]，构造时自动收拢
 * @param expireGameTick 到期的游戏时刻（对比 {@code level.getGameTime()}），
 *                       {@value #NO_EXPIRY} 表示无期限
 * @param resolveHint    化解提示的语言键；传 null 或空串时回落为类型默认提示
 *                       {@link ScarType#getResolveHintKey()}
 */
public record Scar(ScarType type, int severity, long expireGameTick, String resolveHint) {
    public static final int MIN_SEVERITY = 1;
    public static final int MAX_SEVERITY = 5;
    public static final long NO_EXPIRY = -1L;

    private static final String TYPE_KEY = "Type";
    private static final String SEVERITY_KEY = "Severity";
    private static final String EXPIRE_GAME_TICK_KEY = "ExpireGameTick";
    private static final String RESOLVE_HINT_KEY = "ResolveHint";

    public Scar {
        Objects.requireNonNull(type, "type");
        severity = Math.max(MIN_SEVERITY, Math.min(MAX_SEVERITY, severity));
        if (expireGameTick < 0L) {
            expireGameTick = NO_EXPIRY;
        }
        if (resolveHint == null || resolveHint.isBlank()) {
            resolveHint = type.getResolveHintKey();
        }
    }

    /** 使用类型默认化解提示的便捷构造。 */
    public Scar(ScarType type, int severity, long expireGameTick) {
        this(type, severity, expireGameTick, null);
    }

    public boolean isPermanent() {
        return this.expireGameTick == NO_EXPIRY;
    }

    public boolean isExpired(long gameTime) {
        return !isPermanent() && this.expireGameTick <= gameTime;
    }

    public Scar withSeverity(int newSeverity) {
        return new Scar(this.type, newSeverity, this.expireGameTick, this.resolveHint);
    }

    public Scar withExpireGameTick(long newExpireGameTick) {
        return new Scar(this.type, this.severity, newExpireGameTick, this.resolveHint);
    }

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString(TYPE_KEY, this.type.getSerializedName());
        tag.putInt(SEVERITY_KEY, this.severity);
        tag.putLong(EXPIRE_GAME_TICK_KEY, this.expireGameTick);
        tag.putString(RESOLVE_HINT_KEY, this.resolveHint);
        return tag;
    }

    /** 解析失败（类型未知、tag 结构损坏）时返回 empty，调用方直接跳过该条目。 */
    public static Optional<Scar> fromNBT(CompoundTag tag) {
        if (tag == null || !tag.contains(TYPE_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        Optional<ScarType> type = ScarType.fromSerializedName(tag.getString(TYPE_KEY));
        if (type.isEmpty()) {
            return Optional.empty();
        }
        int severity = tag.contains(SEVERITY_KEY, Tag.TAG_ANY_NUMERIC)
                ? tag.getInt(SEVERITY_KEY)
                : MIN_SEVERITY;
        long expireGameTick = tag.contains(EXPIRE_GAME_TICK_KEY, Tag.TAG_ANY_NUMERIC)
                ? tag.getLong(EXPIRE_GAME_TICK_KEY)
                : NO_EXPIRY;
        String resolveHint = tag.contains(RESOLVE_HINT_KEY, Tag.TAG_STRING)
                ? tag.getString(RESOLVE_HINT_KEY)
                : null;
        return Optional.of(new Scar(type.get(), severity, expireGameTick, resolveHint));
    }
}
