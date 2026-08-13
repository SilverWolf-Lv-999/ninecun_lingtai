package net.seraphina.ninecun_lingtai.utility.launch;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.unsafe.UnsafeHacks;
import net.seraphina.ninecun_lingtai.common.myminecraft.ForgeHacksHook;
import net.seraphina.ninecun_lingtai.common.myminecraft.MyLivingEntity;
import net.seraphina.ninecun_lingtai.utility.jvm.SeraphinaHook;

public class HookUtility {
    public static void commonHooks() {

    }

    public static void commonReplaceMethod() {
        SeraphinaHook.replaceMethod(LivingEntity.class, MyLivingEntity.class);
        SeraphinaHook.replaceMethod(UnsafeHacks.class, ForgeHacksHook.class);
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientHooks() {
    }

    @OnlyIn(Dist.CLIENT)
    public static void clientReplaceMethod() {

    }
}
