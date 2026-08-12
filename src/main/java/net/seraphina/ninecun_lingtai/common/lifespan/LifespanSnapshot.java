package net.seraphina.ninecun_lingtai.common.lifespan;

import net.seraphina.ninecun_lingtai.utility.LifespanUtility;

public record LifespanSnapshot(long ageTicks, int maximumLifespanYears, boolean immortal) {
    public LifespanSnapshot {
        ageTicks = Math.max(0L, ageTicks);
        maximumLifespanYears = Math.max(0, maximumLifespanYears);
    }

    public static LifespanSnapshot empty() {
        return new LifespanSnapshot(0L, 0, false);
    }

    public long maximumTicks() {
        return LifespanUtility.ticksForYears(this.maximumLifespanYears);
    }

    public long remainingTicks() {
        if (this.immortal) {
            return Long.MAX_VALUE;
        }
        return Math.max(0L, this.maximumTicks() - this.ageTicks);
    }

    public boolean expired() {
        return !this.immortal && this.ageTicks >= this.maximumTicks();
    }
}
