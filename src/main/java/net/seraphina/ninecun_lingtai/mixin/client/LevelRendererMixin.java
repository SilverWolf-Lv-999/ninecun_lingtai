package net.seraphina.ninecun_lingtai.mixin.client;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.seraphina.ninecun_lingtai.client.player.ClientShrinkEarthManager;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.pre.PreProcessingShaders;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.mixin.api.Type;
import net.seraphina.ninecun_lingtai.mixin.api.MixinDisabled;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(LevelRenderer.class)
@MixinDisabled(type = Type.MOD_LOAD, modId = "embeddium")
public abstract class LevelRendererMixin {
    @Unique
    private static final double NINECUN_LINGTAI_SECTION_SIZE = 16.0D;
    @Unique
    private static final double NINECUN_LINGTAI_SECTION_CENTER_OFFSET = NINECUN_LINGTAI_SECTION_SIZE / 2.0D;

    @Unique
    private boolean ninecunLingtai$shrinkEarthChunkCompressionActive;
    @Unique
    private Vec3 ninecunLingtai$shrinkEarthCompressionCenter;
    @Unique
    private float ninecunLingtai$shrinkEarthCurrentChunkScale = 1.0F;
    @Unique
    private double ninecunLingtai$shrinkEarthCurrentChunkPivotX;
    @Unique
    private double ninecunLingtai$shrinkEarthCurrentChunkPivotZ;

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void ninecunLingtai$renderPreEffects(
            PoseStack poseStack,
            float partialTick,
            long finishTimeNano,
            boolean renderBlockOutline,
            net.minecraft.client.Camera camera,
            net.minecraft.client.renderer.GameRenderer gameRenderer,
            net.minecraft.client.renderer.LightTexture lightTexture,
            Matrix4f projectionMatrix,
            CallbackInfo callbackInfo
    ) {
        PreProcessingShaders.INSTANCE.renderShaders(partialTick);
    }

    @Inject(method = "renderChunkLayer", at = @At("HEAD"))
    private void ninecunLingtai$beginShrinkEarthChunkCompression(
            RenderType renderType,
            PoseStack poseStack,
            double camX,
            double camY,
            double camZ,
            Matrix4f projectionMatrix,
            CallbackInfo callbackInfo
    ) {
        Optional<Vec3> compressionCenter = ClientShrinkEarthManager.getLocalCompressionCenter();
        this.ninecunLingtai$shrinkEarthChunkCompressionActive = compressionCenter.isPresent()
                && ClientShrinkEarthManager.getLocalWorldRenderScale() < 0.999F;
        this.ninecunLingtai$shrinkEarthCompressionCenter = compressionCenter.orElse(null);
        this.ninecunLingtai$shrinkEarthCurrentChunkScale = 1.0F;
        this.ninecunLingtai$shrinkEarthCurrentChunkPivotX = 0.0D;
        this.ninecunLingtai$shrinkEarthCurrentChunkPivotZ = 0.0D;
    }

    @Redirect(
            method = "renderChunkLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/shaders/Uniform;set(FFF)V",
                    ordinal = 0
            )
    )
    private void ninecunLingtai$setShrinkEarthChunkOffset(
            Uniform uniform,
            float offsetX,
            float offsetY,
            float offsetZ,
            RenderType renderType,
            PoseStack poseStack,
            double camX,
            double camY,
            double camZ,
            Matrix4f projectionMatrix
    ) {
        if (!this.ninecunLingtai$shrinkEarthChunkCompressionActive) {
            this.ninecunLingtai$shrinkEarthCurrentChunkScale = 1.0F;
            uniform.set(offsetX, offsetY, offsetZ);
            return;
        }
        double sectionOriginX = camX + offsetX;
        double sectionOriginZ = camZ + offsetZ;
        double horizontalDistance = this.ninecunLingtai$getHorizontalDistanceToSection(sectionOriginX, sectionOriginZ);
        float chunkScale = ClientShrinkEarthManager.getLocalWorldRenderScale(horizontalDistance);
        this.ninecunLingtai$shrinkEarthCurrentChunkScale = chunkScale;
        if (chunkScale >= 0.999F) {
            uniform.set(offsetX, offsetY, offsetZ);
            return;
        }
        double sectionCenterOffsetX = offsetX + NINECUN_LINGTAI_SECTION_CENTER_OFFSET;
        double sectionCenterOffsetZ = offsetZ + NINECUN_LINGTAI_SECTION_CENTER_OFFSET;
        double compressionCenterOffsetX = this.ninecunLingtai$shrinkEarthCompressionCenter.x - camX;
        double compressionCenterOffsetZ = this.ninecunLingtai$shrinkEarthCompressionCenter.z - camZ;
        double compressedSectionCenterOffsetX = compressionCenterOffsetX
                + (sectionCenterOffsetX - compressionCenterOffsetX) * chunkScale;
        double compressedSectionCenterOffsetZ = compressionCenterOffsetZ
                + (sectionCenterOffsetZ - compressionCenterOffsetZ) * chunkScale;
        this.ninecunLingtai$shrinkEarthCurrentChunkPivotX = compressedSectionCenterOffsetX;
        this.ninecunLingtai$shrinkEarthCurrentChunkPivotZ = compressedSectionCenterOffsetZ;
        uniform.set(
                (float) (compressedSectionCenterOffsetX - NINECUN_LINGTAI_SECTION_CENTER_OFFSET),
                offsetY,
                (float) (compressedSectionCenterOffsetZ - NINECUN_LINGTAI_SECTION_CENTER_OFFSET)
        );
    }

    @Redirect(
            method = "renderChunkLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;draw()V"
            )
    )
    private void ninecunLingtai$drawShrinkEarthChunk(
            VertexBuffer vertexBuffer,
            RenderType renderType,
            PoseStack poseStack,
            double camX,
            double camY,
            double camZ,
            Matrix4f projectionMatrix
    ) {
        if (this.ninecunLingtai$shrinkEarthChunkCompressionActive) {
            this.ninecunLingtai$uploadShrinkEarthModelView(poseStack);
        }
        vertexBuffer.draw();
        if (this.ninecunLingtai$shrinkEarthChunkCompressionActive) {
            this.ninecunLingtai$uploadBaseModelView(poseStack);
        }
    }

    @Inject(method = "renderChunkLayer", at = @At("RETURN"))
    private void ninecunLingtai$endShrinkEarthChunkCompression(
            RenderType renderType,
            PoseStack poseStack,
            double camX,
            double camY,
            double camZ,
            Matrix4f projectionMatrix,
            CallbackInfo callbackInfo
    ) {
        this.ninecunLingtai$shrinkEarthChunkCompressionActive = false;
        this.ninecunLingtai$shrinkEarthCompressionCenter = null;
        this.ninecunLingtai$shrinkEarthCurrentChunkScale = 1.0F;
        this.ninecunLingtai$shrinkEarthCurrentChunkPivotX = 0.0D;
        this.ninecunLingtai$shrinkEarthCurrentChunkPivotZ = 0.0D;
    }

    @Unique
    private double ninecunLingtai$getHorizontalDistanceToSection(double sectionOriginX, double sectionOriginZ) {
        double centerX = this.ninecunLingtai$shrinkEarthCompressionCenter.x;
        double centerZ = this.ninecunLingtai$shrinkEarthCompressionCenter.z;
        double nearestX = Math.max(sectionOriginX, Math.min(centerX, sectionOriginX + NINECUN_LINGTAI_SECTION_SIZE));
        double nearestZ = Math.max(sectionOriginZ, Math.min(centerZ, sectionOriginZ + NINECUN_LINGTAI_SECTION_SIZE));
        double distanceX = nearestX - centerX;
        double distanceZ = nearestZ - centerZ;
        return Math.sqrt(distanceX * distanceX + distanceZ * distanceZ);
    }

    @Unique
    private void ninecunLingtai$uploadShrinkEarthModelView(PoseStack poseStack) {
        Matrix4f modelViewMatrix = new Matrix4f(poseStack.last().pose());
        if (this.ninecunLingtai$shrinkEarthCurrentChunkScale < 0.999F) {
            modelViewMatrix
                    .translate(
                            (float) this.ninecunLingtai$shrinkEarthCurrentChunkPivotX,
                            0.0F,
                            (float) this.ninecunLingtai$shrinkEarthCurrentChunkPivotZ
                    )
                    .scale(this.ninecunLingtai$shrinkEarthCurrentChunkScale, 1.0F, this.ninecunLingtai$shrinkEarthCurrentChunkScale)
                    .translate(
                            (float) -this.ninecunLingtai$shrinkEarthCurrentChunkPivotX,
                            0.0F,
                            (float) -this.ninecunLingtai$shrinkEarthCurrentChunkPivotZ
                    );
        }
        this.ninecunLingtai$uploadModelViewMatrix(modelViewMatrix);
    }

    @Unique
    private void ninecunLingtai$uploadBaseModelView(PoseStack poseStack) {
        this.ninecunLingtai$uploadModelViewMatrix(poseStack.last().pose());
    }

    @Unique
    private void ninecunLingtai$uploadModelViewMatrix(Matrix4f modelViewMatrix) {
        ShaderInstance shaderInstance = RenderSystem.getShader();
        if (shaderInstance == null || shaderInstance.MODEL_VIEW_MATRIX == null) {
            return;
        }
        shaderInstance.MODEL_VIEW_MATRIX.set(modelViewMatrix);
        shaderInstance.MODEL_VIEW_MATRIX.upload();
    }
}
