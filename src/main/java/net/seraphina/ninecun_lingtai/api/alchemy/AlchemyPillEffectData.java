package net.seraphina.ninecun_lingtai.api.alchemy;

import java.util.Map;
import java.util.Objects;

public record AlchemyPillEffectData(String type, Map<String, Double> numbers) {
    public AlchemyPillEffectData {
        Objects.requireNonNull(type, "type");
        if (type.isBlank()) {
            throw new IllegalArgumentException("Pill effect type must not be blank");
        }
        numbers = Map.copyOf(numbers);
    }

    public double getNumber(String key, double defaultValue) {
        return this.numbers.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        Double value = this.numbers.get(key);
        return value == null ? defaultValue : (int) Math.round(value);
    }
}
