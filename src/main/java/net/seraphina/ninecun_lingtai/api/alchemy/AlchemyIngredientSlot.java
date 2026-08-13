package net.seraphina.ninecun_lingtai.api.alchemy;

import java.util.Arrays;

public enum AlchemyIngredientSlot {
    PRIMARY("primary", 0),
    SECONDARY_1("secondary_1", 1),
    SECONDARY_2("secondary_2", 2),
    AUXILIARY_1("auxiliary_1", 3),
    AUXILIARY_2("auxiliary_2", 4);

    private final String serializedName;
    private final int slotIndex;

    AlchemyIngredientSlot(String serializedName, int slotIndex) {
        this.serializedName = serializedName;
        this.slotIndex = slotIndex;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    public static AlchemyIngredientSlot fromSerializedName(String name) {
        return Arrays.stream(values())
                .filter(slot -> slot.serializedName.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown alchemy ingredient slot: " + name
                ));
    }

    public static AlchemyIngredientSlot fromSlotIndex(int slotIndex) {
        return Arrays.stream(values())
                .filter(slot -> slot.slotIndex == slotIndex)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown alchemy ingredient slot index: " + slotIndex
                ));
    }
}
