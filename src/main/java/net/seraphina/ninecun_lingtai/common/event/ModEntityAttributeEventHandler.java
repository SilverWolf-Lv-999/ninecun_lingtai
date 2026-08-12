package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import net.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEntityAttributeEventHandler {

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.SOUL_ENTITY.get(), SoulEntity.createAttributes().build());
        event.put(
                ModEntityTypes.DEFENSE_SPELL_GUARDIAN.get(),
                DefenseSpellGuardianEntity.createAttributes().build()
        );
    }
}
