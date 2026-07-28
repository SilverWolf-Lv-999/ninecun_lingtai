package com.seraphina.ninecun_lingtai.common.tribulation;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.entity.tribulation.ThunderTribulationEntity;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import com.seraphina.ninecun_lingtai.utility.NotificationUtility;
import com.seraphina.ninecun_lingtai.utility.TribulationUtility;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TribulationManager {
    private static final Map<UUID, TribulationSession> SESSIONS = new HashMap<>();

    private TribulationManager() {
    }

    public static StartResult request(ServerPlayer player) {
        if (isInTribulation(player)) {
            return StartResult.ALREADY_ACTIVE;
        }
        if (!isGoldenCoreTribulationRequired(player)) {
            return StartResult.NOT_REQUIRED;
        }
        Optional<SpiritualRoot> spiritualRoot = PlayerAptitudeData.getSpiritualRoot(player);
        Optional<SoulStrength> soulStrength = PlayerAptitudeData.getSoulStrength(player);
        if (spiritualRoot.isEmpty() || soulStrength.isEmpty()) {
            return StartResult.NO_APTITUDE;
        }
        CultivationStage targetStage = new CultivationStage(CultivationRealm.GOLDEN_CORE, 1);
        if (!PlayerCultivationManager.hasEnoughEnergyForTarget(player, targetStage)) {
            return StartResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }

        TribulationType type = TribulationUtility.selectType(spiritualRoot.get(), soulStrength.get());
        float aptitudeScore = TribulationUtility.getAptitudeScore(spiritualRoot.get(), soulStrength.get());
        TribulationSession session = new TribulationSession(
                player.getUUID(),
                type,
                player.position(),
                player.level().dimension(),
                aptitudeScore,
                TribulationUtility.getStrikeRadius(aptitudeScore),
                CommonConfig.TRIBULATION_PREPARATION_TICKS.get()
        );
        SESSIONS.put(player.getUUID(), session);
        if (!PlayerMeditationManager.startTribulationMeditating(player)) {
            SESSIONS.remove(player.getUUID());
            return StartResult.CANNOT_MEDITATE;
        }
        if (!PlayerCultivationManager.consumeEnergyForTarget(player, targetStage)) {
            SESSIONS.remove(player.getUUID());
            PlayerMeditationManager.finishTribulationMeditating(player);
            return StartResult.INSUFFICIENT_SPIRITUAL_ENERGY;
        }
        NotificationUtility.send(
                player,
                Component.literal("天劫将至·" + type.chineseName()),
                Component.literal(
                        type.ancientDescription()
                                + "　雷云酝酿十息，劫数共 " + type.strikeCount() + " 道。"
                ),
                20 * 9
        );
        ModNetwork.sendTribulationState(player, session);
        return StartResult.STARTED;
    }

    /** Only Foundation Establishment layer nine crossing into Golden Core starts a tribulation. */
    public static boolean isGoldenCoreTribulationRequired(ServerPlayer player) {
        return PlayerCultivationData.getStage(player)
                .map(stage -> stage.realm() == CultivationRealm.FOUNDATION_ESTABLISHMENT
                        && stage.layer() == CultivationStage.MAXIMUM_LAYER)
                .orElse(false);
    }

    public static boolean isInTribulation(ServerPlayer player) {
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    public static boolean isInTribulation(net.minecraft.world.entity.player.Player player) {
        return player != null && SESSIONS.containsKey(player.getUUID());
    }

    public static Optional<TribulationSession> getSession(ServerPlayer player) {
        return Optional.ofNullable(SESSIONS.get(player.getUUID()));
    }

    public static void tick(ServerPlayer player) {
        TribulationSession session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (!player.isAlive()
                || player.isSpectator()
                || !player.level().dimension().equals(session.dimension())) {
            fail(player);
            return;
        }
        if (player.distanceToSqr(session.center()) > 1.0D) {
            player.teleportTo(session.center().x, session.center().y, session.center().z);
        }
        player.setDeltaMovement(Vec3.ZERO);
        if (!PlayerMeditationManager.isMeditating(player)) {
            PlayerMeditationManager.startTribulationMeditating(player);
        }

        switch (session.phase()) {
            case PREPARING -> tickPreparing(player, session);
            case STRIKING -> tickStriking(player, session);
            case AFTERMATH -> tickAftermath(player, session);
            case DISSIPATING -> tickDissipating(player, session);
        }
        if (player.tickCount % 10 == 0 && SESSIONS.get(player.getUUID()) == session) {
            ModNetwork.sendTribulationState(player, session);
        }
    }

    public static void fail(ServerPlayer player) {
        TribulationSession session = SESSIONS.remove(player.getUUID());
        if (session == null) {
            return;
        }
        PlayerMeditationManager.finishTribulationMeditating(player);
        NotificationUtility.send(
                player,
                Component.literal("天劫未渡"),
                Component.literal("道基震荡，金丹未成；请重新凝神，再寻破境之机。"),
                20 * 5
        );
        ModNetwork.sendTribulationState(player, session, false);
    }

    public static void syncToTracking(ServerPlayer recipient, ServerPlayer subject) {
        TribulationSession session = SESSIONS.get(subject.getUUID());
        if (session != null) {
            ModNetwork.sendTribulationStateTo(recipient, session);
        }
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private static void tickPreparing(ServerPlayer player, TribulationSession session) {
        ServerLevel level = player.serverLevel();
        if (player.tickCount % 40 == 0) {
            level.playSound(
                    null,
                    session.center().x,
                    session.center().y,
                    session.center().z,
                    SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER,
                    0.55F,
                    1.35F + level.random.nextFloat() * 0.25F
            );
        }
        session.tickPhase();
        if (session.phaseRemainingTicks() <= 0) {
            session.startStriking(1);
            spawnStrike(player, session);
        }
    }

    private static void tickStriking(ServerPlayer player, TribulationSession session) {
        session.tickPhase();
        if (session.phaseRemainingTicks() > 0) {
            return;
        }
        if (session.completedStrikes() >= session.type().strikeCount()) {
            session.startAftermath(
                    CommonConfig.TRIBULATION_LIGHTNING_LIFETIME_TICKS.get()
                            + CommonConfig.TRIBULATION_PROMOTION_DELAY_TICKS.get()
            );
            return;
        }
        spawnStrike(player, session);
    }

    private static void tickAftermath(ServerPlayer player, TribulationSession session) {
        session.tickPhase();
        if (session.phaseRemainingTicks() > 0) {
            return;
        }
        PlayerCultivationData.setStage(player, new CultivationStage(CultivationRealm.GOLDEN_CORE, 1));
        PlayerDefenseResourceData.refillToMaximum(player);
        NotificationUtility.send(
                player,
                Component.literal("金丹已成"),
                Component.literal("五行归炉，金丹初结；雷云将散，仙途自此不同。"),
                20 * 6
        );
        ModNetwork.sendCultivationInfo(player);
        ModNetwork.sendDefenseResource(player);
        session.startDissipating(CommonConfig.TRIBULATION_DISSIPATION_TICKS.get());
        PlayerMeditationManager.finishTribulationMeditating(player);
    }

    private static void tickDissipating(ServerPlayer player, TribulationSession session) {
        session.tickPhase();
        if (session.phaseRemainingTicks() > 0) {
            return;
        }
        SESSIONS.remove(player.getUUID());
        ModNetwork.sendTribulationState(player, session, false);
    }

    private static void spawnStrike(ServerPlayer player, TribulationSession session) {
        ServerLevel level = player.serverLevel();
        int strikeIndex = session.completedStrikes() + 1;
        RandomSource random = level.random;
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = Math.sqrt(random.nextDouble()) * session.radius();
        double x = session.center().x + Mth.cos((float) angle) * distance;
        double z = session.center().z + Mth.sin((float) angle) * distance;
        BlockPos groundPosition = level.getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                BlockPos.containing(x, session.center().y, z)
        );
        ThunderTribulationEntity lightning = new ThunderTribulationEntity(
                com.seraphina.ninecun_lingtai.common.registry.ModEntityTypes.THUNDER_TRIBULATION.get(),
                level
        );
        lightning.initialize(
                player,
                strikeIndex,
                TribulationUtility.getStrikeDamage(session.type(), strikeIndex, session.aptitudeScore()),
                TribulationUtility.getVisualIntensity(session.type(), strikeIndex, session.aptitudeScore())
        );
        lightning.moveTo(x, groundPosition.getY(), z, 0.0F, 0.0F);
        if (level.addFreshEntity(lightning)) {
            session.completeStrike();
            level.playSound(
                    null,
                    x,
                    groundPosition.getY(),
                    z,
                    SoundEvents.LIGHTNING_BOLT_IMPACT,
                    SoundSource.WEATHER,
                    1.2F,
                    0.72F + random.nextFloat() * 0.18F
            );
            level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    x,
                    groundPosition.getY() + 1.0D,
                    z,
                    32,
                    1.6D,
                    1.8D,
                    1.6D,
                    0.18D
            );
        }
        if (session.completedStrikes() >= session.type().strikeCount()) {
            session.startAftermath(
                    CommonConfig.TRIBULATION_LIGHTNING_LIFETIME_TICKS.get()
                            + CommonConfig.TRIBULATION_PROMOTION_DELAY_TICKS.get()
            );
        } else {
            session.startStriking(CommonConfig.TRIBULATION_STRIKE_INTERVAL_TICKS.get());
        }
        NotificationUtility.send(
                player,
                Component.literal(session.type().chineseName() + "·第" + strikeIndex + "道"),
                Component.literal("雷威递增，守住神魂与道心。"),
                20 * 2
        );
        ModNetwork.sendTribulationState(player, session);
    }

    public enum StartResult {
        STARTED,
        ALREADY_ACTIVE,
        NOT_REQUIRED,
        NO_APTITUDE,
        INSUFFICIENT_SPIRITUAL_ENERGY,
        CANNOT_MEDITATE
    }
}
