package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.debug.DaoScrollSnapshot;
import com.seraphina.ninecun_lingtai.common.debug.DaoScrollTechniqueData;
import com.seraphina.ninecun_lingtai.common.defense.EntityDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DaoScrollUtility {
    private static final int MAXIMUM_TECHNIQUE_ENTRIES = 2048;
    private static final double MAXIMUM_SPIRITUAL_ENERGY = Double.MAX_VALUE;

    public static DaoScrollSnapshot capture(LivingEntity entity) {
        Map<ElementType, Double> spiritualEnergy = new EnumMap<>(ElementType.class);
        List<DaoScrollTechniqueData> techniques = List.of();
        if (entity instanceof Player player) {
            spiritualEnergy.putAll(PlayerSpiritualEnergyData.getAllEnergy(player));
            techniques = TechniqueManager.INSTANCE.getTechniques().stream()
                    .limit(MAXIMUM_TECHNIQUE_ENTRIES)
                    .map(technique -> captureTechnique(player, technique))
                    .toList();
        }
        return new DaoScrollSnapshot(
                entity.getId(),
                entity.getDisplayName().getString(),
                entity instanceof Player,
                CreatureCultivationData.getStage(entity),
                CreatureCultivationData.getSpiritualRoot(entity),
                entity instanceof Player player
                        ? PlayerAptitudeData.getSoulStrength(player)
                        : Optional.empty(),
                EntityLifespanData.getAgeTicks(entity),
                EntityDefenseResourceData.getCurrent(entity),
                spiritualEnergy,
                techniques
        );
    }

    public static Optional<String> validateSnapshot(DaoScrollSnapshot snapshot) {
        if (snapshot == null) {
            return Optional.of("missing_snapshot");
        }
        if (snapshot.entityId() < 0) {
            return Optional.of("invalid_entity");
        }
        if (snapshot.ageTicks() < 0L) {
            return Optional.of("invalid_age");
        }
        if (snapshot.defenseCurrent() < 0) {
            return Optional.of("invalid_defense");
        }
        if (snapshot.cultivationStage().isPresent()) {
            CultivationStage stage = snapshot.cultivationStage().get();
            if (stage.layer() < CultivationStage.MINIMUM_LAYER
                    || stage.layer() > CultivationStage.MAXIMUM_LAYER) {
                return Optional.of("invalid_stage");
            }
        }
        if (snapshot.spiritualRoot().isPresent()) {
            SpiritualRoot root = snapshot.spiritualRoot().get();
            if (!SpiritualRoot.SELECTABLE_ELEMENTS.containsAll(root.elements())) {
                return Optional.of("invalid_spiritual_root");
            }
        }
        if (snapshot.soulStrength().isPresent()) {
            int stars = snapshot.soulStrength().get().illuminatedStars();
            if (stars < SoulStrength.MINIMUM_ILLUMINATED_STARS
                    || stars > SoulStrength.MAXIMUM_ILLUMINATED_STARS) {
                return Optional.of("invalid_soul_strength");
            }
        }
        for (Map.Entry<ElementType, Double> entry : snapshot.spiritualEnergy().entrySet()) {
            if (entry.getKey() == null
                    || entry.getValue() == null
                    || !Double.isFinite(entry.getValue())
                    || entry.getValue() < 0.0D
                    || entry.getValue() > MAXIMUM_SPIRITUAL_ENERGY) {
                return Optional.of("invalid_spiritual_energy");
            }
        }
        if (snapshot.techniques().size() > MAXIMUM_TECHNIQUE_ENTRIES) {
            return Optional.of("too_many_techniques");
        }
        for (DaoScrollTechniqueData techniqueData : snapshot.techniques()) {
            if (techniqueData == null
                    || techniqueData.id() == null
                    || techniqueData.comprehensionScore() < 0
                    || techniqueData.comprehensionScore() > 100
                    || techniqueData.proficiency() < 0L) {
                return Optional.of("invalid_technique");
            }
        }
        return Optional.empty();
    }

    public static Optional<Component> apply(ServerPlayer editor, LivingEntity target, DaoScrollSnapshot snapshot) {
        Optional<String> validationError = validateSnapshot(snapshot);
        if (validationError.isPresent()) {
            return Optional.of(Component.translatable(
                    "message.ninecun_lingtai.dao_scroll.invalid_data"
            ));
        }
        if (snapshot.player() != (target instanceof Player)) {
            return Optional.of(Component.translatable(
                    "message.ninecun_lingtai.dao_scroll.invalid_target"
            ));
        }

        if (snapshot.cultivationStage().isPresent()) {
            CreatureCultivationData.setStage(target, snapshot.cultivationStage().get());
        } else {
            CreatureCultivationData.clearStage(target);
        }
        if (snapshot.spiritualRoot().isPresent()) {
            CreatureCultivationData.setSpiritualRoot(target, snapshot.spiritualRoot().get());
        } else {
            CreatureCultivationData.clearSpiritualRoot(target);
        }
        if (target instanceof Player player) {
            if (snapshot.soulStrength().isPresent()) {
                PlayerAptitudeData.setSoulStrength(player, snapshot.soulStrength().get());
            } else {
                PlayerAptitudeData.clearSoulStrength(player);
            }
            applyPlayerData(player, snapshot);
        }

        EntityLifespanData.setAgeTicks(target, snapshot.ageTicks());
        CultivationDefenseUtility.syncMaxHealthModifier(target);
        EntityDefenseResourceData.clampToStage(target);
        EntityDefenseResourceData.setCurrent(target, snapshot.defenseCurrent());
        target.setHealth(Math.min(target.getHealth(), target.getMaxHealth()));
        if (target instanceof ServerPlayer serverTarget) {
            ModNetworkSyncUtility.syncAfterDaoScroll(serverTarget);
        }
        return Optional.empty();
    }

    private static DaoScrollTechniqueData captureTechnique(Player player, TechniqueDefinition technique) {
        Optional<TechniqueProgress> progress = PlayerTechniqueData.getTechnique(player, technique.id());
        return new DaoScrollTechniqueData(
                technique.id(),
                technique.name(),
                progress.isPresent(),
                progress.map(TechniqueProgress::comprehensionScore).orElse(0),
                PlayerTechniqueProficiencyData.get(player, technique.id()),
                PlayerImprintedTechniqueData.hasTechnique(player, technique.id())
        );
    }

    private static void applyPlayerData(Player player, DaoScrollSnapshot snapshot) {
        for (ElementType element : ElementType.values()) {
            PlayerSpiritualEnergyData.setEnergy(player, element, snapshot.getEnergy(element));
        }

        PlayerTechniqueData.clear(player);
        PlayerTechniqueProficiencyData.clear(player);
        PlayerImprintedTechniqueData.clear(player);
        for (DaoScrollTechniqueData techniqueData : snapshot.techniques()) {
            Optional<TechniqueDefinition> definition = TechniqueManager.INSTANCE.getTechnique(techniqueData.id());
            if (definition.isEmpty()) {
                continue;
            }
            if (techniqueData.learned()) {
                ResourceLocation mastery = definition.get().comprehension()
                        .getMastery(techniqueData.comprehensionScore())
                        .orElse(definition.get().comprehension().masteryThresholds().get(0).mastery());
                PlayerTechniqueData.setTechnique(
                        player,
                        techniqueData.id(),
                        new TechniqueProgress(
                                techniqueData.comprehensionScore(),
                                mastery,
                                "Dao Scroll"
                        )
                );
                PlayerTechniqueProficiencyData.set(
                        player,
                        techniqueData.id(),
                        techniqueData.proficiency()
                );
            }
            if (techniqueData.imprinted()) {
                PlayerImprintedTechniqueData.imprint(player, techniqueData.id());
            }
        }
    }
}
