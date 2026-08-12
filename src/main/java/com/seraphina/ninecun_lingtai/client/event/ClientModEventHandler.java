package com.seraphina.ninecun_lingtai.client.event;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.animation.LightFlightAnimationManager;
import com.seraphina.ninecun_lingtai.client.animation.MeditationAnimationManager;
import com.seraphina.ninecun_lingtai.client.config.ClientConfigScreenRegistrar;
import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.client.inventorytab.CultivationInfoTabRegistrar;
import com.seraphina.ninecun_lingtai.client.inventorytab.AlchemyRecipeLibraryTabRegistrar;
import com.seraphina.ninecun_lingtai.client.inventorytab.PlayerValueControlTabRegistrar;
import com.seraphina.ninecun_lingtai.client.inventorytab.TechniqueLibraryTabRegistrar;
import com.seraphina.ninecun_lingtai.client.model.BodyDefenseSpellModel;
import com.seraphina.ninecun_lingtai.client.render.AptitudeFormationShader;
import com.seraphina.ninecun_lingtai.client.render.AlchemyFurnaceBlockRenderer;
import com.seraphina.ninecun_lingtai.client.screen.AlchemyFurnaceScreen;
import com.seraphina.ninecun_lingtai.client.screen.StorageRingScreen;
import com.seraphina.ninecun_lingtai.client.render.DefenseBarShader;
import com.seraphina.ninecun_lingtai.client.render.DefenseSpellGuardianRenderer;
import com.seraphina.ninecun_lingtai.client.render.ElementFlowShader;
import com.seraphina.ninecun_lingtai.client.render.NotificationScrollShader;
import com.seraphina.ninecun_lingtai.client.render.SoulConstellationShader;
import com.seraphina.ninecun_lingtai.client.render.SoulRenderer;
import com.seraphina.ninecun_lingtai.client.render.SpiritualEnergyShader;
import com.seraphina.ninecun_lingtai.client.render.TechniqueCloudShader;
import com.seraphina.ninecun_lingtai.client.render.TribulationCloudShader;
import com.seraphina.ninecun_lingtai.client.render.PushingMountainsCollisionRenderer;
import com.seraphina.ninecun_lingtai.client.render.PushingMountainsEntityRenderer;
import com.seraphina.ninecun_lingtai.client.render.layer.BodyDefenseSpellLayer;
import com.seraphina.ninecun_lingtai.common.item.ColorItem;
import com.seraphina.ninecun_lingtai.common.entity.tribulation.ThunderTribulationEntity;
import com.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import com.seraphina.ninecun_lingtai.common.registry.ModBlockEntityTypes;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import com.seraphina.ninecun_lingtai.common.registry.ModMenuTypes;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LightningBoltRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEventHandler {
    private static final ModelLayerLocation BODY_DEFENSE_SPELL_MODEL = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "body_defense_spell"),
            "default"
    );
    private static final ModelLayerLocation SLIM_BODY_DEFENSE_SPELL_MODEL = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(ModMain.MOD_ID, "body_defense_spell"),
            "slim"
    );
    private static final ResourceLocation ELEMENT_FLOW_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "element_flow"
    );
    private static final ResourceLocation APTITUDE_FORMATION_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "aptitude_formation"
    );
    private static final ResourceLocation SOUL_CONSTELLATION_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "soul_constellation"
    );
    private static final ResourceLocation SPIRITUAL_ENERGY_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "spiritual_energy"
    );
    private static final ResourceLocation TECHNIQUE_CLOUD_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "technique_cloud"
    );
    private static final ResourceLocation DEFENSE_BAR_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "defense_bar"
    );
    private static final ResourceLocation NOTIFICATION_SCROLL_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "notification_scroll"
    );
    private static final ResourceLocation TRIBULATION_CLOUD_SHADER = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "tribulation_cloud"
    );

    private ClientModEventHandler() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ClientConfigScreenRegistrar.register();
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.ALCHEMY_FURNACE.get(), AlchemyFurnaceScreen::new);
            MenuScreens.register(ModMenuTypes.STORAGE_RING.get(), StorageRingScreen::new);
            MeditationAnimationManager.register();
            LightFlightAnimationManager.register();
            CultivationInfoTabRegistrar.register();
            TechniqueLibraryTabRegistrar.register();
            AlchemyRecipeLibraryTabRegistrar.register();
            PlayerValueControlTabRegistrar.register();
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.MEDITATION);
        event.register(ModKeyMappings.PRESSURE);
        event.register(ModKeyMappings.SPELL_SELECTION);
        event.register(ModKeyMappings.CAST_SPELL);
        event.register(ModKeyMappings.PUSHING_MOUNTAINS);
        event.register(ModKeyMappings.STORAGE_RING_IMMERSION);
    }

    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        event.register(
                (itemStack, tintIndex) -> tintIndex == 0 && itemStack.getItem() instanceof ColorItem colorItem
                        ? colorItem.getColor()
                        : 0xFFFFFF,
                ModItems.getColorItems()
        );
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntityTypes.ALCHEMY_FURNACE.get(),
                context -> new AlchemyFurnaceBlockRenderer()
        );
        event.registerEntityRenderer(ModEntityTypes.SOUL_ENTITY.get(), SoulRenderer::new);
        event.registerEntityRenderer(
                ModEntityTypes.DEFENSE_SPELL_GUARDIAN.get(),
                DefenseSpellGuardianRenderer::new
        );
        event.registerEntityRenderer(
                ModEntityTypes.THUNDER_TRIBULATION.get(),
                ClientModEventHandler::createVanillaLightningRenderer
        );
        event.registerEntityRenderer(
                ModEntityTypes.PUSHING_MOUNTAINS_ENTITY.get(),
                PushingMountainsEntityRenderer::new
        );
        event.registerEntityRenderer(
                ModEntityTypes.PUSHING_MOUNTAINS_COLLISION.get(),
                PushingMountainsCollisionRenderer::new
        );
    }

    @SuppressWarnings("unchecked")
    private static EntityRenderer<ThunderTribulationEntity> createVanillaLightningRenderer(
            EntityRendererProvider.Context context
    ) {
        return (EntityRenderer<ThunderTribulationEntity>) (EntityRenderer<?>) new LightningBoltRenderer(context);
    }

    @SubscribeEvent
    public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(
                BODY_DEFENSE_SPELL_MODEL,
                () -> BodyDefenseSpellModel.createLayerDefinition(false)
        );
        event.registerLayerDefinition(
                SLIM_BODY_DEFENSE_SPELL_MODEL,
                () -> BodyDefenseSpellModel.createLayerDefinition(true)
        );
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skinName : event.getSkins()) {
            PlayerRenderer playerRenderer = event.getPlayerSkin(skinName);
            if (playerRenderer == null) {
                continue;
            }
            boolean slim = "slim".equals(skinName);
            ModelLayerLocation modelLayer = slim
                    ? SLIM_BODY_DEFENSE_SPELL_MODEL
                    : BODY_DEFENSE_SPELL_MODEL;
            playerRenderer.addLayer(new BodyDefenseSpellLayer(
                    playerRenderer,
                    new BodyDefenseSpellModel(event.getEntityModels().bakeLayer(modelLayer), slim)
            ));
        }
    }

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        APTITUDE_FORMATION_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                AptitudeFormationShader::setAptitudeFormationShader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ELEMENT_FLOW_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                ElementFlowShader::setElementFlowShader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        SOUL_CONSTELLATION_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                SoulConstellationShader::setSoulConstellationShader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        SPIRITUAL_ENERGY_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                SpiritualEnergyShader::setSpiritualEnergyShader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        TECHNIQUE_CLOUD_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                TechniqueCloudShader::setTechniqueCloudShader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        DEFENSE_BAR_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                DefenseBarShader::setDefenseBarShader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        NOTIFICATION_SCROLL_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                NotificationScrollShader::setNotificationScrollShader
        );
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        TRIBULATION_CLOUD_SHADER,
                        DefaultVertexFormat.POSITION_COLOR_TEX
                ),
                TribulationCloudShader::setTribulationCloudShader
        );
    }
}
