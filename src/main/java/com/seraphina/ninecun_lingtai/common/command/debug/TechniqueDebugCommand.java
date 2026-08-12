package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueComprehension;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueProgress;
import com.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerImprintedTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueData;
import com.seraphina.ninecun_lingtai.common.player.PlayerTechniqueProficiencyData;
import com.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class TechniqueDebugCommand {
    private static final String DEBUG_FEEDBACK = "由调试指令直接授予";
    private static final DynamicCommandExceptionType UNKNOWN_TECHNIQUE = new DynamicCommandExceptionType(
            value -> Component.literal("未知功法：" + value)
    );
    private static final DynamicCommandExceptionType UNKNOWN_MASTERY = new DynamicCommandExceptionType(
            value -> Component.literal("未知功法境界：" + value)
    );
    private static final Map<String, TechniqueMasteryStage> MASTERY_ALIASES = createMasteryAliases();

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("technique")
                .executes(context -> getTechniques(
                        context.getSource(),
                        context.getSource().getPlayerOrException()
                ))
                .then(Commands.literal("get")
                        .executes(context -> getTechniques(
                                context.getSource(),
                                context.getSource().getPlayerOrException()
                        ))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> getTechniques(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("list")
                        .executes(context -> listTechniques(context.getSource())))
                .then(Commands.literal("learn")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(techniqueArgument()
                                        .executes(context -> learn(
                                                context,
                                                TechniqueMasteryStage.ENTRY
                                        ))
                                        .then(masteryArgument()
                                                .executes(context -> learn(
                                                        context,
                                                        getMasteryStage(context)
                                                ))))))
                .then(Commands.literal("score")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(techniqueArgument()
                                        .then(Commands.argument(
                                                        "score",
                                                        IntegerArgumentType.integer(
                                                                TechniqueComprehension.MINIMUM_SCORE,
                                                                TechniqueComprehension.MAXIMUM_SCORE
                                                        )
                                                )
                                                .executes(TechniqueDebugCommand::setScore)))))
                .then(Commands.literal("mastery")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(techniqueArgument()
                                        .then(masteryArgument()
                                                .executes(TechniqueDebugCommand::setMastery)))))
                .then(Commands.literal("proficiency")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(techniqueArgument()
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0L))
                                                .executes(TechniqueDebugCommand::setProficiency)))))
                .then(Commands.literal("imprint")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(techniqueArgument()
                                        .executes(TechniqueDebugCommand::imprint))))
                .then(Commands.literal("max")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(techniqueArgument()
                                        .executes(context -> learn(context, TechniqueMasteryStage.PERFECTION)))))
                .then(Commands.literal("forget")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(techniqueArgument()
                                        .executes(TechniqueDebugCommand::forget))))
                .then(Commands.literal("clear")
                        .executes(context -> clear(
                                context.getSource(),
                                List.of(context.getSource().getPlayerOrException())
                        ))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clear(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")
                                ))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> techniqueArgument() {
        return Commands.argument("technique", ResourceLocationArgument.id())
                .suggests(TechniqueDebugCommand::suggestTechniques);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> masteryArgument() {
        return Commands.argument("mastery", StringArgumentType.word())
                .suggests(TechniqueDebugCommand::suggestMasteryStages);
    }

    private static int getTechniques(CommandSourceStack commandSource, ServerPlayer player) {
        String playerName = player.getGameProfile().getName();
        Map<ResourceLocation, TechniqueProgress> learned = PlayerTechniqueData.getTechniques(player);
        Map<ResourceLocation, Long> proficiencies = PlayerTechniqueProficiencyData.getAll(player);
        int imprintedCount = PlayerImprintedTechniqueData.getTechniques(player).size();

        commandSource.sendSuccess(() -> Component.literal("§6" + playerName + " 的功法"), false);
        commandSource.sendSuccess(() -> Component.literal("已铭记：" + imprintedCount + " 部"), false);
        if (learned.isEmpty()) {
            commandSource.sendSuccess(() -> Component.literal("已修习：尚未修习任何功法"), false);
            return Command.SINGLE_SUCCESS;
        }

        commandSource.sendSuccess(() -> Component.literal("已修习：" + learned.size() + " 部"), false);
        learned.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    ResourceLocation techniqueId = entry.getKey();
                    TechniqueProgress progress = entry.getValue();
                    long proficiency = proficiencies.getOrDefault(techniqueId, 0L);
                    commandSource.sendSuccess(() -> Component.literal(
                            "  §e" + getTechniqueName(techniqueId) + "§r：境界 "
                                    + TechniqueMasteryStage.getDisplayName(progress.mastery())
                                    + "，理解度 " + progress.comprehensionScore()
                                    + "，熟练度 " + proficiency
                    ), false);
                });
        return Command.SINGLE_SUCCESS;
    }

    private static int listTechniques(CommandSourceStack commandSource) {
        List<TechniqueDefinition> techniques = TechniqueManager.INSTANCE.getTechniques();
        commandSource.sendSuccess(() -> Component.literal("§6全部功法（" + techniques.size() + "）"), false);
        techniques.forEach(technique -> commandSource.sendSuccess(() -> Component.literal(
                "  §e" + technique.name() + "§r（" + technique.id() + "）"
        ), false));
        return techniques.size();
    }

    private static int learn(
            CommandContext<CommandSourceStack> context,
            TechniqueMasteryStage masteryStage
    ) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation techniqueId = getTechniqueId(context);
        TechniqueProgress progress = new TechniqueProgress(
                TechniqueComprehension.MAXIMUM_SCORE,
                masteryStage.getMasteryId(),
                DEBUG_FEEDBACK
        );
        for (ServerPlayer player : players) {
            PlayerImprintedTechniqueData.imprint(player, techniqueId);
            PlayerTechniqueData.setTechnique(player, techniqueId, progress);
            PlayerTechniqueProficiencyData.set(player, techniqueId, masteryStage.getRequiredProficiency());
            ModNetwork.sendTechniqueLibrary(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "已授予功法 §e" + getTechniqueName(techniqueId) + "§r（" + masteryStage.getChineseName() + "）："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int setScore(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation techniqueId = getTechniqueId(context);
        TechniqueDefinition technique = requireTechnique(techniqueId);
        int score = IntegerArgumentType.getInteger(context, "score");
        for (ServerPlayer player : players) {
            Optional<TechniqueProgress> existing = PlayerTechniqueData.getTechnique(player, techniqueId);
            ResourceLocation mastery = technique.comprehension().getMastery(score)
                    .orElseGet(() -> existing
                            .map(TechniqueProgress::mastery)
                            .orElse(TechniqueMasteryStage.ENTRY.getMasteryId()));
            PlayerImprintedTechniqueData.imprint(player, techniqueId);
            PlayerTechniqueData.setTechnique(
                    player,
                    techniqueId,
                    new TechniqueProgress(
                            score,
                            mastery,
                            existing.map(TechniqueProgress::feedback).orElse(DEBUG_FEEDBACK)
                    )
            );
            ModNetwork.sendTechniqueLibrary(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "已设置功法 §e" + getTechniqueName(techniqueId) + "§r 理解度为 " + score + "："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int setMastery(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation techniqueId = getTechniqueId(context);
        TechniqueMasteryStage masteryStage = getMasteryStage(context);
        for (ServerPlayer player : players) {
            Optional<TechniqueProgress> existing = PlayerTechniqueData.getTechnique(player, techniqueId);
            int score = existing.map(TechniqueProgress::comprehensionScore)
                    .orElse(TechniqueComprehension.MAXIMUM_SCORE);
            PlayerImprintedTechniqueData.imprint(player, techniqueId);
            PlayerTechniqueData.setTechnique(
                    player,
                    techniqueId,
                    new TechniqueProgress(
                            score,
                            masteryStage.getMasteryId(),
                            existing.map(TechniqueProgress::feedback).orElse(DEBUG_FEEDBACK)
                    )
            );
            PlayerTechniqueProficiencyData.set(player, techniqueId, masteryStage.getRequiredProficiency());
            ModNetwork.sendTechniqueLibrary(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "已将功法 §e" + getTechniqueName(techniqueId) + "§r 境界设为 " + masteryStage.getChineseName() + "："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int setProficiency(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation techniqueId = getTechniqueId(context);
        long amount = LongArgumentType.getLong(context, "amount");
        for (ServerPlayer player : players) {
            PlayerTechniqueProficiencyData.set(player, techniqueId, amount);
            PlayerTechniqueData.getTechnique(player, techniqueId).ifPresent(progress -> {
                ResourceLocation raisedMastery = TechniqueMasteryStage.highest(
                        progress.mastery(),
                        TechniqueMasteryStage.getStageForProficiency(amount).getMasteryId()
                );
                if (!raisedMastery.equals(progress.mastery())) {
                    PlayerTechniqueData.setTechnique(
                            player,
                            techniqueId,
                            new TechniqueProgress(progress.comprehensionScore(), raisedMastery, progress.feedback())
                    );
                }
            });
            ModNetwork.sendTechniqueLibrary(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "已设置功法 §e" + getTechniqueName(techniqueId) + "§r 熟练度为 " + amount + "："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int imprint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation techniqueId = getTechniqueId(context);
        for (ServerPlayer player : players) {
            PlayerImprintedTechniqueData.imprint(player, techniqueId);
            ModNetwork.sendTechniqueLibrary(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "已铭记（点亮）功法 §e" + getTechniqueName(techniqueId) + "§r："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int forget(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(context, "targets");
        ResourceLocation techniqueId = getTechniqueId(context);
        for (ServerPlayer player : players) {
            PlayerTechniqueData.removeTechnique(player, techniqueId);
            PlayerTechniqueProficiencyData.set(player, techniqueId, 0L);
            ModNetwork.sendTechniqueLibrary(player);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "已遗忘功法 §e" + getTechniqueName(techniqueId) + "§r（保留铭记记录）："
                        + DebugCommandUtility.getPlayerNames(players)
        ), false);
        return players.size();
    }

    private static int clear(CommandSourceStack commandSource, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            PlayerTechniqueData.clear(player);
            PlayerTechniqueProficiencyData.clear(player);
            ModNetwork.sendTechniqueLibrary(player);
        }
        commandSource.sendSuccess(() -> Component.literal(
                "已清除全部已修习功法与熟练度：" + DebugCommandUtility.getPlayerNames(players)
        ), true);
        return players.size();
    }

    private static ResourceLocation getTechniqueId(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        ResourceLocation techniqueId = ResourceLocationArgument.getId(context, "technique");
        requireTechnique(techniqueId);
        return techniqueId;
    }

    private static TechniqueDefinition requireTechnique(
            ResourceLocation techniqueId
    ) throws CommandSyntaxException {
        return TechniqueManager.INSTANCE.getTechnique(techniqueId)
                .orElseThrow(() -> UNKNOWN_TECHNIQUE.create(techniqueId));
    }

    private static TechniqueMasteryStage getMasteryStage(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {
        String masteryName = StringArgumentType.getString(context, "mastery");
        TechniqueMasteryStage masteryStage = MASTERY_ALIASES.get(masteryName.trim().toLowerCase(Locale.ROOT));
        if (masteryStage == null) {
            throw UNKNOWN_MASTERY.create(masteryName);
        }
        return masteryStage;
    }

    private static String getTechniqueName(ResourceLocation techniqueId) {
        return TechniqueManager.INSTANCE.getTechnique(techniqueId)
                .map(TechniqueDefinition::name)
                .orElseGet(techniqueId::toString);
    }

    private static CompletableFuture<Suggestions> suggestTechniques(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggestResource(
                TechniqueManager.INSTANCE.getTechniques().stream().map(TechniqueDefinition::id),
                builder
        );
    }

    private static CompletableFuture<Suggestions> suggestMasteryStages(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(MASTERY_ALIASES.keySet(), builder);
    }

    private static Map<String, TechniqueMasteryStage> createMasteryAliases() {
        Map<String, TechniqueMasteryStage> aliases = new LinkedHashMap<>();
        Arrays.stream(TechniqueMasteryStage.values()).forEach(stage -> {
            aliases.put(stage.getSerializedName(), stage);
            aliases.put(stage.getChineseName(), stage);
        });
        return Map.copyOf(aliases);
    }
}
