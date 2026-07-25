package com.seraphina.ninecun_lingtai.client.animation;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
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

public final class MeditationAnimationManager {
    private static final int MEDITATION_LAYER_PRIORITY = 1_000;
    private static final ResourceLocation MEDITATION_ANIMATION_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "animation.model.meditation"
    );
    private static final ResourceLocation MEDITATION_LAYER_ID = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "meditation_layer"
    );

    private static boolean registered;

    private MeditationAnimationManager() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register(MeditationAnimationManager::registerPlayerLayer);
    }

    public static void setMeditating(UUID playerId, boolean meditating) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Player player = minecraft.level.getPlayerByUUID(playerId);
        if (player instanceof AbstractClientPlayer clientPlayer) {
            setMeditating(clientPlayer, meditating);
        }
    }

    private static void registerPlayerLayer(AbstractClientPlayer player, AnimationStack animationStack) {
        MeditationAnimationLayer meditationLayer = new MeditationAnimationLayer();
        animationStack.addAnimLayer(MEDITATION_LAYER_PRIORITY, meditationLayer);
        PlayerAnimationAccess.getPlayerAssociatedData(player).set(MEDITATION_LAYER_ID, meditationLayer);
        if (ClientMeditationManager.isMeditating(player.getUUID())) {
            setMeditating(player, true);
        }
    }

    private static void setMeditating(AbstractClientPlayer player, boolean meditating) {
        IAnimation animation = PlayerAnimationAccess.getPlayerAssociatedData(player).get(MEDITATION_LAYER_ID);
        if (!(animation instanceof MeditationAnimationLayer meditationLayer)) {
            return;
        }
        if (!meditating) {
            meditationLayer.setAnim(null);
            return;
        }
        if (meditationLayer.isActive()) {
            return;
        }
        KeyframeAnimation meditationAnimation = PlayerAnimationRegistry.getAnimation(MEDITATION_ANIMATION_ID);
        if (meditationAnimation == null) {
            return;
        }
        HoldOnLastFrameAnimationPlayer animationPlayer = new HoldOnLastFrameAnimationPlayer(meditationAnimation);
        animationPlayer.setFirstPersonMode(FirstPersonMode.DISABLED);
        meditationLayer.setAnim(animationPlayer);
    }

    private static final class MeditationAnimationLayer extends AnimationContainer<IAnimation> {
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
