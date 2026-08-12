package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class PlayerShrinkEarthData {
    private static final String SHRINK_EARTH_KEY = ModMain.MOD_ID + ":shrink_earth";
    private static final String ACTIVE_KEY = "active";
    private static final String COMPRESSION_SCALE_KEY = "compression_scale";
    private static final String LAST_X_KEY = "last_x";
    private static final String LAST_Y_KEY = "last_y";
    private static final String LAST_Z_KEY = "last_z";

    private PlayerShrinkEarthData() {
    }

    public static boolean isActive(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(SHRINK_EARTH_KEY, Tag.TAG_COMPOUND)
                && persistentData.getCompound(SHRINK_EARTH_KEY).getBoolean(ACTIVE_KEY);
    }

    public static void start(Player player, int compressionScale) {
        CompoundTag shrinkEarthData = getOrCreateData(player);
        shrinkEarthData.putBoolean(ACTIVE_KEY, true);
        shrinkEarthData.putInt(COMPRESSION_SCALE_KEY, Math.max(1, compressionScale));
        putLastPosition(shrinkEarthData, player.position());
        player.getPersistentData().put(SHRINK_EARTH_KEY, shrinkEarthData);
    }

    public static void stop(Player player) {
        player.getPersistentData().remove(SHRINK_EARTH_KEY);
    }

    public static int getCompressionScale(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SHRINK_EARTH_KEY, Tag.TAG_COMPOUND)) {
            return 1;
        }
        CompoundTag shrinkEarthData = persistentData.getCompound(SHRINK_EARTH_KEY);
        return shrinkEarthData.contains(COMPRESSION_SCALE_KEY, Tag.TAG_ANY_NUMERIC)
                ? Math.max(1, shrinkEarthData.getInt(COMPRESSION_SCALE_KEY))
                : 1;
    }

    public static void setCompressionScale(Player player, int compressionScale) {
        if (!isActive(player)) {
            return;
        }
        CompoundTag shrinkEarthData = getOrCreateData(player);
        shrinkEarthData.putBoolean(ACTIVE_KEY, true);
        shrinkEarthData.putInt(COMPRESSION_SCALE_KEY, Math.max(1, compressionScale));
        player.getPersistentData().put(SHRINK_EARTH_KEY, shrinkEarthData);
    }

    public static Optional<Vec3> getLastPosition(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SHRINK_EARTH_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag shrinkEarthData = persistentData.getCompound(SHRINK_EARTH_KEY);
        if (!shrinkEarthData.contains(LAST_X_KEY, Tag.TAG_ANY_NUMERIC)
                || !shrinkEarthData.contains(LAST_Y_KEY, Tag.TAG_ANY_NUMERIC)
                || !shrinkEarthData.contains(LAST_Z_KEY, Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(
                shrinkEarthData.getDouble(LAST_X_KEY),
                shrinkEarthData.getDouble(LAST_Y_KEY),
                shrinkEarthData.getDouble(LAST_Z_KEY)
        ));
    }

    public static void setLastPosition(Player player, Vec3 position) {
        if (!isActive(player)) {
            return;
        }
        CompoundTag shrinkEarthData = getOrCreateData(player);
        shrinkEarthData.putBoolean(ACTIVE_KEY, true);
        putLastPosition(shrinkEarthData, position);
        player.getPersistentData().put(SHRINK_EARTH_KEY, shrinkEarthData);
    }

    private static CompoundTag getOrCreateData(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        return persistentData.contains(SHRINK_EARTH_KEY, Tag.TAG_COMPOUND)
                ? persistentData.getCompound(SHRINK_EARTH_KEY)
                : new CompoundTag();
    }

    private static void putLastPosition(CompoundTag shrinkEarthData, Vec3 position) {
        shrinkEarthData.putDouble(LAST_X_KEY, position.x);
        shrinkEarthData.putDouble(LAST_Y_KEY, position.y);
        shrinkEarthData.putDouble(LAST_Z_KEY, position.z);
    }
}
