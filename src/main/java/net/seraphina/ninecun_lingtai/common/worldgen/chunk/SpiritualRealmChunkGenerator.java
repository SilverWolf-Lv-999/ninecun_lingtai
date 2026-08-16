package net.seraphina.ninecun_lingtai.common.worldgen.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.seraphina.ninecun_lingtai.common.worldgen.ore.SpiritualRealmOreGenerator;
import net.seraphina.ninecun_lingtai.common.worldgen.settlement.SpiritualRealmSettlementGenerator;
import net.seraphina.ninecun_lingtai.common.worldgen.structure.SpiritualRealmUndergroundStructureGenerator;

import java.util.HashMap;
import java.util.Map;

public final class SpiritualRealmChunkGenerator extends NoiseBasedChunkGenerator {
    public static final Codec<SpiritualRealmChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(ChunkGenerator::getBiomeSource),
                    NoiseGeneratorSettings.CODEC.fieldOf("settings")
                            .forGetter(SpiritualRealmChunkGenerator::generatorSettings)
            ).apply(instance, instance.stable(SpiritualRealmChunkGenerator::new))
    );

    public SpiritualRealmChunkGenerator(
            BiomeSource biomeSource,
            Holder<NoiseGeneratorSettings> settings
    ) {
        super(biomeSource, settings);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void createStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager
    ) {
        super.createStructures(
                registryAccess,
                structureState,
                structureManager,
                chunk,
                structureTemplateManager
        );
        removeReplacedVanillaStructures(
                chunk,
                registryAccess.registryOrThrow(Registries.STRUCTURE)
        );
    }

    @Override
    public void createReferences(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkAccess chunk
    ) {
        super.createReferences(level, structureManager, chunk);
        removeReplacedVanillaStructures(
                chunk,
                level.registryAccess().registryOrThrow(Registries.STRUCTURE)
        );
    }

    @Override
    public void applyBiomeDecoration(
            WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structureManager
    ) {
        super.applyBiomeDecoration(level, chunk, structureManager);
        SpiritualRealmUndergroundStructureGenerator.generate(level, chunk);
        SpiritualRealmOreGenerator.generate(level, chunk);
        SpiritualRealmSettlementGenerator.generate(level, chunk);
    }

    private static void removeReplacedVanillaStructures(
            ChunkAccess chunk,
            Registry<Structure> structureRegistry
    ) {
        Map<Structure, StructureStart> starts = new HashMap<>(chunk.getAllStarts());
        starts.entrySet().removeIf(entry -> isReplacedVanillaStructure(
                structureRegistry,
                entry.getKey()
        ));
        chunk.setAllStarts(starts);

        Map<Structure, LongSet> references = new HashMap<>(chunk.getAllReferences());
        references.entrySet().removeIf(entry -> isReplacedVanillaStructure(
                structureRegistry,
                entry.getKey()
        ));
        chunk.setAllReferences(references);
    }

    private static boolean isReplacedVanillaStructure(
            Registry<Structure> structureRegistry,
            Structure structure
    ) {
        ResourceLocation structureId = structureRegistry.getKey(structure);
        if (structureId == null || !structureId.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
            return false;
        }

        String path = structureId.getPath();
        return path.startsWith("village_")
                || path.equals("mineshaft")
                || path.equals("mineshaft_mesa")
                || path.equals("ancient_city");
    }
}
