package com.seraphina.ninecun_lingtai.common.player;

public record DefenseResourceSnapshot(
        DefenseResourceType type,
        int current,
        int maximum,
        int elementMask
) {
    public DefenseResourceSnapshot {
        type = type == null ? DefenseResourceType.NONE : type;
        maximum = Math.max(0, maximum);
        current = Math.max(0, Math.min(current, maximum));
        if (maximum == 0) {
            type = DefenseResourceType.NONE;
            current = 0;
        }
    }

    public boolean isVisible() {
        return this.type != DefenseResourceType.NONE && this.maximum > 0;
    }

    public float coverage() {
        if (!isVisible()) {
            return 0.0F;
        }
        return this.current / (float) this.maximum;
    }

    public static DefenseResourceSnapshot empty(int elementMask) {
        return new DefenseResourceSnapshot(DefenseResourceType.NONE, 0, 0, elementMask);
    }
}
