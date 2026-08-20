package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public final class CultivationCombatUtility {

    public static boolean canInstantlyKillLowerRealmCultivator(
            Optional<CultivationStage> attackerStage,
            Optional<CultivationStage> defenderStage
    ) {
        return attackerStage
                .map(stage -> stage.realm().ordinal() >= CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(false)
                && defenderStage
                .map(stage -> stage.realm().ordinal() < CultivationRealm.SPIRIT_TRANSFORMATION.ordinal())
                .orElse(true);
    }

    public static boolean canInstantlyKillLowerRealmCultivator(
            LivingEntity defender,
            DamageSource damageSource
    ) {
        if (damageSource.is(ModDamageTypes.REALM_EXECUTION)
                || damageSource.is(DamageTypes.THORNS)
                || !(damageSource.getEntity() instanceof LivingEntity attacker)
                || attacker == defender) {
            return false;
        }
        return canInstantlyKillLowerRealmCultivator(
                CreatureCultivationData.getStage(attacker),
                CreatureCultivationData.getStage(defender)
        );
    }
}
