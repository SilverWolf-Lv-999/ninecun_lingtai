package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRootApi;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementApi;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.ElementalPower;
import com.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationData;
import com.seraphina.ninecun_lingtai.utility.ElementDamageUtility;
import com.seraphina.ninecun_lingtai.utility.RealmBonusDamageUtility;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RealmBonusDamageEventHandler {
    private RealmBonusDamageEventHandler() {
    }

    // LOW priority so the elemental multiplier pass in ElementDamageEventHandler only scales
    // the base damage; the realm bonus applies its own elemental multipliers below.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || event.getEntity() == player) {
            return;
        }

        Optional<CultivationStage> stage = PlayerCultivationData.getStage(player);
        Optional<SpiritualRoot> spiritualRoot = SpiritualRootApi.getSpiritualRoot(player);
        if (stage.isEmpty() || spiritualRoot.isEmpty()) {
            return;
        }

        double bonusDamage = RealmBonusDamageUtility.getBonusDamage(stage.get());
        if (bonusDamage <= 0.0D) {
            return;
        }

        ElementType bonusElement = RealmBonusDamageUtility.rollBonusElement(
                spiritualRoot.get(),
                player.getRandom()
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
