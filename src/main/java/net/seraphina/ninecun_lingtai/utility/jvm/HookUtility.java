package net.seraphina.ninecun_lingtai.utility.jvm;

import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import net.seraphina.ninecun_lingtai.Static;
import net.seraphina.ninecun_lingtai.common.myminecraft.*;

@Deprecated
public class HookUtility {
    public static void commonHooks() {

    }

    public static void commonReplaceMethod() {
        SeraLegitHook.replaceMethod(LivingEntity.class, MyLivingEntity.class);
        SeraLegitHook.replaceMethod(UnsafeHacks.class, ForgeHacksHook.class);
        SeraLegitHook.replaceMethod(FoodData.class, MyFoodData.class);
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientHooks() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientReplaceMethod() {
        if (Static.isApfDay()) {
            SeraLegitHook.replaceMethod(LogoRenderer.class, MyLogoRenderer.class);
            SeraLegitHook.replaceMethod(SplashRenderer.class, MySplashRenderer.class);
        }
    }
}
