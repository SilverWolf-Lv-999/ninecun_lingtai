package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.api.technique.LearnedTechniqueClientData;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueComprehension;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import net.seraphina.ninecun_lingtai.client.menu.TechniqueLibraryMenu;
import net.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import net.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TechniqueLibraryScreen extends AbstractContainerScreen<TechniqueLibraryMenu> {
    private static final int PREFERRED_IMAGE_WIDTH = 320;
    private static final int PREFERRED_IMAGE_HEIGHT = 250;
    private static final int MINIMUM_IMAGE_WIDTH = 250;
    private static final int MINIMUM_IMAGE_HEIGHT = 190;
    private static final int ROW_HEIGHT = 34;
    private static final int PANEL_COLOR = 0xF2181B24;
    private static final int PANEL_BORDER_COLOR = 0xFF75644D;
    private static final int ROW_COLOR = 0xD9282C38;
    private static final int ROW_HOVER_COLOR = 0xE23A3E4A;
    private static final int ROW_SELECTED_COLOR = 0xE252493A;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;
    private static final int MUTED_TEXT_COLOR = 0xFF817D75;
    private static final int STATUS_TEXT_COLOR = 0xFFDFC98A;
    private static final int PROGRESS_TRACK_COLOR = 0xFF2B2D35;
    private static final int PROGRESS_FILL_COLOR = 0xFF9C8768;
    private static final int PROGRESS_FULL_COLOR = 0xFFD9B968;
    private static final long DOUBLE_CLICK_INTERVAL_MILLIS = 300L;
    private static final int REFRESH_INTERVAL_TICKS = 20;

    private EditBox searchBox;
    private String searchQuery = "";
    private int scrollRow;
    private int refreshTicks;
    private ResourceLocation selectedTechniqueId;
    private ResourceLocation lastClickedTechniqueId;
    private long lastClickTime;

    public TechniqueLibraryScreen() {
        super(
                new TechniqueLibraryMenu(),
                Minecraft.getInstance().player.getInventory(),
                Component.translatable("screen.ninecun_lingtai.technique_library")
        );
        this.imageWidth = PREFERRED_IMAGE_WIDTH;
        this.imageHeight = PREFERRED_IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 34, MINIMUM_IMAGE_WIDTH, PREFERRED_IMAGE_WIDTH);
        this.imageHeight = Mth.clamp(this.height - 34, MINIMUM_IMAGE_HEIGHT, PREFERRED_IMAGE_HEIGHT);
        super.init();
        this.searchBox = new EditBox(
                this.font,
                this.leftPos + 12,
                this.topPos + 30,
                this.imageWidth - 24,
                18,
                Component.translatable("screen.ninecun_lingtai.search_techniques")
        );
        this.searchBox.setMaxLength(80);
        this.searchBox.setHint(Component.translatable("screen.ninecun_lingtai.search_techniques"));
        this.searchBox.setValue(this.searchQuery);
        this.searchBox.setResponder(updatedQuery -> {
            this.searchQuery = updatedQuery;
            this.scrollRow = 0;
        });
        this.addRenderableWidget(this.searchBox);
    }

    @Override
    protected void containerTick() {
        this.refreshTicks++;
        if (this.refreshTicks >= REFRESH_INTERVAL_TICKS) {
            this.refreshTicks = 0;
            ClientTechniqueManager.requestRefresh();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderRowTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int panelRight = this.leftPos + this.imageWidth;
        int panelBottom = this.topPos + this.imageHeight;
        guiGraphics.fill(
                this.leftPos,
                this.topPos,
                panelRight,
                panelBottom,
                PANEL_COLOR
        );
        guiGraphics.renderOutline(
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight,
                PANEL_BORDER_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.leftPos + this.imageWidth / 2,
                this.topPos + 10,
                PRIMARY_TEXT_COLOR
        );

        int listTop = getListTop();
        int listBottom = getListBottom();
        List<TechniqueClientData> filteredTechniques = getFilteredTechniques();
        int visibleRows = getVisibleRows();
        int maximumScrollRow = Math.max(0, filteredTechniques.size() - visibleRows);
        this.scrollRow = Mth.clamp(this.scrollRow, 0, maximumScrollRow);

        guiGraphics.enableScissor(this.leftPos + 8, listTop, panelRight - 8, listBottom);
        for (int visibleRowIndex = 0; visibleRowIndex < visibleRows; visibleRowIndex++) {
            int techniqueIndex = this.scrollRow + visibleRowIndex;
            if (techniqueIndex >= filteredTechniques.size()) {
                break;
            }
            TechniqueClientData technique = filteredTechniques.get(techniqueIndex);
            int rowTop = listTop + visibleRowIndex * ROW_HEIGHT;
            boolean hovered = mouseX >= this.leftPos + 10
                    && mouseX < panelRight - 10
                    && mouseY >= rowTop
                    && mouseY < rowTop + ROW_HEIGHT - 3;
            int rowColor = technique.id().equals(this.selectedTechniqueId)
                    ? ROW_SELECTED_COLOR
                    : (hovered ? ROW_HOVER_COLOR : ROW_COLOR);
            guiGraphics.fill(
                    this.leftPos + 10,
                    rowTop,
                    panelRight - 10,
                    rowTop + ROW_HEIGHT - 3,
                    rowColor
            );
            LearnedTechniqueClientData learnedTechnique =
                    ClientTechniqueManager.getLearnedTechniqueData(technique.id()).orElse(null);
            String statusText = getRowStatusText(learnedTechnique);
            int statusWidth = this.font.width(statusText);
            guiGraphics.drawString(
                    this.font,
                    statusText,
                    panelRight - 17 - statusWidth,
                    rowTop + 5,
                    learnedTechnique != null ? STATUS_TEXT_COLOR : MUTED_TEXT_COLOR,
                    false
            );
            String name = technique.name();
            int nameWidth = this.imageWidth - 38 - statusWidth - 6;
            if (this.font.width(name) > nameWidth) {
                name = this.font.plainSubstrByWidth(
                        name,
                        Math.max(0, nameWidth - this.font.width("…"))
                ) + "…";
            }
            guiGraphics.drawString(
                    this.font,
                    name,
                    this.leftPos + 17,
                    rowTop + 5,
                    PRIMARY_TEXT_COLOR,
                    false
            );
            String description = normalizeText(technique.description());
            int descriptionWidth = this.imageWidth - 38;
            if (this.font.width(description) > descriptionWidth) {
                description = this.font.plainSubstrByWidth(
                        description,
                        Math.max(0, descriptionWidth - this.font.width("…"))
                ) + "…";
            }
            guiGraphics.drawString(
                    this.font,
                    description.isBlank() ? technique.id().toString() : description,
                    this.leftPos + 17,
                    rowTop + 18,
                    SECONDARY_TEXT_COLOR,
                    false
            );
            if (learnedTechnique != null) {
                renderProficiencyBar(guiGraphics, learnedTechnique, rowTop, panelRight);
            }
        }
        guiGraphics.disableScissor();

        if (!ClientTechniqueManager.isInitialized()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.loading_techniques"),
                    this.leftPos + this.imageWidth / 2,
                    listTop + 18,
                    MUTED_TEXT_COLOR
            );
        } else if (filteredTechniques.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable(this.searchQuery.isBlank()
                            ? "screen.ninecun_lingtai.no_imprinted_techniques"
                            : "screen.ninecun_lingtai.no_matching_techniques"),
                    this.leftPos + this.imageWidth / 2,
                    listTop + 18,
                    MUTED_TEXT_COLOR
            );
        }

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.double_click_hint"),
                this.leftPos + this.imageWidth / 2,
                panelBottom - 12,
                MUTED_TEXT_COLOR
        );
        renderScrollBar(guiGraphics, filteredTechniques.size(), visibleRows, listTop, listBottom);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= this.leftPos + 10
                && mouseX < this.leftPos + this.imageWidth - 10
                && mouseY >= getListTop()
                && mouseY < getListBottom()) {
            int visibleRowIndex = ((int) mouseY - getListTop()) / ROW_HEIGHT;
            int techniqueIndex = this.scrollRow + visibleRowIndex;
            List<TechniqueClientData> filteredTechniques = getFilteredTechniques();
            if (techniqueIndex >= 0 && techniqueIndex < filteredTechniques.size()) {
                TechniqueClientData technique = filteredTechniques.get(techniqueIndex);
                long clickTime = Util.getMillis();
                boolean doubleClick = technique.id().equals(this.lastClickedTechniqueId)
                        && clickTime - this.lastClickTime <= DOUBLE_CLICK_INTERVAL_MILLIS;
                this.selectedTechniqueId = technique.id();
                this.lastClickedTechniqueId = technique.id();
                this.lastClickTime = clickTime;
                if (doubleClick && this.minecraft != null) {
                    this.minecraft.setScreen(new TechniqueReadingScreen(
                            technique,
                            this,
                            false
                    ));
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

    private List<TechniqueClientData> getFilteredTechniques() {
        String normalizedQuery = this.searchQuery.trim().toLowerCase(Locale.ROOT);
        return ClientTechniqueManager.getImprintedTechniques().stream()
                .filter(technique -> normalizedQuery.isEmpty()
                        || technique.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || technique.description().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || technique.content().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                        || technique.id().toString().contains(normalizedQuery))
                .sorted(Comparator.comparing(
                        technique -> !ClientTechniqueManager.isLearned(technique.id())
                ))
                .toList();
    }

    private String getRowStatusText(LearnedTechniqueClientData learnedTechnique) {
        if (learnedTechnique == null) {
            return Component.translatable("screen.ninecun_lingtai.library.not_learned").getString();
        }
        return Component.translatable(
                "screen.ninecun_lingtai.library.status_learned",
                TechniqueMasteryStage.getDisplayName(learnedTechnique.mastery()),
                learnedTechnique.comprehensionScore()
        ).getString();
    }

    private void renderProficiencyBar(
            GuiGraphics guiGraphics,
            LearnedTechniqueClientData learnedTechnique,
            int rowTop,
            int panelRight
    ) {
        int barLeft = this.leftPos + 10;
        int barRight = panelRight - 10;
        int barTop = rowTop + ROW_HEIGHT - 5;
        int barBottom = rowTop + ROW_HEIGHT - 3;
        guiGraphics.fill(barLeft, barTop, barRight, barBottom, PROGRESS_TRACK_COLOR);
        float progress = getStageProgress(learnedTechnique);
        int fillWidth = Math.round((barRight - barLeft) * progress);
        if (fillWidth > 0) {
            guiGraphics.fill(
                    barLeft,
                    barTop,
                    barLeft + fillWidth,
                    barBottom,
                    progress >= 1.0F ? PROGRESS_FULL_COLOR : PROGRESS_FILL_COLOR
            );
        }
    }

    /** Progress within the current mastery stage toward the next one; 1 when at perfection. */
    private static float getStageProgress(LearnedTechniqueClientData learnedTechnique) {
        TechniqueMasteryStage stage = TechniqueMasteryStage.fromMastery(learnedTechnique.mastery())
                .orElse(TechniqueMasteryStage.getStageForProficiency(learnedTechnique.proficiency()));
        return stage.next()
                .map(nextStage -> {
                    long stageStart = stage.getRequiredProficiency();
                    long stageSpan = nextStage.getRequiredProficiency() - stageStart;
                    return Mth.clamp(
                            (float) (learnedTechnique.proficiency() - stageStart) / stageSpan,
                            0.0F,
                            1.0F
                    );
                })
                .orElse(1.0F);
    }

    private void renderRowTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        TechniqueClientData technique = getTechniqueAt(mouseX, mouseY);
        if (technique == null) {
            return;
        }
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(technique.name()));
        LearnedTechniqueClientData learnedTechnique =
                ClientTechniqueManager.getLearnedTechniqueData(technique.id()).orElse(null);
        if (learnedTechnique == null) {
            lines.add(Component.translatable("screen.ninecun_lingtai.library.tooltip.not_learned"));
        } else {
            lines.add(Component.translatable(
                    "screen.ninecun_lingtai.library.tooltip.stage",
                    TechniqueMasteryStage.getDisplayName(learnedTechnique.mastery())
            ));
            lines.add(Component.translatable(
                    "screen.ninecun_lingtai.library.tooltip.comprehension",
                    learnedTechnique.comprehensionScore(),
                    TechniqueComprehension.MAXIMUM_SCORE
            ));
            TechniqueMasteryStage stage = TechniqueMasteryStage.fromMastery(learnedTechnique.mastery())
                    .orElse(TechniqueMasteryStage.getStageForProficiency(learnedTechnique.proficiency()));
            String proficiencyText =
                    ChineseNumberUtility.toChineseInteger(learnedTechnique.proficiency());
            if (stage.next().isPresent()) {
                TechniqueMasteryStage nextStage = stage.next().get();
                lines.add(Component.translatable(
                        "screen.ninecun_lingtai.library.tooltip.proficiency",
                        proficiencyText,
                        ChineseNumberUtility.toChineseInteger(nextStage.getRequiredProficiency())
                ));
                lines.add(Component.translatable(
                        "screen.ninecun_lingtai.library.tooltip.next_stage",
                        nextStage.getChineseName()
                ));
            } else {
                lines.add(Component.translatable(
                        "screen.ninecun_lingtai.library.tooltip.proficiency_max",
                        proficiencyText
                ));
            }
        }
        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private TechniqueClientData getTechniqueAt(double mouseX, double mouseY) {
        if (mouseX < this.leftPos + 10
                || mouseX >= this.leftPos + this.imageWidth - 10
                || mouseY < getListTop()
                || mouseY >= getListBottom()) {
            return null;
        }
        int visibleRowIndex = ((int) mouseY - getListTop()) / ROW_HEIGHT;
        if ((int) mouseY - getListTop() - visibleRowIndex * ROW_HEIGHT >= ROW_HEIGHT - 3) {
            return null;
        }
        int techniqueIndex = this.scrollRow + visibleRowIndex;
        List<TechniqueClientData> filteredTechniques = getFilteredTechniques();
        return techniqueIndex >= 0 && techniqueIndex < filteredTechniques.size()
                ? filteredTechniques.get(techniqueIndex)
                : null;
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
        int trackLeft = this.leftPos + this.imageWidth - 7;
        int trackHeight = listBottom - listTop;
        int thumbHeight = Math.max(12, trackHeight * visibleRows / techniqueCount);
        int thumbTop = listTop + (trackHeight - thumbHeight) * this.scrollRow / maximumScrollRow;
        guiGraphics.fill(trackLeft, listTop, trackLeft + 2, listBottom, 0xFF2B2D35);
        guiGraphics.fill(trackLeft, thumbTop, trackLeft + 2, thumbTop + thumbHeight, 0xFF9C8768);
    }

    private int getListTop() {
        return this.topPos + 55;
    }

    private int getListBottom() {
        return this.topPos + this.imageHeight - 20;
    }

    private int getVisibleRows() {
        return Math.max(1, (getListBottom() - getListTop()) / ROW_HEIGHT);
    }

    private static String normalizeText(String text) {
        return text.replace('\n', ' ').replace('\r', ' ').trim();
    }
}
