package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CultivationDefenseEventHandler {
    private static final int HEALTH_SYNC_INTERVAL_TICKS = 20;

    private CultivationDefenseEventHandler() {
    }

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
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)
                || attacker == defender
                || event.getSource().is(DamageTypes.THORNS)) {
            return;
        }

        Optional<CultivationStage> defenderStage = CreatureCultivationData.getStage(defender);
        if (defenderStage.isEmpty()) {
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
}
