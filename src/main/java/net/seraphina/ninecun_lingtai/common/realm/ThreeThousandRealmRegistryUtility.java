package net.seraphina.ninecun_lingtai.common.realm;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class ThreeThousandRealmRegistryUtility {
    private ThreeThousandRealmRegistryUtility() {
    }

    public static Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> excludeRealmLevelStemEntries(
            Set<Map.Entry<ResourceKey<LevelStem>, LevelStem>> levelStemEntries,
            Set<ResourceLocation> realmIds
    ) {
        return excludeManagedEntries(
                levelStemEntries,
                entry -> entry.getKey().location(),
                realmIds
        );
    }

    public static <T> Set<T> excludeManagedEntries(
            Set<T> entries,
            Function<T, ResourceLocation> locationResolver,
            Set<ResourceLocation> realmIds
    ) {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(locationResolver, "locationResolver");
        Objects.requireNonNull(realmIds, "realmIds");
        if (realmIds.isEmpty()) {
            return entries;
        }

        Set<T> filteredEntries = new LinkedHashSet<>();
        boolean excludedRealmStem = false;
        for (T entry : entries) {
            ResourceLocation location = Objects.requireNonNull(
                    locationResolver.apply(entry),
                    "locationResolver result"
            );
            if (realmIds.contains(location)) {
                excludedRealmStem = true;
                continue;
            }
            filteredEntries.add(entry);
        }
        if (!excludedRealmStem) {
            return entries;
        }
        return Collections.unmodifiableSet(filteredEntries);
    }

    public static RegistryAccess.Frozen createWorldSaveRegistryAccess(
            RegistryAccess registryAccess,
            Set<ResourceLocation> realmIds
    ) {
        Objects.requireNonNull(registryAccess, "registryAccess");
        Objects.requireNonNull(realmIds, "realmIds");
        if (realmIds.isEmpty()) {
            return registryAccess.freeze();
        }

        Registry<LevelStem> levelStemRegistry = registryAccess.registryOrThrow(Registries.LEVEL_STEM);
        boolean hasRealmLevelStem = levelStemRegistry.registryKeySet().stream()
                .map(ResourceKey::location)
                .anyMatch(realmIds::contains);
        if (!hasRealmLevelStem) {
            return registryAccess.freeze();
        }

        List<Registry<?>> registries = new ArrayList<>();
        registryAccess.registries().forEach(entry -> {
            if (!entry.key().equals(Registries.LEVEL_STEM)) {
                registries.add(entry.value());
            }
        });
        registries.add(createLevelStemRegistryWithoutRealms(levelStemRegistry, realmIds));
        return new RegistryAccess.ImmutableRegistryAccess(registries).freeze();
    }

    private static Registry<LevelStem> createLevelStemRegistryWithoutRealms(
            Registry<LevelStem> sourceRegistry,
            Set<ResourceLocation> realmIds
    ) {
        MappedRegistry<LevelStem> filteredRegistry = new MappedRegistry<>(
                Registries.LEVEL_STEM,
                sourceRegistry.registryLifecycle()
        );
        for (Map.Entry<ResourceKey<LevelStem>, LevelStem> entry : sourceRegistry.entrySet()) {
            if (!realmIds.contains(entry.getKey().location())) {
                filteredRegistry.register(
                        entry.getKey(),
                        entry.getValue(),
                        sourceRegistry.lifecycle(entry.getValue())
                );
            }
        }
        return filteredRegistry.freeze();
    }
}
