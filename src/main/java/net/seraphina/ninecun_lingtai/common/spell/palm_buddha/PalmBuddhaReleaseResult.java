package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import java.util.Objects;

/**
 * 服务端放生一名佛国成员后的轻量结果。
 *
 * <p>成功时原因必须为 {@link FailureReason#NONE}；失败时原因用于客户端显示
 * 一条短暂的水墨反馈，不携带位置、维度或实体数据。</p>
 */
public record PalmBuddhaReleaseResult(boolean successful, FailureReason failureReason) {
    public PalmBuddhaReleaseResult {
        Objects.requireNonNull(failureReason, "failureReason");
        if (successful != (failureReason == FailureReason.NONE)) {
            throw new IllegalArgumentException("Palm-buddha release result has inconsistent state");
        }
    }

    public static PalmBuddhaReleaseResult success() {
        return new PalmBuddhaReleaseResult(true, FailureReason.NONE);
    }

    public static PalmBuddhaReleaseResult failure(FailureReason failureReason) {
        if (failureReason == null || failureReason == FailureReason.NONE) {
            throw new IllegalArgumentException("A failed release must have a failure reason");
        }
        return new PalmBuddhaReleaseResult(false, failureReason);
    }

    public enum FailureReason {
        NONE(0),
        MEMBER_NOT_OWNED(1),
        MEMBER_OFFLINE(2),
        MEMBER_NOT_IN_EXPECTED_REALM(3),
        RECOVERY_DESTINATION_UNAVAILABLE(4),
        STATE_UPDATE_FAILED(5),
        TRANSFER_FAILED(6),
        REQUEST_TIMEOUT(7);

        private final int thisWireId;

        FailureReason(int wireId) {
            this.thisWireId = wireId;
        }

        public int wireId() {
            return this.thisWireId;
        }

        public static FailureReason fromWireId(int wireId) {
            for (FailureReason reason : values()) {
                if (reason.thisWireId == wireId) {
                    return reason;
                }
            }
            throw new IllegalArgumentException("Unknown palm-buddha release failure reason: " + wireId);
        }
    }
}
