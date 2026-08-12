package net.seraphina.ninecun_lingtai.common.entity.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public final class PushingMountainsCollisionEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final String ROOT_UUID_KEY = "RootUuid";
    private static final String MIN_X_KEY = "MinX";
    private static final String MIN_Y_KEY = "MinY";
    private static final String MIN_Z_KEY = "MinZ";
    private static final String MAX_X_KEY = "MaxX";
    private static final String MAX_Y_KEY = "MaxY";
    private static final String MAX_Z_KEY = "MaxZ";

    private UUID rootUuid;
    private AABB localBox = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
    private AABB worldBox = this.localBox;

    public PushingMountainsCollisionEntity(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public void initialize(PushingMountainsEntity root, AABB localBox) {
        this.rootUuid = root.getUUID();
        this.localBox = localBox;
        updateWorldBox(root);
    }

    public UUID getRootUuid() {
        return this.rootUuid;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(findRoot() instanceof PushingMountainsEntity root)
                || root.isRemoved()
                || root.isThrown()) {
            this.discard();
            return;
        }
        updateWorldBox(root);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        if (compoundTag.contains(ROOT_UUID_KEY, Tag.TAG_INT_ARRAY)) {
            this.rootUuid = compoundTag.getUUID(ROOT_UUID_KEY);
        }
        this.localBox = readBox(compoundTag);
        if (findRoot() instanceof PushingMountainsEntity root) {
            updateWorldBox(root);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        if (this.rootUuid != null) {
            compoundTag.putUUID(ROOT_UUID_KEY, this.rootUuid);
        }
        writeBox(compoundTag, this.localBox);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.rootUuid == null ? new UUID(0L, 0L) : this.rootUuid);
        writeBox(buffer, this.localBox);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.rootUuid = buffer.readUUID();
        this.localBox = readBox(buffer);
        if (findRoot() instanceof PushingMountainsEntity root) {
            updateWorldBox(root);
        }
    }

    private void updateWorldBox(PushingMountainsEntity root) {
        this.worldBox = this.localBox.move(root.getX(), root.getY(), root.getZ());
        super.setPos(this.worldBox.minX, this.worldBox.minY, this.worldBox.minZ);
        this.setBoundingBox(this.worldBox);
    }

    private Entity findRoot() {
        if (this.rootUuid == null) {
            return null;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getEntity(this.rootUuid);
        }
        return this.level().getEntitiesOfClass(
                        PushingMountainsEntity.class,
                        this.worldBox.inflate(256.0D),
                        root -> root.getUUID().equals(this.rootUuid)
                )
                .stream()
                .findFirst()
                .orElse(null);
    }

    private static void writeBox(FriendlyByteBuf buffer, AABB box) {
        buffer.writeDouble(box.minX);
        buffer.writeDouble(box.minY);
        buffer.writeDouble(box.minZ);
        buffer.writeDouble(box.maxX);
        buffer.writeDouble(box.maxY);
        buffer.writeDouble(box.maxZ);
    }

    private static AABB readBox(FriendlyByteBuf buffer) {
        return new AABB(
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble(),
                buffer.readDouble()
        );
    }

    private static void writeBox(CompoundTag tag, AABB box) {
        tag.putDouble(MIN_X_KEY, box.minX);
        tag.putDouble(MIN_Y_KEY, box.minY);
        tag.putDouble(MIN_Z_KEY, box.minZ);
        tag.putDouble(MAX_X_KEY, box.maxX);
        tag.putDouble(MAX_Y_KEY, box.maxY);
        tag.putDouble(MAX_Z_KEY, box.maxZ);
    }

    private static AABB readBox(CompoundTag tag) {
        return new AABB(
                tag.getDouble(MIN_X_KEY),
                tag.getDouble(MIN_Y_KEY),
                tag.getDouble(MIN_Z_KEY),
                tag.getDouble(MAX_X_KEY),
                tag.getDouble(MAX_Y_KEY),
                tag.getDouble(MAX_Z_KEY)
        );
    }
}
