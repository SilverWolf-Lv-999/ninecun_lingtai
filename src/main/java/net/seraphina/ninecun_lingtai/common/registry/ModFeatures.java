package net.seraphina.ninecun_lingtai.common.registry;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.worldgen.feature.FireLingzhiFeature;
import net.seraphina.ninecun_lingtai.common.worldgen.feature.SpiritualPlantFeature;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(
            ForgeRegistries.FEATURES,
            ModMain.MOD_ID
    );

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> FIRE_LINGZHI = FEATURES.register(
            "fire_lingzhi",
            () -> new FireLingzhiFeature(NoneFeatureConfiguration.CODEC)
    );

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MOON_DEW_ORCHID = FEATURES.register(
            "moon_dew_orchid",
            () -> new SpiritualPlantFeature(
                    NoneFeatureConfiguration.CODEC,
                    () -> ModBlocks.MOON_DEW_ORCHID.get(),
                    () -> Blocks.CHERRY_LOG
            )
    );

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> EARTH_MARROW_GINSENG = FEATURES.register(
            "earth_marrow_ginseng",
            () -> new SpiritualPlantFeature(
                    NoneFeatureConfiguration.CODEC,
                    () -> ModBlocks.EARTH_MARROW_GINSENG.get(),
                    () -> Blocks.DARK_OAK_LOG
            )
    );

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> JADE_MIND_FLOWER = FEATURES.register(
            "jade_mind_flower",
            () -> new SpiritualPlantFeature(
                    NoneFeatureConfiguration.CODEC,
                    () -> ModBlocks.JADE_MIND_FLOWER.get(),
                    () -> Blocks.SPRUCE_LOG
            )
    );

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
    }
}
