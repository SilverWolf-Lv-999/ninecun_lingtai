package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerAptitudeData {
    private static final String SPIRITUAL_ROOT_KEY = ModMain.MOD_ID + ":spiritual_root";
    private static final String SOUL_STRENGTH_KEY = ModMain.MOD_ID + ":soul_strength";
    private static final String ELEMENT_MASK_KEY = "element_mask";
    private static final String LENGTH_INCHES_KEY = "length_inches";
    private static final String ILLUMINATED_STARS_KEY = "illuminated_stars";


    public static Optional<SpiritualRoot> getSpiritualRoot(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SPIRITUAL_ROOT_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag spiritualRootData = persistentData.getCompound(SPIRITUAL_ROOT_KEY);
        if (!spiritualRootData.contains(ELEMENT_MASK_KEY, Tag.TAG_ANY_NUMERIC)
                || !spiritualRootData.contains(LENGTH_INCHES_KEY, Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        return SpiritualRoot.fromElementMask(
                spiritualRootData.getInt(ELEMENT_MASK_KEY),
                spiritualRootData.getFloat(LENGTH_INCHES_KEY)
        );
    }

    public static void setSpiritualRoot(Player player, SpiritualRoot spiritualRoot) {
        CompoundTag spiritualRootData = new CompoundTag();
        spiritualRootData.putInt(ELEMENT_MASK_KEY, spiritualRoot.getElementMask());
        spiritualRootData.putFloat(LENGTH_INCHES_KEY, spiritualRoot.lengthInches());
        player.getPersistentData().put(SPIRITUAL_ROOT_KEY, spiritualRootData);
    }

    public static Optional<SoulStrength> getSoulStrength(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SOUL_STRENGTH_KEY, Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        CompoundTag soulStrengthData = persistentData.getCompound(SOUL_STRENGTH_KEY);
        if (!soulStrengthData.contains(ILLUMINATED_STARS_KEY, Tag.TAG_ANY_NUMERIC)) {
            return Optional.empty();
        }
        return SoulStrength.fromIlluminatedStars(soulStrengthData.getInt(ILLUMINATED_STARS_KEY));
    }

    public static void setSoulStrength(Player player, SoulStrength soulStrength) {
        CompoundTag soulStrengthData = new CompoundTag();
        soulStrengthData.putInt(ILLUMINATED_STARS_KEY, soulStrength.illuminatedStars());
        player.getPersistentData().put(SOUL_STRENGTH_KEY, soulStrengthData);
    }

    public static void setAptitude(Player player, SpiritualRoot spiritualRoot, SoulStrength soulStrength) {
        setSpiritualRoot(player, spiritualRoot);
        setSoulStrength(player, soulStrength);
    }

    public static boolean hasCompleteAptitude(Player player) {
        return getSpiritualRoot(player).isPresent() && getSoulStrength(player).isPresent();
    }

    public static void clearSpiritualRoot(Player player) {
        player.getPersistentData().remove(SPIRITUAL_ROOT_KEY);
    }

    public static void clearSoulStrength(Player player) {
        player.getPersistentData().remove(SOUL_STRENGTH_KEY);
    }

    public static void clearAptitude(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        persistentData.remove(SPIRITUAL_ROOT_KEY);
        persistentData.remove(SOUL_STRENGTH_KEY);
    }

    public static void copySpiritualRoot(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SPIRITUAL_ROOT_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SPIRITUAL_ROOT_KEY,
                    sourceData.getCompound(SPIRITUAL_ROOT_KEY).copy()
            );
        }
    }

    public static void copyAptitude(Player sourcePlayer, Player targetPlayer) {
        copySpiritualRoot(sourcePlayer, targetPlayer);
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SOUL_STRENGTH_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SOUL_STRENGTH_KEY,
                    sourceData.getCompound(SOUL_STRENGTH_KEY).copy()
            );
        }
    }
}
