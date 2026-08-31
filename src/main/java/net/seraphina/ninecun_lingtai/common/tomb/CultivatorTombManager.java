package net.seraphina.ninecun_lingtai.common.tomb;

import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import net.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import net.seraphina.ninecun_lingtai.common.registry.ModTombContent;
import net.seraphina.ninecun_lingtai.common.tomb.CultivatorTombSavedData.TombState;
import net.seraphina.ninecun_lingtai.common.tomb.CultivatorTombUtility.TombPlacement;
import net.seraphina.ninecun_lingtai.utility.CreatureCultivationUtility;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * 墓主三态机（M4 墓穴切片）：沉睡 → 警告 → 破棺追杀。
 *
 * <ul>
 *   <li><b>沉睡：</b>默认态。墓主实体尚未生成——"不 tick AI"以最彻底的方式成立；
 *       墓穴是否存在由棺下强化深板岩签名块验证（未接线/旧区块自然跳过）。</li>
 *   <li><b>警告（外圈）：</b>玩家踏入警戒半径：全服警告播报（按墓冷却）+ 威压体感
 *       （复用 {@code SyncPressureStatePacket} 后处理通道，短脉冲）+ 魂伤 +5
 *       （经 {@code SoulInjuryUtility}，来源 {@code TOMB_WARNING}，按人按墓冷却）。</li>
 *   <li><b>破棺追杀（内圈或动棺椁）：</b>玩家踏入唤醒半径或破坏主室方块：棺椁爆裂，
 *       以 {@code CreatureCultivationData} 为底座生成有境界的墓主（尸修·Husk 载体），
 *       复用境界生物赋能链；追杀范围有上限，超出即瞬移回巢并满血重置。</li>
 *   <li><b>神识抹杀：</b>追杀目标比墓主低两个大境界及以上时，不再近身缠斗，
 *       周期性以 {@code realm_execution} 伤害类型远程处决（护体法术可挡，给准备者留活路）。</li>
 * </ul>
 */
public final class CultivatorTombManager {
    /** 外圈警戒半径。建议配置键 tomb.warningRadius。 */
    private static final double WARNING_RADIUS = 32.0D;
    /** 内圈唤醒半径。建议配置键 tomb.awakenRadius。 */
    private static final double AWAKEN_RADIUS = 10.0D;
    /** 追杀范围上限（离巢距离）。建议配置键 tomb.pursuitRange。 */
    private static final double PURSUIT_RANGE = 48.0D;
    /** 神识处决触发的大境界差。建议配置键 tomb.executionRealmGap。 */
    private static final int EXECUTION_REALM_GAP = 2;
    /** 神识处决射程。建议配置键 tomb.executionRange。 */
    private static final double EXECUTION_RANGE = 16.0D;
    /** 神识处决伤害（realm_execution 类型，护体法术可挡）。建议配置键 tomb.executionDamage。 */
    private static final float EXECUTION_DAMAGE = 500.0F;
    /** 处决间隔（tick）。建议配置键 tomb.executionIntervalTicks。 */
    private static final long EXECUTION_INTERVAL_TICKS = 60L;
    /** 警告魂伤。建议配置键 tomb.warningSoulInjury。 */
    private static final double WARNING_SOUL_INJURY = 5.0D;
    /** 同一玩家对同一墓的警告冷却（tick）。建议配置键 tomb.warningPlayerCooldownTicks。 */
    private static final long WARNING_PLAYER_COOLDOWN_TICKS = 6000L;
    /** 同一墓的全服播报冷却（tick）。建议配置键 tomb.warningBroadcastCooldownTicks。 */
    private static final long WARNING_BROADCAST_COOLDOWN_TICKS = 6000L;
    /** 威压体感脉冲时长（tick）。 */
    private static final long PRESSURE_PULSE_TICKS = 80L;

    private static final String TOMB_MASTER_TAG = "ninecun_lingtai:tomb_master";
    private static final String CELL_X_TAG = "CellX";
    private static final String CELL_Z_TAG = "CellZ";
    private static final String LAST_EXECUTION_TAG = "LastExecutionTick";

    private static final Map<Long, Long> TOMB_BROADCAST_COOLDOWN = new HashMap<>();
    private static final Map<Long, Map<UUID, Long>> TOMB_PLAYER_WARN_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Long> PENDING_PRESSURE_OFF = new HashMap<>();

    private CultivatorTombManager() {
    }

    /** 服务端主 tick 驱动（事件处理器每 10 tick 调一次）。 */
    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        long seed = overworld.getSeed();
        long gameTime = overworld.getGameTime();
        tickPressurePulses(server, gameTime);

        CultivatorTombSavedData savedData = CultivatorTombSavedData.get(overworld);
        Set<Long> upkeptTombs = new HashSet<>();
        for (ServerPlayer player : overworld.players()) {
            if (player.isSpectator()) {
                continue;
            }
            Optional<TombPlacement> tombOptional = CultivatorTombUtility.findTombNear(
                    seed,
                    player.getBlockX(),
                    player.getBlockZ()
            );
            if (tombOptional.isEmpty()) {
                continue;
            }
            TombPlacement tomb = tombOptional.get();
            BlockPos home = tomb.getHomePos();
            double distanceSq = player.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
            if (distanceSq > WARNING_RADIUS * WARNING_RADIUS) {
                continue;
            }
            if (!isTombGenerated(overworld, tomb)) {
                continue;
            }
            TombState state = savedData.getState(tomb.cellKey());
            if (state == TombState.DEFEATED) {
                continue;
            }
            if (state == TombState.DORMANT) {
                if (player.isCreative()) {
                    continue;
                }
                if (distanceSq <= AWAKEN_RADIUS * AWAKEN_RADIUS) {
                    awaken(overworld, savedData, tomb, player);
                } else {
                    warn(overworld, tomb, player, gameTime);
                }
            } else if (upkeptTombs.add(tomb.cellKey())) {
                upkeepMaster(overworld, tomb, gameTime);
            }
        }
    }

    /** 主室方块被破坏（"动棺椁"）：沉睡墓直接唤醒，破坏者即追杀目标。 */
    public static void onChamberBlockBroken(ServerLevel level, BlockPos pos, ServerPlayer breaker) {
        if (!level.dimension().equals(Level.OVERWORLD) || pos.getY() > -8) {
            return;
        }
        Optional<TombPlacement> tombOptional = CultivatorTombUtility.findTombNear(
                level.getSeed(),
                pos.getX(),
                pos.getZ()
        );
        if (tombOptional.isEmpty() || !tombOptional.get().isInsideChamber(pos)) {
            return;
        }
        TombPlacement tomb = tombOptional.get();
        CultivatorTombSavedData savedData = CultivatorTombSavedData.get(level);
        if (savedData.getState(tomb.cellKey()) != TombState.DORMANT) {
            return;
        }
        // BreakEvent 在方块移除前触发，签名块本身被挖时此校验仍然成立、照样唤醒。
        if (!isTombGenerated(level, tomb)) {
            return;
        }
        awaken(level, savedData, tomb, breaker);
    }

    /** 墓主陨落：标记 DEFEATED、掉落墓主残魂、全服播报。 */
    public static void onMasterDeath(ServerLevel level, Mob master) {
        CompoundTag masterTag = getTombMasterTag(master);
        if (masterTag == null) {
            return;
        }
        int cellX = masterTag.getInt(CELL_X_TAG);
        int cellZ = masterTag.getInt(CELL_Z_TAG);
        long cellKey = ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL);
        CultivatorTombSavedData.get(level).setState(cellKey, TombState.DEFEATED);

        Optional<TombPlacement> tombOptional = CultivatorTombUtility.getTombForCell(level.getSeed(), cellX, cellZ);
        TombTier tier = tombOptional.map(TombPlacement::tier).orElse(TombTier.FOUNDATION);
        if (ModTombContent.TOMB_MASTER_REMNANT_SOUL.isPresent()) {
            int count = tier.isHighTier() ? 2 : 1;
            master.spawnAtLocation(new ItemStack(ModTombContent.TOMB_MASTER_REMNANT_SOUL.get(), count));
        }
        String slayerName = master.getLastHurtByMob() instanceof Player slayer
                ? slayer.getName().getString()
                : "无名修士";
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.tomb_defeated_broadcast",
                        "%s 讨灭了%s境墓主，一缕残魂自尸身逸出，上古墓穴就此长眠",
                        slayerName,
                        tier.getRealm().getChineseName()
                ),
                false
        );
    }

    public static boolean isTombMaster(LivingEntity entity) {
        return getTombMasterTag(entity) != null;
    }

    public static void clearAll() {
        TOMB_BROADCAST_COOLDOWN.clear();
        TOMB_PLAYER_WARN_COOLDOWN.clear();
        PENDING_PRESSURE_OFF.clear();
    }

    // ==== 沉睡 → 警告 ====

    private static void warn(ServerLevel level, TombPlacement tomb, ServerPlayer player, long gameTime) {
        Map<UUID, Long> warnedPlayers = TOMB_PLAYER_WARN_COOLDOWN.computeIfAbsent(
                tomb.cellKey(),
                ignored -> new HashMap<>()
        );
        Long nextAllowed = warnedPlayers.get(player.getUUID());
        if (nextAllowed != null && gameTime < nextAllowed) {
            return;
        }
        warnedPlayers.put(player.getUUID(), gameTime + WARNING_PLAYER_COOLDOWN_TICKS);

        SoulInjuryUtility.addInjury(player, WARNING_SOUL_INJURY, SoulInjuryUtility.InjurySource.TOMB_WARNING);
        ModNetwork.sendPressureState(player, true, tomb.tier().getPressureSeverity(), false);
        PENDING_PRESSURE_OFF.put(player.getUUID(), gameTime + PRESSURE_PULSE_TICKS);
        player.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.tomb_warning_self",
                        "冢中传来一声若有若无的冷哼，苍老威压攫住心神，神魂一阵刺痛"
                ),
                false
        );
        player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.6F);

        Long nextBroadcast = TOMB_BROADCAST_COOLDOWN.get(tomb.cellKey());
        if (nextBroadcast == null || gameTime >= nextBroadcast) {
            TOMB_BROADCAST_COOLDOWN.put(tomb.cellKey(), gameTime + WARNING_BROADCAST_COOLDOWN_TICKS);
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.tomb_warning_broadcast",
                            "%s 惊扰了一座上古修士墓穴，九幽之下传来含怒的苏醒之息……",
                            player.getName().getString()
                    ),
                    false
            );
        }
    }

    private static void tickPressurePulses(MinecraftServer server, long gameTime) {
        Iterator<Map.Entry<UUID, Long>> iterator = PENDING_PRESSURE_OFF.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (gameTime < entry.getValue()) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                ModNetwork.sendPressureState(player, false, 0.0F, false);
            }
        }
    }

    // ==== 警告 → 破棺追杀 ====

    private static void awaken(
            ServerLevel level,
            CultivatorTombSavedData savedData,
            TombPlacement tomb,
            ServerPlayer intruder
    ) {
        savedData.setState(tomb.cellKey(), TombState.AWAKENED);
        burstCoffin(level, tomb);
        Mob master = spawnMaster(level, tomb);
        if (master != null) {
            master.setTarget(intruder);
        }
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.tomb_awaken_broadcast",
                        "轰——%s 惊动了墓中之主，%s境墓主破棺而出，誓诛盗墓之人！",
                        intruder.getName().getString(),
                        tomb.tier().getRealm().getChineseName()
                ),
                false
        );
    }

    private static void burstCoffin(ServerLevel level, TombPlacement tomb) {
        for (BlockPos coffinPos : tomb.getCoffinBlocks()) {
            BlockState state = level.getBlockState(coffinPos);
            if (!state.isAir()) {
                level.levelEvent(2001, coffinPos, Block.getId(state));
                level.setBlock(coffinPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        BlockPos home = tomb.getHomePos();
        level.playSound(null, home, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, home, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    private static Mob spawnMaster(ServerLevel level, TombPlacement tomb) {
        Husk master = EntityType.HUSK.create(level);
        if (master == null) {
            return null;
        }
        BlockPos home = tomb.getHomePos();
        master.moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 0.0F, 0.0F);

        // 墓主底座：CreatureCultivationData 挂境界 + 灵根，复用境界生物赋能链。
        CreatureCultivationData.markSpawnRollDone(master);
        CultivationStage stage = new CultivationStage(tomb.tier().getRealm(), tomb.masterLayer());
        CreatureCultivationData.setStage(master, stage);
        CreatureCultivationData.setSpiritualRoot(
                master,
                CreatureCultivationUtility.rollSpawnSpiritualRoot(master.getRandom())
        );
        CultivationDefenseUtility.syncMaxHealthModifier(master);
        applyMasterAttributes(master, tomb.tier());
        master.setHealth(master.getMaxHealth());

        master.setItemSlot(
                EquipmentSlot.MAINHAND,
                new ItemStack(tomb.tier().isHighTier() ? Items.DIAMOND_SWORD : Items.IRON_SWORD)
        );
        master.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        master.setCustomName(Component.translatableWithFallback(
                "entity.ninecun_lingtai.tomb_master",
                "墓主·%s老祖",
                tomb.tier().getRealm().getChineseName()
        ));
        master.setCustomNameVisible(true);
        master.setPersistenceRequired();

        CompoundTag masterTag = new CompoundTag();
        masterTag.putInt(CELL_X_TAG, tomb.cellX());
        masterTag.putInt(CELL_Z_TAG, tomb.cellZ());
        master.getPersistentData().put(TOMB_MASTER_TAG, masterTag);

        level.addFreshEntity(master);
        return master;
    }

    private static void applyMasterAttributes(Mob master, TombTier tier) {
        int realmOrdinal = tier.getRealm().ordinal();
        setBaseValue(master.getAttribute(Attributes.ATTACK_DAMAGE), 4.0D + 2.5D * realmOrdinal);
        setBaseValue(master.getAttribute(Attributes.MOVEMENT_SPEED), 0.23D + 0.02D * realmOrdinal);
        setBaseValue(master.getAttribute(Attributes.KNOCKBACK_RESISTANCE), 1.0D);
        setBaseValue(master.getAttribute(Attributes.FOLLOW_RANGE), PURSUIT_RANGE);
    }

    private static void setBaseValue(AttributeInstance attribute, double value) {
        if (attribute != null) {
            attribute.setBaseValue(value);
        }
    }

    // ==== 追杀期维护：回巢上限 + 神识抹杀 ====

    private static void upkeepMaster(ServerLevel level, TombPlacement tomb, long gameTime) {
        BlockPos home = tomb.getHomePos();
        List<Mob> masters = level.getEntitiesOfClass(
                Mob.class,
                new AABB(home).inflate(PURSUIT_RANGE + 24.0D),
                mob -> isMasterOfTomb(mob, tomb)
        );
        if (masters.isEmpty()) {
            return;
        }
        Mob master = masters.get(0);
        Vec3 homeCenter = new Vec3(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (master.position().distanceToSqr(homeCenter) > PURSUIT_RANGE * PURSUIT_RANGE) {
            // 超出追杀范围上限：瞬移回巢并满血重置，杜绝放风筝遛尸。
            master.teleportTo(homeCenter.x(), homeCenter.y(), homeCenter.z());
            master.setTarget(null);
            master.getNavigation().stop();
            master.setHealth(master.getMaxHealth());
            return;
        }
        if (master.getTarget() instanceof ServerPlayer victim) {
            tryExecuteBySoulSense(level, master, victim, tomb, gameTime);
        }
    }

    /** 神识抹杀：目标低于墓主两大境界及以上时，改用 realm_execution 远程处决。 */
    private static void tryExecuteBySoulSense(
            ServerLevel level,
            Mob master,
            ServerPlayer victim,
            TombPlacement tomb,
            long gameTime
    ) {
        int victimRealmOrdinal = PlayerCultivationData.getStage(victim)
                .map(stage -> stage.realm().ordinal())
                .orElse(0);
        if (tomb.tier().getRealm().ordinal() - victimRealmOrdinal < EXECUTION_REALM_GAP) {
            return;
        }
        if (master.distanceToSqr(victim) > EXECUTION_RANGE * EXECUTION_RANGE) {
            return;
        }
        CompoundTag masterTag = getTombMasterTag(master);
        if (masterTag == null) {
            return;
        }
        if (masterTag.contains(LAST_EXECUTION_TAG, Tag.TAG_ANY_NUMERIC)
                && gameTime - masterTag.getLong(LAST_EXECUTION_TAG) < EXECUTION_INTERVAL_TICKS) {
            return;
        }
        masterTag.putLong(LAST_EXECUTION_TAG, gameTime);

        DamageSource executionSource = new DamageSource(
                level.registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageTypes.REALM_EXECUTION),
                master,
                master
        );
        victim.displayClientMessage(
                Component.translatableWithFallback(
                        "message.ninecun_lingtai.tomb_execution",
                        "墓主神识如渊倾落，直取神魂——境界相差悬殊，速速逃离其领地！"
                ),
                false
        );
        victim.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 1.0F, 0.8F);
        victim.hurt(executionSource, EXECUTION_DAMAGE);
    }

    // ==== 私用 ====

    /** 墓穴确已生成的签名：棺下强化深板岩基座。未接线/旧区块（无墓）自然短路。 */
    private static boolean isTombGenerated(ServerLevel level, TombPlacement tomb) {
        BlockPos pedestal = tomb.getPedestalPos();
        return level.isLoaded(pedestal)
                && level.getBlockState(pedestal).is(Blocks.REINFORCED_DEEPSLATE);
    }

    private static boolean isMasterOfTomb(Mob mob, TombPlacement tomb) {
        CompoundTag masterTag = getTombMasterTag(mob);
        return masterTag != null
                && masterTag.getInt(CELL_X_TAG) == tomb.cellX()
                && masterTag.getInt(CELL_Z_TAG) == tomb.cellZ();
    }

    private static CompoundTag getTombMasterTag(LivingEntity entity) {
        CompoundTag persistentData = entity.getPersistentData();
        return persistentData.contains(TOMB_MASTER_TAG, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(TOMB_MASTER_TAG)
                : null;
    }
}
