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
    public static final ForgeConfigSpec.IntValue TRIBULATION_INNER_DEMON_COUNT;
    public static final ForgeConfigSpec.IntValue TRIBULATION_INNER_DEMON_DURATION_TICKS;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_INNER_DEMON_SOUL_DAMAGE;
    public static final ForgeConfigSpec.IntValue TRIBULATION_FAILURE_LIFESPAN_YEARS;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_REALM_STRENGTH_GROWTH;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_FAILURE_LIFESPAN_SCALE_DEATH;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_FAILURE_LIFESPAN_SCALE_LOGOUT;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_FAILURE_LIFESPAN_SCALE_DIMENSION_CHANGED;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_FAILURE_LIFESPAN_SCALE_INNER_DEMON_TIMEOUT;
    public static final ForgeConfigSpec.DoubleValue TRIBULATION_FAILURE_LIFESPAN_SCALE_INTERRUPTED;
    public static final ForgeConfigSpec.DoubleValue SOUL_SENSE_BASE_RANGE;
    public static final ForgeConfigSpec.DoubleValue SOUL_SENSE_PER_STAR_RANGE;
    public static final ForgeConfigSpec.DoubleValue SOUL_SENSE_REALM_RANGE_BONUS;
    public static final ForgeConfigSpec.DoubleValue SOUL_SENSE_MAX_RANGE;
    public static final ForgeConfigSpec.DoubleValue SOUL_SENSE_BLOCK_RANGE;
    public static final ForgeConfigSpec.DoubleValue SOUL_SENSE_BASE_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue SOUL_SENSE_RANGE_COST_FACTOR;
    public static final ForgeConfigSpec.IntValue SOUL_SENSE_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue SOUL_SENSE_BACKLASH_STAR_GAP;
    public static final ForgeConfigSpec.IntValue SOUL_SENSE_BLOCK_SCAN_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue SOUL_SENSE_BLOCK_CACHE_MAX_AGE_TICKS;
    public static final ForgeConfigSpec.IntValue RISK_BREAKTHROUGH_FAILURE_BASE_BUDGET;
    public static final ForgeConfigSpec.IntValue RISK_TRIBULATION_FAILURE_BASE_BUDGET;
    public static final ForgeConfigSpec.IntValue RISK_DEATH_BASE_BUDGET;
    public static final ForgeConfigSpec.IntValue RISK_QI_DEVIATION_BASE_BUDGET;
    public static final ForgeConfigSpec.DoubleValue RISK_REALM_BUDGET_STEP;
    public static final ForgeConfigSpec.DoubleValue RISK_CORE_LOSS_RATIO_PER_BUDGET;
    public static final ForgeConfigSpec.DoubleValue RISK_MAX_CORE_LOSS_RATIO;
    public static final ForgeConfigSpec.IntValue RISK_LONG_TERM_SCAR_BUDGET_THRESHOLD;
    public static final ForgeConfigSpec.IntValue RISK_SHORT_TERM_SCAR_BUDGET_THRESHOLD;
    public static final ForgeConfigSpec.IntValue RISK_SEVERITY_BUDGET_STEP;
    public static final ForgeConfigSpec.LongValue RISK_SHORT_TERM_DURATION_TICKS_PER_BUDGET;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_FOUNDATION;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_GOLDEN_CORE;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_NASCENT_SOUL;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_SPIRIT_TRANSFORMATION;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_DAO_INTEGRATION;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_VOID_REFINEMENT;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_MAHAYANA;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_BASE_RATE_IMMORTAL;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_CHANCE_FLOOR;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_CHANCE_CEILING;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_PITY_STEP;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_PITY_CAP;
    public static final ForgeConfigSpec.IntValue BREAKTHROUGH_RITUAL_SECONDS;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_FAIL_DISSIPATION_LOW;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_FAIL_DISSIPATION_MID;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_FAIL_DISSIPATION_HIGH;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_INTERRUPT_DISSIPATION;
    public static final ForgeConfigSpec.IntValue BREAKTHROUGH_BOTTLENECK_DAYS;
    public static final ForgeConfigSpec.BooleanValue BREAKTHROUGH_INTERRUPT_ON_ENTITY_DAMAGE_ONLY;
    public static final ForgeConfigSpec.LongValue BREAKTHROUGH_INSIGHT_CHOICE_EXPIRE_TICKS;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_QUALITY_COEF_TURBID;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_QUALITY_COEF_NEUTRAL;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_QUALITY_COEF_CLEAR;
    public static final ForgeConfigSpec.DoubleValue BREAKTHROUGH_QUALITY_COEF_PRISTINE;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_THRESHOLD_DAZED;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_THRESHOLD_SHAKEN;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_THRESHOLD_PERIL;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SOURCE_PIERCE;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SOURCE_SHOCK;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SOURCE_BACKLASH;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SOURCE_WEAK_CAST;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SOURCE_SENSE_RECOIL;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SOURCE_MIND_DEMON_HIT;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_NATURAL_HEAL_PER_DAY;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SEVEN_STAR_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SOUL_INJURY_PERIL_LIFESPAN_YEARS_PER_DAY;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SENSE_RANGE_MULT_DAZED;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_SENSE_RANGE_MULT_SHAKEN;
    public static final ForgeConfigSpec.DoubleValue SOUL_INJURY_COMPREHENSION_PENALTY;
    public static final ForgeConfigSpec.BooleanValue SOUL_INJURY_SEVEN_STAR_REQUIRES_OPPORTUNITY;
    public static final ForgeConfigSpec.BooleanValue MIND_DEMON_ENABLED;
    public static final ForgeConfigSpec.IntValue MIND_DEMON_REVEAL_SECONDS;
    public static final ForgeConfigSpec.IntValue MIND_DEMON_SPAR_SECONDS;
    public static final ForgeConfigSpec.IntValue MIND_DEMON_REFLECT_SECONDS;
    public static final ForgeConfigSpec.IntValue MIND_DEMON_RESOLVE_CHOICE_SECONDS;
    public static final ForgeConfigSpec.IntValue MIND_DEMON_SEVER_TIME_LIMIT_SECONDS;
    public static final ForgeConfigSpec.IntValue MIND_DEMON_HOLD_TIME_LIMIT_SECONDS;
    public static final ForgeConfigSpec.DoubleValue MIND_DEMON_BODY_ECHO_RATIO;
    public static final ForgeConfigSpec.DoubleValue MIND_DEMON_KARMA_SCALE_BASE;
    public static final ForgeConfigSpec.DoubleValue MIND_DEMON_KARMA_SCALE_PER_KILL;
    public static final ForgeConfigSpec.DoubleValue MIND_DEMON_KARMA_SCALE_CAP;
    public static final ForgeConfigSpec.IntValue MIND_DEMON_POCKET_STRIDE;
    public static final ForgeConfigSpec.BooleanValue MIND_DEMON_PVE_OFFLINE_PAUSE;
    public static final ForgeConfigSpec.DoubleValue MIND_DEMON_SOUL_INJURY_CAP_PER_TRIAL;
    public static final ForgeConfigSpec.DoubleValue SOUL_TEMPERING_BASE_PROGRESS;
    public static final ForgeConfigSpec.DoubleValue SOUL_TEMPERING_STAR_GROWTH;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_BOOST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_ENERGY_COST_PER_SECOND;
    public static final ForgeConfigSpec.DoubleValue LIGHT_FLIGHT_CHUNK_LOADING_SPEED_THRESHOLD;
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
    public static final ForgeConfigSpec.DoubleValue JADE_SLIP_LOOT_CHEST_CHANCE;
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
    public static final ForgeConfigSpec.DoubleValue KARMA_VILLAGER_KILL_KARMA;
    public static final ForgeConfigSpec.DoubleValue KARMA_CULTIVATOR_KILL_KARMA;
    public static final ForgeConfigSpec.DoubleValue KARMA_ALLY_KILL_KARMA;
    public static final ForgeConfigSpec.DoubleValue KARMA_CAP;
    public static final ForgeConfigSpec.DoubleValue KARMA_TIER_NEUTRAL_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue KARMA_TIER_STAINED_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue KARMA_TIER_DRENCHED_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue KARMA_DAO_HEART_PURE;
    public static final ForgeConfigSpec.DoubleValue KARMA_DAO_HEART_NEUTRAL;
    public static final ForgeConfigSpec.DoubleValue KARMA_DAO_HEART_STAINED;
    public static final ForgeConfigSpec.DoubleValue KARMA_DAO_HEART_DRENCHED;
    public static final ForgeConfigSpec.DoubleValue AURA_RESERVE_CAP_UNITS;
    public static final ForgeConfigSpec.DoubleValue AURA_REGEN_RATIO_PER_DAY;
    public static final ForgeConfigSpec.DoubleValue AURA_FULL_SPEED_RESERVE_RATIO;
    public static final ForgeConfigSpec.DoubleValue AURA_EXHAUSTED_RESERVE_UNITS;
    public static final ForgeConfigSpec.DoubleValue AURA_HARVEST_RESERVE_REFUND_UNITS;
    public static final ForgeConfigSpec.IntValue AURA_OVERHARVEST_DAILY_THRESHOLD;
    public static final ForgeConfigSpec.IntValue AURA_OVERHARVEST_PENALTY_TIERS;
    public static final ForgeConfigSpec.IntValue AURA_OVERHARVEST_PENALTY_DAYS;
    public static final ForgeConfigSpec.IntValue AURA_QUALITY_CACHE_TTL_TICKS;
    public static final ForgeConfigSpec.IntValue AURA_POPULATION_SCAN_RADIUS;
    public static final ForgeConfigSpec.IntValue AURA_POPULATION_LIGHT_HOMES;
    public static final ForgeConfigSpec.IntValue AURA_POPULATION_HEAVY_HOMES;
    public static final ForgeConfigSpec.IntValue AURA_HIGHLAND_MIN_SURFACE_Y;
    public static final ForgeConfigSpec.IntValue ALCHEMY_FURNACE_DURABILITY_LOSS_PER_REFINE;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_HERB_AGE_POTENCY_CAP;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_FIRE_SOURCE_SPIRITUAL_STABILITY;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_FIRE_SOURCE_EARTH_STABILITY;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_FIRE_SOURCE_SAMADHI_STABILITY;
    public static final ForgeConfigSpec.IntValue ALCHEMY_FIRE_SOURCE_EARTH_HEAT_BONUS;
    public static final ForgeConfigSpec.IntValue ALCHEMY_FIRE_SOURCE_SAMADHI_HEAT_BONUS;
    public static final ForgeConfigSpec.IntValue THREE_THOUSAND_REALMS_MAXIMUM_REALM_COUNT;
    public static final ForgeConfigSpec.BooleanValue THREE_THOUSAND_REALMS_RANDOMIZE_ON_RESPAWN;
    public static final ForgeConfigSpec.IntValue THREE_THOUSAND_REALMS_RANDOM_SPAWN_RADIUS;
    public static final ForgeConfigSpec.IntValue THREE_THOUSAND_REALMS_UNLOAD_DELAY_TICKS;
    // 阵禁（seal 组）
    public static final ForgeConfigSpec.IntValue SEAL_ANCHORS_PER_SOUL_STAR;
    public static final ForgeConfigSpec.DoubleValue SEAL_CARVE_ENERGY_PER_GRADE;
    public static final ForgeConfigSpec.DoubleValue SEAL_CARVE_TEMPERING_COST_PER_GRADE;
    public static final ForgeConfigSpec.IntValue SEAL_CARVE_RITUAL_TICKS;
    public static final ForgeConfigSpec.IntValue SEAL_REVOKE_TICKS;
    public static final ForgeConfigSpec.DoubleValue SEAL_LOCK_GRID_FAIL_SOUL_INJURY;
    public static final ForgeConfigSpec.IntValue SEAL_LOCK_GRID_DISTURB_LOCK_DAYS;
    public static final ForgeConfigSpec.DoubleValue SEAL_LOCK_GRID_ENERGY_PER_SECOND_PER_GRADE;
    public static final ForgeConfigSpec.DoubleValue SEAL_FORCE_BREAK_CRUSH_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SEAL_FORCE_BREAK_BASE_SECONDS;
    public static final ForgeConfigSpec.DoubleValue SEAL_FORCE_BREAK_ENERGY_PER_SECOND;
    public static final ForgeConfigSpec.IntValue SEAL_DEATH_PACT_DAYS;
    public static final ForgeConfigSpec.DoubleValue SEAL_DEATH_PACT_STRENGTH_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue SEAL_DECAY_DAYS;
    public static final ForgeConfigSpec.IntValue SEAL_DEATH_PACT_ALERT_INTERVAL_TICKS;
    // 死亡路线（death 组）
    public static final ForgeConfigSpec.IntValue DEATH_CHOOSE_TIMEOUT_SECONDS;
    public static final ForgeConfigSpec.IntValue DEATH_RESPAWN_QI_LAYER_LOSS;
    public static final ForgeConfigSpec.DoubleValue DEATH_RESPAWN_PROGRESS_LOSS_RATIO;
    public static final ForgeConfigSpec.DoubleValue DEATH_RESPAWN_SOUL_INJURY;
    public static final ForgeConfigSpec.IntValue DEATH_SOUL_RETURN_MINUTES_PER_STAR;
    public static final ForgeConfigSpec.DoubleValue DEATH_SOUL_RETURN_SUCCESS_SOUL_INJURY;
    public static final ForgeConfigSpec.DoubleValue DEATH_SOUL_RETURN_TIMEOUT_EXTRA_SOUL_INJURY;
    public static final ForgeConfigSpec.DoubleValue DEATH_BURN_NASCENT_LIFESPAN_LOSS_RATIO;
    public static final ForgeConfigSpec.BooleanValue DEATH_PVE_MODE;
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
        TRIBULATION_INNER_DEMON_COUNT = BUILDER
                .comment("Number of inner-demon phantoms spawned during an inner-demon phase")
                .defineInRange("innerDemonCount", 4, 1, 16);
        TRIBULATION_INNER_DEMON_DURATION_TICKS = BUILDER
                .comment("Maximum duration of an inner-demon phase")
                .defineInRange("innerDemonDurationTicks", 300, 20, 72000);
        TRIBULATION_INNER_DEMON_SOUL_DAMAGE = BUILDER
                .comment("Damage suffered every second while inner demons remain alive")
                .defineInRange("innerDemonSoulDamage", 2.0D, 0.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        TRIBULATION_FAILURE_LIFESPAN_YEARS = BUILDER
                .comment("Lifespan lost when failing a tribulation at Nascent Soul or above")
                .defineInRange("failureLifespanYears", 10, 0, MAXIMUM_LIFESPAN_YEARS);
        TRIBULATION_REALM_STRENGTH_GROWTH = BUILDER
                .comment("Extra strength multiplier per realm above Spirit Transformation for nine-nine tribulations")
                .defineInRange("realmStrengthGrowth", 0.6D, 0.0D, MAXIMUM_MULTIPLIER);
        TRIBULATION_FAILURE_LIFESPAN_SCALE_DEATH = defineFailureLifespanScale(
                "failureLifespanScaleDeath", 1.0D, "dying to the tribulation (BODY_SLAIN uses the same scale)"
        );
        TRIBULATION_FAILURE_LIFESPAN_SCALE_LOGOUT = defineFailureLifespanScale(
                "failureLifespanScaleLogout", 1.5D, "logging out mid-tribulation (deserting is punished hardest)"
        );
        TRIBULATION_FAILURE_LIFESPAN_SCALE_DIMENSION_CHANGED = defineFailureLifespanScale(
                "failureLifespanScaleDimensionChanged", 1.2D, "escaping through another dimension"
        );
        TRIBULATION_FAILURE_LIFESPAN_SCALE_INNER_DEMON_TIMEOUT = defineFailureLifespanScale(
                "failureLifespanScaleInnerDemonTimeout", 1.0D, "failing the inner-demon phase"
        );
        TRIBULATION_FAILURE_LIFESPAN_SCALE_INTERRUPTED = defineFailureLifespanScale(
                "failureLifespanScaleInterrupted", 0.8D, "external interruption (lightest scale)"
        );
        BUILDER.pop();

        BUILDER.push("soulSense");
        SOUL_SENSE_BASE_RANGE = BUILDER
                .comment("Soul-sense radius in blocks at one illuminated star")
                .defineInRange("baseRange", 8.0D, 1.0D, 512.0D);
        SOUL_SENSE_PER_STAR_RANGE = BUILDER
                .comment("Additional radius granted by every star beyond the first")
                .defineInRange("perStarRange", 4.0D, 0.0D, 512.0D);
        SOUL_SENSE_REALM_RANGE_BONUS = BUILDER
                .comment("Radius multiplier bonus per major realm")
                .defineInRange("realmRangeBonus", 0.5D, 0.0D, MAXIMUM_MULTIPLIER);
        SOUL_SENSE_MAX_RANGE = BUILDER
                .comment("Hard maximum soul-sense radius in blocks")
                .defineInRange("maxRange", 128.0D, 1.0D, 512.0D);
        SOUL_SENSE_BLOCK_RANGE = BUILDER
                .comment("Maximum radius when scanning blocks such as ores and containers")
                .defineInRange("blockRange", 16.0D, 1.0D, 64.0D);
        SOUL_SENSE_BASE_COST_PER_SECOND = BUILDER
                .comment("Base spiritual energy consumed per second while soul sense is active")
                .defineInRange("baseCostPerSecond", 0.3D, 0.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        SOUL_SENSE_RANGE_COST_FACTOR = BUILDER
                .comment("Extra energy per second for every block of effective radius")
                .defineInRange("rangeCostFactor", 0.02D, 0.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        SOUL_SENSE_SCAN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between soul-sense scans")
                .defineInRange("scanIntervalTicks", 10, 1, 200);
        SOUL_SENSE_BACKLASH_STAR_GAP = BUILDER
                .comment("Star gap at or above which scanning a mightier soul triggers backlash")
                .defineInRange("backlashStarGap", 2, 1, 7);
        SOUL_SENSE_BLOCK_SCAN_INTERVAL_TICKS = BUILDER
                .comment("Ticks between block scans, throttled separately from entity scans")
                .defineInRange("blockScanIntervalTicks", 40, 1, 1200);
        SOUL_SENSE_BLOCK_CACHE_MAX_AGE_TICKS = BUILDER
                .comment("Hard expiry age in ticks for cached block scan results")
                .defineInRange("blockCacheMaxAgeTicks", 200, 20, 6000);
        BUILDER.pop();

        BUILDER.push("soulTempering");
        SOUL_TEMPERING_BASE_PROGRESS = BUILDER
                .comment("Tempering progress required to illuminate the second star")
                .defineInRange("baseProgress", 100.0D, 1.0D, MAXIMUM_BASE_ABSORPTION_PER_SECOND);
        SOUL_TEMPERING_STAR_GROWTH = BUILDER
                .comment("Progress requirement multiplier applied for every star already illuminated")
                .defineInRange("starGrowth", 2.0D, 1.0D, MAXIMUM_MULTIPLIER);
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
        LIGHT_FLIGHT_CHUNK_LOADING_SPEED_THRESHOLD = BUILDER
                .comment(
                        "Movement distance per server tick above which light-flight chunk tracking is suppressed",
                        "The current and previous server tick are combined so several small packets can also trigger suppression",
                        "Tracking stays suppressed until a slow packet arrives or no movement packet arrives for a few ticks"
                )
                .defineInRange("chunkLoadingSpeedThreshold", 16.0D, 0.0D, 1000000.0D);
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

        BUILDER.push("loot");
        JADE_SLIP_LOOT_CHEST_CHANCE = BUILDER
                .comment(
                        "Chance for a loot container to receive one random jade slip after its loot generates",
                        "Includes blank jade slips, technique jade slips, blank formula slips, and alchemy recipes"
                )
                .defineInRange("jadeSlipLootChestChance", 0.000005D, 0.0D, 1.0D);
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

        BUILDER.push("karma");
        KARMA_VILLAGER_KILL_KARMA = BUILDER
                .comment("Karma gained per villager kill")
                .defineInRange("villagerKillKarma", 3.0D, 0.0D, 1000.0D);
        KARMA_CULTIVATOR_KILL_KARMA = BUILDER
                .comment("Karma gained per cultivator-creature kill")
                .defineInRange("cultivatorKillKarma", 5.0D, 0.0D, 1000.0D);
        KARMA_ALLY_KILL_KARMA = BUILDER
                .comment("Karma gained per player kill")
                .defineInRange("allyKillKarma", 5.0D, 0.0D, 1000.0D);
        KARMA_CAP = BUILDER
                .comment("Hard cap of the raw karma value")
                .defineInRange("karmaCap", 200.0D, 1.0D, 100000.0D);
        KARMA_TIER_NEUTRAL_THRESHOLD = BUILDER
                .comment("Karma at which the Neutral (slightly flawed) tier begins")
                .defineInRange("tierNeutralThreshold", 20.0D, 0.0D, 100000.0D);
        KARMA_TIER_STAINED_THRESHOLD = BUILDER
                .comment("Karma at which the Stained tier begins; also the blood-mark threshold for soul sense")
                .defineInRange("tierStainedThreshold", 80.0D, 0.0D, 100000.0D);
        KARMA_TIER_DRENCHED_THRESHOLD = BUILDER
                .comment("Karma at which the Drenched tier begins")
                .defineInRange("tierDrenchedThreshold", 140.0D, 0.0D, 100000.0D);
        KARMA_DAO_HEART_PURE = defineDaoHeartCoefficient("daoHeartPure", 1.05D);
        KARMA_DAO_HEART_NEUTRAL = defineDaoHeartCoefficient("daoHeartNeutral", 1.00D);
        KARMA_DAO_HEART_STAINED = defineDaoHeartCoefficient("daoHeartStained", 0.92D);
        KARMA_DAO_HEART_DRENCHED = defineDaoHeartCoefficient("daoHeartDrenched", 0.80D);
        BUILDER.pop();

        BUILDER.push("aura");
        AURA_RESERVE_CAP_UNITS = BUILDER
                .comment(
                        "Spiritual reserve cap of one aura cell, in full-speed meditation ticks",
                        "Default 3600 = one player meditating at full speed for 3 cultivation days"
                )
                .defineInRange("reserveCapUnits", 3600.0D, 1.0D, 10000000.0D);
        AURA_REGEN_RATIO_PER_DAY = BUILDER
                .comment("Fraction of the reserve cap regenerated per cultivation day")
                .defineInRange("regenRatioPerDay", 0.05D, 0.0D, 1.0D);
        AURA_FULL_SPEED_RESERVE_RATIO = BUILDER
                .comment("Reserve ratio above which absorption stays at full speed; below it decays linearly to zero")
                .defineInRange("fullSpeedReserveRatio", 0.30D, 0.0D, 1.0D);
        AURA_EXHAUSTED_RESERVE_UNITS = BUILDER
                .comment("Reserve units below which the cell counts as exhausted (absorption drops to zero)")
                .defineInRange("exhaustedReserveUnits", 1.0D, 0.0D, 100000.0D);
        AURA_HARVEST_RESERVE_REFUND_UNITS = BUILDER
                .comment("Reserve units refunded to the cell when a spiritual plant is harvested")
                .defineInRange("harvestReserveRefund", 5.0D, 0.0D, 100000.0D);
        AURA_OVERHARVEST_DAILY_THRESHOLD = BUILDER
                .comment("Harvests per cell per cultivation day beyond which the overharvest penalty triggers")
                .defineInRange("overharvestDailyThreshold", 16, 1, 100000);
        AURA_OVERHARVEST_PENALTY_TIERS = BUILDER
                .comment("Aura quality tiers lost while the overharvest penalty is active")
                .defineInRange("overharvestPenaltyTiers", 1, 1, 3);
        AURA_OVERHARVEST_PENALTY_DAYS = BUILDER
                .comment("Cultivation days the overharvest penalty lasts")
                .defineInRange("overharvestPenaltyDays", 3, 1, 10000);
        AURA_QUALITY_CACHE_TTL_TICKS = BUILDER
                .comment("Ticks a derived aura quality stays cached before re-derivation")
                .defineInRange("qualityCacheTtlTicks", 1200, 20, 72000);
        AURA_POPULATION_SCAN_RADIUS = BUILDER
                .comment("Radius in blocks of the POI (bed) scan used to derive population pressure")
                .defineInRange("populationScanRadius", 40, 8, 256);
        AURA_POPULATION_LIGHT_HOMES = BUILDER
                .comment("Bed count at which the aura quality drops one tier")
                .defineInRange("populationLightHomes", 5, 1, 10000);
        AURA_POPULATION_HEAVY_HOMES = BUILDER
                .comment("Bed count at which the aura quality drops two tiers")
                .defineInRange("populationHeavyHomes", 15, 1, 10000);
        AURA_HIGHLAND_MIN_SURFACE_Y = BUILDER
                .comment("Surface height at or above which the cell counts as highland (+1 quality tier)")
                .defineInRange("highlandMinSurfaceY", 130, -64, 320);
        BUILDER.pop();

        BUILDER.push("alchemyM4");
        ALCHEMY_FURNACE_DURABILITY_LOSS_PER_REFINE = BUILDER
                .comment("Furnace durability consumed by each completed or failed refining session")
                .defineInRange("durabilityLossPerRefine", 1, 0, 1000);
        ALCHEMY_HERB_AGE_POTENCY_CAP = BUILDER
                .comment("Maximum potency bonus granted by herb age (0.10 = +10% at one century and beyond)")
                .defineInRange("herbAgePotencyCap", 0.10D, 0.0D, 10.0D);
        ALCHEMY_FIRE_SOURCE_SPIRITUAL_STABILITY = defineFireSourceStability(
                "fireSourceSpiritualStability", 1.10D, "spiritual fire (soul flame blocks below the furnace)"
        );
        ALCHEMY_FIRE_SOURCE_EARTH_STABILITY = defineFireSourceStability(
                "fireSourceEarthStability", 0.95D, "earth fire (lava adjacent below the furnace)"
        );
        ALCHEMY_FIRE_SOURCE_SAMADHI_STABILITY = defineFireSourceStability(
                "fireSourceSamadhiStability", 1.20D, "samadhi true fire (owner at Spirit Transformation or above)"
        );
        ALCHEMY_FIRE_SOURCE_EARTH_HEAT_BONUS = defineFireSourceHeatBonus("fireSourceEarthHeatBonus", 10);
        ALCHEMY_FIRE_SOURCE_SAMADHI_HEAT_BONUS = defineFireSourceHeatBonus("fireSourceSamadhiHeatBonus", 20);
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

        BUILDER.push("risk");
        RISK_BREAKTHROUGH_FAILURE_BASE_BUDGET = BUILDER
                .comment("Base risk budget settled when a breakthrough attempt fails")
                .defineInRange("breakthroughFailureBaseBudget", 30, 0, 1000);
        RISK_TRIBULATION_FAILURE_BASE_BUDGET = BUILDER
                .comment("Base risk budget settled when a tribulation fails")
                .defineInRange("tribulationFailureBaseBudget", 60, 0, 1000);
        RISK_DEATH_BASE_BUDGET = BUILDER
                .comment("Base risk budget settled on death")
                .defineInRange("deathBaseBudget", 50, 0, 1000);
        RISK_QI_DEVIATION_BASE_BUDGET = BUILDER
                .comment("Base risk budget settled when qi deviation strikes")
                .defineInRange("qiDeviationBaseBudget", 40, 0, 1000);
        RISK_REALM_BUDGET_STEP = BUILDER
                .comment("Budget multiplier gained per major realm above Qi Refining")
                .defineInRange("realmBudgetStep", 0.15D, 0.0D, 10.0D);
        RISK_CORE_LOSS_RATIO_PER_BUDGET = BUILDER
                .comment("Spiritual energy loss ratio applied per point of risk budget")
                .defineInRange("coreLossRatioPerBudget", 0.008D, 0.0D, 1.0D);
        RISK_MAX_CORE_LOSS_RATIO = BUILDER
                .comment("Upper bound of the spiritual energy loss ratio")
                .defineInRange("maxCoreLossRatio", 0.8D, 0.0D, 1.0D);
        RISK_LONG_TERM_SCAR_BUDGET_THRESHOLD = BUILDER
                .comment("Minimum budget required before a long-term scar is inflicted")
                .defineInRange("longTermScarBudgetThreshold", 40, 0, 1000);
        RISK_SHORT_TERM_SCAR_BUDGET_THRESHOLD = BUILDER
                .comment("Minimum budget required before a short-term restriction is inflicted")
                .defineInRange("shortTermScarBudgetThreshold", 15, 0, 1000);
        RISK_SEVERITY_BUDGET_STEP = BUILDER
                .comment("Extra budget required for each additional scar severity level")
                .defineInRange("severityBudgetStep", 20, 1, 1000);
        RISK_SHORT_TERM_DURATION_TICKS_PER_BUDGET = BUILDER
                .comment("Short-term restriction duration in ticks per point of risk budget")
                .defineInRange("shortTermDurationTicksPerBudget", 600L, 1L, 72000L);
        BUILDER.pop();

        BUILDER.push("breakthrough");
        BREAKTHROUGH_BASE_RATE_FOUNDATION = defineBreakthroughBaseRate("baseRateFoundation", 0.80D);
        BREAKTHROUGH_BASE_RATE_GOLDEN_CORE = defineBreakthroughBaseRate("baseRateGoldenCore", 0.55D);
        BREAKTHROUGH_BASE_RATE_NASCENT_SOUL = defineBreakthroughBaseRate("baseRateNascentSoul", 0.35D);
        BREAKTHROUGH_BASE_RATE_SPIRIT_TRANSFORMATION = defineBreakthroughBaseRate(
                "baseRateSpiritTransformation", 0.20D
        );
        BREAKTHROUGH_BASE_RATE_DAO_INTEGRATION = defineBreakthroughBaseRate("baseRateDaoIntegration", 0.12D);
        BREAKTHROUGH_BASE_RATE_VOID_REFINEMENT = defineBreakthroughBaseRate("baseRateVoidRefinement", 0.08D);
        BREAKTHROUGH_BASE_RATE_MAHAYANA = defineBreakthroughBaseRate("baseRateMahayana", 0.05D);
        BREAKTHROUGH_BASE_RATE_IMMORTAL = defineBreakthroughBaseRate("baseRateImmortal", 0.03D);
        BREAKTHROUGH_CHANCE_FLOOR = BUILDER
                .comment("Lower clamp of the final major-breakthrough success chance")
                .defineInRange("chanceFloor", 0.05D, 0.0D, 1.0D);
        BREAKTHROUGH_CHANCE_CEILING = BUILDER
                .comment("Upper clamp of the final major-breakthrough success chance")
                .defineInRange("chanceCeiling", 0.95D, 0.0D, 1.0D);
        BREAKTHROUGH_PITY_STEP = BUILDER
                .comment("Success chance compensation gained per failed attempt at the same realm")
                .defineInRange("pityStep", 0.03D, 0.0D, 1.0D);
        BREAKTHROUGH_PITY_CAP = BUILDER
                .comment("Upper bound of the accumulated failure compensation")
                .defineInRange("pityCap", 0.15D, 0.0D, 1.0D);
        BREAKTHROUGH_RITUAL_SECONDS = BUILDER
                .comment("Duration of the spirit-channeling ritual before the breakthrough roll")
                .defineInRange("ritualSeconds", 30, 5, 600);
        BREAKTHROUGH_FAIL_DISSIPATION_LOW = BUILDER
                .comment("Spiritual energy dissipated on a failed roll toward Foundation or Golden Core")
                .defineInRange("failDissipationLow", 0.40D, 0.0D, 1.0D);
        BREAKTHROUGH_FAIL_DISSIPATION_MID = BUILDER
                .comment("Spiritual energy dissipated on a failed roll toward Nascent Soul or Spirit Transformation")
                .defineInRange("failDissipationMid", 0.55D, 0.0D, 1.0D);
        BREAKTHROUGH_FAIL_DISSIPATION_HIGH = BUILDER
                .comment("Spiritual energy dissipated on a failed roll toward Dao Integration and above")
                .defineInRange("failDissipationHigh", 0.60D, 0.0D, 1.0D);
        BREAKTHROUGH_INTERRUPT_DISSIPATION = BUILDER
                .comment("Spiritual energy dissipated when the ritual is interrupted (light penalty, no roll)")
                .defineInRange("interruptDissipation", 0.15D, 0.0D, 1.0D);
        BREAKTHROUGH_BOTTLENECK_DAYS = BUILDER
                .comment("Days of bottleneck after a failed breakthrough roll before another attempt is allowed")
                .defineInRange("bottleneckDays", 3, 0, 1000);
        BREAKTHROUGH_INTERRUPT_ON_ENTITY_DAMAGE_ONLY = BUILDER
                .comment("Whether only entity-sourced damage interrupts the ritual (environment/DoT ignored)")
                .define("interruptOnEntityDamageOnly", true);
        BREAKTHROUGH_INSIGHT_CHOICE_EXPIRE_TICKS = BUILDER
                .comment("Ticks before pending insight candidates expire after a successful breakthrough")
                .defineInRange("insightChoiceExpireTicks", 24000L, 20L, 20L * 60L * 60L * 24L * 7L);
        BREAKTHROUGH_QUALITY_COEF_TURBID = defineBreakthroughQualityCoefficient("qualityCoefTurbid", 0.70D);
        BREAKTHROUGH_QUALITY_COEF_NEUTRAL = defineBreakthroughQualityCoefficient("qualityCoefNeutral", 1.00D);
        BREAKTHROUGH_QUALITY_COEF_CLEAR = defineBreakthroughQualityCoefficient("qualityCoefClear", 1.15D);
        BREAKTHROUGH_QUALITY_COEF_PRISTINE = defineBreakthroughQualityCoefficient("qualityCoefPristine", 1.30D);
        BUILDER.pop();

        BUILDER.push("soulInjury");
        SOUL_INJURY_THRESHOLD_DAZED = BUILDER
                .comment("Soul injury value at which the Dazed tier begins")
                .defineInRange("thresholdDazed", 20.0D, 0.0D, 100.0D);
        SOUL_INJURY_THRESHOLD_SHAKEN = BUILDER
                .comment("Soul injury value at which the Shaken tier begins; keep above thresholdDazed")
                .defineInRange("thresholdShaken", 50.0D, 0.0D, 100.0D);
        SOUL_INJURY_THRESHOLD_PERIL = BUILDER
                .comment("Soul injury value at which the Peril tier begins; keep above thresholdShaken")
                .defineInRange("thresholdPeril", 80.0D, 0.0D, 100.0D);
        SOUL_INJURY_SOURCE_PIERCE = defineSoulInjurySource(
                "sourcePierce", 15.0D, "Soul injury dealt to a player hit by Soul Pierce"
        );
        SOUL_INJURY_SOURCE_SHOCK = defineSoulInjurySource(
                "sourceShock", 8.0D, "Soul injury dealt to each player hit by Soul Shock"
        );
        SOUL_INJURY_SOURCE_BACKLASH = defineSoulInjurySource(
                "sourceBacklash", 20.0D, "Soul injury from a resisted soul-spell backlash"
        );
        SOUL_INJURY_SOURCE_WEAK_CAST = defineSoulInjurySource(
                "sourceWeakCast", 10.0D, "Soul injury from forcing a soul spell with a weak soul"
        );
        SOUL_INJURY_SOURCE_SENSE_RECOIL = defineSoulInjurySource(
                "sourceSenseRecoil", 10.0D, "Soul injury from soul-sense recoil against a mightier soul"
        );
        SOUL_INJURY_SOURCE_MIND_DEMON_HIT = defineSoulInjurySource(
                "sourceMindDemonHit", 3.0D, "Soul injury per mind-demon hit during the trial"
        );
        SOUL_INJURY_NATURAL_HEAL_PER_DAY = BUILDER
                .comment("Soul injury healed naturally per cultivation day")
                .defineInRange("naturalHealPerDay", 2.0D, 0.0D, 100.0D);
        SOUL_INJURY_SEVEN_STAR_HEAL_MULTIPLIER = BUILDER
                .comment("Natural heal multiplier for players with all seven soul stars illuminated")
                .defineInRange("sevenStarHealMultiplier", 2.0D, 0.0D, MAXIMUM_MULTIPLIER);
        SOUL_INJURY_PERIL_LIFESPAN_YEARS_PER_DAY = BUILDER
                .comment("Lifespan lost per cultivation day while at the Peril tier")
                .defineInRange("perilLifespanYearsPerDay", 1, 0, 1000);
        SOUL_INJURY_SENSE_RANGE_MULT_DAZED = BUILDER
                .comment("Soul-sense range multiplier at the Dazed tier")
                .defineInRange("senseRangeMultDazed", 0.7D, 0.0D, 1.0D);
        SOUL_INJURY_SENSE_RANGE_MULT_SHAKEN = BUILDER
                .comment("Soul-sense range multiplier at the Shaken tier and above")
                .defineInRange("senseRangeMultShaken", 0.5D, 0.0D, 1.0D);
        SOUL_INJURY_COMPREHENSION_PENALTY = BUILDER
                .comment("Comprehension score penalty applied from the Dazed tier onward")
                .defineInRange("comprehensionPenalty", 10.0D, 0.0D, 100.0D);
        SOUL_INJURY_SEVEN_STAR_REQUIRES_OPPORTUNITY = BUILDER
                .comment("Whether illuminating the seventh soul star requires a soul-dao opportunity item")
                .define("sevenStarRequiresOpportunity", true);
        BUILDER.pop();

        BUILDER.push("mindDemon");
        MIND_DEMON_ENABLED = BUILDER
                .comment("Whether the mirror mind-demon trial replaces the legacy phantom trial")
                .define("enabled", true);
        MIND_DEMON_REVEAL_SECONDS = defineMindDemonSeconds(
                "revealSeconds", 12, "Duration of the Reveal phase"
        );
        MIND_DEMON_SPAR_SECONDS = defineMindDemonSeconds(
                "sparSeconds", 45, "Maximum duration of the Spar phase"
        );
        MIND_DEMON_REFLECT_SECONDS = defineMindDemonSeconds(
                "reflectSeconds", 60, "Maximum duration of the Reflect phase"
        );
        MIND_DEMON_RESOLVE_CHOICE_SECONDS = defineMindDemonSeconds(
                "resolveChoiceSeconds", 20, "Window for choosing a resolve; defaults to Hold on timeout"
        );
        MIND_DEMON_SEVER_TIME_LIMIT_SECONDS = defineMindDemonSeconds(
                "severTimeLimitSeconds", 60, "Time limit to slay the demon after choosing Sever"
        );
        MIND_DEMON_HOLD_TIME_LIMIT_SECONDS = defineMindDemonSeconds(
                "holdTimeLimitSeconds", 90, "Time to survive the frenzied demon after choosing Hold"
        );
        MIND_DEMON_BODY_ECHO_RATIO = BUILDER
                .comment("Fraction of damage dealt to the mortal body echoed to the player inside the mind realm")
                .defineInRange("bodyEchoRatio", 0.6D, 0.0D, 1.0D);
        MIND_DEMON_KARMA_SCALE_BASE = BUILDER
                .comment("Base stat scale of the mind demon relative to its owner")
                .defineInRange("karmaScaleBase", 0.60D, 0.0D, 2.0D);
        MIND_DEMON_KARMA_SCALE_PER_KILL = BUILDER
                .comment("Additional stat scale per recorded kill in the behavior ledger")
                .defineInRange("karmaScalePerKill", 0.004D, 0.0D, 1.0D);
        MIND_DEMON_KARMA_SCALE_CAP = BUILDER
                .comment("Upper bound of the mind-demon stat scale")
                .defineInRange("karmaScaleCap", 1.2D, 0.0D, 2.0D);
        MIND_DEMON_POCKET_STRIDE = BUILDER
                .comment(
                        "Grid stride between mind-realm arenas in blocks.",
                        "Avoid changing on an existing world while any trial session is saved"
                )
                .defineInRange("pocketStride", 256, 64, 4096);
        MIND_DEMON_PVE_OFFLINE_PAUSE = BUILDER
                .comment(
                        "Whether trial timers pause while the owner is offline.",
                        "The current trial loop always pauses; this key is reserved for a future PVP preset"
                )
                .define("pveOfflinePause", true);
        MIND_DEMON_SOUL_INJURY_CAP_PER_TRIAL = BUILDER
                .comment("Maximum accumulated soul injury the demon may inflict in a single trial")
                .defineInRange("soulInjuryCapPerTrial", 30.0D, 0.0D, 100.0D);
        BUILDER.pop();

        BUILDER.push("seal");
        SEAL_ANCHORS_PER_SOUL_STAR = BUILDER
                .comment("Soul-sense anchors granted per illuminated soul star")
                .defineInRange("anchorsPerSoulStar", 2, 1, 10);
        SEAL_CARVE_ENERGY_PER_GRADE = BUILDER
                .comment("Spiritual energy cost of carving one seal layer, per seal grade")
                .defineInRange("carveEnergyPerGrade", 30.0D, 0.0D, 10000.0D);
        SEAL_CARVE_TEMPERING_COST_PER_GRADE = BUILDER
                .comment("Soul tempering progress consumed by carving, per seal grade")
                .defineInRange("carveTemperingCostPerGrade", 5.0D, 0.0D, 1000.0D);
        SEAL_CARVE_RITUAL_TICKS = BUILDER
                .comment("Duration of the carve ritual in ticks (sneak and hold still)")
                .defineInRange("carveRitualTicks", 600, 20, 24000);
        SEAL_REVOKE_TICKS = BUILDER
                .comment("Duration of revoking an own seal layer in ticks")
                .defineInRange("revokeTicks", 100, 20, 24000);
        SEAL_LOCK_GRID_FAIL_SOUL_INJURY = BUILDER
                .comment("Base soul injury inflicted when a lock-grid attempt fails")
                .defineInRange("lockGridFailSoulInjury", 5.0D, 0.0D, 100.0D);
        SEAL_LOCK_GRID_DISTURB_LOCK_DAYS = BUILDER
                .comment("Calendar days the finesse path stays locked after three disturbances")
                .defineInRange("lockGridDisturbLockDays", 1, 0, 30);
        SEAL_LOCK_GRID_ENERGY_PER_SECOND_PER_GRADE = BUILDER
                .comment("Spiritual energy drained per second per seal grade while the lock grid is open")
                .defineInRange("lockGridEnergyPerSecondPerGrade", 1.9D, 0.0D, 100.0D);
        SEAL_FORCE_BREAK_CRUSH_MULTIPLIER = BUILDER
                .comment("Force break shatters every layer instantly when break power >= strength x this")
                .defineInRange("forceBreakCrushMultiplier", 4.0D, 1.0D, 100.0D);
        SEAL_FORCE_BREAK_BASE_SECONDS = BUILDER
                .comment("Base grinding seconds of a non-crushing force break (doubled per strength tier)")
                .defineInRange("forceBreakBaseSeconds", 60, 5, 3600);
        SEAL_FORCE_BREAK_ENERGY_PER_SECOND = BUILDER
                .comment("Spiritual energy drained per second while force breaking")
                .defineInRange("forceBreakEnergyPerSecond", 6.0D, 0.0D, 1000.0D);
        SEAL_DEATH_PACT_DAYS = BUILDER
                .comment("Calendar days the death pact lasts after the owner dies")
                .defineInRange("deathPactDays", 3, 0, 30);
        SEAL_DEATH_PACT_STRENGTH_MULTIPLIER = BUILDER
                .comment("Seal strength multiplier while the death pact is active")
                .defineInRange("deathPactStrengthMultiplier", 1.5D, 1.0D, 10.0D);
        SEAL_DECAY_DAYS = BUILDER
                .comment("Calendar days before the seals of a permanently dead owner crumble (stored, judged later)")
                .defineInRange("decayDays", 7, 1, 90);
        SEAL_DEATH_PACT_ALERT_INTERVAL_TICKS = BUILDER
                .comment("Interval in ticks of the owner-facing direction pulse while the death pact is active")
                .defineInRange("deathPactAlertIntervalTicks", 600, 100, 24000);
        BUILDER.pop();

        BUILDER.push("death");
        DEATH_CHOOSE_TIMEOUT_SECONDS = BUILDER
                .comment("Seconds a dying player may ponder the death-route choice before the default applies")
                .defineInRange("chooseTimeoutSeconds", 30, 5, 600);
        DEATH_RESPAWN_QI_LAYER_LOSS = BUILDER
                .comment("Minor layers lost by Qi Refining players on a normal respawn")
                .defineInRange("respawnQiLayerLoss", 1, 0, 8);
        DEATH_RESPAWN_PROGRESS_LOSS_RATIO = BUILDER
                .comment("Fraction of accumulated spiritual energy lost by Foundation+ players on a normal respawn")
                .defineInRange("respawnProgressLossRatio", 0.3D, 0.0D, 1.0D);
        DEATH_RESPAWN_SOUL_INJURY = BUILDER
                .comment("Soul injury inflicted by a normal respawn")
                .defineInRange("respawnSoulInjury", 15.0D, 0.0D, 100.0D);
        DEATH_SOUL_RETURN_MINUTES_PER_STAR = BUILDER
                .comment("Soul-return time limit in real minutes per illuminated soul star")
                .defineInRange("soulReturnMinutesPerStar", 2, 1, 30);
        DEATH_SOUL_RETURN_SUCCESS_SOUL_INJURY = BUILDER
                .comment("Soul injury cost of a successful soul return")
                .defineInRange("soulReturnSuccessSoulInjury", 10.0D, 0.0D, 100.0D);
        DEATH_SOUL_RETURN_TIMEOUT_EXTRA_SOUL_INJURY = BUILDER
                .comment("Extra soul injury stacked on the normal respawn penalty when a soul return times out (PVE preset)")
                .defineInRange("soulReturnTimeoutExtraSoulInjury", 25.0D, 0.0D, 100.0D);
        DEATH_BURN_NASCENT_LIFESPAN_LOSS_RATIO = BUILDER
                .comment("Fraction of the maximum lifespan burned by the nascent-soul revival")
                .defineInRange("burnNascentLifespanLossRatio", 0.2D, 0.0D, 1.0D);
        DEATH_PVE_MODE = BUILDER
                .comment("PVE preset: soul-return timeout falls back to a heavier normal respawn instead of permanent death")
                .define("pveMode", true);
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

    private static ForgeConfigSpec.DoubleValue defineBreakthroughBaseRate(String name, double defaultValue) {
        return BUILDER
                .comment("Base success rate of the major breakthrough roll toward this realm")
                .defineInRange(name, defaultValue, 0.0D, 1.0D);
    }

    private static ForgeConfigSpec.DoubleValue defineBreakthroughQualityCoefficient(
            String name,
            double defaultValue
    ) {
        return BUILDER
                .comment("Qi-quality coefficient of the breakthrough chance formula (consumed once qi ecology lands)")
                .defineInRange(name, defaultValue, 0.0D, 10.0D);
    }

    private static ForgeConfigSpec.DoubleValue defineSoulInjurySource(
            String name,
            double defaultValue,
            String comment
    ) {
        return BUILDER.comment(comment).defineInRange(name, defaultValue, 0.0D, 100.0D);
    }

    private static ForgeConfigSpec.IntValue defineMindDemonSeconds(
            String name,
            int defaultValue,
            String comment
    ) {
        return BUILDER.comment(comment).defineInRange(name, defaultValue, 1, 3600);
    }

    private static ForgeConfigSpec.DoubleValue defineFailureLifespanScale(
            String name,
            double defaultValue,
            String reasonDescription
    ) {
        return BUILDER
                .comment("Lifespan-loss scale applied when the tribulation fails by " + reasonDescription)
                .defineInRange(name, defaultValue, 0.0D, 10.0D);
    }

    private static ForgeConfigSpec.DoubleValue defineDaoHeartCoefficient(String name, double defaultValue) {
        return BUILDER
                .comment("Dao-heart coefficient of this karma tier in the breakthrough chance formula")
                .defineInRange(name, defaultValue, 0.0D, 10.0D);
    }

    private static ForgeConfigSpec.DoubleValue defineFireSourceStability(
            String name,
            double defaultValue,
            String sourceDescription
    ) {
        return BUILDER
                .comment("Stability multiplier granted by " + sourceDescription)
                .defineInRange(name, defaultValue, 0.0D, 10.0D);
    }

    private static ForgeConfigSpec.IntValue defineFireSourceHeatBonus(String name, int defaultValue) {
        return BUILDER
                .comment("Extra heat ceiling granted by this fire source")
                .defineInRange(name, defaultValue, 0, 1000);
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
