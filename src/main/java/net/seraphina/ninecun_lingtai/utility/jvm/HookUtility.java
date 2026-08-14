package net.seraphina.ninecun_lingtai.utility.jvm;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import net.seraphina.ninecun_lingtai.common.myminecraft.ForgeHacksHook;
import net.seraphina.ninecun_lingtai.common.myminecraft.MyFoodData;
import net.seraphina.ninecun_lingtai.common.myminecraft.MyLivingEntity;

public class HookUtility {
    public static void commonHooks() {

    }

    public static void commonReplaceMethod() {
        SeraphinaHook.replaceMethod(LivingEntity.class, MyLivingEntity.class);
        SeraphinaHook.replaceMethod(UnsafeHacks.class, ForgeHacksHook.class);
        SeraphinaHook.replaceMethod(FoodData.class, MyFoodData.class);
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientHooks() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientReplaceMethod() {

    }
}
