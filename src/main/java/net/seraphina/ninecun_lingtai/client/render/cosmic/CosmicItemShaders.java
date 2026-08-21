package net.seraphina.ninecun_lingtai.client.render.cosmic;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.config.ClientConfig;
import net.seraphina.ninecun_lingtai.client.render.cosmic.core.MUniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2d;

import java.util.Objects;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class CosmicItemShaders {
    public static final float[] COSMIC_UVS = new float[40];
    public static CosmicShaderInstance cosmicShader;
    public static MUniform cosmicTime;
    public static MUniform cosmicYaw;
    public static MUniform cosmicPitch;
    public static MUniform cosmicExternalScale;
    public static MUniform cosmicOpacity;
    public static MUniform cosmicUVs;
    public static MUniform cosmicUVs1;
    public static MUniform cosmicUVs2;
    public static MUniform cosmicUVs3;
    public static MUniform cosmicUVs4;
    public static MUniform cosmicUVs5;
    public static MUniform cosmicUVs6;
    public static MUniform cosmicUVs7;
    public static MUniform cosmicUVs8;
    public static MUniform cosmicUVs9;
    public static RenderType COSMIC_RENDER_TYPE = create();

    public CosmicItemShaders() {
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        event.registerShader(CosmicShaderInstance.create(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "cosmic"), DefaultVertexFormat.BLOCK), (e) -> {
            cosmicShader = (CosmicShaderInstance) e;
            cosmicTime = Objects.requireNonNull(cosmicShader.getUniform("time"));
            cosmicYaw = Objects.requireNonNull(cosmicShader.getUniform("yaw"));
            cosmicPitch = Objects.requireNonNull(cosmicShader.getUniform("pitch"));
            cosmicExternalScale = Objects.requireNonNull(cosmicShader.getUniform("externalScale"));
            cosmicOpacity = Objects.requireNonNull(cosmicShader.getUniform("opacity"));
            cosmicUVs = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs"));
            cosmicUVs1 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs1"));
            cosmicUVs2 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs2"));
            cosmicUVs3 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs3"));
            cosmicUVs4 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs4"));
            cosmicUVs5 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs5"));
            cosmicUVs6 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs6"));
            cosmicUVs7 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs7"));
            cosmicUVs8 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs8"));
            cosmicUVs9 = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs9"));
        });


    }

    public static RenderType create() {
        return RenderType.create("fantasy_ending:cosmic",
                MegaRenderType.ANOTHER_BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
                RenderType.CompositeState.builder()
                        .setShaderState(MegaRenderType.COSMIC_STATE)
                        .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
                        .createCompositeState(true));
    }

    public static void updateShaderData(ItemDisplayContext transformType) {
        Minecraft mc = Minecraft.getInstance();
        float yaw = 0.0F;
        float pitch = 0.0F;
        float scale = 0.5F * ClientConfig.COSMIC_SCALE.get().floatValue();
        if (Minecraft.getInstance().screen == null && transformType != ItemDisplayContext.GUI) {
            if (mc.player != null) {
                Camera camera = mc.gameRenderer.getMainCamera();
                float yd = (float) camera.getPosition().y;
                float xzd = (float) new Vector2d(camera.getPosition().x, camera.getPosition().z).distance(0, 0);
                if (transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                    xzd = -xzd;
                pitch = -((float) ((double) (mc.player.getXRot() * 2.0F) * Math.PI / 360.0) - yd / 16F);
                yaw = -(float) ((double) (mc.player.getYRot() * 2.0F) * Math.PI / 360.0) - xzd / 16F;
            }
        } else {
            scale = 25.0F * ClientConfig.COSMIC_SCALE.get().floatValue();
        }
        CosmicItemShaders.cosmicTime.set(Util.getMillis() / (100F / ClientConfig.COSMIC_SPEED_MULTIPLIER.get().floatValue()));
        CosmicItemShaders.cosmicYaw.set(yaw);
        CosmicItemShaders.cosmicPitch.set(pitch);
        CosmicItemShaders.cosmicExternalScale.set(scale);
        CosmicItemShaders.cosmicOpacity.set(1.0F);
        CosmicItemShaders.cosmicShader.setCosmicIcon();
    }
}
