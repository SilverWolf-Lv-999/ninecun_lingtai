package com.seraphina.ninecun_lingtai.api.aptitude;

import java.util.Optional;

public record SoulStrength(int illuminatedStars) {
    public static final int MINIMUM_ILLUMINATED_STARS = 1;
    public static final int MAXIMUM_ILLUMINATED_STARS = 7;

    public SoulStrength {
        if (illuminatedStars < MINIMUM_ILLUMINATED_STARS
                || illuminatedStars > MAXIMUM_ILLUMINATED_STARS) {
            throw new IllegalArgumentException("Soul strength must illuminate between one and seven stars");
        }
    }

    public float quality() {
        return this.illuminatedStars / (float) MAXIMUM_ILLUMINATED_STARS;
    }

    public static Optional<SoulStrength> fromIlluminatedStars(int illuminatedStars) {
        try {
            return Optional.of(new SoulStrength(illuminatedStars));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
