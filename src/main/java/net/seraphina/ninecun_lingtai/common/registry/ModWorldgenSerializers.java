package net.seraphina.ninecun_lingtai.common.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.worldgen.biome.SpiritualRealmBiomeSource;
import net.seraphina.ninecun_lingtai.common.worldgen.chunk.SpiritualRealmChunkGenerator;

public final class ModWorldgenSerializers {
    private static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, ModMain.MOD_ID);
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ModMain.MOD_ID);

    public static final RegistryObject<Codec<? extends BiomeSource>> SPIRITUAL_REALM_BIOME_SOURCE =
            BIOME_SOURCES.register("spiritual_realm", () -> SpiritualRealmBiomeSource.CODEC);
    public static final RegistryObject<Codec<? extends ChunkGenerator>> SPIRITUAL_REALM_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("spiritual_realm", () -> SpiritualRealmChunkGenerator.CODEC);

    public static void register(IEventBus modEventBus) {
        BIOME_SOURCES.register(modEventBus);
        CHUNK_GENERATORS.register(modEventBus);
    }
}
