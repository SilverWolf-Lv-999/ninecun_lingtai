package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.spell.SpellGrade;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueMasteryStage;
import com.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import com.seraphina.ninecun_lingtai.client.player.ClientSpellManager;
import com.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.Util;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 按下法术选择键（默认 R）后出现在屏幕左侧的择法列表。只列出玩家已参悟（学会）的法术类功法；
 * 呼吸法不带 spell 块，天然不会出现在此。
 */
public final class SpellSelectionScreen extends Screen {
    private static final float TITLE_FONT_SCALE = 0.5F;
    private static final float NAME_FONT_SCALE = 0.42F;
    private static final float DETAIL_FONT_SCALE = 0.28F;
    private static final float HINT_FONT_SCALE = 0.3F;
    private static final int NAME_ROW_HEIGHT = 20;
    private static final int DETAIL_ROW_HEIGHT = 13;
    private static final int LIST_LEFT = 40;
    private static final int COLUMN_GAP = 30;
    private static final int LIST_TOP = 52;
    private static final int PANEL_BACKGROUND_COLOR = 0x59000000;

    private record SpellEntry(TechniqueClientData technique, String detailText) {
    }

    private List<SpellEntry> spells = List.of();
    private int highlightedIndex;
    private int libraryRevision = -1;

    public SpellSelectionScreen() {
        super(Component.translatable("screen.ninecun_lingtai.spell_selection.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        refreshEntriesWhenChanged();
        long currentTime = Util.getMillis();
        renderPanel(guiGraphics);
        renderTitle(guiGraphics);
        if (!ClientTechniqueManager.isInitialized()) {
            drawScaledText(
                    guiGraphics,
                    techniqueText(Component.translatable("screen.ninecun_lingtai.loading_techniques")),
                    16.0F,
                    LIST_TOP,
                    HINT_FONT_SCALE,
                    TechniqueImprintRenderUtility.MUTED_GOLD_COLOR
            );
        } else if (this.spells.isEmpty()) {
            drawScaledText(
                    guiGraphics,
                    techniqueText(Component.translatable("screen.ninecun_lingtai.spell_selection.empty")),
                    16.0F,
                    LIST_TOP,
                    HINT_FONT_SCALE,
                    TechniqueImprintRenderUtility.MUTED_GOLD_COLOR
            );
        } else {
            int hoveredIndex = getColumnAt(mouseX, mouseY);
            if (hoveredIndex >= 0) {
                this.highlightedIndex = hoveredIndex;
            }
            for (int spellIndex = 0; spellIndex < this.spells.size(); spellIndex++) {
                renderSpellColumn(guiGraphics, spellIndex, currentTime);
            }
        }
        renderHint(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int clickedIndex = getColumnAt(mouseX, mouseY);
            if (clickedIndex >= 0) {
                confirmSelection(clickedIndex);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeyMappings.SPELL_SELECTION.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (this.spells.isEmpty()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_A -> {
                this.highlightedIndex = Math.floorMod(this.highlightedIndex - 1, this.spells.size());
                return true;
            }
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_D -> {
                this.highlightedIndex = Math.floorMod(this.highlightedIndex + 1, this.spells.size());
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
                confirmSelection(this.highlightedIndex);
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    private void refreshEntriesWhenChanged() {
        if (ClientTechniqueManager.getRevision() == this.libraryRevision) {
            return;
        }
        this.libraryRevision = ClientTechniqueManager.getRevision();
        this.spells = ClientTechniqueManager.getLearnedTechniques().stream()
                .filter(TechniqueClientData::isSpellTechnique)
                .filter(technique -> !technique.isBreathingTechnique())
                .map(technique -> new SpellEntry(technique, buildDetailText(technique)))
                .toList();
        if (this.spells.isEmpty()) {
            this.highlightedIndex = 0;
            return;
        }
        int selectedIndex = ClientSpellManager.getSelectedSpell()
                .map(selectedId -> {
                    for (int spellIndex = 0; spellIndex < this.spells.size(); spellIndex++) {
                        if (this.spells.get(spellIndex).technique().id().equals(selectedId)) {
                            return spellIndex;
                        }
                    }
                    return 0;
                })
                .orElse(0);
        this.highlightedIndex = Mth.clamp(selectedIndex, 0, this.spells.size() - 1);
    }

    private static String buildDetailText(TechniqueClientData technique) {
        String gradeName = SpellGrade.fromRank(technique.spellGrade())
                .map(SpellGrade::getChineseName)
                .orElse("");
        String masteryName = ClientTechniqueManager.getLearnedTechniqueData(technique.id())
                .map(learned -> TechniqueMasteryStage.getDisplayName(learned.mastery()))
                .orElse("");
        if (gradeName.isEmpty()) {
            return masteryName;
        }
        return masteryName.isEmpty() ? gradeName : gradeName + "·" + masteryName;
    }

    private void confirmSelection(int spellIndex) {
        ClientSpellManager.selectSpell(this.spells.get(spellIndex).technique().id());
        onClose();
    }

    private void renderPanel(GuiGraphics guiGraphics) {
        int panelWidth = getPanelWidth();
        guiGraphics.fill(0, 0, panelWidth, this.height, PANEL_BACKGROUND_COLOR);
        guiGraphics.fill(
                panelWidth,
                0,
                panelWidth + 1,
                this.height,
                ColorUtility.withAlpha(TechniqueImprintRenderUtility.GOLD_COLOR, 0.45F)
        );
    }

    private void renderTitle(GuiGraphics guiGraphics) {
        drawScaledText(
                guiGraphics,
                techniqueText(this.title),
                14.0F,
                16.0F,
                TITLE_FONT_SCALE,
                TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
        );
    }

    private void renderSpellColumn(GuiGraphics guiGraphics, int spellIndex, long currentTime) {
        SpellEntry entry = this.spells.get(spellIndex);
        float columnX = getColumnCenterX(spellIndex);
        boolean highlighted = spellIndex == this.highlightedIndex;
        boolean selected = ClientSpellManager.getSelectedSpell()
                .map(entry.technique().id()::equals)
                .orElse(false);
        if (selected || highlighted) {
            TechniqueImprintRenderUtility.drawSeal(
                    guiGraphics,
                    columnX,
                    LIST_TOP - 13.0F,
                    5.0F,
                    selected,
                    highlighted,
                    currentTime,
                    1.0F
            );
        }
        int nameColor = highlighted || selected
                ? TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR
                : TechniqueImprintRenderUtility.GOLD_COLOR;
        int maximumNameRows = getMaximumNameRows();
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                entry.technique().name(),
                columnX,
                LIST_TOP,
                NAME_FONT_SCALE,
                NAME_ROW_HEIGHT,
                nameColor,
                highlighted || selected ? 1.0F : 0.82F,
                maximumNameRows,
                false
        );
        int nameRows = Math.min(
                entry.technique().name().codePointCount(0, entry.technique().name().length()),
                maximumNameRows
        );
        TechniqueImprintRenderUtility.drawVerticalText(
                guiGraphics,
                this.font,
                entry.detailText(),
                columnX,
                LIST_TOP + nameRows * NAME_ROW_HEIGHT + 8.0F,
                DETAIL_FONT_SCALE,
                DETAIL_ROW_HEIGHT,
                TechniqueImprintRenderUtility.MUTED_GOLD_COLOR,
                highlighted || selected ? 1.0F : 0.75F,
                entry.detailText().length(),
                false
        );
    }

    private void renderHint(GuiGraphics guiGraphics) {
        drawScaledText(
                guiGraphics,
                techniqueText(Component.translatable("screen.ninecun_lingtai.spell_selection.hint")),
                12.0F,
                this.height - 20.0F,
                HINT_FONT_SCALE,
                TechniqueImprintRenderUtility.MUTED_GOLD_COLOR
        );
    }

    private int getColumnAt(double mouseX, double mouseY) {
        for (int spellIndex = 0; spellIndex < this.spells.size(); spellIndex++) {
            float columnX = getColumnCenterX(spellIndex);
            if (TechniqueImprintRenderUtility.isInside(
                    mouseX,
                    mouseY,
                    Math.round(columnX) - COLUMN_GAP / 2,
                    LIST_TOP - 22,
                    COLUMN_GAP,
                    getColumnHeight(spellIndex) + 22
            )) {
                return spellIndex;
            }
        }
        return -1;
    }

    private float getColumnCenterX(int spellIndex) {
        return LIST_LEFT + spellIndex * COLUMN_GAP;
    }

    private int getColumnHeight(int spellIndex) {
        SpellEntry entry = this.spells.get(spellIndex);
        int nameRows = Math.min(
                entry.technique().name().codePointCount(0, entry.technique().name().length()),
                getMaximumNameRows()
        );
        return nameRows * NAME_ROW_HEIGHT + 8 + entry.detailText().length() * DETAIL_ROW_HEIGHT;
    }

    private int getMaximumNameRows() {
        return Math.max(
                1,
                (this.height - LIST_TOP - 60 - 4 * DETAIL_ROW_HEIGHT) / NAME_ROW_HEIGHT
        );
    }

    private int getPanelWidth() {
        int columnCount = Math.max(1, this.spells.size());
        return Mth.clamp(
                LIST_LEFT + (columnCount - 1) * COLUMN_GAP + COLUMN_GAP / 2 + 12,
                96,
                this.width / 2
        );
    }

    private Component techniqueText(Component text) {
        return text.copy().withStyle(style -> style.withFont(
                com.seraphina.ninecun_lingtai.common.font.CustomFontConstants.TECHNIQUE_FONT_ID
        ));
    }

    private void drawScaledText(
            GuiGraphics guiGraphics,
            Component text,
            float x,
            float y,
            float scale,
            int color
    ) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, ColorUtility.withAlpha(color, 1.0F), false);
        guiGraphics.pose().popPose();
    }
}
