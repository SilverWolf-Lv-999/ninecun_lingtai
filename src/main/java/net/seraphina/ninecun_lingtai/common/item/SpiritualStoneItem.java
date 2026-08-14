package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public final class SpiritualStoneItem extends Item {
    public SpiritualStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        return Component.translatable(SpiritualStoneUtility.getGrade(itemStack).getDescriptionId());
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        SpiritualStoneGrade grade = SpiritualStoneUtility.getGrade(itemStack);
        tooltip.add(Component.translatable(
                "tooltip.ninecun_lingtai.spiritual_stone.energy",
                formatEnergy(SpiritualStoneUtility.getRemainingEnergy(itemStack)),
                formatEnergy(grade.getEnergyCapacity())
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(grade.getAbsorptionDescriptionId())
                .withStyle(ChatFormatting.GRAY));
    }

    private static String formatEnergy(double energy) {
        if (!Double.isFinite(energy)) {
            return String.valueOf(energy);
        }
        return BigDecimal.valueOf(energy)
                .stripTrailingZeros()
                .toPlainString();
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(ItemFontStyle.SPIRITUAL_STONE);
            }
        });
    }
}
