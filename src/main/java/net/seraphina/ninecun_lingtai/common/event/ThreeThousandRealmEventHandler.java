package net.seraphina.ninecun_lingtai.common.event;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.player.PlayerThreeThousandRealmData;
import net.seraphina.ninecun_lingtai.common.realm.RealmDefinition;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.seraphina.ninecun_lingtai.utility.NotificationUtility;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ThreeThousandRealmEventHandler {
    private static final int REALM_ENTRY_NOTIFICATION_DURATION_TICKS = 20 * 3;

    private ThreeThousandRealmEventHandler() {
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        ThreeThousandRealmManager.initialize(event.getServer());
        ThreeThousandRealmManager.generateConfiguredRealmDefinitions(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        ThreeThousandRealmManager.registerPersistedRealmDefinitions(event.getServer());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ThreeThousandRealmManager.tick(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ThreeThousandRealmManager.clear(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        PlayerThreeThousandRealmData.copy(event.getOriginal(), event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && !PlayerThreeThousandRealmData.hasReceivedInitialRealmPlacement(player)) {
            placePlayerInRandomRealm(player, true);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && CommonConfig.THREE_THOUSAND_REALMS_RANDOMIZE_ON_RESPAWN.get()) {
            placePlayerInRandomRealm(player, false);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendRealmEntryNotification(player, event.getTo());
        }
    }

    private static void placePlayerInRandomRealm(ServerPlayer player, boolean initialPlacement) {
        ServerLevel previousLevel = player.serverLevel();
        try {
            boolean placed = ThreeThousandRealmManager.get(player.server)
                    .placePlayerInRandomRealm(player, new Random());
            if (!placed) {
                return;
            }
            if (initialPlacement) {
                PlayerThreeThousandRealmData.markInitialRealmPlacementReceived(player);
            }
            if (player.serverLevel() == previousLevel) {
                sendRealmEntryNotification(player, player.level().dimension());
            }
        } catch (RuntimeException exception) {
            ModMain.LOGGER.error(
                    "Unable to place player {} in a random three-thousand realm",
                    player.getGameProfile().getName(),
                    exception
            );
        }
    }

    private static void sendRealmEntryNotification(ServerPlayer player, net.minecraft.resources.ResourceKey<Level> levelKey) {
        ThreeThousandRealmManager.get(player.server).getRealmDefinition(levelKey)
                .ifPresent(realmDefinition -> sendRealmEntryNotification(player, realmDefinition));
    }

    private static void sendRealmEntryNotification(ServerPlayer player, RealmDefinition realmDefinition) {
        NotificationUtility.send(
                player,
                Component.translatable("notification.ninecun_lingtai.three_thousand_realm_entry_title"),
                Component.translatable(
                        "notification.ninecun_lingtai.three_thousand_realm_entry",
                        Component.literal(realmDefinition.getDisplayName())
                ),
                REALM_ENTRY_NOTIFICATION_DURATION_TICKS
        );
    }
}
