package net.seraphina.ninecun_lingtai.common.myminecraft;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import org.jetbrains.annotations.NotNull;

public abstract class MyLivingEntity extends LivingEntity {
    protected MyLivingEntity(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean canUpdate() {
        return CultivationDefenseUtility.hasActiveDefense(this) || super.canUpdate();
    }

    @Override
    public boolean hurt(@NotNull DamageSource pSource, float pAmount) {
        if (CultivationDefenseUtility.hasActiveDefense(this) && pSource.getEntity() == null) return false;
        return super.hurt(pSource, pAmount);
    }
}
