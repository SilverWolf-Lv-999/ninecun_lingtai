package com.seraphina.ninecun_lingtai.client.event;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.player.ClientBodyShieldManager;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationFeedbackManager;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import com.seraphina.ninecun_lingtai.client.player.ClientPlayerValueControlManager;
import com.seraphina.ninecun_lingtai.client.player.ClientSpellManager;
import com.seraphina.ninecun_lingtai.client.player.ClientSwordFlightManager;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.client.screen.ClientAptitudeScreenManager;
import com.seraphina.ninecun_lingtai.client.utility.BodyShieldRenderUtility;
import com.seraphina.ninecun_lingtai.client.utility.SpiritualEnergyRenderUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
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
            ClientBodyShieldManager.tick();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS
                && (event.getKey() == GLFW.GLFW_KEY_LEFT_SHIFT || event.getKey() == GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            if (ClientMeditationManager.isLocalPlayerMeditating()) {
                ClientMeditationManager.requestStop();
            }
            if (Minecraft.getInstance().screen == null) {
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
        if (ClientMeditationManager.isLocalPlayerMeditating()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
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
    }

    @SubscribeEvent
    public static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide && event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            ClientMeditationManager.removePlayer(player.getUUID());
            ClientSwordFlightManager.removePlayer(player.getUUID());
            ClientLightFlightManager.removePlayer(player.getUUID());
        }
        if (event.getLevel().isClientSide) {
            ClientBodyShieldManager.removeEntity(event.getEntity().getId());
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
        ClientBodyShieldManager.reset();
        ClientCultivationFeedbackManager.reset();
        ClientPlayerValueControlManager.reset();
    }
}
