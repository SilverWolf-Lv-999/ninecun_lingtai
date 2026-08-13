package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.player.PlayerPillToxinData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PillToxinEventHandler {
    private static final int MINUTE_TICKS = 1200;
    private static final float OUTBREAK_DAMAGE_PER_MINUTE = 2.0F;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % MINUTE_TICKS != 0) {
            return;
        }
        double toxin = PlayerPillToxinData.getToxin(player);
        if (toxin <= 0.0D) {
            return;
        }
        PlayerPillToxinData.reduceToxin(
                player,
                CommonConfig.ALCHEMY_TOXIN_DECAY_PER_DAY.get() / 20.0D
        );
        if (toxin >= PlayerPillToxinData.OUTBREAK_THRESHOLD) {
            player.hurt(player.damageSources().magic(), OUTBREAK_DAMAGE_PER_MINUTE);
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.pill_toxin_outbreak_tick"),
                    true
            );
        }
        if (toxin >= PlayerPillToxinData.MARROW_THRESHOLD) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            if (player.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SMOKE,
                        player.getX(),
                        player.getY() + 1.0D,
                        player.getZ(),
                        8,
                        0.3D,
                        0.5D,
                        0.3D,
                        0.01D
                );
            }
        }
    }
}
