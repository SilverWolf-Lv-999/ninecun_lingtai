package net.seraphina.ninecun_lingtai;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

public class Static {
    public static boolean isDev() {
        return !FMLEnvironment.production;
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }
}
