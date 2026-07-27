package com.seraphina.ninecun_lingtai.common.flight;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRootApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerLightFlightData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSwordFlightData;
import com.seraphina.ninecun_lingtai.common.spell.SwordFlightManager;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

public final class LightFlightManager {
    private static final float BASE_FLYING_SPEED = 0.05F;
    private static final float MAXIMUM_LIGHT_FLYING_SPEED = 1300.0F;
    private static final int ENERGY_CONSUMPTION_INTERVAL_TICKS = 20;

    private LightFlightManager() {
    }

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
        if (!canUseLightFlight(player)) {
            sendActionBar(player, "只有金丹期可身化遁光");
            syncState(player);
            return;
        }
        if (PlayerSwordFlightData.isFlying(player)) {
            SwordFlightManager.dismount(player);
        }
        PlayerLightFlightData.start(player);
        applyFlightAbilities(player);
        player.fallDistance = 0.0F;
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.PLAYERS,
                0.7F,
                1.35F
        );
        sendActionBar(player, "金丹流转，身化遁光");
        syncState(player);
    }

    public static void stop(ServerPlayer player) {
        if (!PlayerLightFlightData.isFlying(player)) {
            syncState(player);
            return;
        }
        PlayerLightFlightData.stop(player);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
        }
        player.getAbilities().setFlyingSpeed(BASE_FLYING_SPEED);
        player.onUpdateAbilities();
        player.fallDistance = 0.0F;
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS,
                0.7F,
                1.15F
        );
        sendActionBar(player, "遁光敛去");
        syncState(player);
    }

    public static void requestBoost(ServerPlayer player) {
        if (!PlayerLightFlightData.isFlying(player)) {
            return;
        }
        long gameTime = player.level().getGameTime();
        if (!PlayerLightFlightData.canBoost(player, gameTime)) {
            long remainingTicks = Math.max(1L, PlayerLightFlightData.getNextBoostTick(player) - gameTime);
            sendActionBar(player, String.format("遁光未复，还需 %.1f 秒", remainingTicks / 20.0D));
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
        long gameTime = player.level().getGameTime();
        boolean boostExpired = PlayerLightFlightData.getBoostUntilTick(player) > 0L
                && !PlayerLightFlightData.isBoosting(player, gameTime);
        if (boostExpired) {
            PlayerLightFlightData.clearBoost(player);
        }
        if (!player.isCreative() && player.tickCount % ENERGY_CONSUMPTION_INTERVAL_TICKS == 0) {
            double energyCost = CommonConfig.LIGHT_FLIGHT_ENERGY_COST_PER_SECOND.get();
            if (energyCost > 0.0D && PlayerSpiritualEnergyData.consumeEnergyAny(player, energyCost) == null) {
                stop(player);
                sendActionBar(player, "灵气不继，遁光散去");
                return;
            }
        }
        if (applyFlightAbilities(player) || boostExpired) {
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
        applyFlightAbilities(player);
        syncState(player);
    }

    public static float getFlightSpeed(ServerPlayer player) {
        long gameTime = player.level().getGameTime();
        double multiplier = CommonConfig.LIGHT_FLIGHT_SPEED_MULTIPLIER.get();
        if (PlayerLightFlightData.isBoosting(player, gameTime)) {
            multiplier *= CommonConfig.LIGHT_FLIGHT_BOOST_MULTIPLIER.get();
        }
        return (float) Mth.clamp(
                SwordFlightManager.getPerfectionFlightSpeed(player) * multiplier,
                BASE_FLYING_SPEED,
                MAXIMUM_LIGHT_FLYING_SPEED
        );
    }

    public static boolean isBoosting(ServerPlayer player) {
        return PlayerLightFlightData.isBoosting(player, player.level().getGameTime());
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
        return CultivationApi.getStage(player)
                .map(stage -> stage.realm() == CultivationRealm.GOLDEN_CORE)
                .orElse(false);
    }

    private static void syncState(ServerPlayer player) {
        ModNetwork.sendLightFlightState(player);
    }

    private static void sendActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
