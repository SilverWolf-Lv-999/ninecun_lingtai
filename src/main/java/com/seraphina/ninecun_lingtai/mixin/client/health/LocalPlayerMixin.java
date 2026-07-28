package com.seraphina.ninecun_lingtai.mixin.client.health;

import com.seraphina.ninecun_lingtai.client.utility.ClientCultivationDefenseUtility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LocalPlayer.class, priority = Integer.MAX_VALUE)
public abstract class LocalPlayerMixin extends LivingEntity {
    protected LocalPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public float getHealth() {
        return ClientCultivationDefenseUtility.getProtectedHealth(this, super.getHealth());
    }

    @Override
    public boolean isAlive() {
        return ClientCultivationDefenseUtility.getProtectedAlive(this, super.isAlive());
    }

    @Override
    public boolean isDeadOrDying() {
        return ClientCultivationDefenseUtility.getProtectedDeadOrDying(this, super.isDeadOrDying());
    }
}
