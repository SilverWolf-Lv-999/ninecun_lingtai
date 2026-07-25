package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerAptitudeData {
    private static final String SPIRITUAL_ROOT_KEY = ModMain.MOD_ID + ":spiritual_root";
    private static final String ELEMENT_MASK_KEY = "element_mask";
    private static final String LENGTH_INCHES_KEY = "length_inches";

    private PlayerAptitudeData() {
    }

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

    public static void copySpiritualRoot(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SPIRITUAL_ROOT_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    SPIRITUAL_ROOT_KEY,
                    sourceData.getCompound(SPIRITUAL_ROOT_KEY).copy()
            );
        }
    }
}
