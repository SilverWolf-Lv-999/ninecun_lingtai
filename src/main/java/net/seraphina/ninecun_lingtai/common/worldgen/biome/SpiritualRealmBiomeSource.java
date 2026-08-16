package net.seraphina.ninecun_lingtai.common.worldgen.biome;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;

import java.util.stream.Stream;

public final class SpiritualRealmBiomeSource extends BiomeSource {
    public static final int DEFAULT_REGION_SIZE_BLOCKS = 65_536;
    private static final int MINIMUM_REGION_SIZE_BLOCKS = 16_384;
    private static final int MAXIMUM_REGION_SIZE_BLOCKS = 262_144;
    private static final int SURFACE_SAMPLE_QUART_Y = QuartPos.fromBlock(64);
    private static final int CAVE_CUTOFF_QUART_Y = QuartPos.fromBlock(32);

    public static final Codec<SpiritualRealmBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MultiNoiseBiomeSourceParameterList.CODEC.fieldOf("preset")
                            .forGetter(source -> source.thisPreset),
                    Codec.intRange(MINIMUM_REGION_SIZE_BLOCKS, MAXIMUM_REGION_SIZE_BLOCKS)
                            .optionalFieldOf("region_size", DEFAULT_REGION_SIZE_BLOCKS)
                            .forGetter(source -> source.thisRegionSizeBlocks)
            ).apply(instance, SpiritualRealmBiomeSource::new)
    );

    private final Holder<MultiNoiseBiomeSourceParameterList> thisPreset;
    private final int thisRegionSizeBlocks;
    private final int thisRegionSizeQuarts;

    public SpiritualRealmBiomeSource(
            Holder<MultiNoiseBiomeSourceParameterList> preset,
            int regionSizeBlocks
    ) {
        this.thisPreset = preset;
        this.thisRegionSizeBlocks = regionSizeBlocks;
        this.thisRegionSizeQuarts = Math.max(1, QuartPos.fromBlock(regionSizeBlocks));
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return this.thisPreset.value()
                .parameters()
                .values()
                .stream()
                .map(Pair::getSecond);
    }

    @Override
    public Holder<Biome> getNoiseBiome(
            int quartX,
            int quartY,
            int quartZ,
            Climate.Sampler sampler
    ) {
        int regionX = Math.floorDiv(quartX, this.thisRegionSizeQuarts);
        int regionZ = Math.floorDiv(quartZ, this.thisRegionSizeQuarts);
        int sampleX = regionX * this.thisRegionSizeQuarts + this.thisRegionSizeQuarts / 2;
        int sampleZ = regionZ * this.thisRegionSizeQuarts + this.thisRegionSizeQuarts / 2;
        int sampleY = quartY >= CAVE_CUTOFF_QUART_Y ? SURFACE_SAMPLE_QUART_Y : quartY;
        return this.thisPreset.value()
                .parameters()
                .findValue(sampler.sample(sampleX, sampleY, sampleZ));
    }
}
