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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector2d;

import java.util.Objects;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class VanillaCosmicShaders {
    public static final float[] COSMIC_UVS = new float[40];
    public static CosmicShaderInstance cosmicShader;
    public static MUniform cosmicTime;
    public static MUniform cosmicYaw;
    public static MUniform cosmicPitch;
    public static MUniform cosmicExternalScale;
    public static MUniform cosmicOpacity;
    public static MUniform cosmicUVs;
    public static RenderType COSMIC_RENDER_TYPE = create();

    public VanillaCosmicShaders() {
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        event.registerShader(CosmicShaderInstance.create(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "cosmic_2"), DefaultVertexFormat.BLOCK), (e) -> {
            cosmicShader = (CosmicShaderInstance) e;
            cosmicTime = Objects.requireNonNull(cosmicShader.getUniform("time"));
            cosmicYaw = Objects.requireNonNull(cosmicShader.getUniform("yaw")); 
            cosmicPitch = Objects.requireNonNull(cosmicShader.getUniform("pitch"));
            cosmicExternalScale = Objects.requireNonNull(cosmicShader.getUniform("externalScale"));
            cosmicOpacity = Objects.requireNonNull(cosmicShader.getUniform("opacity"));
            cosmicUVs = Objects.requireNonNull(cosmicShader.getUniform("cosmicuvs"));
        });


    }

    public static RenderType create() {
        return RenderType.create("fantasy_ending:cosmic_2",
                DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 2097152, true, false,
                RenderType.CompositeState.builder()
                        .setShaderState(new RenderStateShard.ShaderStateShard(() -> cosmicShader))
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
        float scale = 1.5F * ClientConfig.COSMIC_SCALE.get().floatValue();
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
        VanillaCosmicShaders.cosmicTime.set(Util.getMillis() / (100F / ClientConfig.COSMIC_SPEED_MULTIPLIER.get().floatValue()));
        VanillaCosmicShaders.cosmicYaw.set(yaw);
        VanillaCosmicShaders.cosmicPitch.set(pitch);
        VanillaCosmicShaders.cosmicExternalScale.set(scale);
        VanillaCosmicShaders.cosmicOpacity.set(1.0F);
        for (int i = 0; i < 10; i++) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(CosmicTextures.rl(i));
            COSMIC_UVS[i * 4] = sprite.getU0();
            COSMIC_UVS[i * 4 + 1] = sprite.getV0();
            COSMIC_UVS[i * 4 + 2] = sprite.getU1();
            COSMIC_UVS[i * 4 + 3] = sprite.getV1();
        }
        if (cosmicUVs != null)
            cosmicUVs.glUniformF(false, COSMIC_UVS);
    }
}
