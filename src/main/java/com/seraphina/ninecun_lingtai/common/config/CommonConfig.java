package com.seraphina.ninecun_lingtai.common.config;

import com.seraphina.ninecun_lingtai.api.ai.AiProvider;
import net.minecraftforge.common.ForgeConfigSpec;

public final class CommonConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final double MINIMUM_MULTIPLIER = 0.0D;
    public static final double MAXIMUM_MULTIPLIER = 100.0D;
    public static final double MINIMUM_NATURAL_RESTRAINT_ADVANTAGE = 1.0D;
    public static final double MAXIMUM_BASE_ABSORPTION_PER_SECOND = 1000000.0D;
    public static final int MAXIMUM_LIFESPAN_YEARS = 1000000;

    public static final ForgeConfigSpec.DoubleValue GENERATION_WEAKENING_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue RESTRAINT_ENHANCEMENT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SUPPRESSED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue NATURAL_RESTRAINT_ADVANTAGE;
    public static final ForgeConfigSpec.DoubleValue CHAOS_ENHANCEMENT_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CHAOS_SUPPRESSED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue REALM_BONUS_BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue REALM_BONUS_LAYER_GROWTH;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_BASE_ABSORPTION_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_ABSORPTION_REQUIREMENT_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_GENERATES_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_GENERATED_BY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_OVERCOMES_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_OVERCOME_BY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_BOOST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_ENERGY_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue SHRINK_EARTH_ENERGY_COST_PER_SECOND;
    public static final ForgeConfigSpec.IntValue LIGHT_FLIGHT_BOOST_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue LIGHT_FLIGHT_BOOST_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MORTAL_PLAYER_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue QI_REFINING_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue FOUNDATION_ESTABLISHMENT_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue GOLDEN_CORE_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue NASCENT_SOUL_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue DEFAULT_CREATURE_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.DoubleValue CREATURE_CULTIVATOR_CHANCE;
    public static final ForgeConfigSpec.EnumValue<AiProvider> AI_PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> AI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> AI_MODEL;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("elementDamage");
        GENERATION_WEAKENING_MULTIPLIER = BUILDER
                .comment("Damage multiplier when the attack element generates the target element")
                .defineInRange("generationWeakeningMultiplier", 0.5D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        RESTRAINT_ENHANCEMENT_MULTIPLIER = BUILDER
                .comment("Damage multiplier when an element successfully suppresses another element")
                .defineInRange("restraintEnhancementMultiplier", 1.5D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        SUPPRESSED_MULTIPLIER = BUILDER
                .comment("Damage multiplier when the attack element is suppressed by the target element")
                .defineInRange("suppressedMultiplier", 0.5D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        NATURAL_RESTRAINT_ADVANTAGE = BUILDER
                .comment("Strength advantage granted by the natural five-element restraint direction")
                .defineInRange(
                        "naturalRestraintAdvantage",
                        1.25D,
                        MINIMUM_NATURAL_RESTRAINT_ADVANTAGE,
                        MAXIMUM_MULTIPLIER
                );
        CHAOS_ENHANCEMENT_MULTIPLIER = BUILDER
                .comment("Damage multiplier when chaos suppresses another element")
                .defineInRange("chaosEnhancementMultiplier", 2.0D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        CHAOS_SUPPRESSED_MULTIPLIER = BUILDER
                .comment("Damage multiplier when another element attacks a chaos target")
                .defineInRange("chaosSuppressedMultiplier", 0.25D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        BUILDER.pop();

        BUILDER.push("realmBonusDamage");
        REALM_BONUS_BASE_DAMAGE = BUILDER
                .comment("Bonus damage added to player-sourced damage at the first qi refining layer")
                .defineInRange("baseBonusDamage", 1.0D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        REALM_BONUS_LAYER_GROWTH = BUILDER
                .comment("Bonus damage growth multiplier applied for each cultivation layer above the first")
                .defineInRange(
                        "layerGrowthMultiplier",
                        1.25D,
                        MINIMUM_NATURAL_RESTRAINT_ADVANTAGE,
                        MAXIMUM_MULTIPLIER
                );
        BUILDER.pop();

        BUILDER.push("meditation");
        MEDITATION_BASE_ABSORPTION_PER_SECOND = BUILDER
                .comment("Base spiritual energy absorbed per second before root quality, environment, and element relations")
                .defineInRange(
                        "baseAbsorptionPerSecond",
                        1.0D,
                        MINIMUM_MULTIPLIER,
                        MAXIMUM_BASE_ABSORPTION_PER_SECOND
                );
        MEDITATION_ABSORPTION_REQUIREMENT_EXPONENT = BUILDER
                .comment(
                        "Absorption speed scales with (next layer requirement / first layer requirement)^exponent.",
                        "1.0 makes every layer take the same time; 0.0 disables realm scaling entirely"
                )
                .defineInRange("absorptionRequirementExponent", 0.95D, 0.0D, 1.0D);
        MEDITATION_GENERATES_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when the incoming element generates another element in a multi-element root")
                .defineInRange("generatesMultiplier", 1.1D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        MEDITATION_GENERATED_BY_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when another element in a multi-element root generates the incoming element")
                .defineInRange("generatedByMultiplier", 1.25D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        MEDITATION_OVERCOMES_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when the incoming element overcomes another element in a multi-element root")
                .defineInRange("overcomesMultiplier", 0.85D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        MEDITATION_OVERCOME_BY_MULTIPLIER = BUILDER
                .comment("Absorption multiplier when another element in a multi-element root overcomes the incoming element")
                .defineInRange("overcomeByMultiplier", 0.65D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        BUILDER.pop();

        BUILDER.push("lightFlight");
        LIGHT_FLIGHT_SPEED_MULTIPLIER = BUILDER
                .comment("Golden-core light-flight speed multiplier against the default sword-flight baseline")
                .defineInRange("speedMultiplier", 1.3D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        LIGHT_FLIGHT_BOOST_MULTIPLIER = BUILDER
                .comment("Temporary light-flight boost multiplier")
                .defineInRange("boostMultiplier", 1.5D, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
        LIGHT_FLIGHT_ENERGY_COST_PER_SECOND = BUILDER
                .comment("Spiritual energy consumed per second while light-flying")
                .defineInRange(
                        "energyCostPerSecond",
                        0.5D,
                        MINIMUM_MULTIPLIER,
                        MAXIMUM_BASE_ABSORPTION_PER_SECOND
                );
        LIGHT_FLIGHT_BOOST_DURATION_TICKS = BUILDER
                .comment("Duration of the temporary light-flight boost, in ticks")
                .defineInRange("boostDurationTicks", 14, 1, 200);
        LIGHT_FLIGHT_BOOST_COOLDOWN_TICKS = BUILDER
                .comment("Cooldown between light-flight boosts, in ticks")
                .defineInRange("boostCooldownTicks", 40, 1, 1200);
        BUILDER.pop();

        BUILDER.push("shrinkEarth");
        SHRINK_EARTH_ENERGY_COST_PER_SECOND = BUILDER
                .comment("Spiritual energy consumed per second while shrink-earth is active")
                .defineInRange(
                        "energyCostPerSecond",
                        1.0D,
                        MINIMUM_MULTIPLIER,
                        MAXIMUM_BASE_ABSORPTION_PER_SECOND
                );
        BUILDER.pop();

        BUILDER.push("lifespan");
        MORTAL_PLAYER_LIFESPAN_YEARS = defineLifespanYears(
                "mortalPlayerYears",
                100,
                "Maximum lifespan in years for a mortal player"
        );
        QI_REFINING_LIFESPAN_YEARS = defineLifespanYears(
                "qiRefiningYears",
                120,
                "Maximum lifespan in years for a Qi Refining player"
        );
        FOUNDATION_ESTABLISHMENT_LIFESPAN_YEARS = defineLifespanYears(
                "foundationEstablishmentYears",
                200,
                "Maximum lifespan in years for a Foundation Establishment player"
        );
        GOLDEN_CORE_LIFESPAN_YEARS = defineLifespanYears(
                "goldenCoreYears",
                500,
                "Maximum lifespan in years for a Golden Core player"
        );
        NASCENT_SOUL_LIFESPAN_YEARS = defineLifespanYears(
                "nascentSoulYears",
                1000,
                "Maximum lifespan in years for a Nascent Soul player"
        );
        DEFAULT_CREATURE_LIFESPAN_YEARS = defineLifespanYears(
                "defaultCreatureYears",
                30,
                "Maximum lifespan in years for non-undead creatures"
        );
        BUILDER.pop();

        BUILDER.push("creatureCultivation");
        CREATURE_CULTIVATOR_CHANCE = BUILDER
                .comment(
                        "Chance for a newly spawned creature (spawn eggs included) to be born with",
                        "cultivation between Qi Refining layer one and Foundation Establishment layer three"
                )
                .defineInRange("cultivatorChance", 0.05D, 0.0D, 1.0D);
        BUILDER.pop();

        BUILDER.push("ai");
        AI_PROVIDER = BUILDER
                .comment("AI provider used to assess technique comprehension. Supported values: OPENAI, DEEPSEEK")
                .defineEnum("provider", AiProvider.OPENAI);
        AI_API_KEY = BUILDER
                .comment("Server-side API key for the selected provider. Keep the generated config file private")
                .define("apiKey", "");
        AI_MODEL = BUILDER
                .comment("Model id from the selected provider. This is intentionally configurable as models change")
                .define("model", "");
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private CommonConfig() {
    }

    private static ForgeConfigSpec.IntValue defineLifespanYears(
            String name,
            int defaultValue,
            String comment
    ) {
        return BUILDER
                .comment(comment, "One year equals 36 Minecraft days")
                .defineInRange(name, defaultValue, 1, MAXIMUM_LIFESPAN_YEARS);
    }
}
