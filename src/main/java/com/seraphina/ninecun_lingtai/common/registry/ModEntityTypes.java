package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(
            ForgeRegistries.ENTITY_TYPES,
            ModMain.MOD_ID
    );

    public static final RegistryObject<EntityType<SoulEntity>> SOUL_ENTITY = ENTITY_TYPES.register(
            "soul_entity",
            () -> EntityType.Builder
                    .of(SoulEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(1024)
                    .updateInterval(1)
                    .build("soul_entity")
    );

    private ModEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
