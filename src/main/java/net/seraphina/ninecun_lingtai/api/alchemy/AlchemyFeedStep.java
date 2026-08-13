package net.seraphina.ninecun_lingtai.api.alchemy;

import java.util.Objects;

public record AlchemyFeedStep(
        AlchemyIngredientSlot slot,
        int stageIndex,
        double windowStart,
        double idealPoint,
        double windowEnd
) {
    public AlchemyFeedStep {
        Objects.requireNonNull(slot, "slot");
        if (stageIndex < 0) {
            throw new IllegalArgumentException("Alchemy feed stage index cannot be negative");
        }
        requireFiniteFraction(windowStart, "windowStart");
        requireFiniteFraction(idealPoint, "idealPoint");
        requireFiniteFraction(windowEnd, "windowEnd");
        if (windowStart > idealPoint || idealPoint > windowEnd) {
            throw new IllegalArgumentException(
                    "Alchemy feed window must satisfy start <= ideal <= end"
            );
        }
    }

    public int slotMask() {
        return 1 << this.slot.getSlotIndex();
    }

    private static void requireFiniteFraction(double value, String fieldName) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException(
                    "Alchemy feed " + fieldName + " must be between zero and one"
            );
        }
    }
}
