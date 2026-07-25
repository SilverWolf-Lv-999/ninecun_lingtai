package com.seraphina.ninecun_lingtai.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.DoubleValue GENERATION_WEAKENING_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue RESTRAINT_ENHANCEMENT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue NATURAL_RESTRAINT_ADVANTAGE;
    public static final ForgeConfigSpec.DoubleValue CHAOS_ENHANCEMENT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CHAOS_SUPPRESSED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_BASE_ABSORPTION_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_GENERATES_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_GENERATED_BY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_OVERCOMES_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_OVERCOME_BY_MULTIPLIER;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("elementDamage");
        GENERATION_WEAKENING_MULTIPLIER = BUILDER
                .comment("Damage multiplier when the attack element generates the target element")
                .defineInRange("generationWeakeningMultiplier", 0.5D, 0.0D, 100.0D);
        RESTRAINT_ENHANCEMENT_MULTIPLIER = BUILDER
                .comment("Damage multiplier when an element successfully suppresses another element")
                .defineInRange("restraintEnhancementMultiplier", 1.5D, 0.0D, 100.0D);
        SUPPRESSED_MULTIPLIER = BUILDER
                .comment("Damage multiplier when the attack element is suppressed by the target element")
                .defineInRange("suppressedMultiplier", 0.5D, 0.0D, 100.0D);
        NATURAL_RESTRAINT_ADVANTAGE = BUILDER
                .comment("Strength advantage granted by the natural five-element restraint direction")
                .defineInRange("naturalRestraintAdvantage", 1.25D, 1.0D, 100.0D);
        CHAOS_ENHANCEMENT_MULTIPLIER = BUILDER
                .comment("Damage multiplier when chaos suppresses another element")
                .defineInRange("chaosEnhancementMultiplier", 2.0D, 0.0D, 100.0D);
        CHAOS_SUPPRESSED_MULTIPLIER = BUILDER
                .comment("Damage multiplier when another element attacks a chaos target")
                .defineInRange("chaosSuppressedMultiplier", 0.25D, 0.0D, 100.0D);
        BUILDER.pop();

        BUILDER.push("meditation");
        MEDITATION_BASE_ABSORPTION_PER_SECOND = BUILDER
                .comment("Base spiritual energy absorbed per second before root quality, environment, and element relations")
                .defineInRange("baseAbsorptionPerSecond", 1.0D, 0.0D, 1000000.0D);
        MEDITATION_GENERATES_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when the incoming element generates another element in a multi-element root")
                .defineInRange("generatesMultiplier", 1.1D, 0.0D, 100.0D);
        MEDITATION_GENERATED_BY_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when another element in a multi-element root generates the incoming element")
                .defineInRange("generatedByMultiplier", 1.25D, 0.0D, 100.0D);
        MEDITATION_OVERCOMES_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when the incoming element overcomes another element in a multi-element root")
                .defineInRange("overcomesMultiplier", 0.85D, 0.0D, 100.0D);
        MEDITATION_OVERCOME_BY_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when another element in a multi-element root overcomes the incoming element")
                .defineInRange("overcomeByMultiplier", 0.65D, 0.0D, 100.0D);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private CommonConfig() {
    }
}
