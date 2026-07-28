package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.element.ElementRelation;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.spell.SpellCastContext;
import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.data.DamageElementManager;
import com.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import com.seraphina.ninecun_lingtai.common.player.PlayerAptitudeData;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseSpellData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import com.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import com.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DefenseSpellUtility {
    private DefenseSpellUtility() {
    }

    public static boolean canUse(ServerPlayer player, DefenseSpellKind kind) {
        return PlayerAptitudeData.getSpiritualRoot(player)
                .map(root -> canUse(root, kind))
                .orElse(false);
    }

    public static boolean canUse(SpiritualRoot root, DefenseSpellKind kind) {
        Set<ElementType> elements = root.elements();
        return switch (kind) {
            case METAL_BODY_SHIELD -> elements.contains(ElementType.METAL);
            case WOOD_GUARDIAN -> elements.contains(ElementType.WOOD);
            case WATER_BODY_SHIELD -> elements.contains(ElementType.WATER);
            case FIRE_GUARDIAN -> elements.contains(ElementType.FIRE);
            case EARTH_GUARDIAN -> elements.contains(ElementType.EARTH);
            case YIN_YANG_BODY_SHIELD -> elements.contains(ElementType.YIN_YANG);
            case FIVE_ELEMENT_CYCLE -> elements.size() == ElementType.FIVE_ELEMENTS.size()
                    && elements.containsAll(ElementType.FIVE_ELEMENTS);
            case CHAOS_VOID -> root.isChaos();
            case COMPOSITE_BODY_SHIELD -> elements.size() >= 2;
            case FOUR_PHASE_GUARDIAN -> elements.size() >= 4;
        };
    }

    public static double getMaximumPoints(DefenseSpellKind kind, SpellCastContext context) {
        return Math.max(
                1.0D,
                kind.getPointsPerCultivationStrength()
                        * Math.max(1, context.cultivationStrength())
                        * context.powerEfficiency()
        );
    }

    public static int getDurationTicks(SpellCastContext context) {
        return Math.max(
                20,
                (int) Math.round(CommonConfig.DEFENSE_SPELL_DURATION_TICKS.get() * context.powerEfficiency())
        );
    }

    public static double repairWithSpiritualEnergy(
            ServerPlayer player,
            DefenseSpellKind kind,
            int elementMask,
            double currentPoints,
            double maximumPoints
    ) {
        double missingPoints = maximumPoints - currentPoints;
        if (missingPoints <= 1.0E-6D) {
            return 0.0D;
        }
        double requestedRepair = Math.min(
                missingPoints,
                maximumPoints * CommonConfig.DEFENSE_SPELL_REPAIR_PERCENT_PER_SECOND.get() / 100.0D
        );
        if (requestedRepair <= 0.0D || player.isCreative()) {
            return player.isCreative() ? requestedRepair : 0.0D;
        }
        double energyPerPoint = CommonConfig.DEFENSE_SPELL_REPAIR_ENERGY_PER_POINT.get();
        if (energyPerPoint <= 0.0D) {
            return requestedRepair;
        }
        double requestedEnergy = requestedRepair * energyPerPoint;
        double consumedEnergy = PlayerSpiritualEnergyData.consumeEnergyUpTo(
                player,
                getRepairElements(kind, elementMask),
                requestedEnergy
        );
        return requestedRepair * Math.min(1.0D, consumedEnergy / requestedEnergy);
    }

    public static Optional<ElementType> getAttackElement(DamageSource source) {
        return DamageElementManager.INSTANCE.getElement(source).map(elementalPower -> elementalPower.element());
    }

    public static double getPointCostMultiplier(
            DefenseSpellKind kind,
            int elementMask,
            Optional<ElementType> attackElementOptional
    ) {
        if (attackElementOptional.isEmpty()) {
            return 1.0D;
        }
        ElementType attackElement = attackElementOptional.get();
        if (kind == DefenseSpellKind.CHAOS_VOID) {
            return attackElement == ElementType.CHAOS
                    ? 1.0D
                    : CommonConfig.DEFENSE_SPELL_CHAOS_DEFENSE_MULTIPLIER.get();
        }
        if (attackElement == ElementType.CHAOS) {
            return CommonConfig.DEFENSE_SPELL_CHAOS_ATTACK_MULTIPLIER.get();
        }
        if (kind == DefenseSpellKind.YIN_YANG_BODY_SHIELD && attackElement.isFiveElement()) {
            return CommonConfig.DEFENSE_SPELL_YIN_YANG_BALANCE_MULTIPLIER.get();
        }
        if (kind == DefenseSpellKind.FIVE_ELEMENT_CYCLE && attackElement.isFiveElement()) {
            return CommonConfig.DEFENSE_SPELL_FIVE_ELEMENT_CYCLE_MULTIPLIER.get();
        }
        if (kind == DefenseSpellKind.COMPOSITE_BODY_SHIELD
                || kind == DefenseSpellKind.FOUR_PHASE_GUARDIAN) {
            return getCompositeMultiplier(kind, elementMask, attackElement);
        }
        return kind.getFixedElement()
                .map(defenseElement -> getFixedElementMultiplier(attackElement, defenseElement))
                .orElse(1.0D);
    }

    public static boolean canBodyShieldBlock(DefenseSpellKind kind, DamageSource source) {
        if (source.is(ModDamageTypes.REALM_EXECUTION)
                || source.is(DamageTypes.FALL)
                || source.is(DamageTypes.STALAGMITE)
                || source.is(DamageTypes.FLY_INTO_WALL)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.STARVE)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.FELL_OUT_OF_WORLD)
                || source.is(DamageTypes.OUTSIDE_BORDER)
                || source.is(DamageTypes.GENERIC_KILL)
                || source.is(DamageTypes.CRAMMING)
                || source.is(DamageTypes.DRY_OUT)) {
            return false;
        }
        if (kind == DefenseSpellKind.WATER_BODY_SHIELD
                && (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE))) {
            return true;
        }
        return source.getEntity() instanceof LivingEntity;
    }

    public static boolean summonGuardian(
            ServerPlayer player,
            DefenseSpellKind kind,
            SpellCastContext context
    ) {
        dismissGuardian(player);
        DefenseSpellGuardianEntity guardian = ModEntityTypes.DEFENSE_SPELL_GUARDIAN.get().create(player.serverLevel());
        if (guardian == null) {
            return false;
        }
        int elementMask = PlayerAptitudeData.getSpiritualRoot(player)
                .map(SpiritualRoot::getElementMask)
                .orElse(0);
        guardian.initialize(
                player,
                kind,
                getMaximumPoints(kind, context),
                elementMask,
                getDurationTicks(context)
        );
        guardian.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        if (!player.serverLevel().addFreshEntity(guardian)) {
            return false;
        }
        PlayerDefenseSpellData.setGuardian(player, kind, guardian.getUUID());
        redirectTargetsToGuardian(player, guardian);
        return true;
    }

    public static Set<ElementType> getRepairElements(DefenseSpellKind kind, int elementMask) {
        Optional<ElementType> fixedElement = kind.getFixedElement();
        if (fixedElement.isPresent()) {
            return Set.of(fixedElement.get());
        }
        if (kind == DefenseSpellKind.FIVE_ELEMENT_CYCLE) {
            return Set.copyOf(ElementType.FIVE_ELEMENTS);
        }
        return getElements(elementMask);
    }

    public static Optional<DefenseSpellGuardianEntity> getGuardian(ServerPlayer player) {
        Optional<UUID> guardianUuid = PlayerDefenseSpellData.getGuardianUuid(player);
        if (guardianUuid.isEmpty()) {
            return Optional.empty();
        }
        Entity entity = player.serverLevel().getEntity(guardianUuid.get());
        if (entity instanceof DefenseSpellGuardianEntity guardian
                && guardian.isAlive()
                && guardian.getOwnerUuid().filter(player.getUUID()::equals).isPresent()) {
            return Optional.of(guardian);
        }
        PlayerDefenseSpellData.clearGuardian(player);
        return Optional.empty();
    }

    public static void dismissGuardian(ServerPlayer player) {
        getGuardian(player).ifPresent(DefenseSpellGuardianEntity::discard);
        PlayerDefenseSpellData.clearGuardian(player);
    }

    public static void redirectTargetsToGuardian(
            ServerPlayer owner,
            DefenseSpellGuardianEntity guardian
    ) {
        double radius = guardian.getSpellKind().getTargetAttractionRadius();
        if (radius <= 0.0D) {
            return;
        }
        AABB searchBox = owner.getBoundingBox().inflate(radius);
        for (Mob mob : owner.level().getEntitiesOfClass(
                Mob.class,
                searchBox,
                mob -> mob.getTarget() == owner && canGuardianIntercept(mob, owner)
        )) {
            mob.setTarget(guardian);
        }
    }

    public static boolean canGuardianIntercept(LivingEntity attacker, ServerPlayer owner) {
        return !CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                CreatureCultivationData.getStage(attacker),
                CreatureCultivationData.getStage(owner)
        );
    }

    public static int getColor(DefenseSpellKind kind, int elementMask) {
        if (kind == DefenseSpellKind.FIVE_ELEMENT_CYCLE) {
            return 0xF3D35A;
        }
        if (kind == DefenseSpellKind.CHAOS_VOID) {
            return SpiritualRootUtility.getElementColor(ElementType.CHAOS);
        }
        if (kind == DefenseSpellKind.COMPOSITE_BODY_SHIELD
                || kind == DefenseSpellKind.FOUR_PHASE_GUARDIAN) {
            List<Integer> colors = getElements(elementMask).stream()
                    .map(SpiritualRootUtility::getElementColor)
                    .toList();
            return colors.isEmpty() ? 0xFFFFFF : ColorUtility.mix(colors);
        }
        return kind.getFixedElement()
                .map(SpiritualRootUtility::getElementColor)
                .orElse(0xFFFFFF);
    }

    private static double getCompositeMultiplier(
            DefenseSpellKind kind,
            int elementMask,
            ElementType attackElement
    ) {
        List<Double> multipliers = new ArrayList<>();
        for (ElementType defenseElement : getElements(elementMask)) {
            if (defenseElement == ElementType.YIN_YANG && attackElement.isFiveElement()) {
                multipliers.add(CommonConfig.DEFENSE_SPELL_YIN_YANG_BALANCE_MULTIPLIER.get());
            } else {
                multipliers.add(getFixedElementMultiplier(attackElement, defenseElement));
            }
        }
        if (multipliers.isEmpty()) {
            return 1.0D;
        }
        if (kind == DefenseSpellKind.FOUR_PHASE_GUARDIAN) {
            return multipliers.stream().mapToDouble(Double::doubleValue).min().orElse(1.0D);
        }
        return multipliers.stream().mapToDouble(Double::doubleValue).average().orElse(1.0D);
    }

    private static double getFixedElementMultiplier(ElementType attackElement, ElementType defenseElement) {
        ElementRelation relation = attackElement.getRelationTo(defenseElement);
        return switch (relation) {
            case OVERCOME_BY -> CommonConfig.DEFENSE_SPELL_RESTRAINS_ATTACK_MULTIPLIER.get();
            case GENERATES -> CommonConfig.DEFENSE_SPELL_ATTACK_GENERATES_DEFENSE_MULTIPLIER.get();
            case GENERATED_BY -> CommonConfig.DEFENSE_SPELL_DEFENSE_GENERATES_ATTACK_MULTIPLIER.get();
            case OVERCOMES -> CommonConfig.DEFENSE_SPELL_ATTACK_RESTRAINS_DEFENSE_MULTIPLIER.get();
            case SUPPRESSES -> CommonConfig.DEFENSE_SPELL_CHAOS_ATTACK_MULTIPLIER.get();
            case SUPPRESSED_BY -> CommonConfig.DEFENSE_SPELL_CHAOS_DEFENSE_MULTIPLIER.get();
            default -> 1.0D;
        };
    }

    private static Set<ElementType> getElements(int elementMask) {
        EnumSet<ElementType> elements = EnumSet.noneOf(ElementType.class);
        for (ElementType element : SpiritualRoot.SELECTABLE_ELEMENTS) {
            if ((elementMask & (1 << element.ordinal())) != 0) {
                elements.add(element);
            }
        }
        return elements;
    }
}
