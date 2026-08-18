package net.seraphina.ninecun_lingtai.client.animation;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.client.player.ClientLightFlightManager;
import net.seraphina.ninecun_lingtai.common.flight.LightFlightStyle;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
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
        LightFlightAnimationLayer lightFlightLayer = new LightFlightAnimationLayer(player);
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
            lightFlightLayer.setAnimation(null, null);
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
        lightFlightLayer.setAnimation(animationPlayer, sanitizedStyle);
    }

    private static ResourceLocation getAnimationId(LightFlightStyle style) {
        return style == LightFlightStyle.AIR_WALKING ? AIR_WALKING_ANIMATION_ID : LIGHT_FLIGHT_ANIMATION_ID;
    }

    private static final class LightFlightAnimationLayer extends AnimationStack {
        private final AirWalkingLegAnimation airWalkingLegAnimation;
        private KeyframeAnimationPlayer animationPlayer;
        private LightFlightStyle style;

        private LightFlightAnimationLayer(AbstractClientPlayer player) {
            this.airWalkingLegAnimation = new AirWalkingLegAnimation(player);
            addAnimLayer(1, this.airWalkingLegAnimation);
        }

        private LightFlightStyle getStyle() {
            return this.style;
        }

        private void setAnimation(KeyframeAnimationPlayer animationPlayer, LightFlightStyle style) {
            if (this.animationPlayer != null) {
                removeLayer(this.animationPlayer);
            }
            this.animationPlayer = animationPlayer;
            if (animationPlayer != null) {
                addAnimLayer(0, animationPlayer);
            }
            this.style = style;
            this.airWalkingLegAnimation.setActive(
                    animationPlayer != null && style == LightFlightStyle.AIR_WALKING
            );
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
