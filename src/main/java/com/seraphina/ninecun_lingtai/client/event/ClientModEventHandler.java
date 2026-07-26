package com.seraphina.ninecun_lingtai.client.event;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.animation.MeditationAnimationManager;
import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.client.inventorytab.CultivationInfoTabRegistrar;
import com.seraphina.ninecun_lingtai.client.render.AptitudeFormationShader;
import com.seraphina.ninecun_lingtai.client.render.ElementFlowShader;
import com.seraphina.ninecun_lingtai.client.render.SoulConstellationShader;
import com.seraphina.ninecun_lingtai.client.render.SpiritualEnergyShader;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEventHandler {
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

    private ClientModEventHandler() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MeditationAnimationManager.register();
            CultivationInfoTabRegistrar.register();
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.MEDITATION);
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
    }
}
