package com.seraphina.ninecun_lingtai.common.player;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class PlayerTechniqueData {
    private static final String TECHNIQUES_KEY = ModMain.MOD_ID + ":techniques";
    private static final String ENTRIES_KEY = "entries";
    private static final String ID_KEY = "id";
    private static final String SCORE_KEY = "score";
    private static final String MASTERY_KEY = "mastery";
    private static final String FEEDBACK_KEY = "feedback";

    private PlayerTechniqueData() {
        throw new RuntimeException("欸？云朵？");
    }

    public static Map<ResourceLocation, TechniqueProgress> getTechniques(Player player) {
        CompoundTag persistentData = player.getPersistentData();
        if (!persistentData.contains(TECHNIQUES_KEY, Tag.TAG_COMPOUND)) {
            return Map.of();
        }
        ListTag entries = persistentData.getCompound(TECHNIQUES_KEY).getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        Map<ResourceLocation, TechniqueProgress> techniques = new HashMap<>();
        for (Tag entryTag : entries) {
            CompoundTag entry = (CompoundTag) entryTag;
            ResourceLocation techniqueId = ResourceLocation.tryParse(entry.getString(ID_KEY));
            ResourceLocation mastery = ResourceLocation.tryParse(entry.getString(MASTERY_KEY));
            if (techniqueId == null || mastery == null || !entry.contains(SCORE_KEY, Tag.TAG_ANY_NUMERIC)) {
                continue;
            }
            try {
                techniques.put(techniqueId, new TechniqueProgress(
                        entry.getInt(SCORE_KEY),
                        mastery,
                        entry.getString(FEEDBACK_KEY)
                ));
            } catch (IllegalArgumentException exception) {
                // Ignore malformed persistent data without preventing the player from loading.
            }
        }
        return Map.copyOf(techniques);
    }

    public static Optional<TechniqueProgress> getTechnique(Player player, ResourceLocation techniqueId) {
        return Optional.ofNullable(getTechniques(player).get(techniqueId));
    }

    public static boolean hasTechnique(Player player, ResourceLocation techniqueId) {
        return getTechnique(player, techniqueId).isPresent();
    }

    public static void setTechnique(
            Player player,
            ResourceLocation techniqueId,
            TechniqueProgress techniqueProgress
    ) {
        Map<ResourceLocation, TechniqueProgress> techniques = new HashMap<>(getTechniques(player));
        techniques.put(techniqueId, techniqueProgress);
        saveTechniques(player, techniques);
    }

    public static void removeTechnique(Player player, ResourceLocation techniqueId) {
        Map<ResourceLocation, TechniqueProgress> techniques = new HashMap<>(getTechniques(player));
        techniques.remove(techniqueId);
        saveTechniques(player, techniques);
    }

    public static void clear(Player player) {
        player.getPersistentData().remove(TECHNIQUES_KEY);
    }

    public static void copy(Player sourcePlayer, Player targetPlayer) {
        CompoundTag sourceData = sourcePlayer.getPersistentData();
        if (sourceData.contains(TECHNIQUES_KEY, Tag.TAG_COMPOUND)) {
            targetPlayer.getPersistentData().put(
                    TECHNIQUES_KEY,
                    sourceData.getCompound(TECHNIQUES_KEY).copy()
            );
        }
    }

    private static void saveTechniques(Player player, Map<ResourceLocation, TechniqueProgress> techniques) {
        ListTag entries = new ListTag();
        techniques.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entries.add(createEntry(entry.getKey(), entry.getValue())));
        CompoundTag techniquesData = new CompoundTag();
        techniquesData.put(ENTRIES_KEY, entries);
        player.getPersistentData().put(TECHNIQUES_KEY, techniquesData);
    }

    private static CompoundTag createEntry(ResourceLocation techniqueId, TechniqueProgress techniqueProgress) {
        CompoundTag entry = new CompoundTag();
        entry.putString(ID_KEY, techniqueId.toString());
        entry.putInt(SCORE_KEY, techniqueProgress.comprehensionScore());
        entry.putString(MASTERY_KEY, techniqueProgress.mastery().toString());
        entry.putString(FEEDBACK_KEY, techniqueProgress.feedback());
        return entry;
    }
}
