package com.seraphina.ninecun_lingtai.client.animation;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import dev.kosmx.playerAnim.api.layered.AnimationContainer;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class LightFlightAnimationManager {
    private static final int LIGHT_FLIGHT_LAYER_PRIORITY = 950;
    private static final ResourceLocation LIGHT_FLIGHT_ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "animation.model.fly"
    );
    private static final ResourceLocation LIGHT_FLIGHT_LAYER_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "light_flight_layer"
    );

    private static boolean registered;

    private LightFlightAnimationManager() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(LightFlightAnimationManager::registerPlayerLayer);
    }

    public static void setFlying(UUID playerId, boolean flying) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Player player = minecraft.level.getPlayerByUUID(playerId);
        if (player instanceof AbstractClientPlayer clientPlayer) {
            setFlying(clientPlayer, flying);
        }
    }

    private static void registerPlayerLayer(AbstractClientPlayer player, AnimationStack animationStack) {
        LightFlightAnimationLayer lightFlightLayer = new LightFlightAnimationLayer();
        animationStack.addAnimLayer(LIGHT_FLIGHT_LAYER_PRIORITY, lightFlightLayer);
        PlayerAnimationAccess.getPlayerAssociatedData(player).set(LIGHT_FLIGHT_LAYER_ID, lightFlightLayer);
        if (ClientLightFlightManager.isFlying(player.getUUID())) {
            setFlying(player, true);
        }
    }

    private static void setFlying(AbstractClientPlayer player, boolean flying) {
        IAnimation animation = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LIGHT_FLIGHT_LAYER_ID);
        if (!(animation instanceof LightFlightAnimationLayer lightFlightLayer)) {
            return;
        }
        if (!flying) {
            lightFlightLayer.setAnim(null);
            return;
        }
        if (lightFlightLayer.isActive()) {
            return;
        }
        KeyframeAnimation lightFlightAnimation = PlayerAnimationRegistry.getAnimation(LIGHT_FLIGHT_ANIMATION_ID);
        if (lightFlightAnimation == null) {
            return;
        }
        HoldOnLastFrameAnimationPlayer animationPlayer = new HoldOnLastFrameAnimationPlayer(lightFlightAnimation);
        animationPlayer.setFirstPersonMode(FirstPersonMode.DISABLED);
        lightFlightLayer.setAnim(animationPlayer);
    }

    private static final class LightFlightAnimationLayer extends AnimationContainer<IAnimation> {
    }

    private static final class HoldOnLastFrameAnimationPlayer extends KeyframeAnimationPlayer {
        private HoldOnLastFrameAnimationPlayer(KeyframeAnimation animation) {
            super(animation);
        }

        @Override
        public void tick() {
            if (getCurrentTick() < getData().endTick) {
                super.tick();
            }
        }

        @Override
        public void setupAnim(float tickDelta) {
            super.setupAnim(getCurrentTick() >= getData().endTick ? 0.0F : tickDelta);
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }
}
