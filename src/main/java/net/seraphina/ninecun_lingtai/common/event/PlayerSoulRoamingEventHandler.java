package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import net.seraphina.ninecun_lingtai.common.player.PlayerPossessionManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerSoulRoamingManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PlayerSoulRoamingEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            PlayerSoulRoamingManager.tick(player);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            cancelSoulInteraction(event);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            cancelSoulInteraction(event);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelSoulInteraction(event);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelSoulInteraction(event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelSoulInteraction(event);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setNewSpeed(0.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            player.setHealth(Math.max(1.0F, player.getHealth()));
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof SoulEntity soul) {
            PlayerSoulRoamingManager.onSoulDestroyed(soul);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerPossessionManager.onPlayerLoggedIn(player);
            PlayerSoulRoamingManager.syncState(player);
            PlayerPossessionManager.syncState(player);
            PlayerPossessionManager.syncStatesTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original
                && event.getEntity() instanceof ServerPlayer clone) {
            PlayerPossessionManager.onPlayerClone(original, clone);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerSoulRoamingManager.cleanup(player);
            PlayerPossessionManager.cleanup(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            PlayerSoulRoamingManager.syncState(player);
        }
        if (event.getEntity() instanceof ServerPlayer player && PlayerPossessionManager.isPossessing(player)) {
            PlayerPossessionManager.syncState(player);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PlayerSoulRoamingManager.clear(event.getServer());
        PlayerPossessionManager.clear(event.getServer());
    }

    private static void cancelSoulInteraction(PlayerInteractEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PlayerSoulRoamingManager.isSoulRoaming(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
