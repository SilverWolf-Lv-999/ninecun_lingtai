package com.seraphina.ninecun_lingtai.common.command.debug;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationGoalInstaller;
import com.seraphina.ninecun_lingtai.common.defense.EntityDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import com.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import com.seraphina.ninecun_lingtai.common.player.DefenseResourceSnapshot;
import com.seraphina.ninecun_lingtai.common.player.DefenseResourceType;
import com.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import com.seraphina.ninecun_lingtai.utility.CultivationUtility;
import com.seraphina.ninecun_lingtai.utility.DebugCommandUtility;
import com.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class CreatureDebugCommand {
    private static final int TOTAL_STAGE_COUNT = CultivationRealm.values().length * CultivationStage.MAXIMUM_LAYER;
    private static final int MAXIMUM_SET_YEARS = 1_000_000;
    private static final int MAXIMUM_ADJUST_YEARS = 1_000_000;

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("creature")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(CreatureDebugCommand::getCreature)))
                .then(createCultivationCommand())
                .then(createSpiritualRootCommand())
                .then(createLifespanCommand())
                .then(createDefenseCommand());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createCultivationCommand() {
        return Commands.literal("cultivation")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> getCultivation(
                                        context.getSource(),
                                        DebugCommandUtility.getCreature(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("realm", StringArgumentType.string())
                                        .suggests(DebugCommandUtility::suggestRealms)
                                        .then(Commands.argument(
                                                        "layer",
                                                        IntegerArgumentType.integer(
                                                                CultivationStage.MINIMUM_LAYER,
                                                                CultivationStage.MAXIMUM_LAYER
                                                        )
                                                )
                                                .executes(CreatureDebugCommand::setCultivation)))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument(
                                                "stages",
                                                IntegerArgumentType.integer(-TOTAL_STAGE_COUNT, TOTAL_STAGE_COUNT)
                                        )
                                        .executes(CreatureDebugCommand::addCultivationStages))))
                .then(Commands.literal("max")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(CreatureDebugCommand::maximizeCultivation)))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(CreatureDebugCommand::clearCultivation)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createSpiritualRootCommand() {
        return Commands.literal("spiritual_root")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> getSpiritualRoot(
                                        context.getSource(),
                                        DebugCommandUtility.getCreature(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("elements", StringArgumentType.string())
                                        .suggests(DebugCommandUtility::suggestRootElements)
                                        .then(Commands.argument(
                                                        "length",
                                                        FloatArgumentType.floatArg(
                                                                SpiritualRoot.MINIMUM_LENGTH_INCHES,
                                                                SpiritualRoot.MAXIMUM_LENGTH_INCHES
                                                        )
                                                )
                                                .executes(CreatureDebugCommand::setSpiritualRoot)))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(CreatureDebugCommand::clearSpiritualRoot)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createLifespanCommand() {
        return Commands.literal("lifespan")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> getLifespan(
                                        context.getSource(),
                                        DebugCommandUtility.getCreature(context, "target")
                                ))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("years", IntegerArgumentType.integer(0, MAXIMUM_SET_YEARS))
                                        .executes(context -> setAge(
                                                context.getSource(),
                                                DebugCommandUtility.getCreatures(context, "targets"),
                                                IntegerArgumentType.getInteger(context, "years"),
                                                0
                                        ))
                                        .then(Commands.argument(
                                                        "days",
                                                        IntegerArgumentType.integer(0, LifespanUtility.DAYS_PER_YEAR - 1)
                                                )
                                                .executes(context -> setAge(
                                                        context.getSource(),
                                                        DebugCommandUtility.getCreatures(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "years"),
                                                        IntegerArgumentType.getInteger(context, "days")
                                                ))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument(
                                                "years",
                                                IntegerArgumentType.integer(-MAXIMUM_ADJUST_YEARS, MAXIMUM_ADJUST_YEARS)
                                        )
                                        .executes(CreatureDebugCommand::addLifespanYears))))
                .then(Commands.literal("expire")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(CreatureDebugCommand::expireLifespan)))
                .then(Commands.literal("reset")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(CreatureDebugCommand::resetLifespan)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createDefenseCommand() {
        return Commands.literal("defense")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(context -> getDefense(
                                        context.getSource(),
                                        DebugCommandUtility.getCreature(context, "target")
                                ))))
                .then(Commands.literal("refill")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(CreatureDebugCommand::refillDefense)))
                .then(Commands.literal("deplete")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(CreatureDebugCommand::depleteDefense)));
    }

    private static int getCreature(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        LivingEntity creature = DebugCommandUtility.getCreature(context, "target");
        CommandSourceStack commandSource = context.getSource();
        String entityType = BuiltInRegistries.ENTITY_TYPE.getKey(creature.getType()).toString();
        commandSource.sendSuccess(() -> Component.literal(
                "§6" + getCreatureName(creature) + "§r（" + entityType + "）"
        ), false);
        getCultivation(commandSource, creature);
        getSpiritualRoot(commandSource, creature);
        commandSource.sendSuccess(() -> Component.literal(
                "生命值：" + DebugCommandUtility.formatNumber(creature.getHealth())
                        + "/" + DebugCommandUtility.formatNumber(creature.getMaxHealth())
                        + "（境界加成=" + DebugCommandUtility.formatNumber(
                        CultivationDefenseUtility.getMaxHealthBonus(CreatureCultivationData.getStage(creature))
                ) + "）"
        ), false);
        getDefense(commandSource, creature);
        getLifespan(commandSource, creature);
        return Command.SINGLE_SUCCESS;
    }

    private static int getCultivation(CommandSourceStack commandSource, LivingEntity creature) {
        String stageDescription = CreatureCultivationData.getStage(creature)
                .map(DebugCommandUtility::formatCultivationStage)
                .orElse("未入道");
        commandSource.sendSuccess(() -> Component.literal(
                getCreatureName(creature) + " 的境界：" + stageDescription
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setCultivation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        CultivationStage stage = new CultivationStage(
                DebugCommandUtility.getRealm(context, "realm"),
                IntegerArgumentType.getInteger(context, "layer")
        );
        creatures.forEach(creature -> {
            CreatureCultivationData.setStage(creature, stage);
            syncCultivation(creature, true);
        });
        context.getSource().sendSuccess(() -> Component.literal(
                "已设置生物境界：" + DebugCommandUtility.getCreatureNames(creatures)
                        + " -> " + DebugCommandUtility.formatCultivationStage(stage)
        ), false);
        return creatures.size();
    }

    private static int addCultivationStages(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        int offset = IntegerArgumentType.getInteger(context, "stages");
        for (LivingEntity creature : creatures) {
            int currentIndex = CreatureCultivationData.getStage(creature)
                    .map(CultivationUtility::getProgressionIndex)
                    .orElse(-1);
            int targetIndex = Math.max(-1, Math.min(TOTAL_STAGE_COUNT - 1, currentIndex + offset));
            if (targetIndex < 0) {
                CreatureCultivationData.clearStage(creature);
            } else {
                CreatureCultivationData.setStage(creature, getStageByProgressionIndex(targetIndex));
            }
            syncCultivation(creature, true);
        }
        context.getSource().sendSuccess(() -> Component.literal(
                "已将生物境界进度调整 " + (offset >= 0 ? "+" : "") + offset + " 阶："
                        + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static int maximizeCultivation(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        CultivationStage maximumStage = getStageByProgressionIndex(TOTAL_STAGE_COUNT - 1);
        creatures.forEach(creature -> {
            CreatureCultivationData.setStage(creature, maximumStage);
            syncCultivation(creature, true);
        });
        context.getSource().sendSuccess(() -> Component.literal(
                "已将生物境界设为最高：" + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static int clearCultivation(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        creatures.forEach(creature -> {
            CreatureCultivationData.clearStage(creature);
            syncCultivation(creature, false);
        });
        context.getSource().sendSuccess(() -> Component.literal(
                "已清除生物境界：" + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static int getSpiritualRoot(CommandSourceStack commandSource, LivingEntity creature) {
        String rootDescription = CreatureCultivationData.getSpiritualRoot(creature)
                .map(DebugCommandUtility::formatSpiritualRoot)
                .orElse("未设置");
        commandSource.sendSuccess(() -> Component.literal(
                getCreatureName(creature) + " 的灵根：" + rootDescription
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setSpiritualRoot(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        SpiritualRoot spiritualRoot = new SpiritualRoot(
                DebugCommandUtility.getSpiritualRootElements(context, "elements"),
                FloatArgumentType.getFloat(context, "length")
        );
        creatures.forEach(creature -> CreatureCultivationData.setSpiritualRoot(creature, spiritualRoot));
        context.getSource().sendSuccess(() -> Component.literal(
                "已设置生物灵根：" + DebugCommandUtility.getCreatureNames(creatures)
                        + " -> " + DebugCommandUtility.formatSpiritualRoot(spiritualRoot)
        ), false);
        return creatures.size();
    }

    private static int clearSpiritualRoot(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        creatures.forEach(CreatureCultivationData::clearSpiritualRoot);
        context.getSource().sendSuccess(() -> Component.literal(
                "已清除生物灵根：" + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static int getLifespan(CommandSourceStack commandSource, LivingEntity creature) {
        if (!LifespanUtility.hasLifespan(creature)) {
            commandSource.sendSuccess(() -> Component.literal(
                    getCreatureName(creature) + " 不受寿元系统影响"
            ), false);
            return Command.SINGLE_SUCCESS;
        }
        LifespanSnapshot lifespan = EntityLifespanData.capture(creature);
        commandSource.sendSuccess(() -> Component.literal(
                getCreatureName(creature) + " 的寿元：年龄=" + LifespanUtility.formatAge(lifespan.ageTicks())
                        + "，上限=" + LifespanUtility.formatMaximum(lifespan)
                        + "，剩余=" + LifespanUtility.formatRemaining(lifespan)
                        + "，状态=" + describeLifespanStatus(lifespan)
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setAge(
            CommandSourceStack commandSource,
            Collection<LivingEntity> creatures,
            int years,
            int days
    ) {
        List<LivingEntity> affectedCreatures = getLifespanCreatures(creatures);
        long ageTicks = LifespanUtility.ticksForYearsAndDays(years, days);
        affectedCreatures.forEach(creature -> EntityLifespanData.setAgeTicks(creature, ageTicks));
        commandSource.sendSuccess(() -> Component.literal(
                "已设置生物年龄为 " + LifespanUtility.formatAge(ageTicks) + "："
                        + DebugCommandUtility.getCreatureNames(affectedCreatures)
        ), false);
        return affectedCreatures.size();
    }

    private static int addLifespanYears(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = getLifespanCreatures(
                DebugCommandUtility.getCreatures(context, "targets")
        );
        int years = IntegerArgumentType.getInteger(context, "years");
        long deltaTicks = years >= 0
                ? LifespanUtility.ticksForYears(years)
                : -LifespanUtility.ticksForYears(-years);
        creatures.forEach(creature -> EntityLifespanData.addAgeTicks(creature, deltaTicks));
        context.getSource().sendSuccess(() -> Component.literal(
                "已将生物年龄调整 " + (years >= 0 ? "+" : "") + years + " 年："
                        + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static int expireLifespan(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> expiredCreatures = new ArrayList<>();
        List<LivingEntity> immortalCreatures = new ArrayList<>();
        for (LivingEntity creature : getLifespanCreatures(DebugCommandUtility.getCreatures(context, "targets"))) {
            LifespanSnapshot lifespan = EntityLifespanData.capture(creature);
            if (lifespan.immortal()) {
                immortalCreatures.add(creature);
            } else {
                EntityLifespanData.setAgeTicks(creature, lifespan.maximumTicks());
                expiredCreatures.add(creature);
            }
        }
        if (!expiredCreatures.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal(
                    "已令生物到达寿元大限（下一次寿元检查时消亡）："
                            + DebugCommandUtility.getCreatureNames(expiredCreatures)
            ), true);
        }
        if (!immortalCreatures.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "以下生物已入长生之境：" + DebugCommandUtility.getCreatureNames(immortalCreatures)
            ));
        }
        return expiredCreatures.size();
    }

    private static int resetLifespan(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = getLifespanCreatures(
                DebugCommandUtility.getCreatures(context, "targets")
        );
        creatures.forEach(creature -> EntityLifespanData.setAgeTicks(creature, 0L));
        context.getSource().sendSuccess(() -> Component.literal(
                "已重置生物寿元：" + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static int getDefense(CommandSourceStack commandSource, LivingEntity creature) {
        DefenseResourceSnapshot defense = EntityDefenseResourceData.capture(creature);
        String defenseDescription = defense.isVisible()
                ? getDefenseResourceName(defense.type()) + "=" + defense.current() + "/" + defense.maximum()
                : "无境界防御资源";
        commandSource.sendSuccess(() -> Component.literal(
                getCreatureName(creature) + " 的境界防御：" + defenseDescription
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int refillDefense(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        creatures.forEach(EntityDefenseResourceData::refillToMaximum);
        context.getSource().sendSuccess(() -> Component.literal(
                "已补满生物境界防御：" + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static int depleteDefense(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        List<LivingEntity> creatures = DebugCommandUtility.getCreatures(context, "targets");
        creatures.forEach(EntityDefenseResourceData::deplete);
        context.getSource().sendSuccess(() -> Component.literal(
                "已耗尽生物境界防御：" + DebugCommandUtility.getCreatureNames(creatures)
        ), false);
        return creatures.size();
    }

    private static void syncCultivation(LivingEntity creature, boolean restoreHealth) {
        if (CreatureCultivationData.getStage(creature).isPresent()) {
            EntityDefenseResourceData.refillToMaximum(creature);
            if (creature instanceof Mob mob) {
                CreatureCultivationGoalInstaller.install(mob);
            }
        } else {
            EntityDefenseResourceData.clear(creature);
        }
        CultivationDefenseUtility.syncMaxHealthModifier(creature);
        if (restoreHealth) {
            creature.setHealth(creature.getMaxHealth());
        }
    }

    private static List<LivingEntity> getLifespanCreatures(Collection<LivingEntity> creatures) {
        return creatures.stream().filter(LifespanUtility::hasLifespan).toList();
    }

    private static CultivationStage getStageByProgressionIndex(int progressionIndex) {
        CultivationRealm realm = CultivationRealm.values()[progressionIndex / CultivationStage.MAXIMUM_LAYER];
        int layer = progressionIndex % CultivationStage.MAXIMUM_LAYER + CultivationStage.MINIMUM_LAYER;
        return new CultivationStage(realm, layer);
    }

    private static String getCreatureName(LivingEntity creature) {
        return creature.getDisplayName().getString() + "[" + creature.getStringUUID() + "]";
    }

    private static String getDefenseResourceName(DefenseResourceType resourceType) {
        return switch (resourceType) {
            case BODY_WIND -> "护体罡风";
            case SPIRITUAL_POWER -> "元神之力";
            case DAO -> "道";
            case NONE -> "无";
        };
    }

    private static String describeLifespanStatus(LifespanSnapshot lifespan) {
        if (lifespan.immortal()) {
            return "长生";
        }
        if (lifespan.expired()) {
            return "已至大限";
        }
        return "正常";
    }
}
