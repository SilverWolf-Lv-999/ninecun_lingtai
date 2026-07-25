package com.seraphina.ninecun_lingtai.client.event;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.render.AptitudeFormationShader;
import com.seraphina.ninecun_lingtai.client.render.ElementFlowShader;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
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

    private ClientModEventHandler() {
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
    }
}
