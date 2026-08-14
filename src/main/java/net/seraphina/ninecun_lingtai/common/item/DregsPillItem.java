package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.client.render.PillItemRenderer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public final class DregsPillItem extends ColorItem {
    private static final FoodProperties DREGS_FOOD = new FoodProperties.Builder()
            .nutrition(1)
            .saturationMod(0.1F)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200, 0), 0.6F)
            .alwaysEat()
            .build();

    public DregsPillItem(Item.Properties properties, int color) {
        super(properties.food(DREGS_FOOD), color);
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            @NotNull TooltipFlag tooltipFlag
    ) {
        tooltip.add(Component.translatable("tooltip.ninecun_lingtai.dregs_pill.description")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return PillItemRenderer.INSTANCE;
            }

            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(ItemFontStyle.DREGS_PILL);
            }
        });
    }
}
