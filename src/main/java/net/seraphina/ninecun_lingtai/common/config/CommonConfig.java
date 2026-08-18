package net.seraphina.ninecun_lingtai.common.config;

import net.seraphina.ninecun_lingtai.api.ai.AiProvider;
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
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_RESTRAINS_ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_ATTACK_GENERATES_DEFENSE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_DEFENSE_GENERATES_ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_ATTACK_RESTRAINS_DEFENSE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_YIN_YANG_BALANCE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_CHAOS_ATTACK_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_FIVE_ELEMENT_CYCLE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_CHAOS_DEFENSE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue METAL_BODY_SHIELD_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue WOOD_GUARDIAN_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue WATER_BODY_SHIELD_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue FIRE_GUARDIAN_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue EARTH_GUARDIAN_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue YIN_YANG_BODY_SHIELD_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue FIVE_ELEMENT_BODY_SHIELD_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue CHAOS_BODY_SHIELD_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue COMPOSITE_BODY_SHIELD_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.DoubleValue FOUR_PHASE_GUARDIAN_POINTS_PER_STRENGTH;
    public static final ForgeConfigSpec.IntValue DEFENSE_SPELL_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_REPAIR_PERCENT_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue DEFENSE_SPELL_REPAIR_ENERGY_PER_POINT;
    public static final ForgeConfigSpec.DoubleValue REALM_BONUS_BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue REALM_BONUS_LAYER_GROWTH;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_BASE_ABSORPTION_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_ABSORPTION_REQUIREMENT_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_DEFENSE_RESOURCE_REPAIR_PERCENT_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_GENERATES_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_GENERATED_BY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_OVERCOMES_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue MEDITATION_OVERCOME_BY_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue RANDOMIZE_PLAYER_APTITUDE;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_ROOT_QUALITY_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_SOUL_QUALITY_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_FOUR_NINE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_BASE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_APTITUDE_RADIUS_BONUS;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_BASE_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_STRIKE_GROWTH;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_MINIMUM_DAMAGE_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_APTITUDE_DAMAGE_BONUS;
    public static final ForgeConfigSpec.IntValue TRIBULATION_PREPARATION_TICKS;
    public static final ForgeConfigSpec.IntValue TRIBULATION_STRIKE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue TRIBULATION_LIGHTNING_LIFETIME_TICKS;
    public static final ForgeConfigSpec.IntValue TRIBULATION_PROMOTION_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue TRIBULATION_DISSIPATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_BOOST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_ENERGY_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue SHRINK_EARTH_ENERGY_COST_PER_SECOND;
    public static final ForgeConfigSpec.IntValue LIGHT_FLIGHT_BOOST_DURATION_TICKS;
    public static final ForgeConfigSpec.IntValue LIGHT_FLIGHT_BOOST_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue PUSHING_MOUNTAINS_BASE_SELECTION_DISTANCE;
    public static final ForgeConfigSpec.IntValue PUSHING_MOUNTAINS_MAX_SELECTION_DISTANCE;
    public static final ForgeConfigSpec.IntValue PUSHING_MOUNTAINS_MAX_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue PUSHING_MOUNTAINS_THROW_DAMAGE;
    public static final ForgeConfigSpec.IntValue PUSHING_MOUNTAINS_MAX_ENTITY_TICKS;
    public static final ForgeConfigSpec.DoubleValue PRESSURE_BASE_RANGE;
    public static final ForgeConfigSpec.DoubleValue PRESSURE_LAYER_RANGE_BONUS;
    public static final ForgeConfigSpec.DoubleValue PRESSURE_REALM_RANGE_BONUS;
    public static final ForgeConfigSpec.DoubleValue PRESSURE_MAX_RANGE;
    public static final ForgeConfigSpec.IntValue PRESSURE_INSTANT_DEATH_REALM_GAP;
    public static final ForgeConfigSpec.IntValue MORTAL_PLAYER_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue QI_REFINING_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue FOUNDATION_ESTABLISHMENT_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue GOLDEN_CORE_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue NASCENT_SOUL_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue DEFAULT_CREATURE_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.IntValue BOSS_CREATURE_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.DoubleValue CREATURE_CULTIVATOR_CHANCE;
    public static final ForgeConfigSpec.EnumValue<AiProvider> AI_PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> AI_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> AI_MODEL;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_FIRE_SCORE_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_ENVIRONMENT_SCORE_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_ROOT_SCORE_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_SOUL_SCORE_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_REALM_SCORE_WEIGHT;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_MAINTAIN_BASE_COST;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_MAINTAIN_TIER_GROWTH;
    public static final ForgeConfigSpec.IntValue ALCHEMY_STOKE_HEAT_PER_TICK;
    public static final ForgeConfigSpec.IntValue ALCHEMY_VENT_HEAT_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_STOKE_COST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_VENT_COST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_MAINTAIN_COST_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue ALCHEMY_EXPLOSION_BREAKS_BLOCKS;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_TOXIN_DECAY_PER_DAY;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_TOXIN_MEDITATION_DECAY_PER_MINUTE;
    public static final ForgeConfigSpec.IntValue THREE_THOUSAND_REALMS_MAXIMUM_REALM_COUNT;
    public static final ForgeConfigSpec.BooleanValue THREE_THOUSAND_REALMS_RANDOMIZE_ON_RESPAWN;
    public static final ForgeConfigSpec.IntValue THREE_THOUSAND_REALMS_RANDOM_SPAWN_RADIUS;
    public static final ForgeConfigSpec.IntValue THREE_THOUSAND_REALMS_UNLOAD_DELAY_TICKS;
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

        BUILDER.push("defenseSpells");
        DEFENSE_SPELL_RESTRAINS_ATTACK_MULTIPLIER = defineDefenseSpellMultiplier(
                "defenseRestrainsAttackMultiplier",
                0.60D,
                "Defense point cost when the spell element restrains the incoming element"
        );
        DEFENSE_SPELL_ATTACK_GENERATES_DEFENSE_MULTIPLIER = defineDefenseSpellMultiplier(
                "attackGeneratesDefenseMultiplier",
                0.75D,
                "Defense point cost when the incoming element generates the spell element"
        );
        DEFENSE_SPELL_DEFENSE_GENERATES_ATTACK_MULTIPLIER = defineDefenseSpellMultiplier(
                "defenseGeneratesAttackMultiplier",
                1.25D,
                "Defense point cost when the spell element generates the incoming element"
        );
        DEFENSE_SPELL_ATTACK_RESTRAINS_DEFENSE_MULTIPLIER = defineDefenseSpellMultiplier(
                "attackRestrainsDefenseMultiplier",
                1.50D,
                "Defense point cost when the incoming element restrains the spell element"
        );
        DEFENSE_SPELL_YIN_YANG_BALANCE_MULTIPLIER = defineDefenseSpellMultiplier(
                "yinYangBalanceMultiplier",
                0.85D,
                "Defense point cost for five-element damage balanced by a yin-yang spell"
        );
        DEFENSE_SPELL_CHAOS_ATTACK_MULTIPLIER = defineDefenseSpellMultiplier(
                "chaosAttackMultiplier",
                2.0D,
                "Defense point cost when chaos damage strikes a non-chaos defense spell"
        );
        DEFENSE_SPELL_FIVE_ELEMENT_CYCLE_MULTIPLIER = defineDefenseSpellMultiplier(
                "fiveElementCycleMultiplier",
                0.70D,
                "Defense point cost for five-element damage against the five-element cycle spell"
        );
        DEFENSE_SPELL_CHAOS_DEFENSE_MULTIPLIER = defineDefenseSpellMultiplier(
                "chaosDefenseMultiplier",
                0.50D,
                "Defense point cost for non-chaos elemental damage against a chaos defense spell"
        );
        METAL_BODY_SHIELD_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "metalBodyShieldPointsPerStrength", 16.0D
        );
        WOOD_GUARDIAN_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "woodGuardianPointsPerStrength", 12.0D
        );
        WATER_BODY_SHIELD_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "waterBodyShieldPointsPerStrength", 13.0D
        );
        FIRE_GUARDIAN_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "fireGuardianPointsPerStrength", 11.0D
        );
        EARTH_GUARDIAN_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "earthGuardianPointsPerStrength", 18.0D
        );
        YIN_YANG_BODY_SHIELD_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "yinYangBodyShieldPointsPerStrength", 10.0D
        );
        FIVE_ELEMENT_BODY_SHIELD_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "fiveElementBodyShieldPointsPerStrength", 14.0D
        );
        CHAOS_BODY_SHIELD_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "chaosBodyShieldPointsPerStrength", 8.0D
        );
        COMPOSITE_BODY_SHIELD_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "compositeBodyShieldPointsPerStrength", 11.0D
        );
        FOUR_PHASE_GUARDIAN_POINTS_PER_STRENGTH = defineDefenseSpellPoints(
                "fourPhaseGuardianPointsPerStrength", 14.0D
        );
        DEFENSE_SPELL_DURATION_TICKS = BUILDER
                .comment("Base defense spell duration in ticks before mastery scaling")
                .defineInRange("durationTicks", 6000, 20, 72000);
        DEFENSE_SPELL_REPAIR_PERCENT_PER_SECOND = BUILDER
                .comment("Percentage of maximum defense points repaired per second while spiritual energy is available")
                .defineInRange(
                        "repairPercentPerSecond",
                        2.0D,
                        MINIMUM_MULTIPLIER,
                        MAXIMUM_MULTIPLIER
                );
        DEFENSE_SPELL_REPAIR_ENERGY_PER_POINT = BUILDER
                .comment("Spiritual energy consumed for each defense point repaired")
                .defineInRange(
                        "repairEnergyPerPoint",
                        0.1D,
                        MINIMUM_MULTIPLIER,
                        MAXIMUM_BASE_ABSORPTION_PER_SECOND
                );
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
        MEDITATION_DEFENSE_RESOURCE_REPAIR_PERCENT_PER_SECOND = BUILDER
                .comment("Percentage of the maximum defense resource repaired per second while meditating")
                .defineInRange(
                        "defenseResourceRepairPercentPerSecond",
                        1.0D,
                        MINIMUM_MULTIPLIER,
                        MAXIMUM_MULTIPLIER
                );
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

        BUILDER.push("aptitude");
        RANDOMIZE_PLAYER_APTITUDE = BUILDER
                .comment(
                        "Whether players without a complete aptitude receive a random aptitude",
                        "instead of choosing their spiritual root and soul strength"
                )
                .define("randomizePlayerAptitude", true);
        BUILDER.pop();

        BUILDER.push("tribulation");
        TRIBULATION_ROOT_QUALITY_WEIGHT = BUILDER
                .comment("Weight of spiritual-root quality when selecting a heavenly tribulation")
                .defineInRange("rootQualityWeight", 0.55D, 0.0D, 1.0D);
        TRIBULATION_SOUL_QUALITY_WEIGHT = BUILDER
                .comment("Weight of soul-strength quality when selecting a heavenly tribulation")
                .defineInRange("soulQualityWeight", 0.45D, 0.0D, 1.0D);
        TRIBULATION_FOUR_NINE_THRESHOLD = BUILDER
                .comment("Minimum combined aptitude quality required for the four-nine tribulation")
                .defineInRange("fourNineThreshold", 0.70D, 0.0D, 1.0D);
        TRIBULATION_BASE_RADIUS = BUILDER
                .comment("Base radius of a tribulation in blocks")
                .defineInRange("baseRadius", 30.0D, 1.0D, 256.0D);
        TRIBULATION_APTITUDE_RADIUS_BONUS = BUILDER
                .comment("Additional tribulation radius granted by perfect aptitude")
                .defineInRange("aptitudeRadiusBonus", 30.0D, 0.0D, 256.0D);
        TRIBULATION_BASE_DAMAGE = BUILDER
                .comment("Base damage of the first tribulation lightning strike")
                .defineInRange("baseDamage", 6.0D, 0.1D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        TRIBULATION_STRIKE_GROWTH = BUILDER
                .comment("Damage growth applied to every subsequent lightning strike")
                .defineInRange("strikeGrowth", 0.08D, 0.0D, MAXIMUM_MULTIPLIER);
        TRIBULATION_MINIMUM_DAMAGE_MULTIPLIER = BUILDER
                .comment("Damage multiplier for the weakest aptitude")
                .defineInRange("minimumDamageMultiplier", 0.8D, 0.0D, MAXIMUM_MULTIPLIER);
        TRIBULATION_APTITUDE_DAMAGE_BONUS = BUILDER
                .comment("Additional damage multiplier granted by perfect aptitude")
                .defineInRange("aptitudeDamageBonus", 0.7D, 0.0D, MAXIMUM_MULTIPLIER);
        TRIBULATION_PREPARATION_TICKS = BUILDER
                .comment("Time spent gathering the tribulation clouds")
                .defineInRange("preparationTicks", 200, 1, 72000);
        TRIBULATION_STRIKE_INTERVAL_TICKS = BUILDER
                .comment("Time between successive tribulation lightning strikes")
                .defineInRange("strikeIntervalTicks", 30, 1, 72000);
        TRIBULATION_LIGHTNING_LIFETIME_TICKS = BUILDER
                .comment("How long a custom tribulation lightning strike remains visible and damaging")
                .defineInRange("lightningLifetimeTicks", 60, 1, 72000);
        TRIBULATION_PROMOTION_DELAY_TICKS = BUILDER
                .comment("Delay after surviving the final strike before promotion")
                .defineInRange("promotionDelayTicks", 100, 1, 72000);
        TRIBULATION_DISSIPATION_TICKS = BUILDER
                .comment("Time for the tribulation clouds to dissipate after promotion")
                .defineInRange("dissipationTicks", 100, 1, 72000);
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

        BUILDER.push("pushingMountains");
        PUSHING_MOUNTAINS_BASE_SELECTION_DISTANCE = BUILDER
                .comment("Selection distance at Golden Core layer one")
                .defineInRange("baseSelectionDistance", 24, 1, 400);
        PUSHING_MOUNTAINS_MAX_SELECTION_DISTANCE = BUILDER
                .comment("Hard maximum selection distance")
                .defineInRange("maxSelectionDistance", 400, 1, 400);
        PUSHING_MOUNTAINS_MAX_BLOCKS = BUILDER
                .comment("Maximum number of non-air blocks that may be entityized at once")
                .defineInRange("maxBlocks", 32768, 1, 1048576);
        PUSHING_MOUNTAINS_THROW_DAMAGE = BUILDER
                .comment("Damage dealt to living entities inside a thrown region when it annihilates")
                .defineInRange("throwDamage", 100.0D, 0.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        PUSHING_MOUNTAINS_MAX_ENTITY_TICKS = BUILDER
                .comment("Maximum lifetime of a thrown region before it annihilates")
                .defineInRange("maxEntityTicks", 1200, 1, 72000);
        BUILDER.pop();

        BUILDER.push("pressure");
        PRESSURE_BASE_RANGE = BUILDER
                .comment("Base pressure radius in blocks")
                .defineInRange("baseRange", 5.0D, 0.0D, 2000.0D);
        PRESSURE_LAYER_RANGE_BONUS = BUILDER
                .comment("Pressure radius added for each completed minor layer")
                .defineInRange("layerRangeBonus", 1.0D, 0.0D, 2000.0D);
        PRESSURE_REALM_RANGE_BONUS = BUILDER
                .comment("Pressure radius added for each completed major realm")
                .defineInRange("realmRangeBonus", 30.0D, 0.0D, 2000.0D);
        PRESSURE_MAX_RANGE = BUILDER
                .comment("Hard maximum pressure radius in blocks")
                .defineInRange("maxRange", 2000.0D, 0.0D, 2000.0D);
        PRESSURE_INSTANT_DEATH_REALM_GAP = BUILDER
                .comment("Major-realm gap required for pressure to instantly kill a lower target")
                .defineInRange("instantDeathRealmGap", 2, 1, 9);
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
        BOSS_CREATURE_LIFESPAN_YEARS = defineLifespanYears(
                "bossCreatureYears",
                1000,
                "Base maximum lifespan for boss-level creatures such as Wardens, Withers, and Ender Dragons"
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

        BUILDER.push("alchemy");
        ALCHEMY_FIRE_SCORE_WEIGHT = defineAlchemyScoreWeight("fireScoreWeight", 0.35D);
        ALCHEMY_ENVIRONMENT_SCORE_WEIGHT = defineAlchemyScoreWeight("envScoreWeight", 0.20D);
        ALCHEMY_ROOT_SCORE_WEIGHT = defineAlchemyScoreWeight("rootScoreWeight", 0.15D);
        ALCHEMY_SOUL_SCORE_WEIGHT = defineAlchemyScoreWeight("soulScoreWeight", 0.15D);
        ALCHEMY_REALM_SCORE_WEIGHT = defineAlchemyScoreWeight("realmScoreWeight", 0.15D);
        ALCHEMY_MAINTAIN_BASE_COST = BUILDER
                .comment("Spiritual energy per second to maintain medium heat for a tier-one recipe")
                .defineInRange("maintainBaseCost", 0.4D, 0.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        ALCHEMY_MAINTAIN_TIER_GROWTH = BUILDER
                .comment("Heat maintenance cost multiplier applied for every recipe tier above one")
                .defineInRange("maintainTierGrowth", 1.9D, 1.0D, 10.0D);
        ALCHEMY_STOKE_HEAT_PER_TICK = BUILDER
                .comment("Heat added per tick while the player holds the stoke control")
                .defineInRange("stokeHeatPerTick", 3, 1, 100);
        ALCHEMY_VENT_HEAT_PER_TICK = BUILDER
                .comment("Heat removed per tick while the player holds the vent control")
                .defineInRange("ventHeatPerTick", 4, 1, 100);
        ALCHEMY_STOKE_COST_MULTIPLIER = BUILDER
                .comment("Spiritual-energy cost multiplier for stoking")
                .defineInRange("stokeCostMultiplier", 1.4D, 0.0D, 10.0D);
        ALCHEMY_VENT_COST_MULTIPLIER = BUILDER
                .comment("Spiritual-energy cost multiplier for venting")
                .defineInRange("ventCostMultiplier", 1.0D, 0.0D, 10.0D);
        ALCHEMY_MAINTAIN_COST_MULTIPLIER = BUILDER
                .comment("Spiritual-energy cost multiplier for maintaining the target heat band")
                .defineInRange("maintainCostMultiplier", 1.0D, 0.0D, 10.0D);
        ALCHEMY_EXPLOSION_BREAKS_BLOCKS = BUILDER
                .comment("Whether a furnace explosion caused by harvesting too early destroys blocks")
                .define("explosionBreaksBlocks", false);
        ALCHEMY_TOXIN_DECAY_PER_DAY = BUILDER
                .comment("Pill toxin naturally dissipated per full game day")
                .defineInRange("toxinDecayPerDay", 10.0D, 0.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        ALCHEMY_TOXIN_MEDITATION_DECAY_PER_MINUTE = BUILDER
                .comment("Additional pill toxin dissipated per minute while meditating")
                .defineInRange("toxinMeditationDecayPerMinute", 2.0D, 0.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        BUILDER.pop();

        BUILDER.push("threeThousandRealms");
        THREE_THOUSAND_REALMS_MAXIMUM_REALM_COUNT = BUILDER
                .comment("Maximum number of dynamically created realms. This hard-caps at 10")
                .defineInRange("maximumRealmCount", 10, 1, 10);
        THREE_THOUSAND_REALMS_RANDOMIZE_ON_RESPAWN = BUILDER
                .comment(
                        "Whether player respawns are redirected to a random realm instead of",
                        "returning to the three-thousand realm where they died"
                )
                .define("randomizeOnRespawn", false);
        THREE_THOUSAND_REALMS_RANDOM_SPAWN_RADIUS = BUILDER
                .comment("Maximum horizontal distance from the realm origin used for random player placement")
                .defineInRange("randomSpawnRadius", 10000, 128, 1000000);
        THREE_THOUSAND_REALMS_UNLOAD_DELAY_TICKS = BUILDER
                .comment("Ticks an empty dynamically created realm stays loaded before it is unloaded")
                .defineInRange("unloadDelayTicks", 20 * 30, 20, 20 * 60 * 60);
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

    private static ForgeConfigSpec.DoubleValue defineDefenseSpellMultiplier(
            String name,
            double defaultValue,
            String comment
    ) {
        return BUILDER.comment(comment)
                .defineInRange(name, defaultValue, MINIMUM_MULTIPLIER, MAXIMUM_MULTIPLIER);
    }

    private static ForgeConfigSpec.DoubleValue defineDefenseSpellPoints(String name, double defaultValue) {
        return BUILDER.comment("Base defense points gained per cultivation strength before mastery scaling")
                .defineInRange(name, defaultValue, 0.1D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
    }

    private static ForgeConfigSpec.DoubleValue defineAlchemyScoreWeight(String name, double defaultValue) {
        return BUILDER
                .comment("Alchemy score weight; the five weights should sum to one or defaults are used")
                .defineInRange(name, defaultValue, 0.0D, 1.0D);
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
