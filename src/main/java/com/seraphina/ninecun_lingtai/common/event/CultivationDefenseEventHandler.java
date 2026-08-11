package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.defense.EntityDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.registry.ModDamageTypes;
import com.seraphina.ninecun_lingtai.utility.CultivationCombatUtility;
import com.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;
import java.util.OptionalInt;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivationDefenseEventHandler {
    private static final int HEALTH_SYNC_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        CultivationDefenseUtility.syncMaxHealthModifier(entity);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || entity.tickCount % HEALTH_SYNC_INTERVAL_TICKS != 0) {
            return;
        }
        CultivationDefenseUtility.syncMaxHealthModifier(entity);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity defender = event.getEntity();
        if (event.getSource().is(DamageTypes.THORNS)) {
            return;
        }

        if (tryInstantlyKillLowerRealmCultivator(event, defender)) {
            return;
        }

        Optional<CultivationStage> defenderStage = CreatureCultivationData.getStage(defender);
        if (defenderStage.isEmpty()) {
            return;
        }

        if (tryBlockHighDamageCommandAttack(event, defender, defenderStage.get())) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker == defender) {
            return;
        }

        Optional<CultivationDefenseUtility.DefensiveReaction> reaction = CultivationDefenseUtility.getDefensiveReaction(
                defenderStage.get(),
                CreatureCultivationData.getStage(attacker),
                event.getAmount()
        );
        if (reaction.isEmpty()) {
            return;
        }

        CultivationDefenseUtility.DefensiveReaction defensiveReaction = reaction.get();
        event.setCanceled(true);
        attacker.hurt(defender.damageSources().thorns(defender), (float) defensiveReaction.reflectedDamage());
        CultivationDefenseUtility.applyKnockback(defender, attacker, defensiveReaction);
        if (defensiveReaction.showsBodyShield()) {
            ModNetwork.sendBodyShield(defender);
        }
    }

    private static boolean tryInstantlyKillLowerRealmCultivator(
            LivingAttackEvent event,
            LivingEntity defender
    ) {
        if (defender instanceof DefenseSpellGuardianEntity guardian
                && event.getSource().getEntity() instanceof LivingEntity guardianAttacker) {
            Optional<ServerPlayer> owner = guardian.getOwnerPlayer();
            if (owner.isPresent()
                    && CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                    CreatureCultivationData.getStage(guardianAttacker),
                    CreatureCultivationData.getStage(owner.get())
            )) {
                event.setCanceled(true);
                if (guardianAttacker instanceof Mob mob) {
                    mob.setTarget(owner.get());
                }
                owner.get().hurt(event.getSource(), event.getAmount());
                return true;
            }
            return false;
        }
        if (event.getSource().is(ModDamageTypes.REALM_EXECUTION)
                || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker == defender
                || !CultivationCombatUtility.canInstantlyKillLowerRealmCultivator(
                        CreatureCultivationData.getStage(attacker),
                        CreatureCultivationData.getStage(defender)
                )) {
            return false;
        }

        event.setCanceled(true);
        EntityDefenseResourceData.deplete(defender);
        if (defender instanceof ServerPlayer player) {
            ModNetwork.sendDefenseResource(player);
        }

        DamageSource executionSource = new DamageSource(
                defender.level()
                        .registryAccess()
                        .registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(ModDamageTypes.REALM_EXECUTION),
                event.getSource().getDirectEntity(),
                attacker
        );
        defender.hurt(executionSource, Float.MAX_VALUE);
        return true;
    }

    private static boolean tryBlockHighDamageCommandAttack(
            LivingAttackEvent event,
            LivingEntity defender,
            CultivationStage defenderStage
    ) {
        if (!CultivationDefenseUtility.isHighDamageCommandAttack(event.getSource(), event.getAmount())) {
            return false;
        }
        if (CultivationDefenseUtility.isFullyImmuneToHighDamage(defenderStage)) {
            event.setCanceled(true);
            syncPlayerDefenseResource(defender);
            return true;
        }
        OptionalInt immunityCost = CultivationDefenseUtility.getHighDamageImmunityCost(defenderStage);
        if (immunityCost.isEmpty()) {
            return false;
        }
        if (!EntityDefenseResourceData.consume(defender, immunityCost.getAsInt())) {
            EntityDefenseResourceData.clampToStage(defender);
            syncPlayerDefenseResource(defender);
            return false;
        }
        event.setCanceled(true);
        syncPlayerDefenseResource(defender);
        if (defenderStage.realm() == CultivationRealm.GOLDEN_CORE) {
            ModNetwork.sendBodyShield(defender);
        }
        return true;
    }

    private static void syncPlayerDefenseResource(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            ModNetwork.sendDefenseResource(player);
        }
    }
}
