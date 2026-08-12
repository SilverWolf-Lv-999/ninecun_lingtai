package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

public final class PlayerLightFlightData {
    private static final String LIGHT_FLIGHT_KEY = ModMain.MOD_ID + ":light_flight";
    private static final String FLYING_KEY = "flying";
    private static final String STYLE_KEY = "style";
    private static final String BOOST_UNTIL_TICK_KEY = "boost_until_tick";
    private static final String NEXT_BOOST_TICK_KEY = "next_boost_tick";

    public static boolean isFlying(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(LIGHT_FLIGHT_KEY, Tag.TAG_COMPOUND)
                && persistentData.getCompound(LIGHT_FLIGHT_KEY).getBoolean(FLYING_KEY);
    }

    public static void start(Player player, LightFlightStyle style) {
        CompoundTag lightFlightData = getOrCreateData(player);
        lightFlightData.putBoolean(FLYING_KEY, true);
        putStyle(lightFlightData, style);
        player.getPersistentData().put(LIGHT_FLIGHT_KEY, lightFlightData);
    }

    public static void stop(Player player) {
        player.getPersistentData().remove(LIGHT_FLIGHT_KEY);
    }

    public static void setBoost(Player player, long boostUntilTick, long nextBoostTick) {
        CompoundTag lightFlightData = getOrCreateData(player);
        lightFlightData.putBoolean(FLYING_KEY, true);
        if (!lightFlightData.contains(STYLE_KEY, Tag.TAG_STRING)) {
            putStyle(lightFlightData, getStyle(player));
        }
        lightFlightData.putLong(BOOST_UNTIL_TICK_KEY, Math.max(0L, boostUntilTick));
        lightFlightData.putLong(NEXT_BOOST_TICK_KEY, Math.max(0L, nextBoostTick));
        player.getPersistentData().put(LIGHT_FLIGHT_KEY, lightFlightData);
    }

    public static void clearBoost(Player player) {
        if (!isFlying(player)) {
            return;
        }
        CompoundTag lightFlightData = getOrCreateData(player);
        lightFlightData.remove(BOOST_UNTIL_TICK_KEY);
        player.getPersistentData().put(LIGHT_FLIGHT_KEY, lightFlightData);
    }

    public static boolean isBoosting(Player player, long gameTime) {
        return isFlying(player) && getBoostUntilTick(player) > gameTime;
    }

    public static boolean canBoost(Player player, long gameTime) {
        return isFlying(player) && getNextBoostTick(player) <= gameTime;
    }

    public static LightFlightStyle getStyle(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(LIGHT_FLIGHT_KEY, Tag.TAG_COMPOUND)) {
            return LightFlightStyle.LIGHT;
        }
        CompoundTag lightFlightData = persistentData.getCompound(LIGHT_FLIGHT_KEY);
        return lightFlightData.contains(STYLE_KEY, Tag.TAG_STRING)
                ? LightFlightStyle.fromSerializedName(lightFlightData.getString(STYLE_KEY))
                : LightFlightStyle.LIGHT;
    }

    public static void setStyle(Player player, LightFlightStyle style) {
        if (!isFlying(player)) {
            return;
        }
        CompoundTag lightFlightData = getOrCreateData(player);
        putStyle(lightFlightData, style);
        player.getPersistentData().put(LIGHT_FLIGHT_KEY, lightFlightData);
    }

    public static long getBoostUntilTick(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(LIGHT_FLIGHT_KEY, Tag.TAG_COMPOUND)) {
            return 0L;
        }
        CompoundTag lightFlightData = persistentData.getCompound(LIGHT_FLIGHT_KEY);
        return lightFlightData.contains(BOOST_UNTIL_TICK_KEY, Tag.TAG_ANY_NUMERIC)
                ? lightFlightData.getLong(BOOST_UNTIL_TICK_KEY)
                : 0L;
    }

    public static long getNextBoostTick(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(LIGHT_FLIGHT_KEY, Tag.TAG_COMPOUND)) {
            return 0L;
        }
        CompoundTag lightFlightData = persistentData.getCompound(LIGHT_FLIGHT_KEY);
        return lightFlightData.contains(NEXT_BOOST_TICK_KEY, Tag.TAG_ANY_NUMERIC)
                ? lightFlightData.getLong(NEXT_BOOST_TICK_KEY)
                : 0L;
    }

    private static CompoundTag getOrCreateData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(LIGHT_FLIGHT_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(LIGHT_FLIGHT_KEY)
                : new CompoundTag();
    }

    private static void putStyle(CompoundTag lightFlightData, LightFlightStyle style) {
        LightFlightStyle sanitizedStyle = style == null ? LightFlightStyle.LIGHT : style;
        lightFlightData.putString(STYLE_KEY, sanitizedStyle.getSerializedName());
    }
}
