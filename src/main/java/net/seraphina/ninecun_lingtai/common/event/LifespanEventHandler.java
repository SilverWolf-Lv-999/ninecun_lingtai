package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.lifespan.EntityLifespanData;
import net.seraphina.ninecun_lingtai.common.lifespan.LifespanSnapshot;
import net.seraphina.ninecun_lingtai.common.player.PlayerMeditationManager;
import net.seraphina.ninecun_lingtai.utility.LifespanUtility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LifespanEventHandler {
    private static final int CHECK_INTERVAL_TICKS = 20;

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide
                || entity.tickCount % CHECK_INTERVAL_TICKS != 0
                || !LifespanUtility.hasLifespan(entity)) {
            return;
        }

        LifespanSnapshot lifespan = EntityLifespanData.capture(entity);
        if (!lifespan.expired()) {
            return;
        }

        boolean expirationHandled = EntityLifespanData.isExpirationHandled(entity);
        if (entity instanceof ServerPlayer player) {
            if (!expirationHandled) {
                EntityLifespanData.markExpirationHandled(player);
                PlayerMeditationManager.stopMeditating(player);
                player.sendSystemMessage(Component.translatable("message.ninecun_lingtai.lifespan_expired"));
            }
            if (!player.isSpectator()) {
                player.setGameMode(GameType.SPECTATOR);
            }
            return;
        }
        if (!expirationHandled) {
            EntityLifespanData.markExpirationHandled(entity);
        }
        entity.discard();
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        EntityLifespanData.copy(event.getOriginal(), event.getEntity());
    }
}
