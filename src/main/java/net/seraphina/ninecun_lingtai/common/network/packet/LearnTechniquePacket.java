package net.seraphina.ninecun_lingtai.common.network.packet;

import net.seraphina.ninecun_lingtai.api.technique.TechniqueApi;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueLearnability;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueLearningResult;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueLearningStatus;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueRequirementFailure;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import net.seraphina.ninecun_lingtai.utility.CultivationUtility;
import net.seraphina.ninecun_lingtai.utility.TechniqueUtility;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public record LearnTechniquePacket(ResourceLocation techniqueId, String understanding) {
    public static final int MAXIMUM_UNDERSTANDING_LENGTH = 2000;

    private static final Set<UUID> PENDING_EVALUATIONS = ConcurrentHashMap.newKeySet();

    public static void encode(LearnTechniquePacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.techniqueId);
        buffer.writeUtf(packet.understanding, MAXIMUM_UNDERSTANDING_LENGTH);
    }

    public static LearnTechniquePacket decode(FriendlyByteBuf buffer) {
        return new LearnTechniquePacket(
                buffer.readResourceLocation(),
                buffer.readUtf(MAXIMUM_UNDERSTANDING_LENGTH)
        );
    }

    public static void handle(
            LearnTechniquePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }
        if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
            return;
        }
        Optional<TechniqueDefinition> technique = TechniqueManager.INSTANCE.getTechnique(packet.techniqueId);
        if (technique.isEmpty()) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.missing_technique")
            );
            return;
        }
        if (!PlayerImprintedTechniqueData.hasTechnique(player, packet.techniqueId)
                && !PlayerTechniqueData.hasTechnique(player, packet.techniqueId)) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.learn_not_imprinted")
            );
            return;
        }
        if (packet.understanding.isBlank()) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.learn_empty_understanding")
            );
            return;
        }
        TechniqueLearnability learnability = TechniqueUtility.getLearnability(player, technique.get());
        if (!learnability.canLearn()) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    createRequirementFailureMessage(learnability.failures())
            );
            return;
        }
        if (!PENDING_EVALUATIONS.add(player.getUUID())) {
            ModNetwork.sendCultivationFeedback(
                    player,
                    false,
                    Component.translatable("message.ninecun_lingtai.learn_evaluation_in_progress")
            );
            return;
        }

        MinecraftServer server = player.server;
        UUID playerId = player.getUUID();
        TechniqueApi.evaluateUnderstanding(technique.get(), packet.understanding)
                .whenComplete((assessment, error) -> server.execute(() -> {
                    PENDING_EVALUATIONS.remove(playerId);
                    if (player.hasDisconnected()) {
                        return;
                    }
                    if (PlayerSoulRoamingManager.isSoulRoaming(player)) {
                        ModNetwork.sendTechniqueLibrary(player);
                        return;
                    }
                    if (error != null || assessment == null) {
                        ModNetwork.sendCultivationFeedback(
                                player,
                                false,
                                Component.translatable("message.ninecun_lingtai.comprehension_failed")
                        );
                        return;
                    }
                    TechniqueLearningResult result = TechniqueApi.applyAssessment(
                            player,
                            packet.techniqueId,
                            assessment
                    );
                    boolean success = result.status() == TechniqueLearningStatus.LEARNED
                            || result.status() == TechniqueLearningStatus.IMPROVED;
                    ModNetwork.sendTechniqueLibrary(player);
                    ModNetwork.sendCultivationFeedback(player, success, createResultMessage(
                            result,
                            technique.get().name(),
                            assessment.score(),
                            assessment.feedback()
                    ));
                }));
    }

    private static Component createResultMessage(
            TechniqueLearningResult result,
            String techniqueName,
            int score,
            String feedback
    ) {
        String stageName = result.progress()
                .map(TechniqueProgress::mastery)
                .map(TechniqueMasteryStage::getDisplayName)
                .orElse(TechniqueMasteryStage.ENTRY.getChineseName());
        return switch (result.status()) {
            case LEARNED -> Component.translatable(
                    "message.ninecun_lingtai.learn_result_learned",
                    score,
                    techniqueName,
                    stageName,
                    feedback
            );
            case IMPROVED -> Component.translatable(
                    "message.ninecun_lingtai.learn_result_improved",
                    score,
                    techniqueName,
                    stageName,
                    feedback
            );
            case UNCHANGED -> Component.translatable(
                    "message.ninecun_lingtai.learn_result_unchanged",
                    score,
                    techniqueName,
                    feedback
            );
            case SCORE_TOO_LOW -> Component.translatable(
                    "message.ninecun_lingtai.learn_result_too_low",
                    score,
                    techniqueName,
                    feedback
            );
            case REQUIREMENTS_NOT_MET -> createRequirementFailureMessage(result.requirementFailures());
            case TECHNIQUE_NOT_FOUND -> Component.translatable("message.ninecun_lingtai.missing_technique");
        };
    }

    private static Component createRequirementFailureMessage(List<TechniqueRequirementFailure> failures) {
        MutableComponent joinedFailures = Component.empty();
        for (int failureIndex = 0; failureIndex < failures.size(); failureIndex++) {
            if (failureIndex > 0) {
                joinedFailures.append("、");
            }
            joinedFailures.append(createRequirementFailureText(failures.get(failureIndex)));
        }
        return Component.translatable(
                "message.ninecun_lingtai.learn_requirements_not_met",
                joinedFailures
        );
    }

    private static Component createRequirementFailureText(TechniqueRequirementFailure failure) {
        return switch (failure.type()) {
            case SPIRITUAL_ROOT -> Component.translatable("requirement.ninecun_lingtai.spiritual_root");
            case SPIRITUAL_ROOT_COUNT -> createSpiritualRootCountFailureText(failure.detail());
            case COMPATIBLE_ELEMENT -> Component.translatable("requirement.ninecun_lingtai.compatible_element");
            case REQUIRED_ELEMENT -> Component.translatable("requirement.ninecun_lingtai.required_element");
            case CULTIVATION_STAGE -> Component.translatable(
                    "requirement.ninecun_lingtai.cultivation_stage",
                    formatStageDetail(failure.detail())
            );
            case SOUL_STRENGTH -> Component.translatable(
                    "requirement.ninecun_lingtai.soul_strength",
                    failure.detail()
            );
            case PREREQUISITE_TECHNIQUE -> Component.translatable(
                    "requirement.ninecun_lingtai.prerequisite_technique",
                    formatTechniqueDetail(failure.detail())
            );
        };
    }

    private static Component createSpiritualRootCountFailureText(String detail) {
        String[] bounds = detail.split(":", -1);
        if (bounds.length != 2) {
            return Component.translatable("requirement.ninecun_lingtai.spiritual_root_count", detail);
        }
        int minimum;
        int maximum;
        try {
            minimum = Integer.parseInt(bounds[0]);
            maximum = Integer.parseInt(bounds[1]);
        } catch (NumberFormatException exception) {
            return Component.translatable("requirement.ninecun_lingtai.spiritual_root_count", detail);
        }
        if (minimum > 0 && maximum > 0 && minimum == maximum) {
            return Component.translatable("requirement.ninecun_lingtai.spiritual_root_count_exact", minimum);
        }
        if (maximum > 0) {
            return Component.translatable(
                    "requirement.ninecun_lingtai.spiritual_root_count_range",
                    minimum,
                    maximum
            );
        }
        return Component.translatable("requirement.ninecun_lingtai.spiritual_root_count_minimum", minimum);
    }

    private static String formatStageDetail(String detail) {
        int separatorIndex = detail.lastIndexOf(':');
        if (separatorIndex > 0) {
            Optional<CultivationRealm> realm = CultivationRealm.fromSerializedName(
                    detail.substring(0, separatorIndex)
            );
            if (realm.isPresent()) {
                try {
                    int layer = Integer.parseInt(detail.substring(separatorIndex + 1));
                    return CultivationUtility.getDisplayName(new CultivationStage(realm.get(), layer));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return detail;
    }

    private static String formatTechniqueDetail(String detail) {
        ResourceLocation techniqueId = ResourceLocation.tryParse(detail);
        if (techniqueId == null) {
            return detail;
        }
        return TechniqueManager.INSTANCE.getTechnique(techniqueId)
                .map(TechniqueDefinition::name)
                .orElse(detail);
    }
}
