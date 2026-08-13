package net.seraphina.ninecun_lingtai.api.alchemy;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum HeatBand {
    GENTLE("gentle", "文火", 1, 199),
    MEDIUM("medium", "中火", 200, 399),
    FIERCE("fierce", "武火", 400, 600);

    private static final Map<String, HeatBand> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(HeatBand::getSerializedName, Function.identity()));

    private final String serializedName;
    private final String chineseName;
    private final int minimumHeat;
    private final int maximumHeat;

    HeatBand(String serializedName, String chineseName, int minimumHeat, int maximumHeat) {
        this.serializedName = serializedName;
        this.chineseName = chineseName;
        this.minimumHeat = minimumHeat;
        this.maximumHeat = maximumHeat;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public String getChineseName() {
        return this.chineseName;
    }

    public int getMinimumHeat() {
        return this.minimumHeat;
    }

    public int getMaximumHeat() {
        return this.maximumHeat;
    }

    public int getCenterHeat() {
        return (this.minimumHeat + this.maximumHeat) / 2;
    }

    public boolean contains(int heat) {
        return heat >= this.minimumHeat && heat <= this.maximumHeat;
    }

    public static Optional<HeatBand> ofHeat(int heat) {
        return Arrays.stream(values())
                .filter(band -> band.contains(heat))
                .findFirst();
    }

    public static Optional<HeatBand> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
