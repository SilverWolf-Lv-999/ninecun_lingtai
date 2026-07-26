package com.seraphina.ninecun_lingtai.integration.jade;

import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public final class LifespanJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(LifespanJadeProvider.INSTANCE, LivingEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(LifespanJadeProvider.INSTANCE, LivingEntity.class);
    }
}
