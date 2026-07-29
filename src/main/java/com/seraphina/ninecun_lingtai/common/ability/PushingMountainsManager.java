package com.seraphina.ninecun_lingtai.common.ability;

import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsEntity;
import com.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsEntity.StoredBlock;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import com.seraphina.ninecun_lingtai.utility.PushingMountainsUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PushingMountainsManager {
    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();
    private static final double INTERACTION_DISTANCE_PADDING = 16.0D;

    private PushingMountainsManager() {
    }

    public static void selectPoint(ServerPlayer player, BlockPos position) {
        if (!canUse(player)) {
            sendMessage(player, "金丹期方可排山倒海");
            return;
        }
        if (!isWithinSelectionDistance(player, position)) {
            sendMessage(player, "神念所及之外，尚不可摄取");
            return;
        }
        Selection current = SELECTIONS.get(player.getUUID());
        if (current == null || current.second() != null) {
            SELECTIONS.put(player.getUUID(), new Selection(position, null));
            sendMessage(player, "已定第一点，再选第二点");
            return;
        }
        SELECTIONS.put(player.getUUID(), new Selection(current.first(), position));
        sendMessage(player, "范围已定，右键实体化，按住~右键还原");
    }

    public static void clearSelection(ServerPlayer player) {
        SELECTIONS.remove(player.getUUID());
    }

    public static void entityizeSelection(ServerPlayer player) {
        if (!canUse(player)) {
            return;
        }
        Selection selection = SELECTIONS.remove(player.getUUID());
        if (selection == null || selection.second() == null) {
            sendMessage(player, "需先选定两个点");
            return;
        }
        entityize(player, selection.first(), selection.second());
    }

    public static void restore(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof PushingMountainsEntity region)
                || region.isThrown()
                || !canInteract(player, region)) {
            return;
        }
        BlockPos origin = BlockPos.containing(region.position());
        for (StoredBlock block : region.getBlocks()) {
            BlockPos position = origin.offset(block.x(), block.y(), block.z());
            player.serverLevel().setBlock(position, block.state(), Block.UPDATE_ALL);
        }
        player.serverLevel().playSound(
                null,
                origin,
                SoundEvents.STONE_PLACE,
                SoundSource.BLOCKS,
                1.0F,
                0.7F
        );
        region.discard();
    }

    public static void throwRegion(ServerPlayer player, int entityId) {
        Entity entity = player.serverLevel().getEntity(entityId);
        if (!(entity instanceof PushingMountainsEntity region)
                || region.isThrown()
                || !canInteract(player, region)) {
            return;
        }
        Vec3 look = player.getLookAngle().normalize();
        double speed = 1.15D + CultivationApi.getStage(player)
                .map(stage -> Math.max(0, stage.realm().ordinal() - 2) * 0.18D)
                .orElse(0.0D);
        region.beginThrow(look.scale(speed).add(0.0D, 0.16D, 0.0D));
        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENDER_DRAGON_SHOOT,
                SoundSource.PLAYERS,
                0.8F,
                0.75F
        );
    }

    public static void annihilate(PushingMountainsEntity region) {
        if (!(region.level() instanceof ServerLevel level) || region.isRemoved()) {
            return;
        }
        BlockPos origin = BlockPos.containing(region.position());
        for (StoredBlock block : region.getBlocks()) {
            BlockPos position = origin.offset(block.x(), block.y(), block.z());
            level.removeBlockEntity(position);
            level.setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        AABB damageBox = region.getBoundingBox();
        for (LivingEntity livingEntity : level.getEntitiesOfClass(
                LivingEntity.class,
                damageBox,
                LivingEntity::isAlive
        )) {
            livingEntity.hurt(
                    region.damageSources().generic(),
                    CommonConfig.PUSHING_MOUNTAINS_THROW_DAMAGE.get().floatValue()
            );
        }
        level.playSound(
                null,
                BlockPos.containing(region.position()),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                1.4F,
                0.65F
        );
        region.discard();
    }

    public static boolean canUse(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && PushingMountainsUtility.canUse(CultivationApi.getStage(player));
    }

    private static void entityize(ServerPlayer player, BlockPos first, BlockPos second) {
        long volume = PushingMountainsUtility.getSelectionVolume(first, second);
        int maximumBlocks = CommonConfig.PUSHING_MOUNTAINS_MAX_BLOCKS.get();
        if (volume > (long) maximumBlocks * 16L) {
            sendMessage(player, "所选范围过大，须缩小后再摄取");
            return;
        }
        if (!PushingMountainsUtility.createSelectionBox(first, second).contains(player.position())) {
            // The player may stand beside the selection; no special handling is needed.
        } else {
            sendMessage(player, "不可将自身所在之地收入掌中");
            return;
        }

        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX());
        int maxY = Math.max(first.getY(), second.getY());
        int maxZ = Math.max(first.getZ(), second.getZ());
        List<StoredBlock> blocks = new ArrayList<>();
        BlockPos.MutableBlockPos mutablePosition = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    mutablePosition.set(x, y, z);
                    if (!player.level().hasChunkAt(mutablePosition)) {
                        sendMessage(player, "所选区域包含未加载的天地，无法摄取");
                        return;
                    }
                    BlockState state = player.level().getBlockState(mutablePosition);
                    if (state.isAir()) {
                        continue;
                    }
                    if (blocks.size() >= maximumBlocks) {
                        sendMessage(player, "所选实体过多，须缩小范围");
                        return;
                    }
                    blocks.add(new StoredBlock(
                            x - minX,
                            y - minY,
                            z - minZ,
                            Block.getId(state)
                    ));
                }
            }
        }
        if (blocks.isEmpty()) {
            sendMessage(player, "所选范围内没有可摄取的方块");
            return;
        }

        for (StoredBlock block : blocks) {
            BlockPos position = new BlockPos(minX + block.x(), minY + block.y(), minZ + block.z());
            player.level().removeBlockEntity(position);
            player.level().setBlock(position, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        PushingMountainsEntity region = new PushingMountainsEntity(
                ModEntityTypes.PUSHING_MOUNTAINS_ENTITY.get(),
                player.level()
        );
        region.initialize(
                player.getUUID(),
                blocks,
                maxX - minX + 1,
                maxY - minY + 1,
                maxZ - minZ + 1
        );
        region.setPos(minX, minY, minZ);
        player.level().addFreshEntity(region);
        region.createCollisionEntities();
        player.level().playSound(
                null,
                BlockPos.containing(region.position()),
                SoundEvents.BASALT_BREAK,
                SoundSource.BLOCKS,
                1.0F,
                0.8F
        );
    }

    private static boolean canInteract(ServerPlayer player, PushingMountainsEntity region) {
        double maximumDistance = CultivationApi.getStage(player)
                .map(PushingMountainsUtility::getMaximumSelectionDistance)
                .orElse(0.0D) + INTERACTION_DISTANCE_PADDING;
        return canUse(player) && player.distanceToSqr(region) <= maximumDistance * maximumDistance;
    }

    private static boolean isWithinSelectionDistance(ServerPlayer player, BlockPos position) {
        double maximumDistance = CultivationApi.getStage(player)
                .map(PushingMountainsUtility::getMaximumSelectionDistance)
                .orElse(0.0D) + 2.0D;
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(position))
                <= maximumDistance * maximumDistance;
    }

    private static void sendMessage(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }

    private record Selection(BlockPos first, BlockPos second) {
    }
}
