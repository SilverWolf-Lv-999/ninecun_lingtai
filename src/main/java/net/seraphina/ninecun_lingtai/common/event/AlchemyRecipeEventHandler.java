package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.data.AlchemyFurnaceDefinitionManager;
import net.seraphina.ninecun_lingtai.common.data.AlchemyHerbManager;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AlchemyRecipeEventHandler {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(AlchemyRecipeManager.INSTANCE);
        event.addListener(AlchemyFurnaceDefinitionManager.INSTANCE);
        event.addListener(AlchemyHerbManager.INSTANCE);
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getPlayers().forEach(ModNetwork::sendAlchemyRecipes);
    }
}
