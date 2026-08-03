package com.seraphina.ninecun_lingtai.api.alchemy;

public record AlchemyPillEffect(
        double maxHealthFraction,
        double flatHealing,
        int regenerationDuration,
        int regenerationAmplifier
) {
    public AlchemyPillEffect {
        if (!Double.isFinite(maxHealthFraction)
                || maxHealthFraction <= 0.0D
                || maxHealthFraction > 1.0D) {
            throw new IllegalArgumentException("Maximum health fraction must be in the range (0, 1]");
        }
        if (!Double.isFinite(flatHealing) || flatHealing < 0.0D) {
            throw new IllegalArgumentException("Flat healing must be finite and non-negative");
        }
        if (regenerationDuration < 0) {
            throw new IllegalArgumentException("Regeneration duration must be non-negative");
        }
        if (regenerationAmplifier < 0 || regenerationAmplifier > 255) {
            throw new IllegalArgumentException("Regeneration amplifier must be between zero and 255");
        }
        if (regenerationDuration == 0 && regenerationAmplifier != 0) {
            throw new IllegalArgumentException("Regeneration amplifier must be zero without regeneration");
        }
    }
}
