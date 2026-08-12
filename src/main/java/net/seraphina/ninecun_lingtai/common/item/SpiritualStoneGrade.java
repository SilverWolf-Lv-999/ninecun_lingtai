package net.seraphina.ninecun_lingtai.common.item;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum SpiritualStoneGrade {
    HUMAN("human", 400.0D, 0.5D),
    EARTH("earth", 5200.0D, 1.0D),
    HEAVEN("heaven", 10000.0D, 3.0D);

    private static final Map<String, SpiritualStoneGrade> BY_NAME = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(SpiritualStoneGrade::getSerializedName, Function.identity()));

    private final String serializedName;
    private final double energyCapacity;
    private final double absorptionMultiplier;

    SpiritualStoneGrade(String serializedName, double energyCapacity, double absorptionMultiplier) {
        this.serializedName = serializedName;
        this.energyCapacity = energyCapacity;
        this.absorptionMultiplier = absorptionMultiplier;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public double getEnergyCapacity() {
        return this.energyCapacity;
    }

    public double getAbsorptionMultiplier() {
        return this.absorptionMultiplier;
    }

    public String getDescriptionId() {
        return "item.ninecun_lingtai.spiritual_stone." + this.serializedName;
    }

    public String getAbsorptionDescriptionId() {
        return "tooltip.ninecun_lingtai.spiritual_stone.absorption." + this.serializedName;
    }

    public static Optional<SpiritualStoneGrade> fromSerializedName(String serializedName) {
        return Optional.ofNullable(BY_NAME.get(serializedName));
    }
}
