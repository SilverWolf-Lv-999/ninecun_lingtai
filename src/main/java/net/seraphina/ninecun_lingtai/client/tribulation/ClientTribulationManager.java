package net.seraphina.ninecun_lingtai.client.tribulation;

import net.seraphina.ninecun_lingtai.client.screen.CultivationScreen;
import net.seraphina.ninecun_lingtai.client.screen.ClientAscensionQuestionScreenManager;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncTribulationStatePacket;
import net.seraphina.ninecun_lingtai.common.tribulation.TribulationPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientTribulationManager {
    private static final int PREPARATION_TICKS = 200;
    private static final int DISSIPATION_TICKS = 100;
    private static final Map<UUID, ClientTribulationState> STATES = new HashMap<>();


    public static void tick() {
        STATES.values().forEach(ClientTribulationState::tick);
    }

    public static void accept(SyncTribulationStatePacket packet) {
        if (!packet.active()) {
            STATES.remove(packet.playerId());
            LocalPlayer localPlayer = Minecraft.getInstance().player;
            if (localPlayer != null && localPlayer.getUUID().equals(packet.playerId())) {
                ClientAscensionQuestionScreenManager.close();
            }
            return;
        }
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer != null
                && localPlayer.getUUID().equals(packet.playerId())
                && Minecraft.getInstance().screen instanceof CultivationScreen) {
            Minecraft.getInstance().setScreen(null);
        }
        ClientTribulationState state = STATES.get(packet.playerId());
        if (state == null) {
            STATES.put(packet.playerId(), new ClientTribulationState(
                    packet.phase(),
                    packet.phaseRemainingTicks(),
                    packet.totalStrikes(),
                    packet.completedStrikes(),
                    new Vec3(packet.centerX(), packet.centerY(), packet.centerZ()),
                    packet.radius(),
                    packet.intensity()
            ));
            return;
        }
        state.update(packet);
    }

    public static float getLocalDarkness(float partialTick) {
        ClientTribulationState state = getLocalState();
        LocalPlayer player = Minecraft.getInstance().player;
        if (state == null || player == null || !isInsideArea(player, state)) {
            return 0.0F;
        }
        float phaseDarkness = getCloudIntensity(state, partialTick);
        float distanceFactor = 1.0F - Mth.clamp(
                (float) Math.sqrt(player.distanceToSqr(state.center)) / Math.max(1.0F, state.radius),
                0.0F,
                1.0F
        );
        return Mth.clamp(phaseDarkness * (0.45F + distanceFactor * 0.55F), 0.0F, 1.0F);
    }

    public static float getLocalCloudAlpha(float partialTick) {
        return getLocalDarkness(partialTick) * 0.78F;
    }

    public static float getCloudIntensity(ClientTribulationState state, float partialTick) {
        float remainingTicks = Math.max(0.0F, state.phaseRemainingTicks - partialTick);
        return switch (state.phase) {
            case PREPARING -> 1.0F - Mth.clamp(
                    remainingTicks / (float) PREPARATION_TICKS,
                    0.0F,
                    1.0F
            );
            case STRIKING -> 1.0F;
            case AFTERMATH -> 0.92F;
            case DISSIPATING -> Mth.clamp(
                    remainingTicks / (float) DISSIPATION_TICKS,
                    0.0F,
                    1.0F
            );
        };
    }

    public static float applyCameraYRot(Entity cameraEntity, float baseRotation, float partialTick) {
        if (!isLocalCamera(cameraEntity)) {
            return baseRotation;
        }
        return baseRotation + getShake(cameraEntity, partialTick, 0.0F);
    }

    public static float applyCameraXRot(Entity cameraEntity, float baseRotation, float partialTick) {
        if (!isLocalCamera(cameraEntity)) {
            return baseRotation;
        }
        return baseRotation + getShake(cameraEntity, partialTick, 1.7F) * 0.72F;
    }

    public static Vec3 applyCameraViewVector(Entity cameraEntity, Vec3 baseVector, float partialTick) {
        if (!isLocalCamera(cameraEntity)) {
            return baseVector;
        }
        float yRot = (float) Math.toDegrees(Math.atan2(-baseVector.x, baseVector.z))
                + getShake(cameraEntity, partialTick, 0.0F);
        float xRot = (float) Math.toDegrees(Math.asin(Mth.clamp(-baseVector.y, -1.0D, 1.0D)))
                + getShake(cameraEntity, partialTick, 1.7F) * 0.72F;
        return Vec3.directionFromRotation(xRot, yRot);
    }

    public static Map<UUID, ClientTribulationState> getStates() {
        return Collections.unmodifiableMap(STATES);
    }

    public static boolean isLocalPlayerInTribulation() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && STATES.containsKey(player.getUUID());
    }

    public static void reset() {
        STATES.clear();
    }

    private static ClientTribulationState getLocalState() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : STATES.get(player.getUUID());
    }

    private static boolean isLocalCamera(Entity cameraEntity) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && cameraEntity == player && getLocalState() != null;
    }

    private static boolean isInsideArea(LocalPlayer player, ClientTribulationState state) {
        return player.level().dimension().equals(state.dimension)
                && player.distanceToSqr(state.center) <= (state.radius + 16.0D) * (state.radius + 16.0D);
    }

    private static float getShake(Entity cameraEntity, float partialTick, float phaseOffset) {
        ClientTribulationState state = getLocalState();
        if (state == null || state.phase == TribulationPhase.PREPARING || state.phase == TribulationPhase.DISSIPATING) {
            return 0.0F;
        }
        float distanceFactor = Mth.clamp(
                1.0F - (float) Math.sqrt(cameraEntity.distanceToSqr(state.center)) / Math.max(1.0F, state.radius),
                0.0F,
                1.0F
        );
        float strikeFactor = state.completedStrikes <= 0 ? 0.0F : 1.0F;
        float time = cameraEntity.tickCount + partialTick;
        return Mth.sin(time * 1.9F + phaseOffset) * 0.55F * distanceFactor * strikeFactor;
    }

    public static final class ClientTribulationState {
        private TribulationPhase phase;
        private float phaseRemainingTicks;
        private int totalStrikes;
        private int completedStrikes;
        private Vec3 center;
        private float radius;
        private float intensity;
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;

        private ClientTribulationState(
                TribulationPhase phase,
                int phaseRemainingTicks,
                int totalStrikes,
                int completedStrikes,
                Vec3 center,
                float radius,
                float intensity
        ) {
            this.phase = phase;
            this.phaseRemainingTicks = phaseRemainingTicks;
            this.totalStrikes = totalStrikes;
            this.completedStrikes = completedStrikes;
            this.center = center;
            this.radius = radius;
            this.intensity = intensity;
            this.dimension = Minecraft.getInstance().level == null
                    ? net.minecraft.world.level.Level.OVERWORLD
                    : Minecraft.getInstance().level.dimension();
        }

        private void tick() {
            this.phaseRemainingTicks = Math.max(0.0F, this.phaseRemainingTicks - 1.0F);
        }

        private void update(SyncTribulationStatePacket packet) {
            TribulationPhase packetPhase = packet.phase();
            if (this.phase != packetPhase) {
                this.phaseRemainingTicks = packet.phaseRemainingTicks();
            } else {
                // Correct network drift gradually so the cloud animation never jumps when
                // the server sends its periodic authoritative state.
                this.phaseRemainingTicks = Mth.lerp(
                        0.40F,
                        this.phaseRemainingTicks,
                        packet.phaseRemainingTicks()
                );
            }
            this.phase = packetPhase;
            this.totalStrikes = packet.totalStrikes();
            this.completedStrikes = packet.completedStrikes();
            this.center = new Vec3(packet.centerX(), packet.centerY(), packet.centerZ());
            this.radius = packet.radius();
            this.intensity = packet.intensity();
        }

        public Vec3 center() {
            return this.center;
        }

        public TribulationPhase phase() {
            return this.phase;
        }

        public int phaseRemainingTicks() {
            return (int) Math.ceil(this.phaseRemainingTicks);
        }

        public int completedStrikes() {
            return this.completedStrikes;
        }

        public float radius() {
            return this.radius;
        }

        public float intensity() {
            return this.intensity;
        }
    }
}
