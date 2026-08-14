package net.seraphina.ninecun_lingtai.client.utility;

import net.seraphina.ninecun_lingtai.client.ItemFontTheme;
import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.common.item.ItemFontStyle;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;

import java.util.Optional;

public final class ItemTooltipUtility {
    private static final int BACKGROUND_BASE_COLOR = 0x09070F;
    private static final int BACKGROUND_ALPHA = 0xEE;
    private static final int BORDER_ALPHA = 0xFF;

    private ItemTooltipUtility() {
    }

    public static Optional<Font> resolveTooltipFont(ItemStack itemStack) {
        return resolveTooltipTheme(itemStack).map(SeraFont::get);
    }

    public static void applyTooltipColors(RenderTooltipEvent.Color event) {
        resolveTooltipTheme(event.getItemStack()).ifPresent(fontTheme -> {
            ItemFontStyle primaryStyle = fontTheme.primaryStyle();
            ItemFontStyle accentStyle = fontTheme.accentStyle();
            float primaryWave = getColorWave(primaryStyle, 0.0F);
            float accentWave = getColorWave(accentStyle, 0.25F);
            int primaryColor = mixColors(
                    primaryStyle.getPrimaryColor(),
                    primaryStyle.getSecondaryColor(),
                    primaryWave
            );
            int accentColor = mixColors(
                    accentStyle.getPrimaryColor(),
                    accentStyle.getSecondaryColor(),
                    accentWave
            );
            float rankWeight = fontTheme.rank() * 0.012F;
            event.setBackgroundStart(withAlpha(
                    mixColors(BACKGROUND_BASE_COLOR, primaryColor, 0.17F + rankWeight),
                    BACKGROUND_ALPHA
            ));
            event.setBackgroundEnd(withAlpha(
                    mixColors(BACKGROUND_BASE_COLOR, accentColor, 0.23F + rankWeight),
                    BACKGROUND_ALPHA
            ));
            event.setBorderStart(withAlpha(mixColors(primaryColor, 0xFFFFFF, 0.18F), BORDER_ALPHA));
            event.setBorderEnd(withAlpha(mixColors(accentColor, 0xFFFFFF, 0.24F), BORDER_ALPHA));
        });
    }

    public static Optional<ItemFontTheme> resolveTooltipTheme(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return Optional.empty();
        }
        Item item = itemStack.getItem();
        if (item == ModItems.JADE_SLIP.get()) {
            return Optional.of(ItemFontStyleResolver.resolveTechnique(itemStack));
        }
        if (item == ModItems.ALCHEMY_RECIPE.get()) {
            return Optional.of(ItemFontStyleResolver.resolveAlchemyRecipe(itemStack));
        }
        if (item == ModItems.SPIRITUAL_STONE.get()) {
            return Optional.of(ItemFontStyleResolver.resolveSpiritualStone(itemStack));
        }
        if (item == ModItems.DAO_SCROLL.get()) {
            return Optional.of(ItemFontTheme.of(ItemFontStyle.DAO_SCROLL));
        }
        if (item == ModItems.HAN_SWORD.get()) {
            return Optional.of(ItemFontStyleResolver.resolveSpiritualItem(itemStack));
        }
        if (item == ModItems.STORAGE_RING.get()) {
            return Optional.of(ItemFontTheme.of(ItemFontStyle.STORAGE_RING));
        }
        if (ModItems.isPillItem(item)) {
            return Optional.of(ItemFontStyleResolver.resolvePill(itemStack));
        }
        if (item == ModItems.DREGS_PILL.get()) {
            return Optional.of(ItemFontTheme.of(ItemFontStyle.DREGS_PILL));
        }
        if (item == ModItems.SPIRITUAL_STONE_ORE.get()) {
            return Optional.of(ItemFontTheme.of(ItemFontStyle.SPIRITUAL_STONE_ORE));
        }
        if (item == ModItems.DEEPSLATE_SPIRITUAL_STONE_ORE.get()) {
            return Optional.of(ItemFontTheme.of(ItemFontStyle.DEEPSLATE_SPIRITUAL_STONE_ORE));
        }
        if (item == ModItems.ALCHEMY_FURNACE.get()) {
            return Optional.of(ItemFontTheme.of(ItemFontStyle.ALCHEMY_FURNACE));
        }
        if (item == ModItems.FIRE_LINGZHI.get()) {
            return Optional.of(ItemFontTheme.of(ItemFontStyle.FIRE_LINGZHI));
        }
        return Optional.empty();
    }

    private static float getColorWave(ItemFontStyle fontStyle, float phaseOffset) {
        long currentTime = Util.getMillis();
        float progress = (currentTime % fontStyle.getCycleDuration())
                / (float) fontStyle.getCycleDuration();
        return 0.5F + 0.5F * (float) Math.sin(Math.PI * 2.0D * (progress + phaseOffset));
    }

    private static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 0xFFFFFF;
    }

    private static int mixColors(int firstColor, int secondColor, float progress) {
        int red = mixChannel(firstColor >> 16 & 255, secondColor >> 16 & 255, progress);
        int green = mixChannel(firstColor >> 8 & 255, secondColor >> 8 & 255, progress);
        int blue = mixChannel(firstColor & 255, secondColor & 255, progress);
        return red << 16 | green << 8 | blue;
    }

    private static int mixChannel(int firstChannel, int secondChannel, float progress) {
        return Math.round(firstChannel + (secondChannel - firstChannel) * progress);
    }
}
