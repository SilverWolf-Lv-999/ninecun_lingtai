package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.api.book.BookPageData;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestBookPagePacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class BookReadingScreen extends Screen {
    private static final int MAXIMUM_PANEL_WIDTH = 540;
    private static final int MAXIMUM_PANEL_HEIGHT = 390;
    private static final int MINIMUM_PANEL_WIDTH = 260;
    private static final int MINIMUM_PANEL_HEIGHT = 200;
    private static final int PANEL_BORDER_COLOR = 0xFF5A3D24;
    private static final int PANEL_COLOR = 0xFFF1E0B3;
    private static final int TITLE_COLOR = 0xFF3A2514;
    private static final int TEXT_COLOR = 0xFF302014;
    private static final int SECONDARY_TEXT_COLOR = 0xFF6B5136;
    private static final int PAGE_BUTTON_SIZE = 20;

    private BookPageData page;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private Button previousButton;
    private Button nextButton;
    private boolean awaitingPage;

    public BookReadingScreen(BookPageData page) {
        super(Component.literal(page.title()));
        this.page = page;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(MAXIMUM_PANEL_WIDTH, Math.max(MINIMUM_PANEL_WIDTH, this.width - 30));
        this.panelHeight = Math.min(MAXIMUM_PANEL_HEIGHT, Math.max(MINIMUM_PANEL_HEIGHT, this.height - 30));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        this.previousButton = Button.builder(
                        Component.literal("<"),
                        button -> requestPage(this.page.pageIndex() - 1)
                )
                .bounds(
                        this.panelLeft + 12,
                        this.panelTop + this.panelHeight - 31,
                        PAGE_BUTTON_SIZE,
                        PAGE_BUTTON_SIZE
                )
                .build();
        this.nextButton = Button.builder(
                        Component.literal(">"),
                        button -> requestPage(this.page.pageIndex() + 1)
                )
                .bounds(
                        this.panelLeft + this.panelWidth - PAGE_BUTTON_SIZE - 12,
                        this.panelTop + this.panelHeight - 31,
                        PAGE_BUTTON_SIZE,
                        PAGE_BUTTON_SIZE
                )
                .build();
        this.addRenderableWidget(this.previousButton);
        this.addRenderableWidget(this.nextButton);
        updateButtonState();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(
                this.panelLeft,
                this.panelTop,
                this.panelLeft + this.panelWidth,
                this.panelTop + this.panelHeight,
                PANEL_BORDER_COLOR
        );
        guiGraphics.fill(
                this.panelLeft + 2,
                this.panelTop + 2,
                this.panelLeft + this.panelWidth - 2,
                this.panelTop + this.panelHeight - 2,
                PANEL_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                this.page.title(),
                this.width / 2,
                this.panelTop + 12,
                TITLE_COLOR
        );
        if (!this.page.author().isBlank()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("tooltip.ninecun_lingtai.book.author", this.page.author()),
                    this.width / 2,
                    this.panelTop + 27,
                    SECONDARY_TEXT_COLOR
            );
        }
        renderPageContent(guiGraphics);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.ninecun_lingtai.book.page",
                        this.page.pageIndex() + 1,
                        this.page.pageCount()
                ),
                this.width / 2,
                this.panelTop + this.panelHeight - 25,
                SECONDARY_TEXT_COLOR
        );
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && this.page.pageIndex() > 0) {
            requestPage(this.page.pageIndex() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT && this.page.pageIndex() + 1 < this.page.pageCount()) {
            requestPage(this.page.pageIndex() + 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public boolean isReading(net.minecraft.resources.ResourceLocation bookId) {
        return this.page.id().equals(bookId);
    }

    public void updatePage(BookPageData updatedPage) {
        this.page = updatedPage;
        this.awaitingPage = false;
        updateButtonState();
    }

    private void renderPageContent(GuiGraphics guiGraphics) {
        int contentLeft = this.panelLeft + 20;
        int contentTop = this.panelTop + 49;
        int contentWidth = this.panelWidth - 40;
        int contentBottom = this.panelTop + this.panelHeight - 43;
        List<FormattedCharSequence> lines = this.font.split(
                Component.literal(this.page.content()),
                contentWidth
        );
        int maximumLines = Math.max(1, (contentBottom - contentTop) / this.font.lineHeight);
        for (int index = 0; index < Math.min(lines.size(), maximumLines); index++) {
            guiGraphics.drawString(
                    this.font,
                    lines.get(index),
                    contentLeft,
                    contentTop + index * this.font.lineHeight,
                    TEXT_COLOR,
                    false
            );
        }
    }

    private void requestPage(int pageIndex) {
        if (this.awaitingPage || pageIndex < 0 || pageIndex >= this.page.pageCount()) {
            return;
        }
        this.awaitingPage = true;
        updateButtonState();
        ModNetwork.sendToServer(new RequestBookPagePacket(this.page.id(), pageIndex));
    }

    private void updateButtonState() {
        if (this.previousButton == null || this.nextButton == null) {
            return;
        }
        this.previousButton.active = !this.awaitingPage && this.page.pageIndex() > 0;
        this.nextButton.active = !this.awaitingPage
                && this.page.pageIndex() + 1 < this.page.pageCount();
    }
}
