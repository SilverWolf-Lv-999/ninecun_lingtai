package com.seraphina.ninecun_lingtai.common.flight;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRootApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerLightFlightData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSwordFlightData;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;
import com.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUtility;
import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUnlockUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

import java.util.Optional;

public final class LightFlightManager {
    private static final float BASE_FLYING_SPEED = 0.05F;
    private static final float MAXIMUM_LIGHT_FLYING_SPEED = 1300.0F;
    private static final double AIR_WALKING_SPEED_MULTIPLIER = 4.7D;
    private static final int ENERGY_CONSUMPTION_INTERVAL_TICKS = 20;

    public static void requestSetFlying(ServerPlayer player, boolean flying) {
        if (flying) {
            start(player);
        } else {
            stop(player);
        }
    }

    public static void start(ServerPlayer player) {
        if (PlayerLightFlightData.isFlying(player)) {
            syncState(player);
            return;
        }
        Optional<LightFlightStyle> availableStyle = getAvailableStyle(player);
        if (availableStyle.isEmpty()) {
            syncState(player);
            return;
        }
        LightFlightStyle style = availableStyle.get();
        if (PlayerSwordFlightData.isFlying(player)) {
            SwordFlightManager.dismount(player);
        }
        PlayerLightFlightData.start(player, style);
        applyFlightAbilities(player);
        player.fallDistance = 0.0F;
        playStartSound(player, style);
        syncState(player);
    }

    public static void stop(ServerPlayer player) {
        if (!PlayerLightFlightData.isFlying(player)) {
            syncState(player);
            return;
        }
        LightFlightStyle style = PlayerLightFlightData.getStyle(player);
        PlayerLightFlightData.stop(player);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(BASE_FLYING_SPEED);
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        playStopSound(player, style);
        syncState(player);
    }

    public static void requestBoost(ServerPlayer player) {
        if (!PlayerLightFlightData.isFlying(player)) {
            return;
        }
        if (getFlightStyle(player) != LightFlightStyle.LIGHT) {
            syncState(player);
            return;
        }
        long gameTime = player.level().getGameTime();
        if (!PlayerLightFlightData.canBoost(player, gameTime)) {
            syncState(player);
            return;
        }
        PlayerLightFlightData.setBoost(
                player,
                gameTime + CommonConfig.LIGHT_FLIGHT_BOOST_DURATION_TICKS.get(),
                gameTime + CommonConfig.LIGHT_FLIGHT_BOOST_COOLDOWN_TICKS.get()
        );
        applyFlightAbilities(player);
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS,
                0.8F,
                1.6F
        );
        syncState(player);
    }

    public static void serverTick(ServerPlayer player) {
        if (!PlayerLightFlightData.isFlying(player)) {
            return;
        }
        if (!canUseLightFlight(player)) {
            stop(player);
            return;
        }
        boolean styleChanged = synchronizeStyleWithRealm(player);
        long gameTime = player.level().getGameTime();
        boolean boostExpired = PlayerLightFlightData.getBoostUntilTick(player) > 0L
                && (PlayerLightFlightData.getStyle(player) != LightFlightStyle.LIGHT
                || !PlayerLightFlightData.isBoosting(player, gameTime));
        if (boostExpired) {
            PlayerLightFlightData.clearBoost(player);
        }
        if (!player.isCreative() && player.tickCount % ENERGY_CONSUMPTION_INTERVAL_TICKS == 0) {
            double energyCost = CommonConfig.LIGHT_FLIGHT_ENERGY_COST_PER_SECOND.get();
            if (energyCost > 0.0D) {
                if (PlayerSpiritualEnergyData.consumeEnergyAny(player, energyCost) == null) {
                    stop(player);
                    return;
                }
                ModNetwork.sendCultivationInfo(player);
            }
        }
        if (applyFlightAbilities(player) || boostExpired || styleChanged) {
            syncState(player);
        }
    }

    public static void restore(ServerPlayer player) {
        if (!PlayerLightFlightData.isFlying(player)) {
            syncState(player);
            return;
        }
        if (!canUseLightFlight(player)) {
            stop(player);
            return;
        }
        synchronizeStyleWithRealm(player);
        applyFlightAbilities(player);
        syncState(player);
    }

    public static float getFlightSpeed(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        LightFlightStyle style = getFlightStyle(player);
        double multiplier = style == LightFlightStyle.AIR_WALKING
                ? AIR_WALKING_SPEED_MULTIPLIER
                : CommonConfig.LIGHT_FLIGHT_SPEED_MULTIPLIER.get();
        if (style == LightFlightStyle.LIGHT && PlayerLightFlightData.isBoosting(player, gameTime)) {
            multiplier *= CommonConfig.LIGHT_FLIGHT_BOOST_MULTIPLIER.get();
        }
        return (float) Mth.clamp(
                PlayerValueControlUtility.applyApplication(
                        SwordFlightManager.getDefaultFlightSpeed(player) * multiplier,
                        PlayerValueControlUnlockUtility.getEffectiveApplication(
                                player,
                                PlayerValueControlType.LIGHT_FLIGHT_SPEED
                        )
                ),
                BASE_FLYING_SPEED,
                MAXIMUM_LIGHT_FLYING_SPEED
        );
    }

    public static void refreshFlightSpeed(ServerPlayer player) {
        if (!PlayerLightFlightData.isFlying(player)) {
            return;
        }
        if (applyFlightAbilities(player)) {
            syncState(player);
        }
    }

    public static boolean isBoosting(ServerPlayer player) {
        return getFlightStyle(player) == LightFlightStyle.LIGHT
                && PlayerLightFlightData.isBoosting(player, player.level().getGameTime());
    }

    public static LightFlightStyle getFlightStyle(ServerPlayer player) {
        return getAvailableStyle(player).orElse(PlayerLightFlightData.getStyle(player));
    }

    public static int getLightColor(ServerPlayer player) {
        return SpiritualRootApi.getSpiritualRoot(player)
                .map(spiritualRoot -> SpiritualRootUtility.getSpiritualRootGlowColor(spiritualRoot.elements()))
                .orElse(0xF2F7FF);
    }

    private static boolean applyFlightAbilities(ServerPlayer player) {
        boolean abilitiesChanged = false;
        if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            abilitiesChanged = true;
        }
        float flightSpeed = getFlightSpeed(player);
        if (Math.abs(player.getAbilities().getFlyingSpeed() - flightSpeed) > 1.0E-4F) {
            player.getAbilities().setFlyingSpeed(flightSpeed);
            abilitiesChanged = true;
        }
        if (abilitiesChanged) {
            player.onUpdateAbilities();
        }
        return abilitiesChanged;
    }

    private static boolean canUseLightFlight(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        return getAvailableStyle(player).isPresent();
    }

    private static Optional<LightFlightStyle> getAvailableStyle(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return Optional.empty();
        }
        return CultivationApi.getStage(player).flatMap(stage -> LightFlightStyle.forRealm(stage.realm()));
    }

    private static boolean synchronizeStyleWithRealm(ServerPlayer player) {
        Optional<LightFlightStyle> availableStyle = getAvailableStyle(player);
        if (availableStyle.isEmpty()) {
            return false;
        }
        LightFlightStyle style = availableStyle.get();
        if (PlayerLightFlightData.getStyle(player) == style) {
            return false;
        }
        PlayerLightFlightData.setStyle(player, style);
        if (style != LightFlightStyle.LIGHT) {
            PlayerLightFlightData.clearBoost(player);
        }
        return true;
    }

    private static void playStartSound(ServerPlayer player, LightFlightStyle style) {
        if (style != LightFlightStyle.LIGHT) {
            return;
        }
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                0.7F,
                1.35F
        );
    }

    private static void playStopSound(ServerPlayer player, LightFlightStyle style) {
        if (style != LightFlightStyle.LIGHT) {
            return;
        }
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS,
                0.7F,
                1.15F
        );
    }

    private static void syncState(ServerPlayer player) {
        ModNetwork.sendLightFlightState(player);
    }
}
