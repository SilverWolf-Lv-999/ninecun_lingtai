package net.seraphina.ninecun_lingtai.api.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Optional;

public enum SpiritualItemGrade {
    MORTAL("mortal", ChatFormatting.WHITE, true),
    SPIRITUAL("spiritual", ChatFormatting.AQUA, false),
    IMMORTAL("immortal", ChatFormatting.GOLD, false);

    private final String serializedName;
    private final ChatFormatting formatting;
    private final boolean damageable;

    SpiritualItemGrade(
            String serializedName,
            ChatFormatting formatting,
            boolean damageable
    ) {
        this.serializedName = serializedName;
        this.formatting = formatting;
        this.damageable = damageable;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public Component getDisplayName() {
        return Component.translatable(
                "tooltip.ninecun_lingtai.spiritual_item.grade." + serializedName
        ).withStyle(formatting);
    }

    public boolean isDamageable() {
        return damageable;
    }

    public static Optional<SpiritualItemGrade> fromSerializedName(String serializedName) {
        return Arrays.stream(values())
                .filter(grade -> grade.serializedName.equals(serializedName))
                .findFirst();
    }
}
