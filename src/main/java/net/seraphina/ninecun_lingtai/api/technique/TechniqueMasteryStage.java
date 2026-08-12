package net.seraphina.ninecun_lingtai.api.technique;

import net.seraphina.ninecun_lingtai.ModMain;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The canonical learning states of a technique, following the design document: a technique is
 * first imprinted (tracked separately, not yet learned), then progresses through entry, minor
 * mastery, major mastery, and perfection. Comprehension scores grant entry on learning (a
 * perfect score grants minor mastery directly), while training proficiency advances the
 * remaining stages.
 */
public enum TechniqueMasteryStage {
    ENTRY("entry", "入门", 0L, 0.5D),
    MINOR_MASTERY("minor_mastery", "小成", 60L, 0.7D),
    MAJOR_MASTERY("major_mastery", "大成", 360L, 0.85D),
    PERFECTION("perfection", "圆满", 1800L, 1.0D);

    private static final Map<ResourceLocation, TechniqueMasteryStage> BY_MASTERY_ID = createMasteryIndex();

    private final String serializedName;
    private final String chineseName;
    private final long requiredProficiency;
    private final double efficiency;
    private final ResourceLocation masteryId;

    TechniqueMasteryStage(
            String serializedName,
            String chineseName,
            long requiredProficiency,
            double efficiency
    ) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
        this.requiredProficiency = requiredProficiency;
        this.efficiency = efficiency;
        this.masteryId = ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, serializedName);
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    /** Total training proficiency required to reach this stage through cultivation. */
    public long getRequiredProficiency() {
        return this.requiredProficiency;
    }

    /**
     * Fraction of a technique's full power guaranteed by this stage — entry unlocks half of it,
     * perfection all of it. Breathing techniques scale absorption speed by this; spell power
     * uses it as the minimum proficiency efficiency before applying continuous proficiency and
     * aptitude scaling.
     */
    public double getEfficiency() {
        return this.efficiency;
    }

    public ResourceLocation getMasteryId() {
        return this.masteryId;
    }

    public Optional<TechniqueMasteryStage> next() {
        int nextIndex = this.ordinal() + 1;
        return nextIndex < values().length
                ? Optional.of(values()[nextIndex])
                : Optional.empty();
    }

    /** Highest stage whose proficiency requirement is met; never below {@link #ENTRY}. */
    public static TechniqueMasteryStage getStageForProficiency(long proficiency) {
        TechniqueMasteryStage stage = ENTRY;
        for (TechniqueMasteryStage candidate : values()) {
            if (proficiency >= candidate.requiredProficiency) {
                stage = candidate;
            }
        }
        return stage;
    }

    public static Optional<TechniqueMasteryStage> fromMastery(ResourceLocation mastery) {
        return Optional.ofNullable(BY_MASTERY_ID.get(mastery));
    }

    /** Chinese stage name for known masteries, falling back to the raw mastery path. */
    public static String getDisplayName(ResourceLocation mastery) {
        return fromMastery(mastery)
                .map(TechniqueMasteryStage::getChineseName)
                .orElse(mastery.getPath());
    }

    /** Picks the higher of two mastery ids; unknown ids rank below every known stage. */
    public static ResourceLocation highest(ResourceLocation first, ResourceLocation second) {
        int firstRank = fromMastery(first).map(Enum::ordinal).orElse(-1);
        int secondRank = fromMastery(second).map(Enum::ordinal).orElse(-1);
        return firstRank >= secondRank ? first : second;
    }

    private static Map<ResourceLocation, TechniqueMasteryStage> createMasteryIndex() {
        Map<ResourceLocation, TechniqueMasteryStage> index = new HashMap<>();
        for (TechniqueMasteryStage stage : values()) {
            index.put(stage.masteryId, stage);
        }
        // Legacy mastery ids used by earlier technique definitions.
        index.put(
                ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "initial_understanding"),
                ENTRY
        );
        index.put(
                ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "proficient"),
                MINOR_MASTERY
        );
        return Map.copyOf(index);
    }
}
