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
        @SuppressWarnings("all")
        final String[] look1 = {
                "本修仙Mod没有对别的Mod有任何形式的针对，可以屠杀99%的防御，如果你认为写ASM是违法的，那你赢了",
                "这些防御包括：",
                "终环",
                "无尽套",
                "梦幻终焉套",
                "星轨立方套装",
                "EOPL",
                "以及部分准秒杀",
                "如果你拿xxx mod来吹，我会当空气无视"
        };
        @SuppressWarnings("all")
        final String[] look2 = {
                "我极度厌恶针对",
                "如果你进行针对我的Mod这种愚蠢行为",
                "包括 Mixin、ASM、CoreMod、Java Agent、反射等等",
                "只要对 Mod 进行了特殊处理以保证你的、别的Mod的功能可以不受到来自我的Mod的负面影响",
                "我会视为你在挑战我的底线",
                "多群通报并发布视频展示你的愚蠢行为"
        };
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
