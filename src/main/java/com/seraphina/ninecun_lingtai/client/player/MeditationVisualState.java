package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;

public record MeditationVisualState(
        FiveElementValues environmentValues,
        FiveElementValues absorptionEfficiencies
) {
}
