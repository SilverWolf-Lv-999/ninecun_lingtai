package com.seraphina.ninecun_lingtai.client.utility;

import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;

public final class DefenseSpellRenderUtility {
    private DefenseSpellRenderUtility() {
    }

    public static boolean usesPlayerModelLayer(DefenseSpellKind kind) {
        return kind == DefenseSpellKind.METAL_BODY_SHIELD
                || kind == DefenseSpellKind.WATER_BODY_SHIELD;
    }
}
