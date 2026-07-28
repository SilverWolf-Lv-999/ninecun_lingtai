package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseSpellData;
import com.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import com.seraphina.ninecun_lingtai.common.spell.defense.DefenseSpellKind;
import com.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DefenseSpellEventHandler {
    private static final double FULL_BLOCK_EPSILON = 1.0E-5D;

    private DefenseSpellEventHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || event.getSource().getDirectEntity() != attacker
                || !DefenseSpellUtility.canGuardianIntercept(attacker, owner)) {
            return;
        }
        Optional<DefenseSpellGuardianEntity> guardianOptional = DefenseSpellUtility.getGuardian(owner);
        if (guardianOptional.isEmpty()) {
            return;
        }
        DefenseSpellGuardianEntity guardian = guardianOptional.get();
        if (guardian.distanceToSqr(owner) > guardian.getSpellKind().getTargetAttractionRadius()
                * guardian.getSpellKind().getTargetAttractionRadius()) {
            return;
        }
        if (attacker instanceof Mob mob) {
            mob.setTarget(guardian);
        }
        guardian.hurt(event.getSource(), event.getAmount());
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getAmount() <= 0.0F) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            absorbWithBodyShield(event, player);
        } else if (event.getEntity() instanceof DefenseSpellGuardianEntity guardian) {
            absorbWithGuardian(event, guardian);
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            tickBodyShield(player);
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)
                || mob.level().isClientSide
                || mob.tickCount % 5 != 0
                || !(mob.getTarget() instanceof ServerPlayer owner)
                || !DefenseSpellUtility.canGuardianIntercept(mob, owner)) {
            return;
        }
        DefenseSpellUtility.getGuardian(owner).ifPresent(guardian -> {
            double radius = guardian.getSpellKind().getTargetAttractionRadius();
            if (mob.distanceToSqr(guardian) <= radius * radius) {
                mob.setTarget(guardian);
            }
        });
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAll(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            clearAll(original);
        }
        if (event.getEntity() instanceof ServerPlayer cloned) {
            PlayerDefenseSpellData.clearBodyShield(cloned);
            PlayerDefenseSpellData.clearGuardian(cloned);
            ModNetwork.sendDefenseSpellState(cloned);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Optional<UUID> guardianUuid = PlayerDefenseSpellData.getGuardianUuid(player);
        ServerLevel oldLevel = player.getServer() == null ? null : player.getServer().getLevel(event.getFrom());
        if (oldLevel != null && guardianUuid.isPresent()) {
            Entity guardian = oldLevel.getEntity(guardianUuid.get());
            if (guardian != null) {
                guardian.discard();
            }
        }
        PlayerDefenseSpellData.clearGuardian(player);
        PlayerDefenseSpellData.clearBodyShield(player);
        ModNetwork.sendDefenseSpellState(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clearAll(player);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
                && event.getTarget() instanceof ServerPlayer subject) {
            ModNetwork.sendDefenseSpellStateTo(observer, subject);
        }
    }

    private static void absorbWithBodyShield(LivingHurtEvent event, ServerPlayer player) {
        Optional<DefenseSpellSnapshot> snapshotOptional = PlayerDefenseSpellData.getBodyShield(player);
        if (snapshotOptional.isEmpty()) {
            return;
        }
        DefenseSpellSnapshot snapshot = snapshotOptional.get();
        if (!DefenseSpellUtility.canBodyShieldBlock(snapshot.kind(), event.getSource())) {
            return;
        }
        Optional<ElementType> attackElement = DefenseSpellUtility.getAttackElement(event.getSource());
        double multiplier = DefenseSpellUtility.getPointCostMultiplier(
                snapshot.kind(),
                snapshot.elementMask(),
                attackElement
        );
        if (multiplier <= 0.0D) {
            event.setCanceled(true);
            return;
        }
        double incomingDamage = event.getAmount();
        double pointCost = incomingDamage * multiplier;
        double consumed = PlayerDefenseSpellData.consumeBodyShield(player, pointCost);
        if (consumed > FULL_BLOCK_EPSILON) {
            PlayerDefenseSpellData.recordBodyShieldHit(
                    player,
                    attackElement.filter(element -> element == ElementType.CHAOS).isPresent()
            );
        }
        double absorbedDamage = consumed / multiplier;
        double remainingDamage = Math.max(0.0D, incomingDamage - absorbedDamage);
        boolean fullyBlocked = remainingDamage <= FULL_BLOCK_EPSILON;
        if (fullyBlocked) {
            event.setCanceled(true);
            applyFullBodyShieldBlockEffect(player, snapshot.kind(), event);
        } else {
            event.setAmount((float) remainingDamage);
        }
        ModNetwork.sendBodyShield(player);
        ModNetwork.sendDefenseSpellState(player);
    }

    private static void absorbWithGuardian(
            LivingHurtEvent event,
            DefenseSpellGuardianEntity guardian
    ) {
        if (event.getSource().is(ModDamageTypes.REALM_EXECUTION)
                || event.getSource().is(DamageTypes.GENERIC_KILL)
                || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity)) {
            event.setCanceled(true);
            return;
        }
        Optional<ElementType> attackElement = DefenseSpellUtility.getAttackElement(event.getSource());
        double multiplier = DefenseSpellUtility.getPointCostMultiplier(
                guardian.getSpellKind(),
                guardian.getElementMask(),
                attackElement
        );
        guardian.recordHit(attackElement);
        guardian.consumePoints(event.getAmount() * Math.max(multiplier, 0.0D));
        applyGuardianHitEffect(guardian, event.getSource().getEntity());
        ModNetwork.sendBodyShield(guardian);
        guardian.getOwnerPlayer().ifPresent(ModNetwork::sendDefenseSpellState);
        event.setCanceled(true);
        if (guardian.getCurrentPoints() <= FULL_BLOCK_EPSILON) {
            guardian.discard();
        }
    }

    private static void applyFullBodyShieldBlockEffect(
            ServerPlayer player,
            DefenseSpellKind kind,
            LivingHurtEvent event
    ) {
        if (kind == DefenseSpellKind.WATER_BODY_SHIELD) {
            player.clearFire();
        }
        if (kind == DefenseSpellKind.METAL_BODY_SHIELD
                && event.getSource().getEntity() instanceof LivingEntity attacker
                && event.getSource().getDirectEntity() == attacker) {
            attacker.knockback(
                    0.6D,
                    player.getX() - attacker.getX(),
                    player.getZ() - attacker.getZ()
            );
        }
    }

    private static void tickBodyShield(ServerPlayer player) {
        if (PlayerDefenseSpellData.getBodyShield(player).isEmpty()) {
            return;
        }
        if (!PlayerDefenseSpellData.tickBodyShield(player)) {
            ModNetwork.sendDefenseSpellState(player);
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        Optional<DefenseSpellSnapshot> snapshotOptional = PlayerDefenseSpellData.getBodyShield(player);
        if (snapshotOptional.isPresent() && PlayerDefenseSpellData.canRepairBodyShield(player)) {
            DefenseSpellSnapshot snapshot = snapshotOptional.get();
            double repaired = DefenseSpellUtility.repairWithSpiritualEnergy(
                    player,
                    snapshot.kind(),
                    snapshot.elementMask(),
                    snapshot.current(),
                    snapshot.maximum()
            );
            if (PlayerDefenseSpellData.repairBodyShield(player, repaired) > 0.0D) {
                ModNetwork.sendCultivationInfo(player);
            }
        }
        ModNetwork.sendDefenseSpellState(player);
    }

    private static void applyGuardianHitEffect(DefenseSpellGuardianEntity guardian, Entity attackerEntity) {
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }
        if (guardian.getSpellKind() == DefenseSpellKind.WOOD_GUARDIAN) {
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        } else if (guardian.getSpellKind() == DefenseSpellKind.FIRE_GUARDIAN) {
            attacker.setSecondsOnFire(2);
        }
    }

    private static void clearAll(ServerPlayer player) {
        DefenseSpellUtility.dismissGuardian(player);
        PlayerDefenseSpellData.clearBodyShield(player);
        ModNetwork.sendDefenseSpellState(player);
    }
}
