package net.seraphina.ninecun_lingtai;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class Static {
    static final String TIME_DATA;

    public static final Set<String> loadMods;

    static final AtomicBoolean isOptifineLoaded;

    static {
        TIME_DATA = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        loadMods = new HashSet<>();
        FMLLoader.getLoadingModList().getMods().forEach((modInfo) -> {
            loadMods.add(modInfo.getModId());
        });
        isOptifineLoaded = new AtomicBoolean(false);
        try {
            Class.forName("optifine.OptiFineTransformationService");
            isOptifineLoaded.set(true);
        } catch (Exception exception) {
            isOptifineLoaded.set(false);
        }
    }

    public static boolean isOptifineLoaded() {
        return isOptifineLoaded.get()|| loadMods.contains("optifine");
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

    public static boolean is(String side) {
        return switch (side) {
            case "CLIENT" -> FMLEnvironment.dist == Dist.CLIENT;
            case "SERVER" -> FMLEnvironment.dist != Dist.CLIENT;
            case "DEV" -> FMLEnvironment.production;
            default -> throw new IllegalArgumentException("Invalid side " + side);
        };
    }

//    @OnlyIn(Dist.CLIENT)
//    public static boolean isCN() {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.languageManager == null) {
//            return false;
//        }
//        LanguageInfo info = Minecraft.getInstance().languageManager.getLanguage("zh_cn");
//        return info != null && info.name().trim().equals("zh_cn");
//    }
}
