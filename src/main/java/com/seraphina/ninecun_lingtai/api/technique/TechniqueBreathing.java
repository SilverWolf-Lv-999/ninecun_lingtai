package com.seraphina.ninecun_lingtai.api.technique;

/**
 * Marks a technique as a breathing technique (呼吸法). Every realm cultivation session must run
 * through a learned breathing technique; the absorption multiplier expresses how strong the
 * technique is, with the basic breathing technique at {@code 1.0} as the weakest baseline.
 */
public record TechniqueBreathing(double absorptionMultiplier) {
    public TechniqueBreathing {
        if (!Double.isFinite(absorptionMultiplier) || absorptionMultiplier <= 0.0D) {
            throw new IllegalArgumentException("absorptionMultiplier must be a positive finite number");
        }
    }
}
