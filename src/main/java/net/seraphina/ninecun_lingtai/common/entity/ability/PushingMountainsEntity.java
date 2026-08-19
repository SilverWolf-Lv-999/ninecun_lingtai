package net.seraphina.ninecun_lingtai.common.entity.ability;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import net.seraphina.ninecun_lingtai.common.ability.PushingMountainsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PushingMountainsEntity extends Entity implements IEntityAdditionalSpawnData {
    private static final String BLOCKS_KEY = "Blocks";
    private static final String BLOCK_X_KEY = "X";
    private static final String BLOCK_Y_KEY = "Y";
    private static final String BLOCK_Z_KEY = "Z";
    private static final String BLOCK_STATE_KEY = "State";
    private static final String THROWN_KEY = "Thrown";
    private static final String OWNER_UUID_KEY = "OwnerUuid";
    private static final String AGE_KEY = "Age";

    private final List<StoredBlock> blocks = new ArrayList<>();
    private final List<Integer> collisionEntityIds = new ArrayList<>();
    private UUID ownerUuid;
    private boolean thrown;
    private int age;
    private int width;
    private int height;
    private int depth;

    public PushingMountainsEntity(EntityType<? extends Entity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public void initialize(UUID ownerUuid, List<StoredBlock> blocks, int width, int height, int depth) {
        this.ownerUuid = ownerUuid;
        this.blocks.clear();
        this.blocks.addAll(blocks);
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.depth = Math.max(1, depth);
        this.thrown = false;
        this.noPhysics = true;
        this.setNoGravity(true);
        updateBoundingBox();
    }

    public List<StoredBlock> getBlocks() {
        return List.copyOf(this.blocks);
    }

    public boolean isThrown() {
        return this.thrown;
    }

    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public void beginThrow(Vec3 velocity) {
        this.thrown = true;
        this.noPhysics = false;
        this.setNoGravity(false);
        this.setDeltaMovement(velocity);
        this.collisionEntityIds.forEach(id -> {
            Entity child = this.level().getEntity(id);
            if (child != null) {
                child.discard();
            }
        });
        this.collisionEntityIds.clear();
    }

    public void createCollisionEntities() {
        if (this.level().isClientSide || this.thrown || !this.collisionEntityIds.isEmpty()) {
            return;
        }
        List<PushingMountainsCollisionEntity> existingEntities = this.level().getEntitiesOfClass(
                PushingMountainsCollisionEntity.class,
                this.getBoundingBox().inflate(1.0D),
                entity -> this.getUUID().equals(entity.getRootUuid())
        );
        if (!existingEntities.isEmpty()) {
            existingEntities.forEach(entity -> this.collisionEntityIds.add(entity.getId()));
            return;
        }
        for (AABB localBox : createCollisionBoxes()) {
            PushingMountainsCollisionEntity collisionEntity = new PushingMountainsCollisionEntity(
                    ModEntityTypes.PUSHING_MOUNTAINS_COLLISION.get(),
                    this.level()
            );
            collisionEntity.initialize(this, localBox);
            this.level().addFreshEntity(collisionEntity);
            this.collisionEntityIds.add(collisionEntity.getId());
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        if (!this.thrown) {
            this.noPhysics = true;
            this.setNoGravity(true);
            updateBoundingBox();
            if (!this.level().isClientSide) {
                createCollisionEntities();
            }
            return;
        }
        if (this.level().isClientSide) {
            return;
        }
        Vec3 movement = this.getDeltaMovement().add(0.0D, -0.04D, 0.0D).scale(0.99D);
        this.setDeltaMovement(movement);
        this.move(MoverType.SELF, movement);
        if (this.horizontalCollision || this.verticalCollision || this.onGround()
                || this.age >= CommonConfig.PUSHING_MOUNTAINS_MAX_ENTITY_TICKS.get()
                || this.getY() < this.level().getMinBuildHeight() - 64.0D) {
            PushingMountainsManager.annihilate(this);
        }
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        updateBoundingBox();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
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
    public boolean shouldBeSaved() {
        return !this.isRemoved();
    }

    @Override
    public void defineSynchedData() {
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        this.thrown = compoundTag.getBoolean(THROWN_KEY);
        this.age = compoundTag.getInt(AGE_KEY);
        if (compoundTag.contains(OWNER_UUID_KEY, Tag.TAG_INT_ARRAY)) {
            this.ownerUuid = compoundTag.getUUID(OWNER_UUID_KEY);
        }
        readBlocks(compoundTag.getList(BLOCKS_KEY, Tag.TAG_COMPOUND));
        this.noPhysics = !this.thrown;
        this.setNoGravity(!this.thrown);
        updateDimensionsFromBlocks();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.putBoolean(THROWN_KEY, this.thrown);
        compoundTag.putInt(AGE_KEY, this.age);
        if (this.ownerUuid != null) {
            compoundTag.putUUID(OWNER_UUID_KEY, this.ownerUuid);
        }
        compoundTag.put(BLOCKS_KEY, writeBlocks());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        buffer.writeUUID(this.ownerUuid == null ? new UUID(0L, 0L) : this.ownerUuid);
        buffer.writeBoolean(this.thrown);
        buffer.writeVarInt(this.width);
        buffer.writeVarInt(this.height);
        buffer.writeVarInt(this.depth);
        buffer.writeVarInt(this.blocks.size());
        for (StoredBlock block : this.blocks) {
            buffer.writeVarInt(block.x());
            buffer.writeVarInt(block.y());
            buffer.writeVarInt(block.z());
            buffer.writeVarInt(block.stateId());
        }
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buffer) {
        this.ownerUuid = buffer.readUUID();
        this.thrown = buffer.readBoolean();
        this.width = buffer.readVarInt();
        this.height = buffer.readVarInt();
        this.depth = buffer.readVarInt();
        this.blocks.clear();
        int blockCount = Math.min(buffer.readVarInt(), CommonConfig.PUSHING_MOUNTAINS_MAX_BLOCKS.get());
        for (int index = 0; index < blockCount; index++) {
            this.blocks.add(new StoredBlock(
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()
            ));
        }
        this.noPhysics = !this.thrown;
        this.setNoGravity(!this.thrown);
        updateBoundingBox();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            for (int entityId : this.collisionEntityIds) {
                Entity collisionEntity = this.level().getEntity(entityId);
                if (collisionEntity != null) {
                    collisionEntity.discard();
                }
            }
        }
        this.collisionEntityIds.clear();
        super.remove(reason);
    }

    private void updateDimensionsFromBlocks() {
        if (this.blocks.isEmpty()) {
            this.width = 1;
            this.height = 1;
            this.depth = 1;
            return;
        }
        this.width = this.blocks.stream().mapToInt(StoredBlock::x).max().orElse(0) + 1;
        this.height = this.blocks.stream().mapToInt(StoredBlock::y).max().orElse(0) + 1;
        this.depth = this.blocks.stream().mapToInt(StoredBlock::z).max().orElse(0) + 1;
    }

    private void updateBoundingBox() {
        if (this.width <= 0 || this.height <= 0 || this.depth <= 0) {
            return;
        }
        this.setBoundingBox(new AABB(
                this.getX(),
                this.getY(),
                this.getZ(),
                this.getX() + this.width,
                this.getY() + this.height,
                this.getZ() + this.depth
        ));
    }

    private List<AABB> createCollisionBoxes() {
        Map<Long, List<Integer>> horizontalRows = new HashMap<>();
        for (StoredBlock block : this.blocks) {
            long rowKey = ((long) block.y() << 32) ^ (block.z() & 0xFFFFFFFFL);
            horizontalRows.computeIfAbsent(rowKey, ignored -> new ArrayList<>()).add(block.x());
        }
        List<AABB> result = new ArrayList<>();
        for (Map.Entry<Long, List<Integer>> entry : horizontalRows.entrySet()) {
            int y = (int) (entry.getKey() >> 32);
            int z = (int) (long) entry.getKey();
            List<Integer> xValues = entry.getValue();
            xValues.sort(Comparator.naturalOrder());
            int runStart = xValues.get(0);
            int previousX = runStart;
            for (int index = 1; index < xValues.size(); index++) {
                int x = xValues.get(index);
                if (x == previousX + 1) {
                    previousX = x;
                    continue;
                }
                result.add(new AABB(runStart, y, z, previousX + 1.0D, y + 1.0D, z + 1.0D));
                runStart = x;
                previousX = x;
            }
            result.add(new AABB(runStart, y, z, previousX + 1.0D, y + 1.0D, z + 1.0D));
        }
        return result;
    }

    private ListTag writeBlocks() {
        ListTag listTag = new ListTag();
        for (StoredBlock block : this.blocks) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt(BLOCK_X_KEY, block.x());
            blockTag.putInt(BLOCK_Y_KEY, block.y());
            blockTag.putInt(BLOCK_Z_KEY, block.z());
            blockTag.putInt(BLOCK_STATE_KEY, block.stateId());
            listTag.add(blockTag);
        }
        return listTag;
    }

    private void readBlocks(ListTag listTag) {
        this.blocks.clear();
        int blockCount = Math.min(listTag.size(), CommonConfig.PUSHING_MOUNTAINS_MAX_BLOCKS.get());
        for (int index = 0; index < blockCount; index++) {
            CompoundTag blockTag = listTag.getCompound(index);
            this.blocks.add(new StoredBlock(
                    blockTag.getInt(BLOCK_X_KEY),
                    blockTag.getInt(BLOCK_Y_KEY),
                    blockTag.getInt(BLOCK_Z_KEY),
                    blockTag.getInt(BLOCK_STATE_KEY)
            ));
        }
    }

    public record StoredBlock(int x, int y, int z, int stateId) {
        public net.minecraft.world.level.block.state.BlockState state() {
            return Block.stateById(this.stateId);
        }
    }
}
