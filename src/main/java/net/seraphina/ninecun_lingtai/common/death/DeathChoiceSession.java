package net.seraphina.ninecun_lingtai.common.death;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 一名玩家的死亡路线会话（SavedData 持久化——选择界面掉线不能丢状态）。
 * 原始伤害源不落盘：重启/重连后按"天数已尽"结算。
 */
public final class DeathChoiceSession {
    private static final String PLAYER_TAG = "Player";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String DEATH_X_TAG = "DeathX";
    private static final String DEATH_Y_TAG = "DeathY";
    private static final String DEATH_Z_TAG = "DeathZ";
    private static final String AVAILABLE_MASK_TAG = "AvailableMask";
    private static final String NONCE_TAG = "Nonce";
    private static final String STATE_TAG = "State";
    private static final String CHOOSE_DEADLINE_TAG = "ChooseDeadline";
    private static final String RETURN_DEADLINE_TAG = "ReturnDeadline";

    public enum State {
        CHOOSING,
        SOUL_RETURNING
    }

    private final UUID playerUuid;
    private final String dimension;
    private final BlockPos deathPosition;
    private final int availableMask;
    private final long nonce;
    private State state = State.CHOOSING;
    /** 修真历刻口径（重启不倒退）。 */
    private long chooseDeadlineCalendarTick;
    private long returnDeadlineCalendarTick;

    public DeathChoiceSession(
            UUID playerUuid,
            String dimension,
            BlockPos deathPosition,
            int availableMask,
            long nonce,
            long chooseDeadlineCalendarTick
    ) {
        this.playerUuid = playerUuid;
        this.dimension = dimension;
        this.deathPosition = deathPosition;
        this.availableMask = availableMask;
        this.nonce = nonce;
        this.chooseDeadlineCalendarTick = chooseDeadlineCalendarTick;
    }

    public UUID playerUuid() {
        return this.playerUuid;
    }

    public String dimension() {
        return this.dimension;
    }

    public BlockPos deathPosition() {
        return this.deathPosition;
    }

    public boolean isRouteAvailable(DeathRoute route) {
        return (this.availableMask & (1 << route.ordinal())) != 0;
    }

    public int availableMask() {
        return this.availableMask;
    }

    public long nonce() {
        return this.nonce;
    }

    public State state() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public long chooseDeadlineCalendarTick() {
        return this.chooseDeadlineCalendarTick;
    }

    public long returnDeadlineCalendarTick() {
        return this.returnDeadlineCalendarTick;
    }

    public void setReturnDeadlineCalendarTick(long calendarTick) {
        this.returnDeadlineCalendarTick = calendarTick;
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(PLAYER_TAG, this.playerUuid);
        tag.putString(DIMENSION_TAG, this.dimension);
        tag.putInt(DEATH_X_TAG, this.deathPosition.getX());
        tag.putInt(DEATH_Y_TAG, this.deathPosition.getY());
        tag.putInt(DEATH_Z_TAG, this.deathPosition.getZ());
        tag.putInt(AVAILABLE_MASK_TAG, this.availableMask);
        tag.putLong(NONCE_TAG, this.nonce);
        tag.putString(STATE_TAG, this.state.name());
        tag.putLong(CHOOSE_DEADLINE_TAG, this.chooseDeadlineCalendarTick);
        tag.putLong(RETURN_DEADLINE_TAG, this.returnDeadlineCalendarTick);
        return tag;
    }

    @Nullable
    public static DeathChoiceSession fromNbt(CompoundTag tag) {
        if (!tag.hasUUID(PLAYER_TAG)) {
            return null;
        }
        DeathChoiceSession session = new DeathChoiceSession(
                tag.getUUID(PLAYER_TAG),
                tag.getString(DIMENSION_TAG),
                new BlockPos(tag.getInt(DEATH_X_TAG), tag.getInt(DEATH_Y_TAG), tag.getInt(DEATH_Z_TAG)),
                tag.getInt(AVAILABLE_MASK_TAG),
                tag.getLong(NONCE_TAG),
                tag.getLong(CHOOSE_DEADLINE_TAG)
        );
        try {
            session.state = State.valueOf(tag.getString(STATE_TAG));
        } catch (IllegalArgumentException exception) {
            session.state = State.CHOOSING;
        }
        session.returnDeadlineCalendarTick = tag.getLong(RETURN_DEADLINE_TAG);
        return session;
    }
}
