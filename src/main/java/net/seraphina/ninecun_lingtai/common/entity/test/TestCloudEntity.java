package net.seraphina.ninecun_lingtai.common.entity.test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/**
 * A stationary visual test entity for the volumetric cloud renderer.
 */
public final class TestCloudEntity extends Entity {
    public TestCloudEntity(EntityType<? extends TestCloudEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void defineSynchedData() {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
