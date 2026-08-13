package net.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyBrewScore;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.utility.AlchemyProcessUtility;
import net.seraphina.ninecun_lingtai.utility.AlchemyResultCalculator;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AlchemyDebugCommand {
    private static final int MINIMUM_SIMULATION_COUNT = 1;
    private static final int MAXIMUM_SIMULATION_COUNT = 100000;
    private static final String DEFAULT_PROFILE_NAME = "novice";
    private static final double EXPLOSION_SCORE_THRESHOLD = 0.5D;
    private static final String[] RANK_NAMES = {
            "", "一品", "二品", "三品", "四品", "五品", "六品", "七品", "八品", "九品"
    };
    private static final Map<String, SimulationProfile> PROFILES = createProfiles();

    private static final DynamicCommandExceptionType UNKNOWN_RECIPE = new DynamicCommandExceptionType(
            value -> Component.literal("未找到丹方：" + value)
    );
    private static final DynamicCommandExceptionType UNKNOWN_PROFILE = new DynamicCommandExceptionType(
            value -> Component.literal(
                    "未知画像：" + value + "（可选：" + String.join("、", PROFILES.keySet()) + "）"
            )
    );

    private record SimulationProfile(
            String name,
            Set<ElementType> rootElements,
            float rootLengthInches,
            int soulStars,
            double environmentMultiplier,
            double heatAccuracy,
            double feedAccuracy,
            int cultivationLayer
    ) {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("alchemy")
                .then(Commands.argument("recipeId", ResourceLocationArgument.id())
                        .suggests(AlchemyDebugCommand::suggestRecipeIds)
                        .then(Commands.argument(
                                        "count",
                                        IntegerArgumentType.integer(
                                                MINIMUM_SIMULATION_COUNT,
                                                MAXIMUM_SIMULATION_COUNT
                                        )
                                )
                                .executes(context -> simulate(context, DEFAULT_PROFILE_NAME))
                                .then(Commands.argument("profile", StringArgumentType.word())
                                        .suggests(AlchemyDebugCommand::suggestProfiles)
                                        .executes(context -> simulate(
                                                context,
                                                StringArgumentType.getString(context, "profile")
                                        )))));
    }

    private static int simulate(
            CommandContext<CommandSourceStack> context,
            String profileName
    ) throws CommandSyntaxException {
        ResourceLocation recipeId = ResourceLocationArgument.getId(context, "recipeId");
        AlchemyRecipeDefinition recipe = AlchemyRecipeManager.INSTANCE.getRecipe(recipeId)
                .orElseThrow(() -> UNKNOWN_RECIPE.create(recipeId.toString()));
        SimulationProfile profile = PROFILES.get(profileName.trim().toLowerCase(Locale.ROOT));
        if (profile == null) {
            throw UNKNOWN_PROFILE.create(profileName);
        }
        int simulationCount = IntegerArgumentType.getInteger(context, "count");

        SpiritualRoot spiritualRoot = new SpiritualRoot(profile.rootElements(), profile.rootLengthInches());
        SoulStrength soulStrength = new SoulStrength(profile.soulStars());
        int tier = recipe.grade().tier();
        CultivationStage stage = new CultivationStage(
                CultivationRealm.values()[tier - 1],
                profile.cultivationLayer()
        );

        boolean hasHeatCurve = !recipe.heatStages().isEmpty();
        int totalTicks = recipe.brewingDuration();
        int hitTicks = (int) Math.round(profile.heatAccuracy() * totalTicks);

        double fireScore = AlchemyResultCalculator.calculateFireScore(
                hitTicks,
                totalTicks,
                spiritualRoot.quality(),
                hasHeatCurve
        );
        double rawFireScore = AlchemyResultCalculator.getRawFireScore(hitTicks, totalTicks, hasHeatCurve);
        double operationScore = AlchemyProcessUtility.combineOperationScore(
                fireScore,
                profile.feedAccuracy(),
                !recipe.feedPlan().isEmpty()
        );
        double rawOperationScore = AlchemyProcessUtility.combineOperationScore(
                rawFireScore,
                profile.feedAccuracy(),
                !recipe.feedPlan().isEmpty()
        );
        double environmentScore = AlchemyResultCalculator.calculateEnvironmentScore(
                profile.environmentMultiplier(),
                recipe.getPrimaryElement().isPresent()
        );
        double rootScore = AlchemyResultCalculator.calculateRootScore(
                Optional.of(spiritualRoot),
                recipe.getPrimaryElement().orElse(null)
        );
        double soulScore = AlchemyResultCalculator.calculateSoulScore(Optional.of(soulStrength));
        double realmScore = AlchemyResultCalculator.calculateRealmScore(Optional.of(stage), tier);
        AlchemyBrewScore score = AlchemyResultCalculator.combine(
                operationScore,
                environmentScore,
                rootScore,
                soulScore,
                realmScore,
                recipe.baseSuccessRate()
        );

        RandomSource random = context.getSource().getLevel().getRandom();
        int baseRank = recipe.grade().rank();
        int rankOffset = AlchemyResultCalculator.getRankOffset(score.totalScore(), rawOperationScore);
        int resultRank = Math.max(1, Math.min(9, baseRank + rankOffset));
        int[] rankCounts = new int[10];
        int explosionCount = 0;
        int dregFailureCount = 0;
        int bonusYieldCount = 0;
        for (int iteration = 0; iteration < simulationCount; iteration++) {
            if (random.nextDouble() < score.successRate()) {
                rankCounts[resultRank]++;
                if (AlchemyResultCalculator.rollBonusYield(
                        score.totalScore(),
                        rawOperationScore,
                        random
                )) {
                    bonusYieldCount++;
                }
            } else if (score.totalScore() < EXPLOSION_SCORE_THRESHOLD) {
                explosionCount++;
            } else {
                dregFailureCount++;
            }
        }

        int successCount = 0;
        StringBuilder rankDistribution = new StringBuilder();
        for (int rank = 1; rank <= 9; rank++) {
            if (rankCounts[rank] <= 0) {
                continue;
            }
            successCount += rankCounts[rank];
            if (rankDistribution.length() > 0) {
                rankDistribution.append(" / ");
            }
            rankDistribution.append(RANK_NAMES[rank]).append(' ').append(rankCounts[rank]);
        }
        int totalPillCount = successCount * recipe.outputCount() + bonusYieldCount;

        String headerLine = "【炼丹模拟】丹方=" + recipe.name()
                + " 画像=" + profile.name()
                + " 次数=" + simulationCount;
        String scoreLine = String.format(
                Locale.ROOT,
                "评分：火候 %.2f 投药 %.2f 工序 %.2f 环境 %.2f 灵根 %.2f 神识 %.2f 境界 %.2f → 总分 %.3f 成功率 %.1f%%",
                fireScore,
                profile.feedAccuracy(),
                operationScore,
                environmentScore,
                rootScore,
                soulScore,
                realmScore,
                score.totalScore(),
                score.successRate() * 100.0D
        );
        String resultLine = "结果：成丹 " + successCount
                + (rankDistribution.length() > 0 ? "（其中 " + rankDistribution + "）" : "")
                + " 炸炉 " + explosionCount
                + " 药渣 " + dregFailureCount
                + " 总产丹 " + totalPillCount + " 枚"
                + (bonusYieldCount > 0 ? "（含完美加产 " + bonusYieldCount + "）" : "");

        CommandSourceStack commandSource = context.getSource();
        commandSource.sendSuccess(() -> Component.literal(headerLine), false);
        commandSource.sendSuccess(() -> Component.literal(scoreLine), false);
        commandSource.sendSuccess(() -> Component.literal(resultLine), false);
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestRecipeIds(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(
                AlchemyRecipeManager.INSTANCE.getRecipes().stream().map(AlchemyRecipeDefinition::id),
                builder
        );
    }

    private static CompletableFuture<Suggestions> suggestProfiles(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(PROFILES.keySet(), builder);
    }

    private static Map<String, SimulationProfile> createProfiles() {
        Map<String, SimulationProfile> profiles = new LinkedHashMap<>();
        profiles.put("novice", new SimulationProfile(
                "novice",
                Set.of(ElementType.WOOD),
                3.0F,
                3,
                1.1D,
                1.0D,
                1.0D,
                5
        ));
        profiles.put("novice_sloppy", new SimulationProfile(
                "novice_sloppy",
                Set.of(ElementType.WOOD),
                3.0F,
                3,
                1.1D,
                0.4D,
                0.4D,
                5
        ));
        profiles.put("expert", new SimulationProfile(
                "expert",
                Set.of(ElementType.METAL),
                9.0F,
                7,
                1.5D,
                0.95D,
                0.95D,
                3
        ));
        profiles.put("heat_only", new SimulationProfile(
                "heat_only",
                Set.of(ElementType.WOOD),
                6.0F,
                5,
                1.0D,
                0.95D,
                0.35D,
                5
        ));
        profiles.put("feed_only", new SimulationProfile(
                "feed_only",
                Set.of(ElementType.WOOD),
                6.0F,
                5,
                1.0D,
                0.35D,
                0.95D,
                5
        ));
        profiles.put("feed_accuracy", new SimulationProfile(
                "feed_accuracy",
                Set.of(ElementType.WOOD),
                6.0F,
                5,
                1.0D,
                0.95D,
                0.95D,
                5
        ));
        return Collections.unmodifiableMap(profiles);
    }
}
