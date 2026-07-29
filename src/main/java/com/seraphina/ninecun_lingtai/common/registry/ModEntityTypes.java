package com.seraphina.ninecun_lingtai.common.registry;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.common.entity.SoulEntity;
import com.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsCollisionEntity;
import com.seraphina.ninecun_lingtai.common.entity.ability.PushingMountainsEntity;
import com.seraphina.ninecun_lingtai.common.entity.spell.DefenseSpellGuardianEntity;
import com.seraphina.ninecun_lingtai.common.entity.tribulation.ThunderTribulationEntity;
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

    public static final RegistryObject<EntityType<DefenseSpellGuardianEntity>> DEFENSE_SPELL_GUARDIAN =
            ENTITY_TYPES.register(
                    "defense_spell_guardian",
                    () -> EntityType.Builder
                            .of(DefenseSpellGuardianEntity::new, MobCategory.MISC)
                            .sized(0.9F, 1.8F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("defense_spell_guardian")
                    );

    public static final RegistryObject<EntityType<ThunderTribulationEntity>> THUNDER_TRIBULATION =
            ENTITY_TYPES.register(
                    "thunder_tribulation",
                    () -> EntityType.Builder
                            .of(ThunderTribulationEntity::new, MobCategory.MISC)
                            .sized(0.4F, 0.4F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("thunder_tribulation")
            );

    public static final RegistryObject<EntityType<PushingMountainsEntity>> PUSHING_MOUNTAINS_ENTITY = ENTITY_TYPES.register(
            "pushing_mountains_entity",
            () -> EntityType.Builder
                    .of(PushingMountainsEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(256)
                    .updateInterval(1)
                    .build("pushing_mountains_entity")
    );

    public static final RegistryObject<EntityType<PushingMountainsCollisionEntity>> PUSHING_MOUNTAINS_COLLISION =
            ENTITY_TYPES.register(
                    "pushing_mountains_collision",
                    () -> EntityType.Builder
                            .of(PushingMountainsCollisionEntity::new, MobCategory.MISC)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(256)
                            .updateInterval(1)
                            .build("pushing_mountains_collision")
            );

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
