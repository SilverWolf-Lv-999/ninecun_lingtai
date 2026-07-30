package com.seraphina.ninecun_lingtai.common.ability;

import java.util.UUID;

public record PressureEffectState(
        UUID sourceId,
        int sourceRank,
        float severity,
        boolean immobilized
) {
}
