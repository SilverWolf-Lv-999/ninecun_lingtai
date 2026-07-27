package com.seraphina.ninecun_lingtai.common.spell;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerShrinkEarthData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueManager;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.ShrinkEarthUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class ShrinkEarthManager {
    public static final ResourceLocation TECHNIQUE_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "shrink_earth_spell"
    );

    private static final CultivationStage MINIMUM_STAGE = new CultivationStage(CultivationRealm.NASCENT_SOUL, 1);
    private static final int ENERGY_CONSUMPTION_INTERVAL_TICKS = 20;
    private static final int PROFICIENCY_INTERVAL_TICKS = 100;
    private static final long PROFICIENCY_PER_INTERVAL = 1L;
    private static final double MINIMUM_MOVEMENT_DISTANCE = 0.015D;
    private static final double MINIMUM_MOVEMENT_DISTANCE_SQR =
            MINIMUM_MOVEMENT_DISTANCE * MINIMUM_MOVEMENT_DISTANCE;
    private static final double MAXIMUM_RAW_MOVEMENT_DISTANCE = 1.25D;
    private static final int DESTINATION_SEARCH_UP_BLOCKS = 4;
    private static final int DESTINATION_SEARCH_DOWN_BLOCKS = 12;

    private ShrinkEarthManager() {
    }

    public static void requestSetActive(ServerPlayer player, boolean active) {
        if (active) {
            requestStart(player);
        } else {
            stop(player, true);
        }
    }

    public static void requestStart(ServerPlayer player) {
        if (PlayerShrinkEarthData.isActive(player)) {
            syncState(player);
            return;
        }
        Optional<TechniqueDefinition> technique = TechniqueManager.INSTANCE.getTechnique(TECHNIQUE_ID);
        if (technique.isEmpty()) {
            sendActionBar(player, "缩地成寸之法已失传");
            syncState(player);
            return;
        }
        SpellCastManager.castTechnique(player, technique.get());
        if (!PlayerShrinkEarthData.isActive(player)) {
            syncState(player);
        }
    }

    public static boolean start(ServerPlayer player, SpellCastContext context) {
        if (PlayerShrinkEarthData.isActive(player)) {
            sendActionBar(player, "缩地之势已成，无须重施");
            syncState(player);
            return false;
        }
        if (!canUse(player)) {
            sendActionBar(player, "元婴未成，天地之距尚不可折");
            syncState(player);
            return false;
        }
        int compressionScale = ShrinkEarthUtility.getCompressionScale(context.masteryStage());
        PlayerShrinkEarthData.start(player, compressionScale);
        player.fallDistance = 0.0F;
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.7F,
                0.65F
        );
        sendActionBar(player, "缩地成寸，" + compressionScale + " 格归于一格");
        syncState(player);
        return true;
    }

    public static void stop(ServerPlayer player, boolean sendMessage) {
        if (!PlayerShrinkEarthData.isActive(player)) {
            syncState(player);
            return;
        }
        PlayerShrinkEarthData.stop(player);
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.55F,
                1.25F
        );
        if (sendMessage) {
            sendActionBar(player, "咫尺复为天涯");
        }
        syncState(player);
    }

    public static void serverTick(ServerPlayer player) {
        if (!PlayerShrinkEarthData.isActive(player)) {
            return;
        }
        if (!canUse(player)) {
            stop(player, false);
            sendActionBar(player, "境界不继，缩地成寸自行散去");
            return;
        }
        int compressionScale = getCompressionScale(player);
        if (PlayerShrinkEarthData.getCompressionScale(player) != compressionScale) {
            PlayerShrinkEarthData.setCompressionScale(player, compressionScale);
            syncState(player);
        }
        if (!consumeSustainingEnergy(player)) {
            stop(player, false);
            sendActionBar(player, "灵力不继，天地复展");
            return;
        }
        moveThroughCompressedWorld(player, compressionScale);
        if (player.tickCount % PROFICIENCY_INTERVAL_TICKS == 0) {
            PlayerTechniqueManager.addProficiency(player, TECHNIQUE_ID, PROFICIENCY_PER_INTERVAL);
        }
    }

    public static void restore(ServerPlayer player) {
        if (!PlayerShrinkEarthData.isActive(player)) {
            syncState(player);
            return;
        }
        if (!canUse(player)) {
            stop(player, false);
            return;
        }
        int compressionScale = getCompressionScale(player);
        PlayerShrinkEarthData.setCompressionScale(player, compressionScale);
        PlayerShrinkEarthData.setLastPosition(player, player.position());
        syncState(player);
    }

    public static int getCompressionScale(ServerPlayer player) {
        return PlayerTechniqueData.getTechnique(player, TECHNIQUE_ID)
                .flatMap(progress -> TechniqueMasteryStage.fromMastery(progress.mastery()))
                .map(ShrinkEarthUtility::getCompressionScale)
                .orElse(ShrinkEarthUtility.ENTRY_COMPRESSION_SCALE);
    }

    private static boolean consumeSustainingEnergy(ServerPlayer player) {
        if (player.isCreative() || player.tickCount % ENERGY_CONSUMPTION_INTERVAL_TICKS != 0) {
            return true;
        }
        double energyCost = CommonConfig.SHRINK_EARTH_ENERGY_COST_PER_SECOND.get();
        return energyCost <= 0.0D || PlayerSpiritualEnergyData.consumeEnergyAny(player, energyCost) != null;
    }

    private static void moveThroughCompressedWorld(ServerPlayer player, int compressionScale) {
        Optional<Vec3> previousPosition = PlayerShrinkEarthData.getLastPosition(player);
        Vec3 currentPosition = player.position();
        if (previousPosition.isEmpty()) {
            PlayerShrinkEarthData.setLastPosition(player, currentPosition);
            return;
        }
        Vec3 rawMovement = currentPosition.subtract(previousPosition.get());
        double horizontalDistance = Math.hypot(rawMovement.x, rawMovement.z);
        if (horizontalDistance > MAXIMUM_RAW_MOVEMENT_DISTANCE
                || Math.abs(rawMovement.y) > MAXIMUM_RAW_MOVEMENT_DISTANCE) {
            PlayerShrinkEarthData.setLastPosition(player, currentPosition);
            return;
        }
        if (horizontalDistance < MINIMUM_MOVEMENT_DISTANCE || compressionScale <= 1) {
            PlayerShrinkEarthData.setLastPosition(player, currentPosition);
            return;
        }

        Vec3 extraMovement = new Vec3(
                rawMovement.x * (compressionScale - 1),
                0.0D,
                rawMovement.z * (compressionScale - 1)
        );
        Vec3 clippedTarget = clipHorizontalPath(player, currentPosition, currentPosition.add(extraMovement));
        Optional<Vec3> safeDestination = findSafeDestination(player, clippedTarget);
        if (safeDestination.isEmpty()
                || horizontalDistanceSqr(safeDestination.get(), currentPosition) < MINIMUM_MOVEMENT_DISTANCE_SQR) {
            PlayerShrinkEarthData.setLastPosition(player, currentPosition);
            return;
        }
        Vec3 destination = safeDestination.get();
        player.teleportTo(destination.x, destination.y, destination.z);
        player.fallDistance = 0.0F;
        player.setDeltaMovement(0.0D, Math.min(0.0D, player.getDeltaMovement().y), 0.0D);
        PlayerShrinkEarthData.setLastPosition(player, player.position());
    }

    private static Vec3 clipHorizontalPath(ServerPlayer player, Vec3 from, Vec3 to) {
        Vec3 horizontalPath = new Vec3(to.x - from.x, 0.0D, to.z - from.z);
        if (horizontalPath.lengthSqr() < 1.0E-8D) {
            return to;
        }
        Vec3 direction = horizontalPath.normalize();
        double backoff = player.getBbWidth() * 0.5D + 0.15D;
        Vec3 result = to;
        double resultDistance = horizontalDistanceSqr(from, to);
        double[] heights = {0.1D, player.getBbHeight() * 0.5D, player.getEyeHeight()};
        for (double height : heights) {
            Vec3 start = from.add(0.0D, height, 0.0D);
            Vec3 end = to.add(0.0D, height, 0.0D);
            HitResult hit = player.level().clip(new ClipContext(
                    start,
                    end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    player
            ));
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            Vec3 candidate = hit.getLocation().subtract(direction.scale(backoff));
            double candidateDistance = horizontalDistanceSqr(from, candidate);
            if (candidateDistance < resultDistance) {
                result = new Vec3(candidate.x, to.y, candidate.z);
                resultDistance = candidateDistance;
            }
        }
        return result;
    }

    private static Optional<Vec3> findSafeDestination(ServerPlayer player, Vec3 target) {
        Level level = player.level();
        if (!level.hasChunkAt(BlockPos.containing(target))) {
            return Optional.empty();
        }
        Vec3 bestSupportedDestination = null;
        double bestSupportedOffset = Double.MAX_VALUE;
        for (int offset = -DESTINATION_SEARCH_DOWN_BLOCKS; offset <= DESTINATION_SEARCH_UP_BLOCKS; offset++) {
            Vec3 candidate = new Vec3(target.x, target.y + offset, target.z);
            AABB candidateBox = player.getBoundingBox().move(candidate.subtract(player.position()));
            if (!level.noCollision(player, candidateBox)) {
                continue;
            }
            if (hasSupportingCollision(level, player, candidateBox)) {
                double offsetDistance = Math.abs(offset);
                if (offsetDistance < bestSupportedOffset) {
                    bestSupportedDestination = candidate;
                    bestSupportedOffset = offsetDistance;
                }
            }
        }
        return Optional.ofNullable(bestSupportedDestination);
    }

    private static boolean hasSupportingCollision(Level level, ServerPlayer player, AABB candidateBox) {
        return !level.noCollision(player, candidateBox.move(0.0D, -0.08D, 0.0D));
    }

    private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
        double xDistance = first.x - second.x;
        double zDistance = first.z - second.z;
        return xDistance * xDistance + zDistance * zDistance;
    }

    private static boolean canUse(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !PlayerTechniqueData.hasTechnique(player, TECHNIQUE_ID)) {
            return false;
        }
        return CultivationApi.getStage(player)
                .map(stage -> CultivationUtility.getProgressionIndex(stage)
                        >= CultivationUtility.getProgressionIndex(MINIMUM_STAGE))
                .orElse(false);
    }

    private static void syncState(ServerPlayer player) {
        ModNetwork.sendShrinkEarthState(player);
    }

    private static void sendActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
