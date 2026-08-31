package net.seraphina.ninecun_lingtai.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 修士墓穴产出的机缘/素材类物品（M4 墓穴切片）。
 *
 * <p>显示名与说明行均走 {@code translatableWithFallback}：语言文件未接线时直接显示
 * 中文回退文案，键名收集在《灵气生态与死亡复活》实施记录的集中接线清单里。</p>
 */
public class TombRelicItem extends Item {
    private final String fallbackName;
    private final String fallbackLore;
    private final boolean foil;

    public TombRelicItem(Properties properties, String fallbackName, String fallbackLore, boolean foil) {
        super(properties);
        this.fallbackName = fallbackName;
        this.fallbackLore = fallbackLore;
        this.foil = foil;
    }

    @Override
    public Component getName(ItemStack itemStack) {
        return Component.translatableWithFallback(this.getDescriptionId(itemStack), this.fallbackName);
    }

    @Override
    public boolean isFoil(ItemStack itemStack) {
        return this.foil || super.isFoil(itemStack);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(
                Component.translatableWithFallback(this.getDescriptionId(itemStack) + ".lore", this.fallbackLore)
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC)
        );
        super.appendHoverText(itemStack, level, tooltip, flag);
    }
}
