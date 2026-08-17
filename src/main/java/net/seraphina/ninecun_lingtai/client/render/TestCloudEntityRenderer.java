package net.seraphina.ninecun_lingtai.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.seraphina.ninecun_lingtai.common.entity.test.TestCloudEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class TestCloudEntityRenderer extends EntityRenderer<TestCloudEntity> {
    private static final float HALF_WIDTH = 3.5F;
    private static final float HALF_HEIGHT = 2.0F;

    public TestCloudEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(
            TestCloudEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        ShaderInstance shaderInstance = TestCloudShader.getTestCloudShader();
        if (shaderInstance == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        poseStack.mulPose(minecraft.gameRenderer.getMainCamera().rotation());

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
        );
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        try {
            RenderSystem.setShader(() -> shaderInstance);
            shaderInstance.safeGetUniform("CloudTime").set(
                    (minecraft.level.getGameTime() + partialTick) / 20.0F
            );
            shaderInstance.safeGetUniform("CloudSeed").set(getCloudSeed(entity));
            drawCloudBillboard(poseStack.last().pose());
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TestCloudEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static void drawCloudBillboard(Matrix4f matrix) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        appendVertex(bufferBuilder, matrix, -HALF_WIDTH, -HALF_HEIGHT, 0.0F, 0.0F);
        appendVertex(bufferBuilder, matrix, -HALF_WIDTH, HALF_HEIGHT, 0.0F, 1.0F);
        appendVertex(bufferBuilder, matrix, HALF_WIDTH, HALF_HEIGHT, 1.0F, 1.0F);
        appendVertex(bufferBuilder, matrix, HALF_WIDTH, -HALF_HEIGHT, 1.0F, 0.0F);
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void appendVertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            float textureU,
            float textureV
    ) {
        bufferBuilder.vertex(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .uv(textureU, textureV)
                .endVertex();
    }

    private static float getCloudSeed(TestCloudEntity entity) {
        long seed = entity.getUUID().getMostSignificantBits() ^ entity.getUUID().getLeastSignificantBits();
        return ((seed & Long.MAX_VALUE) % 10000L) / 10000.0F;
    }
}
