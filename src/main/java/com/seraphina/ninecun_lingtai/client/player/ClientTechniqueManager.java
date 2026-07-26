package com.seraphina.ninecun_lingtai.client.player;

import com.seraphina.ninecun_lingtai.api.technique.LearnedTechniqueClientData;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.client.screen.TechniqueReadingScreen;
import com.seraphina.ninecun_lingtai.client.utility.ClientCreativeTabUtility;
import com.seraphina.ninecun_lingtai.common.item.TechniqueCreativeTabCache;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.RequestTechniqueLibraryPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ClientTechniqueManager {
    private static Map<ResourceLocation, TechniqueClientData> techniques = Map.of();
    private static Set<ResourceLocation> imprintedTechniqueIds = Set.of();
    private static Map<ResourceLocation, LearnedTechniqueClientData> learnedTechniques = Map.of();
    private static boolean initialized;
    private static int revision;

    private ClientTechniqueManager() {
    }

    public static void beginOpenLibrary() {
        requestRefresh();
    }

    public static void requestRefresh() {
        ModNetwork.sendToServer(RequestTechniqueLibraryPacket.INSTANCE);
    }

    public static void acceptLibrary(
            List<TechniqueClientData> updatedTechniques,
            Set<ResourceLocation> updatedImprintedTechniqueIds,
            List<LearnedTechniqueClientData> updatedLearnedTechniques
    ) {
        Map<ResourceLocation, TechniqueClientData> techniqueMap = new LinkedHashMap<>();
        updatedTechniques.stream()
                .sorted(Comparator.comparing(technique -> technique.id().toString()))
                .forEach(technique -> techniqueMap.put(technique.id(), technique));
        techniques = Map.copyOf(techniqueMap);
        imprintedTechniqueIds = Set.copyOf(updatedImprintedTechniqueIds);
        Map<ResourceLocation, LearnedTechniqueClientData> learnedMap = new LinkedHashMap<>();
        updatedLearnedTechniques.forEach(
                learnedTechnique -> learnedMap.put(learnedTechnique.id(), learnedTechnique)
        );
        learnedTechniques = Map.copyOf(learnedMap);
        initialized = true;
        revision++;
        TechniqueCreativeTabCache.setTechniques(updatedTechniques);
        ClientCreativeTabUtility.rebuildCreativeTabs();
    }

    public static void openTechnique(TechniqueClientData technique, boolean imprinted) {
        Map<ResourceLocation, TechniqueClientData> techniqueMap = new LinkedHashMap<>(techniques);
        techniqueMap.put(technique.id(), technique);
        techniques = Map.copyOf(techniqueMap);
        if (imprinted) {
            HashSet<ResourceLocation> updatedImprints = new HashSet<>(imprintedTechniqueIds);
            updatedImprints.add(technique.id());
            imprintedTechniqueIds = Set.copyOf(updatedImprints);
        }
        Minecraft.getInstance().setScreen(new TechniqueReadingScreen(
                technique,
                null,
                true
        ));
    }

    public static Optional<TechniqueClientData> getTechnique(ResourceLocation techniqueId) {
        return Optional.ofNullable(techniques.get(techniqueId));
    }

    public static List<TechniqueClientData> getImprintedTechniques() {
        return imprintedTechniqueIds.stream()
                .map(techniques::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TechniqueClientData::name)
                        .thenComparing(technique -> technique.id().toString()))
                .toList();
    }

    public static List<TechniqueClientData> getImprintedUnlearnedTechniques() {
        return getImprintedTechniques().stream()
                .filter(technique -> !isLearned(technique.id()))
                .toList();
    }

    public static List<TechniqueClientData> getLearnedTechniques() {
        return learnedTechniques.keySet().stream()
                .map(techniques::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TechniqueClientData::name)
                        .thenComparing(technique -> technique.id().toString()))
                .toList();
    }

    public static Optional<LearnedTechniqueClientData> getLearnedTechniqueData(
            ResourceLocation techniqueId
    ) {
        return Optional.ofNullable(learnedTechniques.get(techniqueId));
    }

    public static boolean isImprinted(ResourceLocation techniqueId) {
        return imprintedTechniqueIds.contains(techniqueId);
    }

    public static boolean isLearned(ResourceLocation techniqueId) {
        return learnedTechniques.containsKey(techniqueId);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static int getRevision() {
        return revision;
    }

    public static void reset() {
        techniques = Map.of();
        imprintedTechniqueIds = Set.of();
        learnedTechniques = Map.of();
        initialized = false;
        revision++;
        TechniqueCreativeTabCache.clear();
    }
}
