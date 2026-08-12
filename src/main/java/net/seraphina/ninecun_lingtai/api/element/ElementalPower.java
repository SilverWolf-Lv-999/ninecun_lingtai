package net.seraphina.ninecun_lingtai.api.element;

import java.util.Objects;

public record ElementalPower(ElementType element, double strength) {
    public ElementalPower {
        Objects.requireNonNull(element, "element");
        if (!Double.isFinite(strength) || strength <= 0.0D) {
            throw new IllegalArgumentException("Element strength must be finite and greater than zero");
        }
    }
}
