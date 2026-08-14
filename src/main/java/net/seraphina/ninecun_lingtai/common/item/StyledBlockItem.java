package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;

import java.util.function.Consumer;

public final class StyledBlockItem extends BlockItem {
    private final ItemFontStyle fontStyle;

    public StyledBlockItem(Block block, Properties properties, ItemFontStyle fontStyle) {
        super(block, properties);
        this.fontStyle = fontStyle;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(StyledBlockItem.this.fontStyle);
            }
        });
    }
}
