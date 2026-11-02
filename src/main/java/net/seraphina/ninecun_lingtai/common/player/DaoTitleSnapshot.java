package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleColors;
import net.seraphina.ninecun_lingtai.api.daotitle.DaoTitleSuffix;

public record DaoTitleSnapshot(
        String customName,
        DaoTitleSuffix immortalSuffix,
        CultivationRealm realm,
        DaoTitleColors colors
) {
    public DaoTitleSnapshot {
        customName = customName == null ? "" : customName;
        immortalSuffix = immortalSuffix == null ? DaoTitleSuffix.IMMORTAL : immortalSuffix;
        colors = colors == null ? DaoTitleColors.defaults() : colors;
    }

    public boolean isUnlocked() {
        return this.realm != null && this.realm.ordinal() >= CultivationRealm.GOLDEN_CORE.ordinal();
    }
}
