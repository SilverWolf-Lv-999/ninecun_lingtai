package net.seraphina.ninecun_lingtai.client.render.cosmic;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.TheEndPortalRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class MegaRenderType extends RenderType {
    public static final RenderStateShard.TransparencyStateShard COSMIC_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "cosmic_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFuncSeparate(
                                GlStateManager.SourceFactor.SRC_COLOR,
                                GlStateManager.DestFactor.ONE,
                                GlStateManager.SourceFactor.SRC_ALPHA,
                                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
                        );
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );
    public static final RenderStateShard.TexturingStateShard MEGA_GLINT_TEXTURING =
            new RenderStateShard.TexturingStateShard(
                    "glint_texturing",
                    () -> setupGlintTexturing(8.0F),
                    RenderSystem::resetTextureMatrix
            );
    public static final VertexFormat ANOTHER_BLOCK = new VertexFormat(
            ImmutableMap.<String, VertexFormatElement>builder()
                    .put("Position", DefaultVertexFormat.ELEMENT_POSITION)
                    .put("Color", DefaultVertexFormat.ELEMENT_COLOR)
                    .put("UV0", DefaultVertexFormat.ELEMENT_UV0)
                    .put("UV2", DefaultVertexFormat.ELEMENT_UV2)
                    .put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
                    .put("Padding", DefaultVertexFormat.ELEMENT_PADDING)
                    .build()
    );
    public static final RenderStateShard.ShaderStateShard COSMIC_STATE =
            new RenderStateShard.ShaderStateShard(() -> CosmicItemShaders.cosmicShader);
    private static int id = 1;

    public MegaRenderType(
            String name,
            VertexFormat format,
            VertexFormat.Mode mode,
            int bufferSize,
            boolean affectsCrumbling,
            boolean sortOnUpload,
            Runnable setupState,
            Runnable clearState
    ) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    private static void setupGlintTexturing(float scale) {
        long time = (long) ((double) Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0D);
        float x = (float) (time % 110000L) / 110000.0F;
        float y = (float) (time % 30000L) / 30000.0F;
        Matrix4f matrix = new Matrix4f().translation(-x, y, 0.0F);
        matrix.rotateZ(0.17453292F).scale(scale);
        RenderSystem.setTextureMatrix(matrix);
    }

    public static RenderType createSphereRenderType2(ResourceLocation texture) {
        id++;
        return create(
                "cosmic_sphere_2_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX,
                VertexFormat.Mode.TRIANGLE_STRIP,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                        .setShaderState(RenderStateShard.RENDERTYPE_BEACON_BEAM_SHADER)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setOutputState(PARTICLES_TARGET)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .createCompositeState(true)
        );
    }

    public static RenderType createCosmicSphereRenderType(boolean blur) {
        id++;
        ResourceLocation cosmicTexture = CosmicTextures.rl(0);
        return create(
                "cosmic_sphere_" + id,
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.TRIANGLE_STRIP,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEndPortalShader))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setTextureState(MultiTextureStateShard.builder()
                                .add(cosmicTexture, blur, false)
                                .add(cosmicTexture, blur, false)
                                .build())
                        .createCompositeState(true)
        );
    }

    public static RenderType createSphereRenderType(ResourceLocation texture) {
        id++;
        return create(
                "cosmic_sphere_lightmap_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .createCompositeState(true)
        );
    }

    public static RenderType createSphereRenderTypeWithLines(ResourceLocation texture) {
        id++;
        return create(
                "cosmic_sphere_lines_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.DEBUG_LINE_STRIP,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .createCompositeState(true)
        );
    }

    public static RenderType createRingRenderType(ResourceLocation texture) {
        id++;
        return create(
                "cosmic_ring_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getPositionColorTexLightmapShader))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(NO_LIGHTMAP)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .createCompositeState(true)
        );
    }

    public static RenderType END_PORTAL() {
        id++;
        return create(
                "cosmic_end_portal_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEndPortalShader))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setTextureState(MultiTextureStateShard.builder()
                                .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                                .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                                .build())
                        .createCompositeState(true)
        );
    }

    public static RenderType createLightingEndPortalSphere() {
        id++;
        return create(
                "cosmic_lighting_end_portal_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.TRIANGLE_STRIP,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEndPortalShader))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setTextureState(MultiTextureStateShard.builder()
                                .add(TheEndPortalRenderer.END_SKY_LOCATION, false, false)
                                .add(TheEndPortalRenderer.END_PORTAL_LOCATION, false, false)
                                .build())
                        .createCompositeState(true)
        );
    }

    public static RenderType END_PORTAL(ResourceLocation texture) {
        id++;
        return create(
                "cosmic_font_end_portal_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEndPortalShader))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setTextureState(MultiTextureStateShard.builder()
                                .add(texture, false, false)
                                .add(texture, false, false)
                                .build())
                        .createCompositeState(true)
        );
    }

    public static RenderType END_COSMIC_ITEM(ResourceLocation texture) {
        id++;
        return create(
                "cosmic_item_" + id,
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                2097152,
                true,
                false,
                CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEndPortalShader))
                        .setDepthTestState(EQUAL_DEPTH_TEST)
                        .setLightmapState(LIGHTMAP)
                        .setTransparencyState(COSMIC_TRANSPARENCY)
                        .setTextureState(MultiTextureStateShard.builder()
                                .add(texture, false, false)
                                .add(texture, false, false)
                                .build())
                        .createCompositeState(true)
        );
    }

    public static RenderType END_PORTAL_TRANSLUCENT(ResourceLocation texture) {
        id++;
        return create(
                "cosmic_translucent_end_portal_" + id,
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEndPortalShader))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setTextureState(MultiTextureStateShard.builder()
                                .add(texture, false, false)
                                .add(texture, false, false)
                                .build())
                        .createCompositeState(true)
        );
    }

    public static RenderType glow(ResourceLocation texture) {
        return create(
                "cosmic_glow",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .setOutputState(PARTICLES_TARGET)
                        .createCompositeState(true)
        );
    }

    public static RenderType mask(ResourceLocation texture) {
        return create(
                "cosmic_mask",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                false,
                CompositeState.builder()
                        .setShaderState(COSMIC_STATE)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setLightmapState(LIGHTMAP)
                        .setWriteMaskState(COLOR_WRITE)
                        .setCullState(NO_CULL)
                        .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                        .createCompositeState(true)
        );
    }

    public static RenderType LIGHTING_ITEM_ENTITY() {
        return create(
                "cosmic_lighting_item_entity",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                        .setWriteMaskState(COLOR_WRITE)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setOutputState(PARTICLES_TARGET)
                        .createCompositeState(false)
        );
    }
}
