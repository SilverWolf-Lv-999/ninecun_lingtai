package com.seraphina.ninecun_lingtai.api.element;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public interface ElementalEntityProvider {
    default Optional<ElementalPower> getAttackElement(LivingEntity entity) {
        return Optional.empty();
    }

    default Optional<ElementalPower> getDefenseElement(LivingEntity entity) {
        return Optional.empty();
    }
}
