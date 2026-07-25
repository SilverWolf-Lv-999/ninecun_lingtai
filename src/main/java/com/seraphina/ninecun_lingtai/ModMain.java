package com.seraphina.ninecun_lingtai;

import com.seraphina.ninecun_lingtai.common.config.CommonConfig;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(ModMain.MOD_ID)
public final class ModMain {
    public static final String MOD_ID = "ninecun_lingtai";

    public ModMain() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        ModNetwork.register();
    }
}
