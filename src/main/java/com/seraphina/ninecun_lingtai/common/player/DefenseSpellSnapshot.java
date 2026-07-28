package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;

public record DefenseSpellSnapshot(
        DefenseSpellKind kind,
        double current,
        double maximum,
        int elementMask,
        int remainingTicks
) {
    public DefenseSpellSnapshot {
        if (kind == null) {
            throw new IllegalArgumentException("Defense spell kind must not be null");
        }
        if (!Double.isFinite(maximum) || maximum <= 0.0D) {
            throw new IllegalArgumentException("Maximum defense points must be positive and finite");
        }
        if (!Double.isFinite(current)) {
            throw new IllegalArgumentException("Current defense points must be finite");
        }
        if (remainingTicks <= 0) {
            throw new IllegalArgumentException("Remaining defense spell duration must be positive");
        }
        current = Math.max(0.0D, Math.min(current, maximum));
    }

    public double ratio() {
        return this.current / this.maximum;
    }
}
