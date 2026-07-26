package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.ImprintTechniqueToJadeSlipPacket;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TechniqueImprintScreen extends Screen {
    private static final int PREFERRED_PANEL_WIDTH = 340;
    private static final int PREFERRED_PANEL_HEIGHT = 260;
    private static final int MINIMUM_PANEL_WIDTH = 250;
    private static final int MINIMUM_PANEL_HEIGHT = 190;
    private static final int ROW_HEIGHT = 34;
    private static final int PANEL_COLOR = 0xF2181B24;
    private static final int PANEL_BORDER_COLOR = 0xFF75644D;
    private static final int ROW_COLOR = 0xD9282C38;
    private static final int ROW_HOVER_COLOR = 0xE23A3E4A;
    private static final int ROW_SELECTED_COLOR = 0xE252493A;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;
    private static final int MUTED_TEXT_COLOR = 0xFF817D75;
    private static final long DOUBLE_CLICK_INTERVAL_MILLIS = 300L;

    private final InteractionHand hand;
    private final List<TechniqueClientData> techniques;

    private EditBox searchBox;
    private Button imprintButton;
    private String searchQuery = "";
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int scrollRow;
    private ResourceLocation selectedTechniqueId;
    private ResourceLocation lastClickedTechniqueId;
    private long lastClickTime;

    public TechniqueImprintScreen(
            InteractionHand hand,
            List<TechniqueClientData> techniques
    ) {
        super(Component.translatable("screen.ninecun_lingtai.imprint_technique"));
        this.hand = hand;
        this.techniques = techniques.stream()
                .sorted(Comparator.comparing(TechniqueClientData::name)
                        .thenComparing(technique -> technique.id().toString()))
                .toList();
    }

    @Override
    protected void init() {
        this.panelWidth = Mth.clamp(this.width - 34, MINIMUM_PANEL_WIDTH, PREFERRED_PANEL_WIDTH);
        this.panelHeight = Mth.clamp(this.height - 34, MINIMUM_PANEL_HEIGHT, PREFERRED_PANEL_HEIGHT);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;

        this.searchBox = new EditBox(
                this.font,
                this.panelLeft + 12,
                this.panelTop + 30,
                this.panelWidth - 24,
                18,
                Component.translatable("screen.ninecun_lingtai.search_techniques")
        );
        this.searchBox.setMaxLength(80);
        this.searchBox.setHint(Component.translatable("screen.ninecun_lingtai.search_techniques"));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(updatedQuery -> {
            this.searchQuery = updatedQuery;
            this.scrollRow = 0;
            this.selectedTechniqueId = null;
        });
        this.addRenderableWidget(this.searchBox);

        this.imprintButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.imprint_to_jade_slip"),
                        button -> imprintSelectedTechnique()
                )
                .bounds(
                        this.panelLeft + (this.panelWidth - 100) / 2,
                        this.panelTop + this.panelHeight - 30,
                        100,
                        20
                )
                .build();
        this.imprintButton.active = false;
        this.addRenderableWidget(this.imprintButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        renderPanel(guiGraphics, mouseX, mouseY);
        this.imprintButton.active = this.selectedTechniqueId != null;
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= this.panelLeft + 10
                && mouseX < this.panelLeft + this.panelWidth - 10
                && mouseY >= getListTop()
                && mouseY < getListBottom()) {
            int visibleRowIndex = ((int) mouseY - getListTop()) / ROW_HEIGHT;
            int techniqueIndex = this.scrollRow + visibleRowIndex;
            List<TechniqueClientData> filteredTechniques = getFilteredTechniques();
            if (visibleRowIndex < getVisibleRows()
                    && techniqueIndex >= 0
                    && techniqueIndex < filteredTechniques.size()) {
                TechniqueClientData technique = filteredTechniques.get(techniqueIndex);
                long clickTime = Util.getMillis();
                boolean doubleClick = technique.id().equals(this.lastClickedTechniqueId)
                        && clickTime - this.lastClickTime <= DOUBLE_CLICK_INTERVAL_MILLIS;
                this.selectedTechniqueId = technique.id();
                this.lastClickedTechniqueId = technique.id();
                this.lastClickTime = clickTime;
                if (doubleClick) {
                    imprintSelectedTechnique();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<TechniqueClientData> filteredTechniques = getFilteredTechniques();
        int maximumScrollRow = Math.max(0, filteredTechniques.size() - getVisibleRows());
        if (maximumScrollRow <= 0) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        this.scrollRow = Mth.clamp(
                this.scrollRow - (int) Math.signum(delta),
                0,
                maximumScrollRow
        );
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && this.selectedTechniqueId != null) {
            imprintSelectedTechnique();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int panelRight = this.panelLeft + this.panelWidth;
        int panelBottom = this.panelTop + this.panelHeight;
        guiGraphics.fill(this.panelLeft, this.panelTop, panelRight, panelBottom, PANEL_COLOR);
        guiGraphics.renderOutline(
                this.panelLeft,
                this.panelTop,
                this.panelWidth,
                this.panelHeight,
                PANEL_BORDER_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.panelLeft + this.panelWidth / 2,
                this.panelTop + 10,
                PRIMARY_TEXT_COLOR
        );

        int listTop = getListTop();
        int listBottom = getListBottom();
        List<TechniqueClientData> filteredTechniques = getFilteredTechniques();
        int visibleRows = getVisibleRows();
        int maximumScrollRow = Math.max(0, filteredTechniques.size() - visibleRows);
        this.scrollRow = Mth.clamp(this.scrollRow, 0, maximumScrollRow);

        guiGraphics.enableScissor(this.panelLeft + 8, listTop, panelRight - 8, listBottom);
        for (int visibleRowIndex = 0; visibleRowIndex < visibleRows; visibleRowIndex++) {
            int techniqueIndex = this.scrollRow + visibleRowIndex;
            if (techniqueIndex >= filteredTechniques.size()) {
                break;
            }
            TechniqueClientData technique = filteredTechniques.get(techniqueIndex);
            int rowTop = listTop + visibleRowIndex * ROW_HEIGHT;
            boolean hovered = mouseX >= this.panelLeft + 10
                    && mouseX < panelRight - 10
                    && mouseY >= rowTop
                    && mouseY < rowTop + ROW_HEIGHT - 3;
            int rowColor = technique.id().equals(this.selectedTechniqueId)
                    ? ROW_SELECTED_COLOR
                    : (hovered ? ROW_HOVER_COLOR : ROW_COLOR);
            guiGraphics.fill(
                    this.panelLeft + 10,
                    rowTop,
                    panelRight - 10,
                    rowTop + ROW_HEIGHT - 3,
                    rowColor
            );
            guiGraphics.drawString(
                    this.font,
                    technique.name(),
                    this.panelLeft + 17,
                    rowTop + 5,
                    PRIMARY_TEXT_COLOR,
                    false
            );
            String description = normalizeText(technique.description());
            int descriptionWidth = this.panelWidth - 38;
            if (this.font.width(description) > descriptionWidth) {
                description = this.font.plainSubstrByWidth(
                        description,
                        Math.max(0, descriptionWidth - this.font.width("…"))
                ) + "…";
            }
            guiGraphics.drawString(
                    this.font,
                    description.isBlank() ? technique.id().toString() : description,
                    this.panelLeft + 17,
                    rowTop + 18,
                    SECONDARY_TEXT_COLOR,
                    false
            );
        }
        guiGraphics.disableScissor();

        if (filteredTechniques.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.no_matching_techniques"),
                    this.panelLeft + this.panelWidth / 2,
                    listTop + 18,
                    MUTED_TEXT_COLOR
            );
        }
        renderScrollBar(guiGraphics, filteredTechniques.size(), visibleRows, listTop, listBottom);
    }

    private void renderScrollBar(
            GuiGraphics guiGraphics,
            int techniqueCount,
            int visibleRows,
            int listTop,
            int listBottom
    ) {
        int maximumScrollRow = Math.max(0, techniqueCount - visibleRows);
        if (maximumScrollRow <= 0) {
            return;
        }
        int trackLeft = this.panelLeft + this.panelWidth - 7;
        int trackHeight = listBottom - listTop;
        int thumbHeight = Math.max(12, trackHeight * visibleRows / techniqueCount);
        int thumbTop = listTop + (trackHeight - thumbHeight) * this.scrollRow / maximumScrollRow;
        guiGraphics.fill(trackLeft, listTop, trackLeft + 2, listBottom, 0xFF2B2D35);
        guiGraphics.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFF9C8768);
    }

    private List<TechniqueClientData> getFilteredTechniques() {
        String normalizedQuery = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return this.techniques;
        }
        return this.techniques.stream()
                .filter(technique -> technique.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || technique.description().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || technique.id().toString().contains(normalizedQuery))
                .toList();
    }

    private void imprintSelectedTechnique() {
        if (this.selectedTechniqueId == null) {
            return;
        }
        ModNetwork.sendToServer(new ImprintTechniqueToJadeSlipPacket(
                this.selectedTechniqueId,
                this.hand
        ));
        this.onClose();
    }

    private int getListTop() {
        return this.panelTop + 55;
    }

    private int getListBottom() {
        return this.panelTop + this.panelHeight - 40;
    }

    private int getVisibleRows() {
        return Math.max(1, (getListBottom() - getListTop()) / ROW_HEIGHT);
    }

    private static String normalizeText(String text) {
        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
