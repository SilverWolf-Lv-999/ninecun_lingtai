package net.seraphina.ninecun_lingtai.common.myminecraft;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;

public abstract class MyLivingEntity extends LivingEntity {
    protected MyLivingEntity(EntityType<? extends LivingEntity> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public boolean canUpdate() {
        return CultivationDefenseUtility.hasActiveDefense(this) || super.canUpdate();
    }
}
