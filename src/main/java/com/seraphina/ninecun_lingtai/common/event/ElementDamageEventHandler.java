package com.seraphina.ninecun_lingtai.common.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.element.ElementApi;
import com.seraphina.ninecun_lingtai.api.element.ElementalPower;
import com.seraphina.ninecun_lingtai.common.data.DamageElementManager;
import com.seraphina.ninecun_lingtai.common.data.ElementEnvironmentManager;
import com.seraphina.ninecun_lingtai.utility.ElementDamageUtility;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ElementDamageEventHandler {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(DamageElementManager.INSTANCE);
        event.addListener(ElementEnvironmentManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Optional<ElementalPower> attackElement = DamageElementManager.INSTANCE.getElement(event.getSource());
        if (attackElement.isEmpty()) {
            return;
        }

        ElementalPower elementalPower = attackElement.get();
        double multiplier = ElementEnvironmentManager.INSTANCE.getDamageMultiplier(
                event.getEntity(),
                elementalPower.element()
        );
        Optional<ElementalPower> defenseElement = ElementApi.getDefenseElement(event.getEntity());
        if (defenseElement.isPresent()) {
            multiplier *= ElementDamageUtility.getDamageMultiplier(elementalPower, defenseElement.get());
        }
        event.setAmount((float) Math.max(0.0D, event.getAmount() * multiplier));
        ElementDamageUtility.spawnHitParticles(event.getEntity(), elementalPower);
    }
}
