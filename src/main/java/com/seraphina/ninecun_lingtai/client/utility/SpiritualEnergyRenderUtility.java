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

    private SpiritualEnergyRenderUtility() {
    }

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
                    shaderInstance,
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
            ShaderInstance shaderInstance,
            Player player,
            MeditationVisualState visualState,
            float currentTick,
            float partialTick,
            Vector3f cameraRight,
            Vector3f cameraUp
    ) {
        FiveElementValues environmentValues = visualState.environmentValues();
        float maximumDensity = Math.max(0.0001F, environmentValues.maximum());
        ElementType dominantElement = environmentValues.dominantElement();
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
            float absorptionEfficiency = visualState.absorptionEfficiencies().get(element);
            int particleCount = Mth.clamp(
                    Math.round(4.0F + density * 4.0F + relativeDensity * 3.0F
                            + (element == dominantElement ? 4.0F : 0.0F)),
                    4,
                    18
            );
            shaderInstance.safeGetUniform("ElementStyle").set((float) element.ordinal());
            renderElementParticles(
                    poseStack.last().pose(),
                    player.getUUID(),
                    element,
                    particleCount,
                    density,
                    relativeDensity,
                    absorptionEfficiency,
                    element == dominantElement,
                    absorptionPosition,
                    currentTick,
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
            float density,
            float relativeDensity,
            float absorptionEfficiency,
            boolean dominant,
            Vec3 absorptionPosition,
            float currentTick,
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
            float speedScale = absorptionEfficiency > 0.0F
                    ? 0.78F + Math.min(absorptionEfficiency, 2.5F) * 0.58F
                    : 0.62F;
            float duration = getBaseDuration(element) / speedScale;
            float phaseOffset = randomValue(baseSeed) * duration;
            double cyclePosition = (currentTick + phaseOffset) / duration;
            long cycleIndex = Mth.floor(cyclePosition);
            float progress = (float) (cyclePosition - Math.floor(cyclePosition));
            long cycleSeed = baseSeed ^ cycleIndex * HASH_MULTIPLIER;

            Vec3 startPosition = createStartPosition(absorptionPosition, cycleSeed);
            Vec3 particlePosition = interpolateAbsorptionPath(
                    startPosition,
                    absorptionPosition,
                    progress,
                    cycleSeed
            );
            float fadeIn = Mth.clamp(progress / 0.09F, 0.0F, 1.0F);
            float fadeOut = 1.0F - Mth.clamp((progress - 0.965F) / 0.035F, 0.0F, 1.0F);
            float absorptionPulse = Mth.clamp((progress - 0.78F) / 0.22F, 0.0F, 1.0F);
            float size = getBaseSize(element)
                    * (0.82F + relativeDensity * 0.32F)
                    * (dominant ? 1.16F : 1.0F)
                    * Mth.lerp(absorptionPulse, 1.0F, 0.42F);
            float alpha = (0.42F + relativeDensity * 0.36F)
                    * (dominant ? 1.18F : 1.0F)
                    * (absorptionEfficiency > 0.0F ? 1.08F : 0.86F)
                    * fadeIn
                    * fadeOut;
            int color = SpiritualRootUtility.getElementColor(element);
            float rotationPhase = randomValue(cycleSeed ^ 0x94D049BB133111EBL) * TWO_PI;
            float rotation = element == ElementType.FIRE
                    ? createFireTilt(currentTick, cycleSeed, rotationPhase)
                    : rotationPhase + currentTick * getRotationSpeed(element);
            appendBillboard(
                    bufferBuilder,
                    matrix,
                    particlePosition,
                    size,
                    rotation,
                    color,
                    Mth.clamp(alpha, 0.0F, 1.0F),
                    cameraRight,
                    cameraUp
            );
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static Vec3 createStartPosition(Vec3 absorptionPosition, long seed) {
        float angle = randomValue(seed ^ 0x632BE59BD9B4E019L) * TWO_PI;
        double radius = Mth.lerp(randomValue(seed ^ 0xC6BC279692B5CC83L), 3.5F, 10.0F);
        double height = Mth.lerp(randomValue(seed ^ 0xDB4F0B9175AE2165L), -0.35F, 5.2F);
        return absorptionPosition.add(
                Mth.cos(angle) * radius,
                height,
                Mth.sin(angle) * radius
        );
    }

    private static Vec3 interpolateAbsorptionPath(
            Vec3 startPosition,
            Vec3 absorptionPosition,
            float progress,
            long seed
    ) {
        float easedProgress = progress * progress;
        Vec3 directPosition = startPosition.lerp(absorptionPosition, easedProgress);
        Vec3 radialDirection = startPosition.subtract(absorptionPosition).multiply(1.0D, 0.0D, 1.0D);
        if (radialDirection.lengthSqr() < 0.0001D) {
            radialDirection = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            radialDirection = radialDirection.normalize();
        }
        Vec3 tangentDirection = new Vec3(-radialDirection.z, 0.0D, radialDirection.x);
        float spiralPhase = randomValue(seed ^ 0xA24BAED4963EE407L) * TWO_PI;
        double remainingDistance = 1.0D - easedProgress;
        double spiralOffset = Mth.sin(progress * TWO_PI * 2.25F + spiralPhase)
                * remainingDistance
                * 0.72D;
        double verticalOffset = Mth.cos(progress * TWO_PI * 1.65F + spiralPhase)
                * remainingDistance
                * 0.28D;
        return directPosition.add(tangentDirection.scale(spiralOffset)).add(0.0D, verticalOffset, 0.0D);
    }

    private static void appendBillboard(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            Vec3 center,
            float size,
            float rotation,
            int color,
            float alpha,
            Vector3f cameraRight,
            Vector3f cameraUp
    ) {
        float cosine = Mth.cos(rotation);
        float sine = Mth.sin(rotation);
        float rightX = (cameraRight.x() * cosine + cameraUp.x() * sine) * size;
        float rightY = (cameraRight.y() * cosine + cameraUp.y() * sine) * size;
        float rightZ = (cameraRight.z() * cosine + cameraUp.z() * sine) * size;
        float upX = (cameraUp.x() * cosine - cameraRight.x() * sine) * size;
        float upY = (cameraUp.y() * cosine - cameraRight.y() * sine) * size;
        float upZ = (cameraUp.z() * cosine - cameraRight.z() * sine) * size;
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

    private static float getBaseSize(ElementType element) {
        return switch (element) {
            case METAL -> 0.17F;
            case WOOD -> 0.21F;
            case WATER -> 0.2F;
            case FIRE -> 0.22F;
            case EARTH -> 0.23F;
            case YIN_YANG, THUNDER, CHAOS -> 0.2F;
        };
    }

    private static float createFireTilt(float currentTick, long seed, float rotationPhase) {
        float baseTilt = (randomValue(seed ^ 0x369DEA0F31A53F85L) - 0.5F) * 0.24F;
        float flickerTilt = Mth.sin(currentTick * 0.14F + rotationPhase) * 0.075F;
        return baseTilt + flickerTilt;
    }

    private static float getRotationSpeed(ElementType element) {
        return switch (element) {
            case METAL -> 0.055F;
            case WOOD -> 0.018F;
            case WATER -> 0.012F;
            case FIRE -> 0.035F;
            case EARTH -> 0.009F;
            case YIN_YANG, THUNDER, CHAOS -> 0.02F;
        };
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
