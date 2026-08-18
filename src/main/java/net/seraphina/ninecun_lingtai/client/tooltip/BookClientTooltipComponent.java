package net.seraphina.ninecun_lingtai.client.tooltip;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;

public final class BookClientTooltipComponent implements ClientTooltipComponent {
    private static final int WIDTH = 188;
    private static final int HEIGHT = 58;
    private static final int COVER_COLOR = 0xFF5A3D24;
    private static final int COVER_HIGHLIGHT_COLOR = 0xFF8D6540;
    private static final int PAPER_COLOR = 0xFFF1E0B3;
    private static final int PAPER_SHADOW_COLOR = 0xFFD4B982;
    private static final int SPINE_COLOR = 0xFF6D452A;
    private static final int TITLE_COLOR = 0xFF3A2514;
    private static final int AUTHOR_COLOR = 0xFF6B5136;
    private static final int ACTION_COLOR = 0xFF8D4C29;
    private static final int TEXT_LEFT = 24;
    private static final int TEXT_WIDTH = WIDTH - TEXT_LEFT - 10;

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
        guiGraphics.fill(x, y, x + WIDTH, y + HEIGHT, COVER_COLOR);
        guiGraphics.fill(x + 1, y + 1, x + WIDTH - 1, y + 2, COVER_HIGHLIGHT_COLOR);
        guiGraphics.fill(x + 1, y + HEIGHT - 2, x + WIDTH - 1, y + HEIGHT - 1, 0xFF3B281A);
        guiGraphics.fill(x + 5, y + 5, x + WIDTH - 5, y + HEIGHT - 5, PAPER_SHADOW_COLOR);
        guiGraphics.fill(x + 8, y + 5, x + WIDTH - 5, y + HEIGHT - 8, PAPER_COLOR);
        guiGraphics.fill(x + 5, y + 5, x + 8, y + HEIGHT - 5, SPINE_COLOR);
        guiGraphics.fill(x + 8, y + HEIGHT - 8, x + WIDTH - 5, y + HEIGHT - 6, PAPER_SHADOW_COLOR);
        guiGraphics.fill(x + 15, y + 32, x + WIDTH - 12, y + 33, PAPER_SHADOW_COLOR);

        guiGraphics.drawString(
                font,
                trim(font, this.title, TEXT_WIDTH),
                x + TEXT_LEFT,
                y + 9,
                TITLE_COLOR,
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
                    y + 22,
                    AUTHOR_COLOR,
                    false
            );
        }
        guiGraphics.drawString(
                font,
                Component.translatable("tooltip.ninecun_lingtai.book.readable"),
                x + TEXT_LEFT,
                y + 39,
                ACTION_COLOR,
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
