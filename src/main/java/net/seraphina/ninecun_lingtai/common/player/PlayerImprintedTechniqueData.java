package net.seraphina.ninecun_lingtai.common.player;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.Set;

public final class PlayerImprintedTechniqueData {
    private static final String IMPRINTED_TECHNIQUES_KEY = ModMain.MOD_ID + ":imprinted_techniques";

    private PlayerImprintedTechniqueData() {
    }

    public static Set<ResourceLocation> getTechniques(Player player) {
        ListTag techniqueTags = player.getPersistentData().getList(
                IMPRINTED_TECHNIQUES_KEY,
                Tag.TAG_STRING
        );
        Set<ResourceLocation> techniques = new HashSet<>();
        for (Tag techniqueTag : techniqueTags) {
            ResourceLocation techniqueId = ResourceLocation.tryParse(techniqueTag.getAsString());
            if (techniqueId != null) {
                techniques.add(techniqueId);
            }
        }
        return Set.copyOf(techniques);
    }

    public static boolean hasTechnique(Player player, ResourceLocation techniqueId) {
        return getTechniques(player).contains(techniqueId);
    }

    public static boolean imprint(Player player, ResourceLocation techniqueId) {
        Set<ResourceLocation> techniques = new HashSet<>(getTechniques(player));
        boolean added = techniques.add(techniqueId);
        saveTechniques(player, techniques);
        return added;
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(IMPRINTED_TECHNIQUES_KEY);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        if (sourcePlayer.getPersistentData().contains(IMPRINTED_TECHNIQUES_KEY, Tag.TAG_LIST)) {
            targetPlayer.getPersistentData().put(
                    IMPRINTED_TECHNIQUES_KEY,
                    sourcePlayer.getPersistentData().getList(
                            IMPRINTED_TECHNIQUES_KEY,
                            Tag.TAG_STRING
                    ).copy()
            );
        }
    }

    private static void saveTechniques(Player player, Set<ResourceLocation> techniques) {
        ListTag techniqueTags = new ListTag();
        techniques.stream()
                .sorted()
                .map(ResourceLocation::toString)
                .map(StringTag::valueOf)
                .forEach(techniqueTags::add);
        player.getPersistentData().put(IMPRINTED_TECHNIQUES_KEY, techniqueTags);
    }
}
