package net.seraphina.ninecun_lingtai.client.animation;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import net.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
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
    private static final ResourceLocation AIR_WALKING_ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "animation.model.air_walking"
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

    public static void setFlying(UUID playerId, boolean flying, LightFlightStyle style) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Player player = minecraft.level.getPlayerByUUID(playerId);
        if (player instanceof AbstractClientPlayer clientPlayer) {
            setFlying(clientPlayer, flying, style);
        }
    }

    private static void registerPlayerLayer(AbstractClientPlayer player, AnimationStack animationStack) {
        LightFlightAnimationLayer lightFlightLayer = new LightFlightAnimationLayer();
        animationStack.addAnimLayer(LIGHT_FLIGHT_LAYER_PRIORITY, lightFlightLayer);
        PlayerAnimationAccess.getPlayerAssociatedData(player).set(LIGHT_FLIGHT_LAYER_ID, lightFlightLayer);
        if (ClientLightFlightManager.isFlying(player.getUUID())) {
            setFlying(player, true, ClientLightFlightManager.getStyle(player.getUUID()));
        }
    }

    private static void setFlying(AbstractClientPlayer player, boolean flying, LightFlightStyle style) {
        IAnimation animation = PlayerAnimationAccess.getPlayerAssociatedData(player).get(LIGHT_FLIGHT_LAYER_ID);
        if (!(animation instanceof LightFlightAnimationLayer lightFlightLayer)) {
            return;
        }
        if (!flying) {
            lightFlightLayer.setAnim(null);
            lightFlightLayer.setStyle(null);
            return;
        }
        LightFlightStyle sanitizedStyle = style == null ? LightFlightStyle.LIGHT : style;
        if (lightFlightLayer.isActive() && lightFlightLayer.getStyle() == sanitizedStyle) {
            return;
        }
        KeyframeAnimation lightFlightAnimation = PlayerAnimationRegistry.getAnimation(getAnimationId(sanitizedStyle));
        if (lightFlightAnimation == null) {
            return;
        }
        HoldOnLastFrameAnimationPlayer animationPlayer = new HoldOnLastFrameAnimationPlayer(lightFlightAnimation);
        animationPlayer.setFirstPersonMode(FirstPersonMode.DISABLED);
        lightFlightLayer.setAnim(animationPlayer);
        lightFlightLayer.setStyle(sanitizedStyle);
    }

    private static ResourceLocation getAnimationId(LightFlightStyle style) {
        return style == LightFlightStyle.AIR_WALKING ? AIR_WALKING_ANIMATION_ID : LIGHT_FLIGHT_ANIMATION_ID;
    }

    private static final class LightFlightAnimationLayer extends AnimationContainer<IAnimation> {
        private LightFlightStyle style;

        private LightFlightStyle getStyle() {
            return this.style;
        }

        private void setStyle(LightFlightStyle style) {
            this.style = style;
        }
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
