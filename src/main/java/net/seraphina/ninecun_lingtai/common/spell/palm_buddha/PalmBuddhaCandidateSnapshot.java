package net.seraphina.ninecun_lingtai.common.spell.palm_buddha;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PalmBuddhaCandidateSnapshot(
        UUID memberId,
        String displayName,
        PalmBuddhaMemberKind kind,
        Optional<CultivationStage> cultivationStage,
        float distance
) {
    public PalmBuddhaCandidateSnapshot {
        Objects.requireNonNull(memberId, "memberId");
        displayName = Objects.requireNonNull(displayName, "displayName").strip();
        if (displayName.isEmpty()) {
            displayName = memberId.toString();
        }
        if (displayName.length() > PalmBuddhaMemberSnapshot.MAXIMUM_DISPLAY_NAME_LENGTH) {
            displayName = displayName.substring(0, PalmBuddhaMemberSnapshot.MAXIMUM_DISPLAY_NAME_LENGTH);
        }
        kind = Objects.requireNonNull(kind, "kind");
        cultivationStage = cultivationStage == null ? Optional.empty() : cultivationStage;
        if (!Float.isFinite(distance) || distance < 0.0F || distance > PalmBuddhaSpell.MAXIMUM_TARGET_DISTANCE) {
            throw new IllegalArgumentException("Invalid palm-buddha candidate distance");
        }
    }

    public static PalmBuddhaCandidateSnapshot from(LivingEntity target, double distance) {
        Objects.requireNonNull(target, "target");
        PalmBuddhaMemberKind kind = target instanceof ServerPlayer
                ? PalmBuddhaMemberKind.PLAYER
                : PalmBuddhaMemberKind.CREATURE;
        String displayName = target.getName().getString();
        if (displayName.isBlank()) {
            displayName = target.getUUID().toString();
        }
        return new PalmBuddhaCandidateSnapshot(
                target.getUUID(),
                displayName,
                kind,
                CreatureCultivationData.getStage(target),
                (float) distance
        );
    }
}
