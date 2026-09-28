package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.seraphina.ninecun_lingtai.common.entity.ability.SpaceRiftEntity;
import org.joml.Matrix4f;

public final class SpaceRiftEntityRenderer extends EntityRenderer<SpaceRiftEntity> {
    public SpaceRiftEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SpaceRiftEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ShaderInstance shader = SpaceRiftShader.getSpaceRiftShader();
        Minecraft minecraft = Minecraft.getInstance();
        if (shader == null || minecraft.level == null) {
            return;
        }
        Vec3 normal = entity.getRiftDirection().normalize();
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        if (Math.abs(normal.dot(up)) > 0.98D) {
            up = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 right = up.cross(normal).normalize();
        up = normal.cross(right).normalize();
        float progress = entity.getProgress(partialTick);
        shader.safeGetUniform("RiftTime").set((minecraft.level.getGameTime() + partialTick) / 20.0F);
        shader.safeGetUniform("RiftProgress").set(progress);
        poseStack.pushPose();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> shader);
            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
            Matrix4f matrix = poseStack.last().pose();
            add(builder, matrix, right, up, -1.65F, -3.0F, 0.0F, 1.0F);
            add(builder, matrix, right, up, 1.65F, -3.0F, 1.0F, 1.0F);
            add(builder, matrix, right, up, 1.65F, 3.0F, 1.0F, 0.0F);
            add(builder, matrix, right, up, -1.65F, 3.0F, 0.0F, 0.0F);
            BufferUploader.drawWithShader(builder.end());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            poseStack.popPose();
        }
    }

    private static void add(BufferBuilder builder, Matrix4f matrix, Vec3 right, Vec3 up, float x, float y, float u, float v) {
        Vec3 position = right.scale(x).add(up.scale(y));
        builder.vertex(matrix, (float) position.x, (float) position.y, (float) position.z)
                .color(1.0F, 1.0F, 1.0F, 1.0F).uv(u, v).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SpaceRiftEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
