package net.seraphina.ninecun_lingtai.client.player;

import net.minecraft.network.chat.Component;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaReleaseResult;

import java.util.UUID;

/** 短暂保存佛国放生反馈，供现有 Shader 覆盖层绘制印章与墨迹。 */
public final class ClientPalmBuddhaReleaseFeedbackManager {
    private static final int FEEDBACK_DURATION_TICKS = 42;

    private static UUID thisMemberId;
    private static PalmBuddhaReleaseResult.FailureReason thisFailureReason =
            PalmBuddhaReleaseResult.FailureReason.NONE;
    private static boolean thisSuccessful;
    private static int thisRemainingTicks;
    private static int thisRevision;

    private ClientPalmBuddhaReleaseFeedbackManager() {
    }

    public static void accept(
            UUID memberId,
            boolean successful,
            PalmBuddhaReleaseResult.FailureReason failureReason
    ) {
        thisMemberId = memberId;
        thisSuccessful = successful;
        thisFailureReason = failureReason == null
                ? PalmBuddhaReleaseResult.FailureReason.TRANSFER_FAILED
                : failureReason;
        thisRemainingTicks = FEEDBACK_DURATION_TICKS;
        thisRevision++;
    }

    public static void tick() {
        if (thisRemainingTicks > 0) {
            thisRemainingTicks--;
        }
    }

    public static UUID getMemberId() {
        return thisMemberId;
    }

    public static boolean isActive() {
        return thisRemainingTicks > 0;
    }

    public static boolean isSuccessful() {
        return thisSuccessful;
    }

    public static PalmBuddhaReleaseResult.FailureReason getFailureReason() {
        return thisFailureReason;
    }

    public static int getRemainingTicks() {
        return thisRemainingTicks;
    }

    public static int getRevision() {
        return thisRevision;
    }

    public static float getProgress(float partialTicks) {
        if (!isActive()) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F,
                (thisRemainingTicks - partialTicks) / (float) FEEDBACK_DURATION_TICKS));
    }

    public static Component getMessage() {
        if (thisSuccessful) {
            return Component.literal("放生已成");
        }
        return Component.literal(switch (thisFailureReason) {
            case MEMBER_NOT_OWNED -> "此人不在掌中";
            case MEMBER_OFFLINE -> "此人暂未入世";
            case MEMBER_NOT_IN_EXPECTED_REALM -> "掌中气机未定";
            case RECOVERY_DESTINATION_UNAVAILABLE -> "归处不可寻";
            case STATE_UPDATE_FAILED -> "掌印未落";
            case TRANSFER_FAILED -> "放生未成";
            case REQUEST_TIMEOUT -> "回应未至";
            case NONE -> "放生未成";
        });
    }

    public static void reset() {
        thisMemberId = null;
        thisFailureReason = PalmBuddhaReleaseResult.FailureReason.NONE;
        thisSuccessful = false;
        thisRemainingTicks = 0;
        thisRevision++;
    }
}
