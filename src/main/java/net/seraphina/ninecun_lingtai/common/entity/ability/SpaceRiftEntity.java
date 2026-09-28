package net.seraphina.ninecun_lingtai.common.entity.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;

public final class SpaceRiftEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final String SOURCE_DIMENSION_KEY = "SourceDimension";
    private static final String TARGET_DIMENSION_KEY = "TargetDimension";
    private static final String SOURCE_X_KEY = "SourceX";
    private static final String SOURCE_Y_KEY = "SourceY";
    private static final String SOURCE_Z_KEY = "SourceZ";
    private static final String TARGET_X_KEY = "TargetX";
    private static final String TARGET_Y_KEY = "TargetY";
    private static final String TARGET_Z_KEY = "TargetZ";
    private static final String DIRECTION_X_KEY = "DirectionX";
    private static final String DIRECTION_Y_KEY = "DirectionY";
    private static final String DIRECTION_Z_KEY = "DirectionZ";
    private static final String AGE_KEY = "Age";
    private static final String TELEPORT_COOLDOWN_KEY = "ninecun_lingtai:space_rift_cooldown";
    private static final int LIFETIME_TICKS = 200;
    private static final int FORCE_RADIUS = 1;

    private ResourceLocation sourceDimension;
    private ResourceLocation targetDimension;
    private Vec3 sourcePosition = Vec3.ZERO;
    private Vec3 targetPosition = Vec3.ZERO;
    private Vec3 direction = new Vec3(0.0D, 0.0D, 1.0D);
    private int age;
    private boolean forced;

    public SpaceRiftEntity(EntityType<? extends SpaceRiftEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public void initialize(
            ResourceLocation sourceDimension,
            ResourceLocation targetDimension,
            Vec3 sourcePosition,
            Vec3 targetPosition,
            Vec3 direction
    ) {
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
        this.sourcePosition = sourcePosition;
        this.targetPosition = targetPosition;
        this.direction = normalize(direction);
        this.setYRot((float) Math.toDegrees(Math.atan2(-this.direction.x, this.direction.z)));
        this.setXRot((float) Math.toDegrees(Math.asin(-this.direction.y)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public Vec3 getRiftDirection() {
        return this.direction;
    }

    public float getProgress(float partialTick) {
        return Math.min(1.0F, (this.age + partialTick) / (float) LIFETIME_TICKS);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        if (this.level().isClientSide) {
            this.age++;
            return;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            forceChunks(serverLevel, true);
            long gameTime = serverLevel.getGameTime();
            for (ServerPlayer player : serverLevel.players()) {
                if (player.distanceToSqr(this) > 2.25D
                        || player.getPersistentData().getLong(TELEPORT_COOLDOWN_KEY) > gameTime) {
                    continue;
                }
                teleport(player, serverLevel, gameTime);
                break;
            }
        }
        this.age++;
        if (this.age >= LIFETIME_TICKS) {
            discard();
        }
    }

    private void teleport(ServerPlayer player, ServerLevel sourceLevel, long gameTime) {
        if (this.targetDimension == null || this.sourceDimension == null) {
            return;
        }
        ResourceLocation destinationId = this.targetDimension;
        Vec3 destination = this.targetPosition;
        ServerLevel destinationLevel = player.server.getLevel(
                net.minecraft.resources.ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        destinationId
                )
        );
        if (destinationLevel == null) {
            return;
        }
        player.getPersistentData().putLong(TELEPORT_COOLDOWN_KEY, gameTime + 20L);
        player.changeDimension(
                destinationLevel,
                ThreeThousandRealmManager.createManagedRealmTeleporter(
                        destination,
                        player.getYRot(),
                        player.getXRot()
                )
        );
    }

    private void forceChunks(ServerLevel level, boolean value) {
        int chunkX = this.blockPosition().getX() >> 4;
        int chunkZ = this.blockPosition().getZ() >> 4;
        for (int x = -FORCE_RADIUS; x <= FORCE_RADIUS; x++) {
            for (int z = -FORCE_RADIUS; z <= FORCE_RADIUS; z++) {
                level.setChunkForced(chunkX + x, chunkZ + z, value);
            }
        }
        this.forced = value;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.forced && this.level() instanceof ServerLevel serverLevel) {
            forceChunks(serverLevel, false);
        }
        super.remove(reason);
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
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void defineSynchedData() {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.sourceDimension = ResourceLocation.tryParse(tag.getString(SOURCE_DIMENSION_KEY));
        this.targetDimension = ResourceLocation.tryParse(tag.getString(TARGET_DIMENSION_KEY));
        this.sourcePosition = new Vec3(tag.getDouble(SOURCE_X_KEY), tag.getDouble(SOURCE_Y_KEY), tag.getDouble(SOURCE_Z_KEY));
        this.targetPosition = new Vec3(tag.getDouble(TARGET_X_KEY), tag.getDouble(TARGET_Y_KEY), tag.getDouble(TARGET_Z_KEY));
        this.direction = normalize(new Vec3(tag.getDouble(DIRECTION_X_KEY), tag.getDouble(DIRECTION_Y_KEY), tag.getDouble(DIRECTION_Z_KEY)));
        this.age = Math.max(0, tag.getInt(AGE_KEY));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putString(SOURCE_DIMENSION_KEY, this.sourceDimension == null ? "minecraft:overworld" : this.sourceDimension.toString());
        tag.putString(TARGET_DIMENSION_KEY, this.targetDimension == null ? "minecraft:overworld" : this.targetDimension.toString());
        tag.putDouble(SOURCE_X_KEY, this.sourcePosition.x);
        tag.putDouble(SOURCE_Y_KEY, this.sourcePosition.y);
        tag.putDouble(SOURCE_Z_KEY, this.sourcePosition.z);
        tag.putDouble(TARGET_X_KEY, this.targetPosition.x);
        tag.putDouble(TARGET_Y_KEY, this.targetPosition.y);
        tag.putDouble(TARGET_Z_KEY, this.targetPosition.z);
        tag.putDouble(DIRECTION_X_KEY, this.direction.x);
        tag.putDouble(DIRECTION_Y_KEY, this.direction.y);
        tag.putDouble(DIRECTION_Z_KEY, this.direction.z);
        tag.putInt(AGE_KEY, this.age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(this.sourceDimension);
        buffer.writeResourceLocation(this.targetDimension);
        buffer.writeDouble(this.sourcePosition.x);
        buffer.writeDouble(this.sourcePosition.y);
        buffer.writeDouble(this.sourcePosition.z);
        buffer.writeDouble(this.targetPosition.x);
        buffer.writeDouble(this.targetPosition.y);
        buffer.writeDouble(this.targetPosition.z);
        buffer.writeDouble(this.direction.x);
        buffer.writeDouble(this.direction.y);
        buffer.writeDouble(this.direction.z);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.sourceDimension = buffer.readResourceLocation();
        this.targetDimension = buffer.readResourceLocation();
        this.sourcePosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.targetPosition = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        this.direction = normalize(new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()));
    }

    private static Vec3 normalize(Vec3 value) {
        return value.lengthSqr() < 1.0E-8D ? new Vec3(0.0D, 0.0D, 1.0D) : value.normalize();
    }
}
