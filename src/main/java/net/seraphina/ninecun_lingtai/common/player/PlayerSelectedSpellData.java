package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerSelectedSpellData {
    private static final String SELECTED_SPELL_KEY = ModMain.MOD_ID + ":selected_spell";

    public static Optional<ResourceLocation> getSelectedSpell(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(SELECTED_SPELL_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(persistentData.getString(SELECTED_SPELL_KEY)));
    }

    public static void setSelectedSpell(Player player, ResourceLocation spellId) {
        player.getPersistentData().putString(SELECTED_SPELL_KEY, spellId.toString());
    }

    public static void clearSelectedSpell(Player player) {
        player.getPersistentData().remove(SELECTED_SPELL_KEY);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(SELECTED_SPELL_KEY, Tag.TAG_STRING)) {
            targetPlayer.getPersistentData().putString(
                    SELECTED_SPELL_KEY,
                    sourceData.getString(SELECTED_SPELL_KEY)
            );
        }
    }
}
