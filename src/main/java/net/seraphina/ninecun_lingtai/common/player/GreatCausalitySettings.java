package net.seraphina.ninecun_lingtai.common.player;

import java.util.UUID;

public record GreatCausalitySettings(
        boolean transferAttack,
        boolean transferHate,
        boolean transferTribulation,
        boolean transferNegativeEffects,
        boolean transferVillagerPrice,
        boolean transferDamage,
        GreatCausalityTargetMode transferTargetMode,
        UUID transferTargetId,
        boolean randomItem,
        boolean corruptBeneficialEffects,
        boolean transferItems,
        boolean transferAdvancements,
        boolean voidCrafting,
        GreatCausalityTargetMode tamperTargetMode,
        UUID tamperTargetId
) {
    public GreatCausalitySettings {
        transferTargetMode = transferTargetMode == null
                ? GreatCausalityTargetMode.RANDOM
                : transferTargetMode;
        tamperTargetMode = tamperTargetMode == null
                ? GreatCausalityTargetMode.RANDOM
                : tamperTargetMode;
    }

    public static GreatCausalitySettings defaultSettings() {
        return new GreatCausalitySettings(
                false,
                false,
                false,
                false,
                false,
                false,
                GreatCausalityTargetMode.RANDOM,
                null,
                false,
                false,
                false,
                false,
                false,
                GreatCausalityTargetMode.RANDOM,
                null
        );
    }
}
