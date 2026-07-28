package com.seraphina.ninecun_lingtai.mixin.common.health;

import com.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ServerPlayer.class, priority = Integer.MAX_VALUE)
public abstract class ServerPlayerMixin extends LivingEntity {
    protected ServerPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public float getHealth() {
        return CultivationDefenseUtility.getProtectedHealth(this, super.getHealth());
    }

    @Override
    public boolean isAlive() {
        return CultivationDefenseUtility.getProtectedAlive(this, super.isAlive());
    }

    @Override
    public boolean isDeadOrDying() {
        return CultivationDefenseUtility.getProtectedDeadOrDying(this, super.isDeadOrDying());
    }
}
