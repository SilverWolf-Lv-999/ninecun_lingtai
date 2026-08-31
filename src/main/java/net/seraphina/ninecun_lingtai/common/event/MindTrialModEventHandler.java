package net.seraphina.ninecun_lingtai.common.event;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.entity.MindDemonEntity;
import net.seraphina.ninecun_lingtai.common.entity.MortalBodyEntity;
import net.seraphina.ninecun_lingtai.common.registry.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 心魔即己新实体的属性注册（MOD 总线）。独立文件而非并入
 * {@code ModEntityAttributeEventHandler}：该文件归其它系统所有，本批不碰。
 */
@Mod.EventBusSubscriber(modid = ModMain.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MindTrialModEventHandler {

    private MindTrialModEventHandler() {
    }

    @SubscribeEvent
    public static void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.MIND_DEMON.get(), MindDemonEntity.createAttributes().build());
        event.put(ModEntityTypes.MORTAL_BODY.get(), MortalBodyEntity.createAttributes().build());
    }
}
