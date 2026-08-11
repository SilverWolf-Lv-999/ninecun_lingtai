package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.api.element.FiveElementValues;
import com.seraphina.ninecun_lingtai.client.player.ClientMeditationManager;
import com.seraphina.ninecun_lingtai.client.player.MeditationVisualState;
import com.seraphina.ninecun_lingtai.client.render.SpiritualEnergyShader;
import com.seraphina.ninecun_lingtai.utility.SpiritualEnergyUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.UUID;

public final class SpiritualEnergyRenderUtility {
    private static final double MAXIMUM_RENDER_DISTANCE_SQUARED = 56.0D * 56.0D;
    private static final long HASH_MULTIPLIER = 0x9E3779B97F4A7C15L;
    private static final float TWO_PI = (float) (Math.PI * 2.0D);
    // Higher realms see more, farther, and faster energy; each axis is hard-capped for performance.
    private static final float REALM_PARTICLE_COUNT_GROWTH = 0.75F;
    private static final int MAXIMUM_PARTICLE_COUNT = 80;
    private static final float REALM_RADIUS_GROWTH = 0.50F;
    private static final double MAXIMUM_DISTANT_RADIUS = 40.0D;
    private static final float REALM_SPEED_GROWTH = 0.18F;
    private static final float MAXIMUM_REALM_SPEED_MULTIPLIER = 2.6F;

    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ShaderInstance shaderInstance = SpiritualEnergyShader.getSpiritualEnergyShader();
        if (minecraft.level == null || shaderInstance == null || ClientMeditationManager.getVisualStates().isEmpty()) {
            return;
        }

        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        Quaternionf cameraRotation = new Quaternionf(camera.rotation());
        Vector3f cameraRight = new Vector3f(1.0F, 0.0F, 0.0F).rotate(cameraRotation);
        Vector3f cameraUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(cameraRotation);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> shaderInstance);
        shaderInstance.safeGetUniform("Time").set(
                (minecraft.level.getGameTime() + event.getPartialTick()) / 20.0F
        );

        for (Map.Entry<UUID, MeditationVisualState> entry : ClientMeditationManager.getVisualStates().entrySet()) {
            Player player = minecraft.level.getPlayerByUUID(entry.getKey());
            if (player == null || player.distanceToSqr(cameraPosition) > MAXIMUM_RENDER_DISTANCE_SQUARED) {
                continue;
            }
            renderPlayerEnergy(
                    poseStack,
                    player,
                    entry.getValue(),
                    minecraft.level.getGameTime() + event.getPartialTick(),
                    event.getPartialTick(),
                    cameraRight,
                    cameraUp
            );
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderPlayerEnergy(
            PoseStack poseStack,
            Player player,
            MeditationVisualState visualState,
            float currentTick,
            float partialTick,
            Vector3f cameraRight,
            Vector3f cameraUp
    ) {
        FiveElementValues environmentValues = visualState.environmentValues();
        float visibility = smoothStep(visualState.visibility());
        if (visibility <= 0.0F) {
            return;
        }
        float maximumDensity = Math.max(0.0001F, environmentValues.maximum());
        ElementType dominantElement = environmentValues.dominantElement();
        int realmLevel = Math.max(0, visualState.realmLevel());
        float realmParticleMultiplier = 1.0F + realmLevel * REALM_PARTICLE_COUNT_GROWTH;
        float realmRadiusMultiplier = 1.0F + realmLevel * REALM_RADIUS_GROWTH;
        float realmSpeedMultiplier = Math.min(
                1.0F + realmLevel * REALM_SPEED_GROWTH,
                MAXIMUM_REALM_SPEED_MULTIPLIER
        );
        Vec3 playerPosition = new Vec3(
                Mth.lerp(partialTick, player.xOld, player.getX()),
                Mth.lerp(partialTick, player.yOld, player.getY()),
                Mth.lerp(partialTick, player.zOld, player.getZ())
        );
        Vec3 absorptionPosition = playerPosition.add(0.0D, player.getBbHeight() * 0.56D, 0.0D);

        for (ElementType element : ElementType.FIVE_ELEMENTS) {
            float density = environmentValues.get(element);
            if (density <= 0.0F) {
                continue;
            }
            float relativeDensity = density / maximumDensity;
            float movementAffinity = visualState.movementAffinities().get(element);
            int particleCount = Mth.clamp(
                    Math.round((4.0F + density * 4.0F + relativeDensity * 3.0F
                            + (element == dominantElement ? 4.0F : 0.0F)) * realmParticleMultiplier),
                    4,
                    MAXIMUM_PARTICLE_COUNT
            );
            renderElementParticles(
                    poseStack.last().pose(),
                    player.getUUID(),
                    element,
                    particleCount,
                    relativeDensity,
                    movementAffinity,
                    element == dominantElement,
                    absorptionPosition,
                    currentTick,
                    visibility,
                    realmRadiusMultiplier,
                    realmSpeedMultiplier,
                    cameraRight,
                    cameraUp
            );
        }
    }

    private static void renderElementParticles(
            Matrix4f matrix,
            UUID playerId,
            ElementType element,
            int particleCount,
            float relativeDensity,
            float movementAffinity,
            boolean dominant,
            Vec3 absorptionPosition,
            float currentTick,
            float visibility,
            float realmRadiusMultiplier,
            float realmSpeedMultiplier,
            Vector3f cameraRight,
            Vector3f cameraUp
    ) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
            long baseSeed = playerId.getMostSignificantBits()
                    ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 17)
                    ^ element.ordinal() * HASH_MULTIPLIER
                    ^ particleIndex * 0xD1B54A32D192ED03L;
            boolean attracted = movementAffinity > 0.5F;
            boolean absorbed = movementAffinity >= SpiritualEnergyUtility.ABSORPTION_AFFINITY;
            float speedScale = (attracted ? 1.08F + movementAffinity * 0.38F : 0.86F)
                    * (attracted ? realmSpeedMultiplier : 1.0F);
            float duration = getBaseDuration(element) / speedScale;
            float phaseOffset = randomValue(baseSeed) * duration;
            double cyclePosition = (currentTick + phaseOffset) / duration;
            long cycleIndex = Mth.floor(cyclePosition);
            float progress = (float) (cyclePosition - Math.floor(cyclePosition));
            long cycleSeed = baseSeed ^ cycleIndex * HASH_MULTIPLIER;

            Vec3 distantPosition = createDistantPosition(absorptionPosition, cycleSeed, realmRadiusMultiplier);
            Vec3 nearbyPosition = absorbed
                    ? absorptionPosition
                    : createNearbyPosition(absorptionPosition, cycleSeed);
            Vec3 particlePosition = interpolateEnergyPath(
                    attracted ? distantPosition : nearbyPosition,
                    attracted ? nearbyPosition : distantPosition,
                    progress,
                    cycleSeed,
                    attracted
            );
            float fadeIn = Mth.clamp(progress / 0.09F, 0.0F, 1.0F);
            float fadeOut = 1.0F - Mth.clamp((progress - 0.965F) / 0.035F, 0.0F, 1.0F);
            float absorptionPulse = absorbed
                    ? Mth.clamp((progress - 0.78F) / 0.22F, 0.0F, 1.0F)
                    : 0.0F;
            float size = (0.105F + randomValue(cycleSeed ^ 0x369DEA0F31A53F85L) * 0.045F)
                    * (0.82F + relativeDensity * 0.32F)
                    * (dominant ? 1.16F : 1.0F)
                    * Mth.lerp(absorptionPulse, 1.0F, 0.42F);
            float alpha = (0.42F + relativeDensity * 0.36F)
                    * (dominant ? 1.18F : 1.0F)
                    * (attracted ? 1.04F : 0.82F)
                    * fadeIn
                    * fadeOut
                    * visibility;
            int color = SpiritualRootUtility.getElementColor(element);
            appendBillboard(
                    bufferBuilder,
                    matrix,
                    particlePosition,
                    size,
                    color,
                    Mth.clamp(alpha, 0.0F, 1.0F),
                    cameraRight,
                    cameraUp
            );
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static Vec3 createDistantPosition(Vec3 absorptionPosition, long seed, float realmRadiusMultiplier) {
        float angle = randomValue(seed ^ 0x632BE59BD9B4E019L) * TWO_PI;
        double radius = Math.min(
                Mth.lerp(randomValue(seed ^ 0xC6BC279692B5CC83L), 3.5F, 10.0F) * realmRadiusMultiplier,
                MAXIMUM_DISTANT_RADIUS
        );
        double height = Mth.lerp(randomValue(seed ^ 0xDB4F0B9175AE2165L), -0.35F, 5.2F);
        return absorptionPosition.add(
                Mth.cos(angle) * radius,
                height,
                Mth.sin(angle) * radius
        );
    }

    private static Vec3 createNearbyPosition(Vec3 absorptionPosition, long seed) {
        float angle = randomValue(seed ^ 0xF1357AEA2E62A9C5L) * TWO_PI;
        double radius = Mth.lerp(randomValue(seed ^ 0xC13FA9A902A6328FL), 0.72F, 1.24F);
        double height = Mth.lerp(randomValue(seed ^ 0x91E10DA5C79E7B1DL), -0.46F, 0.72F);
        return absorptionPosition.add(
                Mth.cos(angle) * radius,
                height,
                Mth.sin(angle) * radius
        );
    }

    private static Vec3 interpolateEnergyPath(
            Vec3 startPosition,
            Vec3 endPosition,
            float progress,
            long seed,
            boolean approaching
    ) {
        float easedProgress = approaching
                ? progress * progress
                : 1.0F - (1.0F - progress) * (1.0F - progress);
        Vec3 directPosition = startPosition.lerp(endPosition, easedProgress);
        Vec3 radialDirection = startPosition.subtract(endPosition).multiply(1.0D, 0.0D, 1.0D);
        if (radialDirection.lengthSqr() < 0.0001D) {
            radialDirection = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            radialDirection = radialDirection.normalize();
        }
        Vec3 tangentDirection = new Vec3(-radialDirection.z, 0.0D, radialDirection.x);
        float spiralPhase = randomValue(seed ^ 0xA24BAED4963EE407L) * TWO_PI;
        double pathEnvelope = Mth.sin(progress * Mth.PI);
        double spiralOffset = Mth.sin(progress * TWO_PI * 2.25F + spiralPhase)
                * pathEnvelope
                * 0.72D;
        double verticalOffset = Mth.cos(progress * TWO_PI * 1.65F + spiralPhase)
                * pathEnvelope
                * 0.28D;
        return directPosition.add(tangentDirection.scale(spiralOffset)).add(0.0D, verticalOffset, 0.0D);
    }

    private static void appendBillboard(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 center,
            float size,
            int color,
            float alpha,
            Vector3f cameraRight,
            Vector3f cameraUp
    ) {
        float rightX = cameraRight.x() * size;
        float rightY = cameraRight.y() * size;
        float rightZ = cameraRight.z() * size;
        float upX = cameraUp.x() * size;
        float upY = cameraUp.y() * size;
        float upZ = cameraUp.z() * size;
        int alphaChannel = Mth.clamp(Math.round(alpha * 255.0F), 0, 255);
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        float centerX = (float) center.x;
        float centerY = (float) center.y;
        float centerZ = (float) center.z;

        vertex(bufferBuilder, matrix, centerX - rightX - upX, centerY - rightY - upY,
                centerZ - rightZ - upZ, red, green, blue, alphaChannel, 0.0F, 1.0F);
        vertex(bufferBuilder, matrix, centerX + rightX - upX, centerY + rightY - upY,
                centerZ + rightZ - upZ, red, green, blue, alphaChannel, 1.0F, 1.0F);
        vertex(bufferBuilder, matrix, centerX + rightX + upX, centerY + rightY + upY,
                centerZ + rightZ + upZ, red, green, blue, alphaChannel, 1.0F, 0.0F);
        vertex(bufferBuilder, matrix, centerX - rightX + upX, centerY - rightY + upY,
                centerZ - rightZ + upZ, red, green, blue, alphaChannel, 0.0F, 0.0F);
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            int red,
            int green,
            int blue,
            int alpha,
            float textureU,
            float textureV
    ) {
        bufferBuilder.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(textureU, textureV)
                .endVertex();
    }

    private static float getBaseDuration(ElementType element) {
        return switch (element) {
            case METAL -> 58.0F;
            case WOOD -> 78.0F;
            case WATER -> 70.0F;
            case FIRE -> 50.0F;
            case EARTH -> 88.0F;
            case YIN_YANG, THUNDER, CHAOS -> 72.0F;
        };
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float randomValue(long seed) {
        long value = seed;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 40) / (float) (1L << 24);
    }
}
