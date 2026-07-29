package com.seraphina.ninecun_lingtai.client.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.player.ClientBodyShieldManager;
import com.seraphina.ninecun_lingtai.client.notification.ClientNotificationManager;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationFeedbackManager;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.client.player.ClientDefenseResourceManager;
import com.seraphina.ninecun_lingtai.client.player.ClientDefenseSpellManager;
import com.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import com.seraphina.ninecun_lingtai.client.player.ClientPlayerValueControlManager;
import com.seraphina.ninecun_lingtai.client.player.ClientPossessionManager;
import com.seraphina.ninecun_lingtai.client.player.ClientShrinkEarthManager;
import com.seraphina.ninecun_lingtai.client.tribulation.ClientTribulationManager;
import com.seraphina.ninecun_lingtai.client.player.ClientSoulRoamingManager;
import com.seraphina.ninecun_lingtai.client.player.ClientSpellManager;
import com.seraphina.ninecun_lingtai.client.player.ClientSwordFlightManager;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.client.screen.ClientAptitudeScreenManager;
import com.seraphina.ninecun_lingtai.client.utility.BodyShieldRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.DefenseBarRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.DefenseSpellHudRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.TribulationCloudRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.NotificationRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.SpiritualEnergyHudRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.SpiritualEnergyRenderUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientForgeEventHandler {
    private ClientForgeEventHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ClientAptitudeScreenManager.tick();
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
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
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
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
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
    }
}
