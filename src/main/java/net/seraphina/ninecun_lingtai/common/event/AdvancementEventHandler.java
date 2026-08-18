package net.seraphina.ninecun_lingtai.common.event;

import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.seraphina.ninecun_lingtai.ModMain;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AdvancementEventHandler {
    private static final ResourceLocation STEP_INTO_IMMORTAL_PATH_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "step_into_immortal_path");
    private static final String JOINED_WORLD_CRITERION = "joined_world";

    private AdvancementEventHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Advancement advancement = serverPlayer.server.getAdvancements()
                .getAdvancement(STEP_INTO_IMMORTAL_PATH_ADVANCEMENT_ID);
        if (advancement == null) {
            return;
        }

        if (!serverPlayer.getAdvancements().getOrStartProgress(advancement).isDone()) {
            serverPlayer.getAdvancements().award(advancement, JOINED_WORLD_CRITERION);
        }
    }
}
