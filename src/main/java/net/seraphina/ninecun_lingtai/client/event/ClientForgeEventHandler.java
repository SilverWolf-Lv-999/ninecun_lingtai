package net.seraphina.ninecun_lingtai.client.event;

import net.minecraftforge.client.event.*;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import net.seraphina.ninecun_lingtai.client.player.ClientBodyShieldManager;
import net.seraphina.ninecun_lingtai.client.notification.ClientNotificationManager;
import net.seraphina.ninecun_lingtai.client.player.ClientCultivationFeedbackManager;
import net.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import net.seraphina.ninecun_lingtai.client.player.ClientDefenseResourceManager;
import net.seraphina.ninecun_lingtai.client.player.ClientDefenseSpellManager;
import net.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import net.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPlayerValueControlManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPossessionManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPressureManager;
import net.seraphina.ninecun_lingtai.client.player.ClientShrinkEarthManager;
import net.seraphina.ninecun_lingtai.client.player.ClientPushingMountainsManager;
import net.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import net.seraphina.ninecun_lingtai.client.player.ClientSoulRoamingManager;
import net.seraphina.ninecun_lingtai.client.player.ClientSpellManager;
import net.seraphina.ninecun_lingtai.client.player.ClientSwordFlightManager;
import net.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import net.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.client.ring.ClientStorageRingImmersionManager;
import net.seraphina.ninecun_lingtai.client.ring.ClientStorageRingProjectionManager;
import net.seraphina.ninecun_lingtai.client.screen.ClientAptitudeScreenManager;
import net.seraphina.ninecun_lingtai.client.utility.BodyShieldRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.AlchemyFurnaceHighlightRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.DefenseBarRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.DefenseSpellHudRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.TribulationCloudRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.NotificationRenderUtility;
import net.seraphina.ninecun_lingtai.client.render.PushingMountainsRenderMeshManager;
import net.seraphina.ninecun_lingtai.client.utility.PushingMountainsRenderUtility;
import net.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsEntity;
import net.seraphina.ninecun_lingtai.client.utility.ItemTooltipUtility;
import net.seraphina.ninecun_lingtai.client.utility.SpiritualEnergyHudRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.SpiritualEnergyRenderUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEventHandler {

    @SubscribeEvent
    public static void renderTooltip(RenderTooltipEvent.Pre event) {
        ItemTooltipUtility.resolveTooltipFont(event.getItemStack()).ifPresent(event::setFont);
    }

    @SubscribeEvent
    public static void colorTooltip(RenderTooltipEvent.Color event) {
        ItemTooltipUtility.applyTooltipColors(event);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientAptitudeScreenManager.tick();
            ClientPressureManager.tick();
            ClientMeditationManager.tick();
            ClientSpellManager.tick();
            ClientSwordFlightManager.tick();
            ClientLightFlightManager.tick();
            ClientShrinkEarthManager.tick();
            ClientBodyShieldManager.tick();
            ClientDefenseSpellManager.tick();
            ClientSoulRoamingManager.tick();
            ClientNotificationManager.tick();
            ClientTribulationManager.tick();
            ClientPushingMountainsManager.tick();
            ClientStorageRingImmersionManager.tick();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && ModKeyMappings.STORAGE_RING_IMMERSION.matches(event.getKey(), event.getScanCode())
                && ClientStorageRingImmersionManager.beginExiting()) {
            return;
        }
        ClientPushingMountainsManager.onKeyInput(event.getKey(), event.getAction());
        ClientShrinkEarthManager.onKeyInput(event.getKey(), event.getAction());
        if (event.getAction() == GLFW.GLFW_PRESS
                && (event.getKey() == GLFW.GLFW_KEY_LEFT_SHIFT || event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            if (!ClientSoulRoamingManager.isLocalPlayerSoulRoaming()
                    && ClientMeditationManager.isLocalPlayerMeditating()) {
                ClientMeditationManager.requestStop();
            }
            if (Minecraft.getInstance().screen == null
                    && !ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
                ClientSwordFlightManager.onShiftPressed();
            }
        }
        if (event.getAction() == GLFW.GLFW_PRESS && event.getKey() == GLFW.GLFW_KEY_SPACE) {
            ClientLightFlightManager.onJumpPressed();
        }
        if (event.getAction() == GLFW.GLFW_PRESS
                && (event.getKey() == GLFW.GLFW_KEY_LEFT_CONTROL || event.getKey() == GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            ClientLightFlightManager.onControlPressed();
        }
    }

    @SubscribeEvent
    public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
        if (ClientStorageRingImmersionManager.handleInteraction(event)) {
            return;
        }
        if (ClientPushingMountainsManager.handleInteraction(event)) {
            return;
        }
        if (ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            if (event.isUseItem()) {
                Minecraft minecraft = Minecraft.getInstance();
                Entity target = minecraft.crosshairPickEntity;
                ClientSoulRoamingManager.sendInteraction(target == null ? -1 : target.getId());
            }
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (ClientMeditationManager.isLocalPlayerMeditating()
                && !ClientTribulationManager.isLocalPlayerInTribulation()) {
            event.setSwingHand(false);
            event.setCanceled(true);
            return;
        }
        if (ClientSwordFlightManager.handleInteraction(event)) {
            return;
        }
    }

    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (ClientPushingMountainsManager.isSelectionMode()) {
            ClientPushingMountainsManager.onMouseScroll(event.getScrollDelta());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (ClientStorageRingImmersionManager.handleMovementInput(event.getInput())) {
            return;
        }
        if (ClientPressureManager.isLocalPlayerImmobilized()) {
            Input input = event.getInput();
            input.leftImpulse = 0.0F;
            input.forwardImpulse = 0.0F;
            input.jumping = false;
            input.shiftKeyDown = false;
            return;
        }
        if (ClientSoulRoamingManager.isLocalPlayerSoulRoaming()) {
            Input input = event.getInput();
            ClientSoulRoamingManager.sendMovement(input);
            input.leftImpulse = 0.0F;
            input.forwardImpulse = 0.0F;
            input.jumping = false;
            input.shiftKeyDown = false;
            return;
        }
        if (!ClientMeditationManager.isLocalPlayerMeditating()) {
            return;
        }
        Input input = event.getInput();
        input.leftImpulse = 0.0F;
        input.forwardImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        SpiritualEnergyRenderUtility.render(event);
        BodyShieldRenderUtility.render(event);
        TribulationCloudRenderUtility.renderWorld(event);
        PushingMountainsRenderUtility.render(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderStorageRingImmersive(RenderLevelStageEvent event) {
        ClientStorageRingImmersionManager.renderImmersive(event);
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ClientStorageRingImmersionManager.isImmersiveProjectionActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        AlchemyFurnaceHighlightRenderUtility.render(event);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        DefenseBarRenderUtility.render(event);
        DefenseSpellHudRenderUtility.render(event);
        SpiritualEnergyHudRenderUtility.render(event);
        ClientSoulRoamingManager.renderBodyArrow(event);
        TribulationCloudRenderUtility.render(event);
        NotificationRenderUtility.render(event);
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ClientMeditationManager.removePlayer(player.getUUID());
            ClientSwordFlightManager.removePlayer(player.getUUID());
            ClientLightFlightManager.removePlayer(player.getUUID());
            ClientShrinkEarthManager.removePlayer(player.getUUID());
        }
        if (event.getLevel().isClientSide) {
            ClientBodyShieldManager.removeEntity(event.getEntity().getId());
            ClientDefenseSpellManager.removeEntity(event.getEntity().getId());
            if (event.getEntity() instanceof PushingMountainsEntity region) {
                PushingMountainsRenderMeshManager.clearEntity(region.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        event.setFOV(event.getFOV() * ClientLightFlightManager.getFovMultiplier((float) event.getPartialTick()));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientAptitudeScreenManager.reset();
        ClientCultivationInfoManager.reset();
        ClientTechniqueManager.reset();
        ClientAlchemyRecipeManager.reset();
        ClientMeditationManager.reset();
        ClientSpellManager.reset();
        ClientSwordFlightManager.reset();
        ClientLightFlightManager.reset();
        ClientShrinkEarthManager.reset();
        ClientBodyShieldManager.reset();
        ClientDefenseResourceManager.reset();
        ClientDefenseSpellManager.reset();
        ClientCultivationFeedbackManager.reset();
        ClientPlayerValueControlManager.reset();
        ClientSoulRoamingManager.reset();
        ClientPossessionManager.reset();
        ClientNotificationManager.reset();
        ClientTribulationManager.reset();
        ClientPushingMountainsManager.reset();
        ClientPressureManager.reset();
        PushingMountainsRenderMeshManager.clearAll();
        ClientStorageRingProjectionManager.reset();
    }
}
