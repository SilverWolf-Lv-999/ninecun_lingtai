package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementApi;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.ElementalPower;
import com.seraphina.ninecun_lingtai.common.creature.CreatureCultivationData;
import com.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerValueControlType;
import com.seraphina.ninecun_lingtai.utility.ElementDamageUtility;
import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUtility;
import com.seraphina.ninecun_lingtai.utility.PlayerValueControlUnlockUtility;
import com.seraphina.ninecun_lingtai.utility.RealmBonusDamageUtility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RealmBonusDamageEventHandler {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || event.getEntity() == attacker) {
            return;
        }

        Optional<CultivationStage> stage = CreatureCultivationData.getStage(attacker);
        Optional<SpiritualRoot> spiritualRoot = CreatureCultivationData.getSpiritualRoot(attacker);
        if (stage.isEmpty() || spiritualRoot.isEmpty()) {
            return;
        }

        double bonusDamage = RealmBonusDamageUtility.getBonusDamage(stage.get());
        if (attacker instanceof Player player) {
            bonusDamage = PlayerValueControlUtility.applyApplication(
                    bonusDamage,
                    PlayerValueControlUnlockUtility.getEffectiveApplication(
                            player,
                            PlayerValueControlType.ATTACK_DAMAGE
                    )
            );
        }
        if (bonusDamage <= 0.0D) {
            return;
        }

        ElementType bonusElement = RealmBonusDamageUtility.rollBonusElement(
                spiritualRoot.get(),
                attacker.getRandom()
        );
        ElementalPower bonusPower = RealmBonusDamageUtility.createBonusElementalPower(
                bonusElement,
                stage.get(),
                spiritualRoot.get()
        );

        double multiplier = ElementEnvironmentManager.INSTANCE.getDamageMultiplier(
                event.getEntity(),
                bonusElement
        );
        Optional<ElementalPower> defenseElement = ElementApi.getDefenseElement(event.getEntity());
        if (defenseElement.isPresent()) {
            multiplier *= ElementDamageUtility.getDamageMultiplier(bonusPower, defenseElement.get());
        }

        double finalBonusDamage = bonusDamage * multiplier;
        if (finalBonusDamage <= 0.0D) {
            return;
        }
        event.setAmount(event.getAmount() + (float) finalBonusDamage);
        ElementDamageUtility.spawnHitParticles(event.getEntity(), bonusPower);
    }
}
