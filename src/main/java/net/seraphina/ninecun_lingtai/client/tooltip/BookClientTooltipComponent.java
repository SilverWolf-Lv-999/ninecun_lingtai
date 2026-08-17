package net.seraphina.ninecun_lingtai.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

public final class BookClientTooltipComponent implements ClientTooltipComponent {
    private static final int WIDTH = 152;
    private static final int HEIGHT = 44;
    private static final int OUTER_COLOR = 0xFF2B170F;
    private static final int COVER_COLOR = 0xFF5F311D;
    private static final int SPINE_COLOR = 0xFF8D4C29;
    private static final int PAGE_COLOR = 0xFFD7B979;
    private static final int FRAME_COLOR = 0xFFE2C57F;
    private static final int MUTED_TEXT_COLOR = 0xFFC9A875;
    private static final int TEXT_LEFT = 18;
    private static final int TEXT_WIDTH = WIDTH - TEXT_LEFT - 8;

    private final String title;
    private final String author;

    public BookClientTooltipComponent(BookTooltipComponent tooltipComponent) {
        this.title = tooltipComponent.getTitle();
        this.author = tooltipComponent.getAuthor();
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        return WIDTH;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        guiGraphics.fill(x, y, x + WIDTH, y + HEIGHT, OUTER_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, COVER_COLOR);
        guiGraphics.fill(x + 3, y + 3, x + 10, y + HEIGHT - 3, SPINE_COLOR);
        guiGraphics.fill(x + 10, y + 4, x + 12, y + HEIGHT - 4, FRAME_COLOR);
        guiGraphics.fill(x + 13, y + 4, x + WIDTH - 4, y + HEIGHT - 4, 0xFF3A2117);
        guiGraphics.fill(x + 14, y + 5, x + WIDTH - 5, y + HEIGHT - 5, 0xFF51301E);
        guiGraphics.fill(x + 15, y + HEIGHT - 10, x + WIDTH - 7, y + HEIGHT - 9, PAGE_COLOR);
        guiGraphics.fill(x + 15, y + HEIGHT - 7, x + WIDTH - 10, y + HEIGHT - 6, PAGE_COLOR);

        guiGraphics.drawString(
                font,
                trim(font, this.title, TEXT_WIDTH),
                x + TEXT_LEFT,
                y + 7,
                0xFFFFFFFF,
                false
        );
        if (!this.author.isBlank()) {
            guiGraphics.drawString(
                    font,
                    trim(
                            font,
                            Component.translatable("tooltip.ninecun_lingtai.book.author", this.author).getString(),
                            TEXT_WIDTH
                    ),
                    x + TEXT_LEFT,
                    y + 19,
                    MUTED_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.drawString(
                font,
                Component.translatable("tooltip.ninecun_lingtai.book.readable"),
                x + TEXT_LEFT,
                y + 31,
                MUTED_TEXT_COLOR,
                false
        );
    }

    private static String trim(Font font, String text, int maximumWidth) {
        if (font.width(text) <= maximumWidth) {
            return text;
        }
        String suffix = "...";
        return font.plainSubstrByWidth(text, Math.max(0, maximumWidth - font.width(suffix))) + suffix;
    }
}
