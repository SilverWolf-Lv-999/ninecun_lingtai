package net.seraphina.ninecun_lingtai.common.soul;

import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.seraphina.ninecun_lingtai.common.karma.KarmaUtility;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncSoulSensePacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulSenseData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.seraphina.ninecun_lingtai.common.registry.ModBlocks;
import net.seraphina.ninecun_lingtai.common.spell.soul.SoulSpellUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.seraphina.ninecun_lingtai.utility.SoulSenseUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SoulSenseManager {
    private static final int MAX_BLOCK_RESULTS = 96;
    /** 反震魂伤冷却（tick）：冷却内再次反震只强制收回、不重复扣伤（设计文档 R4）。 */
    private static final int RECOIL_INJURY_COOLDOWN_TICKS = 100;
    /** 激活玩家 UUID -> 激活时的 tickCount，用于对齐扣费/扫描相位，避免首秒双扣。 */
    private static final Map<UUID, Integer> ACTIVE_PLAYERS = new HashMap<>();
    private static final Map<UUID, BlockScanCache> BLOCK_SCAN_CACHES = new HashMap<>();
    /** 会话级：玩家 UUID -> 上次反震扣伤的 gameTime。 */
    private static final Map<UUID, Long> LAST_RECOIL_TICKS = new HashMap<>();
    /** 服务端方块变更计数，方块放置/破坏时递增，用于判定扫描缓存是否仍然有效。 */
    private static long blockChangeCounter;

    private record EntityScanResult(List<SyncSoulSensePacket.EntityEntry> entries, boolean backlash) {
    }

    private record BlockScanCache(
            long gameTime,
            long blockChangeCount,
            int filterMask,
            ResourceKey<Level> dimension,
            SectionPos section,
            List<SyncSoulSensePacket.BlockEntry> entries
    ) {
    }

    public static boolean isActive(ServerPlayer player) {
        return player != null && ACTIVE_PLAYERS.containsKey(player.getUUID());
    }

    public static void setActive(ServerPlayer player, boolean active) {
        if (!active) {
            deactivate(player, null);
            return;
        }
        if (isActive(player)) {
            return;
        }
        if (!SoulSenseUtility.canUseSoulSense(player)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_sense_no_aptitude"),
                    true
            );
            sendInactive(player);
            return;
        }
        if (PlayerSpiritualEnergyData.consumeEnergyAny(
                player,
                SoulSenseUtility.getEnergyCostPerSecond(player)
        ) == null) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.soul_sense_no_energy"),
                    true
            );
            sendInactive(player);
            return;
        }
        ACTIVE_PLAYERS.put(player.getUUID(), player.tickCount);
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS,
                0.7F,
                1.4F
        );
        scanAndSync(player);
    }

    public static void deactivate(ServerPlayer player, Component message) {
        if (player == null || ACTIVE_PLAYERS.remove(player.getUUID()) == null) {
            return;
        }
        if (message != null) {
            player.displayClientMessage(message, true);
        }
        sendInactive(player);
    }

    public static void refresh(ServerPlayer player) {
        if (isActive(player)) {
            scanAndSync(player);
        }
    }

    /** 把玩家当前的神识状态（含过滤 mask）补发给客户端，用于登录时同步。 */
    public static void syncToClient(ServerPlayer player) {
        if (isActive(player)) {
            scanAndSync(player);
        } else {
            sendInactive(player);
        }
    }

    /** 服务端任意方块放置/破坏时调用，令方块扫描缓存失效。 */
    public static void onBlockChanged() {
        blockChangeCounter++;
    }

    public static void tick(ServerPlayer player) {
        Integer activationTick = player == null ? null : ACTIVE_PLAYERS.get(player.getUUID());
        if (activationTick == null) {
            return;
        }
        if (!player.isAlive() || player.isSpectator()) {
            deactivate(player, null);
            return;
        }
        // 相位对齐激活 tick：激活当刻已在 setActive 扣过首秒费用，这里跳过，避免双扣
        int ticksSinceActivation = player.tickCount - activationTick;
        if (ticksSinceActivation <= 0) {
            return;
        }
        if (ticksSinceActivation % 20 == 0
                && PlayerSpiritualEnergyData.consumeEnergyAny(
                        player,
                        SoulSenseUtility.getEnergyCostPerSecond(player)
                ) == null) {
            deactivate(
                    player,
                    Component.translatable("message.ninecun_lingtai.soul_sense_exhausted")
            );
            return;
        }
        if (ticksSinceActivation % CommonConfig.SOUL_SENSE_SCAN_INTERVAL_TICKS.get() == 0) {
            scanAndSync(player);
        }
    }

    public static void remove(ServerPlayer player) {
        if (player != null) {
            ACTIVE_PLAYERS.remove(player.getUUID());
            BLOCK_SCAN_CACHES.remove(player.getUUID());
            LAST_RECOIL_TICKS.remove(player.getUUID());
        }
    }

    public static void clear() {
        ACTIVE_PLAYERS.clear();
        BLOCK_SCAN_CACHES.clear();
        LAST_RECOIL_TICKS.clear();
    }

    private static void scanAndSync(ServerPlayer player) {
        int filterMask = PlayerSoulSenseData.getFilterMask(player);
        EntityScanResult entityResult = scanEntities(player, filterMask);
        if (entityResult.backlash()) {
            applyBacklash(player);
            return;
        }
        List<SyncSoulSensePacket.BlockEntry> blocks = scanBlocksCached(player, filterMask);
        ModNetwork.sendSoulSense(player, true, filterMask, entityResult.entries(), blocks);
    }

    /** 神识反震：对探测者扣魂伤（带冷却）并强制收回神识。 */
    private static void applyBacklash(ServerPlayer player) {
        long gameTime = player.serverLevel().getGameTime();
        Long lastRecoilTick = LAST_RECOIL_TICKS.get(player.getUUID());
        // 时间倒拨（gameTime < lastRecoilTick）视为冷却已过，重新记点
        if (lastRecoilTick == null
                || gameTime < lastRecoilTick
                || gameTime - lastRecoilTick >= RECOIL_INJURY_COOLDOWN_TICKS) {
            LAST_RECOIL_TICKS.put(player.getUUID(), gameTime);
            SoulInjuryUtility.addInjury(
                    player,
                    CommonConfig.SOUL_INJURY_SOURCE_SENSE_RECOIL.get(),
                    SoulInjuryUtility.InjurySource.SENSE_RECOIL
            );
        }
        deactivate(
                player,
                Component.translatable("message.ninecun_lingtai.soul_sense_backlash")
        );
    }

    private static void sendInactive(ServerPlayer player) {
        ModNetwork.sendSoulSense(
                player,
                false,
                PlayerSoulSenseData.getFilterMask(player),
                List.of(),
                List.of()
        );
    }

    private static EntityScanResult scanEntities(ServerPlayer player, int filterMask) {
        double range = SoulSenseUtility.getRange(player);
        if (range <= 0.0D) {
            return new EntityScanResult(List.of(), false);
        }
        int scannerStars = SoulSpellUtility.getCasterStars(player);
        List<SyncSoulSensePacket.EntityEntry> entries = new ArrayList<>();
        double rangeSquared = range * range;
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(range)
        )) {
            if (entity == player
                    || !entity.isAlive()
                    || entity.isSpectator()
                    || player.distanceToSqr(entity) > rangeSquared) {
                continue;
            }
            int targetStars = SoulSpellUtility.getTargetStars(entity);
            if (targetStars >= scannerStars + CommonConfig.SOUL_SENSE_BACKLASH_STAR_GAP.get()) {
                // 神识扫过高出两星及以上的存在，立即反震（不受显示过滤影响）
                return new EntityScanResult(List.of(), true);
            }
            if (targetStars >= scannerStars) {
                // 高手匿踪：魂强不低于探测者的目标不会被标记
                continue;
            }
            SoulSenseTargetCategory category = classifyEntity(entity);
            if ((filterMask & category.getMaskBit()) != 0) {
                entries.add(new SyncSoulSensePacket.EntityEntry(
                        entity.getId(),
                        (byte) category.ordinal()
                ));
            }
        }
        return new EntityScanResult(entries, false);
    }

    private static SoulSenseTargetCategory classifyEntity(LivingEntity entity) {
        if (entity instanceof Player targetPlayer) {
            // 道心杀孽 §6.3：高杀孽玩家（有效杀孽 ≥ 染血档阈值）呈血色标记，
            // 血色是覆盖性信息——看见煞气就顾不上分辨是玩家还是修士。
            return KarmaUtility.isBloodMarked(targetPlayer)
                    ? SoulSenseTargetCategory.BLOOD_MARKED
                    : SoulSenseTargetCategory.PLAYER;
        }
        if (CreatureCultivationData.getStage(entity).isPresent()) {
            // NPC 修士的血色标记留接口：CreatureCultivationData 侧可存 demonic
            // 标记，魔修生物天生血色（宗门坊市日志排期）。
            return SoulSenseTargetCategory.CULTIVATOR;
        }
        if (entity instanceof Enemy) {
            return SoulSenseTargetCategory.HOSTILE;
        }
        if (entity instanceof AbstractVillager) {
            return SoulSenseTargetCategory.VILLAGER;
        }
        return SoulSenseTargetCategory.ANIMAL;
    }

    private static List<SyncSoulSensePacket.BlockEntry> scanBlocksCached(ServerPlayer player, int filterMask) {
        long gameTime = player.serverLevel().getGameTime();
        ResourceKey<Level> dimension = player.serverLevel().dimension();
        SectionPos section = SectionPos.of(player.blockPosition());
        BlockScanCache cache = BLOCK_SCAN_CACHES.get(player.getUUID());
        if (cache != null && isCacheValid(cache, gameTime, dimension, section, filterMask)) {
            return cache.entries();
        }
        List<SyncSoulSensePacket.BlockEntry> entries = scanBlocks(player, filterMask);
        BLOCK_SCAN_CACHES.put(player.getUUID(), new BlockScanCache(
                gameTime,
                blockChangeCounter,
                filterMask,
                dimension,
                section,
                entries
        ));
        return entries;
    }

    private static boolean isCacheValid(
            BlockScanCache cache,
            long gameTime,
            ResourceKey<Level> dimension,
            SectionPos section,
            int filterMask
    ) {
        if (cache.filterMask() != filterMask
                || cache.dimension() != dimension
                || !cache.section().equals(section)) {
            return false;
        }
        long age = gameTime - cache.gameTime();
        if (age < 0 || age >= CommonConfig.SOUL_SENSE_BLOCK_CACHE_MAX_AGE_TICKS.get()) {
            return false;
        }
        // 节流间隔内直接复用；超过间隔后只要世界方块未发生变化仍复用，直到兜底失效
        return age < CommonConfig.SOUL_SENSE_BLOCK_SCAN_INTERVAL_TICKS.get()
                || cache.blockChangeCount() == blockChangeCounter;
    }

    private static List<SyncSoulSensePacket.BlockEntry> scanBlocks(ServerPlayer player, int filterMask) {
        boolean wantsOres = (filterMask & SoulSenseTargetCategory.ORE.getMaskBit()) != 0;
        boolean wantsSpiritStones = (filterMask & SoulSenseTargetCategory.SPIRIT_STONE.getMaskBit()) != 0;
        boolean wantsContainers = (filterMask & SoulSenseTargetCategory.CONTAINER.getMaskBit()) != 0;
        if (!wantsOres && !wantsSpiritStones && !wantsContainers) {
            return List.of();
        }
        int blockRange = (int) Math.floor(SoulSenseUtility.getBlockRange(player));
        if (blockRange <= 0) {
            return List.of();
        }
        ServerLevel level = player.serverLevel();
        BlockPos center = player.blockPosition();
        int minY = Math.max(level.getMinBuildHeight(), center.getY() - blockRange);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, center.getY() + blockRange);
        List<SyncSoulSensePacket.BlockEntry> entries = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = center.getX() - blockRange; x <= center.getX() + blockRange; x++) {
            for (int z = center.getZ() - blockRange; z <= center.getZ() + blockRange; z++) {
                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    SoulSenseTargetCategory category = classifyBlock(level, cursor, state);
                    if (category == null || (filterMask & category.getMaskBit()) == 0) {
                        continue;
                    }
                    entries.add(new SyncSoulSensePacket.BlockEntry(
                            cursor.immutable(),
                            (byte) category.ordinal()
                    ));
                    if (entries.size() >= MAX_BLOCK_RESULTS) {
                        return entries;
                    }
                }
            }
        }
        return entries;
    }

    private static SoulSenseTargetCategory classifyBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.SPIRITUAL_STONE_ORE.get())
                || state.is(ModBlocks.DEEPSLATE_SPIRITUAL_STONE_ORE.get())) {
            return SoulSenseTargetCategory.SPIRIT_STONE;
        }
        if (state.is(Tags.Blocks.ORES)) {
            return SoulSenseTargetCategory.ORE;
        }
        if (state.hasBlockEntity() && level.getBlockEntity(pos) instanceof Container container) {
            // 隐匿禁（M4）：满隐容器对神识隐形，判定收在 seal 侧
            if (net.seraphina.ninecun_lingtai.common.seal.SealConcealUtility
                    .isContainerFullyConcealed(level, container)) {
                return null;
            }
            return SoulSenseTargetCategory.CONTAINER;
        }
        return null;
    }
}
