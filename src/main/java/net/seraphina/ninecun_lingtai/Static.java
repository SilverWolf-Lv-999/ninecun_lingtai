package net.seraphina.ninecun_lingtai;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

public class Static {
    static final String TIME_DATA;

    public static final Set<String> loadMods;

    static {
        TIME_DATA = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        loadMods = new HashSet<>();
        FMLLoader.getLoadingModList().getMods().forEach((modInfo) -> {
            loadMods.add(modInfo.getModId());
        });
    }

    public static boolean isApfDay() {
        return TIME_DATA.equals("04-01");
    }

    public static boolean isDev() {
        return !FMLEnvironment.production;
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public static void println(String message) {
        System.out.println(message);
    }
}
