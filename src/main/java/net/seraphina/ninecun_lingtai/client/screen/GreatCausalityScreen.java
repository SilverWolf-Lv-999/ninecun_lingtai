package net.seraphina.ninecun_lingtai.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.client.render.shader.screen.ScreenShaderEffects;
import net.seraphina.ninecun_lingtai.client.utility.Render2DUtility;
import net.seraphina.ninecun_lingtai.common.causality.CausalityPlayerSnapshot;
import net.seraphina.ninecun_lingtai.common.causality.CausalityRecord;
import net.seraphina.ninecun_lingtai.common.causality.CausalitySnapshot;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestGreatCausalityPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SetGreatCausalitySettingsPacket;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalitySettings;
import net.seraphina.ninecun_lingtai.common.player.GreatCausalityTargetMode;
import net.seraphina.ninecun_lingtai.utility.BezierUtility;
import org.lwjgl.glfw.GLFW;
import org.joml.Matrix4f;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

public final class GreatCausalityScreen extends Screen {
    private static final int VOID_BACKGROUND = 0xF20C0B0B;
    private static final int VOID_GRID = 0x183F3021;
    private static final int GOLD = 0xFFD9AE58;
    private static final int BRIGHT_GOLD = 0xFFFFD982;
    private static final int GOLD_LINE = 0xFFB17A2C;
    private static final int AQUA = 0xFF8FB9B0;
    private static final int AQUA_DARK = 0xFF385A55;
    private static final int PANEL = 0xE6151212;
    private static final int PANEL_RAISED = 0xE91E1917;
    private static final int PANEL_HOVER = 0xF02C241B;
    private static final int TEXT = 0xFFF5EEE1;
    private static final int MUTED_TEXT = 0xFFC7BBA8;
    private static final int FAINT_TEXT = 0xFF8B8174;
    private static final int MAX_LINE_NODES = 12;
    private static final int EMPTY_LINE_NODES = 5;
    private static final int MAX_VISIBLE_RECORDS = 8;
    private static final int HEADER_HEIGHT = 84;
    private static final int TIMELINE_LABEL_HEIGHT = 32;
    private static final int SETTINGS_TOP = 96;
    private static final int SETTINGS_VIEWPORT_TOP = 88;
    private static final int SETTINGS_VIEWPORT_BOTTOM_MARGIN = 14;
    private static final int SETTINGS_MAX_WIDTH = 1120;
    private static final int SETTINGS_COLUMN_GAP = 20;
    private static final int SETTINGS_SECTION_GAP = 14;
    private static final int SETTINGS_SECTION_TITLE_HEIGHT = 44;
    private static final int SETTINGS_ROW_HEIGHT = 44;
    private static final int SETTINGS_ROW_FILL_HEIGHT = 42;
    private static final int SETTINGS_TARGET_HEIGHT = 44;
    private static final int SETTINGS_SCROLL_STEP = 44;
    private static final int SETTINGS_TRACK_WIDTH = 40;
    private static final int SETTINGS_TRACK_HEIGHT = 16;
    private static final int SETTINGS_KNOB_SIZE = 12;
    private static final int CARD_RADIUS = 8;
    private static final int PLAYER_CARD_HEIGHT = 66;
    private static final int PLAYER_CARD_GAP = 12;
    private static final int PLAYER_ROW_HEIGHT = 34;
    private static final long TOGGLE_ANIMATION_DURATION = 420L;
    private static final long VIEW_TRANSITION_DURATION = 420L;
    private static final long VIEW_FADE_DURATION = 360L;
    private static final long INTRO_DURATION = 1_250L;
    private static final long NODE_APPEAR_INTERVAL = 95L;
    private static final long NODE_APPEAR_DURATION = 480L;
    private static final long RECORD_APPEAR_INTERVAL = 280L;
    private static final long SENTENCE_APPEAR_INTERVAL = 155L;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss"
    ).withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter SHORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(
            "HH:mm:ss"
    ).withZone(ZoneId.systemDefault());

    private final CausalitySnapshot snapshot;
    private ViewMode mode;
    private ViewMode modeBeforeSettings;
    private GreatCausalitySettings settings;
    private final EnumMap<SettingToggle, ToggleAnimation> toggleAnimations = new EnumMap<>(
            SettingToggle.class
    );
    private long openedAt;
    private long viewStartedAt;
    private long transitionStartedAt;
    private long transitionSourceViewStartedAt;
    private ViewMode transitionFrom;
    private int selectedNodeIndex = -1;
    private int recordScrollOffset;
    private int playerScrollOffset;
    private int settingsScrollOffset;
    private int lastMouseX;
    private int lastMouseY;

    public GreatCausalityScreen(CausalitySnapshot snapshot) {
        super(Component.literal("大因果术"));
        this.snapshot = snapshot;
        this.mode = snapshot.playerListMode() ? ViewMode.ENTRY : ViewMode.CAUSALITY_LINE;
        this.modeBeforeSettings = this.mode;
        this.settings = snapshot.settings();
    }

    @Override
    public void added() {
        super.added();
        this.openedAt = Util.getMillis();
        this.viewStartedAt = this.openedAt;
        ScreenShaderEffects.setAptitudeBlurActive(true);
    }

    @Override
    public void removed() {
        ScreenShaderEffects.setAptitudeBlurActive(false);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void onClose() {
        ScreenShaderEffects.setAptitudeBlurActive(false);
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        long currentTime = Util.getMillis();
        renderFullScreenAtmosphere(guiGraphics, currentTime);

        if (this.transitionFrom == ViewMode.CAUSALITY_LINE
                && this.mode == ViewMode.RECORD_FILE) {
            float transition = easeOut(progress(
                    currentTime - this.transitionStartedAt,
                    VIEW_FADE_DURATION
            ));
            renderCausalityLine(guiGraphics, currentTime, 1.0F - transition);
            renderRecordFile(guiGraphics, currentTime, transition);
            if (transition >= 1.0F) {
                this.transitionFrom = null;
            }
        } else if (this.transitionFrom == ViewMode.RECORD_FILE
                && this.mode == ViewMode.CAUSALITY_LINE) {
            float transition = easeOut(progress(
                    currentTime - this.transitionStartedAt,
                    VIEW_FADE_DURATION
            ));
            renderRecordFile(guiGraphics, currentTime, 1.0F - transition);
            renderCausalityLine(guiGraphics, currentTime, transition);
            if (transition >= 1.0F) {
                this.transitionFrom = null;
                this.selectedNodeIndex = -1;
            }
        } else {
            renderCurrentView(guiGraphics, currentTime, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (this.snapshot.settingsAvailable() && isSettingsButtonAt(mouseX, mouseY)) {
            toggleSettingsView();
            return true;
        }
        if (this.mode == ViewMode.SETTINGS) {
            return handleSettingsClick(mouseX, mouseY);
        }
        if (this.mode == ViewMode.ENTRY) {
            if (isEntryConfigurationAt(mouseX, mouseY)) {
                toggleSettingsView();
                return true;
            }
            if (isEntryCausalityAt(mouseX, mouseY)) {
                this.mode = ViewMode.PLAYER_LIST;
                this.viewStartedAt = Util.getMillis();
                this.playerScrollOffset = 0;
                return true;
            }
        } else if (this.mode == ViewMode.PLAYER_LIST) {
            int playerIndex = getPlayerIndexAt(mouseX, mouseY);
            if (playerIndex >= 0) {
                ModNetwork.sendToServer(new RequestGreatCausalityPacket(
                        this.snapshot.players().get(playerIndex).playerId()
                ));
                return true;
            }
        } else if (this.mode == ViewMode.CAUSALITY_LINE) {
            int nodeIndex = getNodeAt(mouseX, mouseY);
            if (nodeIndex >= 0) {
                this.selectedNodeIndex = nodeIndex;
                this.recordScrollOffset = 0;
                switchView(ViewMode.RECORD_FILE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.mode == ViewMode.SETTINGS && this.snapshot.settingsAvailable()) {
            int maximumOffset = getSettingsScrollMax();
            if (maximumOffset > 0) {
                this.settingsScrollOffset = Mth.clamp(
                        this.settingsScrollOffset - (int) Math.signum(delta) * SETTINGS_SCROLL_STEP,
                        0,
                        maximumOffset
                );
            }
            return true;
        }
        if (this.mode == ViewMode.PLAYER_LIST && !this.snapshot.players().isEmpty()) {
            int maximumOffset = Math.max(0, getPlayerRowCount() - getVisiblePlayerRows());
            this.playerScrollOffset = Mth.clamp(
                    this.playerScrollOffset - (int) Math.signum(delta),
                    0,
                    maximumOffset
            );
            return true;
        }
        if (this.mode == ViewMode.RECORD_FILE && !this.snapshot.records().isEmpty()) {
            int maximumOffset = Math.max(0, this.snapshot.records().size() - getVisibleRecordCount());
            this.recordScrollOffset = Mth.clamp(
                    this.recordScrollOffset - (int) Math.signum(delta),
                    0,
                    maximumOffset
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (this.mode == ViewMode.RECORD_FILE) {
            switchView(ViewMode.CAUSALITY_LINE);
            return true;
        }
        if (this.mode == ViewMode.PLAYER_LIST && this.snapshot.playerListMode()) {
            this.mode = ViewMode.ENTRY;
            this.viewStartedAt = Util.getMillis();
            return true;
        }
        if (this.mode == ViewMode.SETTINGS) {
            toggleSettingsView();
            return true;
        }
        onClose();
        return true;
    }

    private void renderCurrentView(
            GuiGraphics guiGraphics,
            long currentTime,
            int mouseX,
            int mouseY
    ) {
        if (this.mode == ViewMode.PLAYER_LIST) {
            renderPlayerList(guiGraphics, currentTime, mouseX, mouseY);
        } else if (this.mode == ViewMode.CAUSALITY_LINE) {
            renderCausalityLine(guiGraphics, currentTime);
        } else if (this.mode == ViewMode.RECORD_FILE) {
            renderRecordFile(guiGraphics, currentTime);
        } else if (this.mode == ViewMode.ENTRY) {
            renderEntry(guiGraphics, currentTime, mouseX, mouseY);
        } else {
            renderSettings(guiGraphics, currentTime, mouseX, mouseY);
        }
    }

    private void switchView(ViewMode nextMode) {
        if (this.mode == nextMode) {
            return;
        }
        this.transitionFrom = this.mode;
        this.transitionStartedAt = Util.getMillis();
        this.transitionSourceViewStartedAt = this.viewStartedAt;
        this.mode = nextMode;
        this.viewStartedAt = this.transitionStartedAt;
    }

    private void renderFullScreenAtmosphere(GuiGraphics guiGraphics, long currentTime) {
        Render2DUtility.drawRoundedRect(guiGraphics, 0.0F, 0.0F, this.width, this.height, 0.0F, VOID_BACKGROUND);

        for (int index = 0; index < 18; index++) {
            float x = Mth.frac(index * 0.371F + currentTime * 0.000012F) * this.width;
            float y = Mth.frac(index * 0.173F + currentTime * 0.000008F) * this.height;
            int size = index % 6 == 0 ? 2 : 1;
            guiGraphics.fill(
                    Mth.floor(x),
                    Mth.floor(y),
                    Mth.floor(x) + size,
                    Mth.floor(y) + size,
                    withAlpha(index % 2 == 0 ? GOLD : AQUA, 0.08F + (index % 3) * 0.025F)
            );
        }
        guiGraphics.fill(0, this.height - 1, this.width, this.height, withAlpha(AQUA_DARK, 0.22F));
    }

    private void renderEntry(
            GuiGraphics guiGraphics,
            long currentTime,
            int mouseX,
            int mouseY
    ) {
        float intro = easeOut(progress(currentTime - this.openedAt, INTRO_DURATION));
        float entryProgress = BezierUtility.cubicEase(
                progress(currentTime - this.viewStartedAt, VIEW_TRANSITION_DURATION),
                0.18F,
                0.92F
        );
        float alpha = intro * entryProgress;
        float centerX = this.width * 0.5F;
        float topNodeX = this.width / 3.0F;
        float topNodeY = this.height * 0.30F;
        float bottomNodeX = this.width * 2.0F / 3.0F;
        float bottomNodeY = this.height * 0.72F;
        boolean configurationHovered = isEntryConfigurationAt(mouseX, mouseY);
        boolean causalityHovered = isEntryCausalityAt(mouseX, mouseY);

        drawEntryTitle(guiGraphics, alpha);
        drawEntryDivider(
                guiGraphics,
                topNodeX,
                topNodeY,
                bottomNodeX,
                bottomNodeY,
                alpha
        );
        drawEntryOption(
                guiGraphics,
                "大因果术",
                this.snapshot.settingsAvailable()
                        ? "圆满术式 · 配置因果转接"
                        : "术式配置 · 尚未圆满",
                this.width * 0.25F,
                this.height * 0.52F,
                configurationHovered,
                alpha
        );
        drawEntryOption(
                guiGraphics,
                "推衍因果",
                "观当前界域 · 追溯众生因果",
                this.width * 0.75F,
                this.height * 0.52F,
                causalityHovered,
                alpha
        );
        guiGraphics.drawCenteredString(
                this.font,
                this.snapshot.players().isEmpty()
                        ? Component.literal("当前界域无可观测玩家")
                        : Component.literal("可观测因果线  " + this.snapshot.players().size()),
                Mth.floor(centerX),
                this.height - 76,
                withAlpha(FAINT_TEXT, alpha * 0.88F)
        );
    }

    private void drawEntryTitle(GuiGraphics guiGraphics, float alpha) {
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("大因果术"),
                this.width / 2,
                62,
                withAlpha(BRIGHT_GOLD, alpha)
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("逆天道，避因果；顺天道，成因果"),
                this.width / 2,
                82,
                withAlpha(MUTED_TEXT, alpha * 0.9F)
        );
        guiGraphics.fill(
                this.width / 2 - 44,
                104,
                this.width / 2 + 44,
                105,
                withAlpha(GOLD_LINE, alpha * 0.72F)
        );
    }

    private void drawEntryOption(
            GuiGraphics guiGraphics,
            String title,
            String subtitle,
            float centerX,
            float centerY,
            boolean hovered,
            float alpha
    ) {
        float lift = hovered ? 4.0F : 0.0F;
        int titleColor = hovered ? BRIGHT_GOLD : TEXT;
        int subtitleColor = hovered ? MUTED_TEXT : FAINT_TEXT;
        int lineColor = hovered ? GOLD : GOLD_LINE;
        int x = Mth.floor(centerX);
        int y = Mth.floor(centerY - lift);
        guiGraphics.fill(
                x - 82,
                y + 31,
                x + 82,
                y + 32,
                withAlpha(lineColor, alpha * (hovered ? 0.92F : 0.48F))
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal(title),
                x,
                y,
                withAlpha(titleColor, alpha)
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal(subtitle),
                x,
                y + 18,
                withAlpha(subtitleColor, alpha)
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal(hovered ? "点击进入" : "·"),
                x,
                y + 44,
                withAlpha(hovered ? GOLD : FAINT_TEXT, alpha * (hovered ? 1.0F : 0.55F))
        );
    }

    private void drawEntryDivider(
            GuiGraphics guiGraphics,
            float topNodeX,
            float topNodeY,
            float bottomNodeX,
            float bottomNodeY,
            float alpha
    ) {
        List<float[]> points = List.of(
                new float[]{topNodeX - this.width * 0.18F, topNodeY - this.height * 0.14F},
                new float[]{topNodeX, topNodeY},
                new float[]{bottomNodeX, bottomNodeY},
                new float[]{bottomNodeX + this.width * 0.18F, bottomNodeY + this.height * 0.14F}
        );
        drawSmoothPolyline(
                guiGraphics,
                points,
                9.0F,
                withAlpha(GOLD_LINE, alpha * 0.10F)
        );
        drawSmoothPolyline(
                guiGraphics,
                points,
                2.0F,
                withAlpha(BRIGHT_GOLD, alpha * 0.82F)
        );
        drawEntryNode(guiGraphics, topNodeX, topNodeY, alpha);
        drawEntryNode(guiGraphics, bottomNodeX, bottomNodeY, alpha);
    }

    private void drawEntryNode(GuiGraphics guiGraphics, float x, float y, float alpha) {
        float pulse = 1.0F + Mth.sin((Util.getMillis() % 1800L) / 1800.0F * Mth.TWO_PI) * 0.10F;
        Render2DUtility.drawCircle(
                guiGraphics,
                x,
                y,
                17.0F * pulse,
                withAlpha(GOLD_LINE, alpha * 0.18F)
        );
        Render2DUtility.drawCircle(
                guiGraphics,
                x,
                y,
                6.0F,
                withAlpha(BRIGHT_GOLD, alpha)
        );
        Render2DUtility.drawCircle(
                guiGraphics,
                x,
                y,
                2.0F,
                withAlpha(TEXT, alpha)
        );
    }

    private void renderPlayerList(GuiGraphics guiGraphics, long currentTime, int mouseX, int mouseY) {
        float intro = easeOut(progress(currentTime - this.openedAt, INTRO_DURATION));
        drawHeader(
                guiGraphics,
                "推衍因果 · 当前界域",
                "左键锁定一条因果线，查看其过去的行迹",
                currentTime
        );
        int contentLeft = getPageContentLeft();
        int contentRight = getPageContentRight();
        guiGraphics.drawString(
                this.font,
                "界域　" + getDimensionName(this.snapshot.players()),
                contentLeft + 8,
                88,
                withAlpha(MUTED_TEXT, intro)
        );
        String playerCount = "可观测因果线  " + this.snapshot.players().size();
        guiGraphics.drawString(
                this.font,
                playerCount,
                contentRight - this.font.width(playerCount) - 8,
                88,
                withAlpha(MUTED_TEXT, intro)
        );

        List<CausalityPlayerSnapshot> players = this.snapshot.players();
        int firstPlayerIndex = this.playerScrollOffset;
        int lastPlayerIndex = Math.min(players.size(), firstPlayerIndex + getVisiblePlayerRows());
        int lineX = contentLeft + 28;
        int listTop = 120;
        int listBottom = Math.min(this.height - 78, listTop + getVisiblePlayerRows() * PLAYER_ROW_HEIGHT);
        guiGraphics.fill(
                lineX,
                listTop,
                lineX + 1,
                listBottom,
                withAlpha(GOLD_LINE, intro * 0.46F)
        );
        for (int index = firstPlayerIndex; index < lastPlayerIndex; index++) {
            int row = index - firstPlayerIndex;
            long delay = row * 115L;
            float alpha = easeOut(progress(currentTime - this.openedAt - delay, 560L));
            if (alpha <= 0.0F) {
                continue;
            }
            int top = listTop + row * PLAYER_ROW_HEIGHT;
            boolean hovered = isInside(
                    mouseX,
                    mouseY,
                    contentLeft,
                    top,
                    contentRight - contentLeft,
                    PLAYER_ROW_HEIGHT
            );
            int textX = contentLeft + (hovered ? 50 : 46);
            int nodeColor = hovered ? BRIGHT_GOLD : GOLD;
            Render2DUtility.drawCircle(
                    guiGraphics,
                    lineX,
                    top + 16,
                    hovered ? 6.0F : 4.0F,
                    withAlpha(nodeColor, alpha)
            );
            guiGraphics.fill(
                    contentLeft + 52,
                    top + PLAYER_ROW_HEIGHT - 1,
                    contentRight - 8,
                    top + PLAYER_ROW_HEIGHT,
                    withAlpha(hovered ? GOLD_LINE : 0x604B4033, alpha * 0.72F)
            );
            guiGraphics.drawString(
                    this.font,
                    String.format("%02d  %s", index + 1, players.get(index).playerName()),
                    textX,
                    top + 5,
                    withAlpha(hovered ? BRIGHT_GOLD : TEXT, alpha)
            );
            guiGraphics.drawString(
                    this.font,
                    players.get(index).cultivation()
                            + "　·　"
                            + trimTextToWidth(players.get(index).dimension(), 190),
                    textX,
                    top + 19,
                    withAlpha(MUTED_TEXT, alpha)
            );
            guiGraphics.drawString(
                    this.font,
                    "→",
                    contentRight - 28,
                    top + 11,
                    withAlpha(hovered ? GOLD : FAINT_TEXT, alpha)
            );
        }
        drawFooter(
                guiGraphics,
                players.size() > getVisiblePlayerRows()
                        ? "滚轮翻阅玩家 · 左键展开 · ESC 返回入口"
                        : "左键展开玩家因果线 · ESC 返回入口"
        );
    }

    private void renderCausalityLine(GuiGraphics guiGraphics, long currentTime) {
        renderCausalityLine(guiGraphics, currentTime, -1.0F);
    }

    private void renderCausalityLine(
            GuiGraphics guiGraphics,
            long currentTime,
            float transitionAlpha
    ) {
        long animationStartedAt = this.transitionFrom == ViewMode.CAUSALITY_LINE
                && this.mode == ViewMode.RECORD_FILE
                ? this.transitionSourceViewStartedAt
                : this.viewStartedAt;
        float lineProgress = transitionAlpha < 0.0F
                ? easeOut(progress(currentTime - animationStartedAt, INTRO_DURATION))
                : transitionAlpha;
        drawHeader(
                guiGraphics,
                "大因果术 · 因果时间线",
                "点击时间节点，查看该节点附近的完整行为记录",
                currentTime,
                lineProgress
        );
        if (this.snapshot.target() != null) {
            guiGraphics.drawString(
                    this.font,
                    this.snapshot.target().playerName() + "  ·  " + this.snapshot.target().cultivation(),
                    52,
                    66,
                    withAlpha(TEXT, lineProgress)
            );
            guiGraphics.drawString(
                    this.font,
                    this.snapshot.fullHistory() ? "圆满：完整因果" : "推衍受限：仅显示可见时间段",
                    this.width - 260,
                    66,
                    withAlpha(this.snapshot.fullHistory() ? GOLD : MUTED_TEXT, lineProgress)
            );
        }

        int contentLeft = getPageContentLeft();
        drawUiPanel(
                guiGraphics,
                contentLeft,
                96,
                getPageContentRight() - contentLeft,
                Math.max(220, this.height - 160),
                10.0F,
                0xD1121825,
                0xD0090C13,
                0x2CFFFFFF,
                lineProgress * 0.96F
        );
        List<CausalityRecord> records = this.snapshot.records();
        drawTimeline(guiGraphics, records, currentTime, lineProgress, animationStartedAt);
        drawFooter(
                guiGraphics,
                records.isEmpty()
                        ? "当前没有已写入的行为 · ESC 关闭"
                        : "左键时间节点查看完整档案 · ESC 关闭"
        );
    }

    private void renderRecordFile(GuiGraphics guiGraphics, long currentTime) {
        renderRecordFile(guiGraphics, currentTime, 1.0F);
    }

    private void renderRecordFile(
            GuiGraphics guiGraphics,
            long currentTime,
            float transitionAlpha
    ) {
        long animationStartedAt = this.transitionFrom == ViewMode.RECORD_FILE
                && this.mode == ViewMode.CAUSALITY_LINE
                ? this.transitionSourceViewStartedAt
                : this.viewStartedAt;
        float reveal = easeOut(progress(currentTime - animationStartedAt, 780L)) * transitionAlpha;
        drawHeader(
                guiGraphics,
                "大因果术 · 因果档案",
                "时间节点附近的行为，按发生顺序逐句显现",
                currentTime,
                transitionAlpha
        );
        if (this.snapshot.target() != null) {
            guiGraphics.drawString(
                    this.font,
                    "观测对象：" + this.snapshot.target().playerName(),
                    52,
                    66,
                    withAlpha(TEXT, reveal)
            );
        }

        List<CausalityRecord> records = getVisibleFileRecords();
        if (records.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal("这个时间节点没有留下行为记录"),
                    this.width / 2,
                    this.height / 2,
                    withAlpha(MUTED_TEXT, reveal)
            );
            drawFooter(guiGraphics, "ESC 返回因果线");
            return;
        }

        int windowStart = getVisibleFileStart();
        int rowHeight = getRecordRowHeight();
        int contentLeft = getPageContentLeft();
        int contentRight = getPageContentRight();
        int columnGap = 14;
        int cardWidth = Math.max(220, (contentRight - contentLeft - columnGap) / 2);
        for (int index = 0; index < records.size(); index++) {
            CausalityRecord record = records.get(index);
            int absoluteIndex = windowStart + index;
            int column = index % 2;
            int row = index / 2;
            int left = contentLeft + column * (cardWidth + columnGap);
            int top = 104 + row * rowHeight;
            float recordAlpha = easeOut(progress(
                    currentTime - animationStartedAt - index * RECORD_APPEAR_INTERVAL,
                    520L
            ));
            if (recordAlpha <= 0.0F) {
                continue;
            }
            renderRecordPage(
                    guiGraphics,
                    record,
                    absoluteIndex,
                    left,
                    top,
                    cardWidth,
                    rowHeight,
                    currentTime,
                    animationStartedAt,
                    recordAlpha * reveal
            );
        }
        drawFooter(guiGraphics, "ESC 返回因果线 · 滚轮翻阅附近记录");
    }

    private void drawHeader(GuiGraphics guiGraphics, String title, String subtitle, long currentTime) {
        drawHeader(guiGraphics, title, subtitle, currentTime, 1.0F);
    }

    private void drawHeader(
            GuiGraphics guiGraphics,
            String title,
            String subtitle,
            long currentTime,
            float transitionAlpha
    ) {
        float alpha = easeOut(progress(currentTime - this.openedAt, 600L)) * transitionAlpha;
        int contentLeft = getPageContentLeft();
        int contentRight = getPageContentRight();
        int headerWidth = contentRight - contentLeft;
        drawUiPanel(
                guiGraphics,
                contentLeft,
                12,
                headerWidth,
                62,
                CARD_RADIUS,
                0xE91A1714,
                0xE60D0B0A,
                0x36D9AE58,
                alpha
        );
        guiGraphics.fill(
                contentLeft + 16,
                24,
                contentLeft + 19,
                52,
                withAlpha(GOLD, alpha)
        );
        guiGraphics.drawString(this.font, title, contentLeft + 30, 24, withAlpha(GOLD, alpha));
        guiGraphics.drawString(
                this.font,
                subtitle,
                contentLeft + 30,
                42,
                withAlpha(MUTED_TEXT, alpha * 0.9F)
        );
        guiGraphics.fill(
                contentLeft + 30,
                61,
                contentLeft + 130,
                62,
                withAlpha(GOLD_LINE, alpha * 0.75F)
        );
        if (this.snapshot.settingsAvailable()) {
            String settingsText = this.mode == ViewMode.SETTINGS ? "返回推衍" : "术式设置";
            int buttonWidth = this.font.width(settingsText) + 24;
            int buttonLeft = contentRight - buttonWidth;
            int buttonTop = 16;
            int buttonBottom = 44;
            boolean hovered = isSettingsButtonAt(this.lastMouseX, this.lastMouseY);
            Render2DUtility.drawRoundedVerticalGradientRect(
                    guiGraphics,
                    buttonLeft,
                    buttonTop,
                    buttonWidth,
                    buttonBottom - buttonTop,
                    6.0F,
                    withAlpha(hovered ? 0xFF4B3820 : 0xFF151413, alpha),
                    withAlpha(hovered ? 0xFF342719 : 0xFF0F0D0C, alpha)
            );
            Render2DUtility.drawOutlineRoundedRect(
                    guiGraphics,
                    buttonLeft,
                    buttonTop,
                    buttonWidth,
                    buttonBottom - buttonTop,
                    6.0F,
                    1.0F,
                    withAlpha(hovered ? GOLD : 0x36FFFFFF, alpha)
            );
            Render2DUtility.drawRoundedRect(
                    guiGraphics,
                    buttonLeft + 7,
                    buttonTop + 6,
                    2.0F,
                    buttonBottom - buttonTop - 12,
                    1.0F,
                    withAlpha(hovered ? BRIGHT_GOLD : GOLD_LINE, alpha)
            );
            guiGraphics.drawString(
                    this.font,
                    settingsText,
                    buttonLeft + 12,
                    25,
                    withAlpha(
                            hovered || this.mode == ViewMode.SETTINGS ? BRIGHT_GOLD : GOLD,
                            alpha
                    )
            );
        }
    }

    private void drawUiPanel(
            GuiGraphics guiGraphics,
            float x,
            float y,
            float width,
            float height,
            float radius,
            int topColor,
            int bottomColor,
            int borderColor,
            float alpha
    ) {
        Render2DUtility.drawDropShadow(
                guiGraphics,
                x,
                y,
                width,
                height,
                radius,
                0.0F,
                3.0F,
                12.0F,
                withAlpha(0x78000000, alpha * 0.72F)
        );
        Render2DUtility.drawRoundedVerticalGradientRect(
                guiGraphics,
                x,
                y,
                width,
                height,
                radius,
                withAlpha(topColor, alpha),
                withAlpha(bottomColor, alpha)
        );
        Render2DUtility.drawRoundedRect(
                guiGraphics,
                x + 1.0F,
                y + 1.0F,
                Math.max(0.0F, width - 2.0F),
                1.0F,
                0.5F,
                withAlpha(0x28FFFFFF, alpha)
        );
        Render2DUtility.drawOutlineRoundedRect(
                guiGraphics,
                x,
                y,
                width,
                height,
                radius,
                1.0F,
                withAlpha(borderColor, alpha)
        );
    }

    private void renderSettings(
            GuiGraphics guiGraphics,
            long currentTime,
            int mouseX,
            int mouseY
    ) {
        float viewProgress = BezierUtility.cubicEase(
                progress(currentTime - this.viewStartedAt, VIEW_TRANSITION_DURATION),
                0.22F,
                0.96F
        );
        float alpha = easeOut(progress(currentTime - this.openedAt, 600L)) * viewProgress;
        drawHeader(
                guiGraphics,
                "大因果术 · 圆满术式",
                "换因果、无因得果与篡改因果的完整配置",
                currentTime
        );
        if (!this.snapshot.settingsAvailable()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal("尚未圆满，无法开启附加因果效果"),
                    this.width / 2,
                    this.height / 2,
                    withAlpha(MUTED_TEXT, alpha)
            );
            return;
        }

        int viewportTop = getSettingsViewportTop();
        int viewportBottom = getSettingsViewportBottom();
        int contentLeft = getSettingsContentLeft();
        int contentRight = getPageContentRight();
        this.settingsScrollOffset = Mth.clamp(
                this.settingsScrollOffset,
                0,
                getSettingsScrollMax()
        );
        guiGraphics.enableScissor(
                contentLeft,
                viewportTop,
                contentRight,
                viewportBottom
        );
        renderSettingsGroup(
                guiGraphics,
                SettingsSection.TRANSFER,
                mouseX,
                mouseY,
                currentTime,
                alpha
        );
        renderSettingsGroup(
                guiGraphics,
                SettingsSection.NO_CAUSE,
                mouseX,
                mouseY,
                currentTime,
                alpha
        );
        renderSettingsGroup(
                guiGraphics,
                SettingsSection.TAMPER,
                mouseX,
                mouseY,
                currentTime,
                alpha
        );
        guiGraphics.disableScissor();
        drawSettingsScrollbar(guiGraphics, alpha);
    }

    private void renderSettingsGroup(
            GuiGraphics guiGraphics,
            SettingsSection section,
            int mouseX,
            int mouseY,
            long currentTime,
            float alpha
    ) {
        int x = getSettingsSectionX(section);
        int y = getSettingsSectionY(section);
        int width = getSettingsColumnWidth();
        renderSettingsSectionPanel(
                guiGraphics,
                section.title,
                section.subtitle,
                x,
                y,
                width,
                y + getSettingsSectionHeight(section),
                alpha
        );
        SettingToggle[] toggles = getSettingsToggles(section);
        for (int index = 0; index < toggles.length; index++) {
            renderToggleRow(
                    guiGraphics,
                    toggles[index],
                    x,
                    y + SETTINGS_SECTION_TITLE_HEIGHT + index * SETTINGS_ROW_HEIGHT,
                    width,
                    mouseX,
                    mouseY,
                    currentTime,
                    alpha
            );
        }
        if (section.hasTarget) {
            renderTargetSelector(
                    guiGraphics,
                    section == SettingsSection.TRANSFER,
                    x,
                    getSettingsTargetY(section),
                    width,
                    mouseX,
                    mouseY,
                    alpha
            );
        }
    }

    private void drawSettingsScrollbar(GuiGraphics guiGraphics, float alpha) {
        int maximumOffset = getSettingsScrollMax();
        if (maximumOffset <= 0) {
            return;
        }
        int viewportTop = getSettingsViewportTop();
        int viewportBottom = getSettingsViewportBottom();
        int viewportHeight = viewportBottom - viewportTop;
        int contentHeight = getSettingsContentHeight();
        int thumbHeight = Math.max(
                28,
                Math.round(viewportHeight * (viewportHeight / (float) contentHeight))
        );
        int thumbTravel = Math.max(0, viewportHeight - thumbHeight);
        int thumbTop = viewportTop + Math.round(
                thumbTravel * (this.settingsScrollOffset / (float) maximumOffset)
        );
        int scrollbarX = getPageContentRight() - 5;
        guiGraphics.fill(
                scrollbarX,
                viewportTop,
                scrollbarX + 2,
                viewportBottom,
                withAlpha(0x805B4933, alpha * 0.42F)
        );
        Render2DUtility.drawPill(
                guiGraphics,
                scrollbarX - 1,
                thumbTop,
                4.0F,
                thumbHeight,
                withAlpha(BRIGHT_GOLD, alpha * 0.78F),
                withAlpha(GOLD_LINE, alpha * 0.28F)
        );
    }

    private void renderSettingsSectionPanel(
            GuiGraphics guiGraphics,
            String title,
            String subtitle,
            int x,
            int y,
            int width,
            int bottom,
            float alpha
    ) {
        drawUiPanel(
                guiGraphics,
                x,
                y - 8,
                width,
                bottom - y + 8,
                CARD_RADIUS,
                0xE9211A15,
                PANEL,
                0x42D9AE58,
                alpha
        );
        guiGraphics.fill(
                x + 16,
                y + 4,
                x + 19,
                y + 28,
                withAlpha(GOLD, alpha * 0.86F)
        );
        guiGraphics.drawString(this.font, title, x + 30, y + 1, withAlpha(GOLD, alpha));
        guiGraphics.drawString(
                this.font,
                subtitle,
                x + 30,
                y + 19,
                withAlpha(MUTED_TEXT, alpha * 0.82F)
        );
        guiGraphics.fill(
                x + width - 20,
                y + 6,
                x + width - 18,
                y + 14,
                withAlpha(AQUA, alpha * 0.72F)
        );
    }

    private void renderToggleRow(
            GuiGraphics guiGraphics,
            SettingToggle toggle,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            long currentTime,
            float alpha
    ) {
        boolean enabled = isSettingEnabled(toggle);
        float toggleProgress = getToggleAnimationProgress(toggle, enabled, currentTime);
        boolean hovered = isInside(mouseX, mouseY, x, y, width, SETTINGS_ROW_FILL_HEIGHT);
        if (hovered) {
            guiGraphics.fill(
                    x + 8,
                    y + 1,
                    x + width - 8,
                    y + SETTINGS_ROW_FILL_HEIGHT - 1,
                    withAlpha(PANEL_HOVER, alpha * 0.72F)
            );
        }
        guiGraphics.fill(
                x + 8,
                y + 8,
                x + 10,
                y + SETTINGS_ROW_FILL_HEIGHT - 8,
                withAlpha(mixColor(0xFF59616B, AQUA, toggleProgress), alpha * 0.78F)
        );
        int trackX = x + 18;
        int trackY = y + 18;
        int trackOffColor = hovered ? 0xFF626B76 : 0xFF3C4651;
        int trackOnColor = hovered ? 0xFF65BDAF : AQUA_DARK;
        Render2DUtility.drawPill(
                guiGraphics,
                trackX - 2,
                trackY - 2,
                SETTINGS_TRACK_WIDTH + 4.0F,
                SETTINGS_TRACK_HEIGHT + 4.0F,
                withAlpha(0xAA000000, alpha * 0.5F),
                0
        );
        Render2DUtility.drawPill(
                guiGraphics,
                trackX,
                trackY,
                SETTINGS_TRACK_WIDTH,
                SETTINGS_TRACK_HEIGHT,
                withAlpha(mixColor(trackOffColor, trackOnColor, toggleProgress), alpha),
                withAlpha(0x35FFFFFF, alpha)
        );
        float knobX = trackX
                + SETTINGS_KNOB_SIZE * 0.5F
                + (SETTINGS_TRACK_WIDTH - SETTINGS_KNOB_SIZE) * toggleProgress;
        int knobColor = mixColor(0xFFD5D9DC, AQUA, toggleProgress);
        int knobGlow = mixColor(0xFF626D77, AQUA, toggleProgress);
        Render2DUtility.drawCircle(
                guiGraphics,
                knobX,
                trackY + SETTINGS_TRACK_HEIGHT * 0.5F,
                SETTINGS_KNOB_SIZE * 0.5F + 2.0F,
                withAlpha(knobGlow, alpha * (0.25F + toggleProgress * 0.32F))
        );
        Render2DUtility.drawCircle(
                guiGraphics,
                knobX,
                trackY + SETTINGS_TRACK_HEIGHT * 0.5F,
                SETTINGS_KNOB_SIZE * 0.5F,
                withAlpha(knobColor, alpha)
        );
        guiGraphics.drawString(
                this.font,
                toggle.displayName,
                x + 78,
                y + 9,
                withAlpha(hovered ? BRIGHT_GOLD : TEXT, alpha)
        );
        guiGraphics.drawString(
                this.font,
                toggle.description,
                x + 78,
                y + 29,
                withAlpha(hovered ? MUTED_TEXT : FAINT_TEXT, alpha * 0.86F)
        );
        guiGraphics.fill(
                x + 78,
                y + SETTINGS_ROW_FILL_HEIGHT - 4,
                x + width - 18,
                y + SETTINGS_ROW_FILL_HEIGHT - 3,
                withAlpha(mixColor(0xFF35404A, AQUA, toggleProgress), alpha * 0.38F)
        );
        float offAlpha = alpha * (1.0F - toggleProgress);
        float onAlpha = alpha * toggleProgress;
        guiGraphics.drawString(
                this.font,
                "关",
                x + width - 28,
                y + 18,
                withAlpha(FAINT_TEXT, offAlpha)
        );
        guiGraphics.drawString(
                this.font,
                "开",
                x + width - 28,
                y + 18,
                withAlpha(AQUA, onAlpha)
        );
    }

    private void renderTargetSelector(
            GuiGraphics guiGraphics,
            boolean transfer,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY,
            float alpha
    ) {
        GreatCausalityTargetMode targetMode = transfer
                ? this.settings.transferTargetMode()
                : this.settings.tamperTargetMode();
        UUID targetId = transfer ? this.settings.transferTargetId() : this.settings.tamperTargetId();
        boolean randomMode = targetMode == GreatCausalityTargetMode.RANDOM;
        String targetName = targetMode == GreatCausalityTargetMode.RANDOM
                ? "自动选择当前界域玩家"
                : getSettingsTargetName(targetId);
        int modeX = x + 128;
        int modeWidth = 70;
        int randomRight = modeX + modeWidth;
        int selectedLeft = randomRight + 8;
        int selectedRight = selectedLeft + modeWidth;
        int targetX = selectedRight + 12;
        int targetRight = x + width;
        int targetWidth = Math.max(64, targetRight - targetX);
        boolean randomHovered = isInside(mouseX, mouseY, modeX, y + 6, modeWidth, 32);
        boolean selectedHovered = isInside(mouseX, mouseY, selectedLeft, y + 6, modeWidth, 32);
        boolean targetHovered = mouseX >= targetX
                && mouseX <= targetRight
                && mouseY >= y
                && mouseY <= y + SETTINGS_TARGET_HEIGHT;
        guiGraphics.fill(
                x + 8,
                y,
                x + width - 8,
                y + SETTINGS_TARGET_HEIGHT - 1,
                withAlpha(PANEL_RAISED, alpha * 0.72F)
        );
        guiGraphics.fill(
                x + 78,
                y,
                x + width - 8,
                y + 1,
                withAlpha(0xFF59616B, alpha * 0.32F)
        );
        guiGraphics.drawString(
                this.font,
                "目标模式",
                x + 14,
                y + 10,
                withAlpha(MUTED_TEXT, alpha)
        );
        guiGraphics.drawString(
                this.font,
                "接收因果的对象",
                x + 14,
                y + 28,
                withAlpha(FAINT_TEXT, alpha * 0.84F)
        );
        Render2DUtility.drawPill(
                guiGraphics,
                modeX,
                y + 6,
                modeWidth,
                32.0F,
                withAlpha(randomMode ? 0xFF604823 : randomHovered ? 0xFF3B2D20 : 0xFF141312, alpha),
                withAlpha(randomMode ? GOLD : 0x36FFFFFF, alpha)
        );
        guiGraphics.drawString(
                this.font,
                "随机",
                modeX + (modeWidth - this.font.width("随机")) / 2,
                y + 17,
                withAlpha(randomMode ? BRIGHT_GOLD : randomHovered ? TEXT : MUTED_TEXT, alpha)
        );
        Render2DUtility.drawPill(
                guiGraphics,
                selectedLeft,
                y + 6,
                modeWidth,
                32.0F,
                withAlpha(!randomMode ? 0xFF3F5B52 : selectedHovered ? 0xFF263A36 : 0xFF141312, alpha),
                withAlpha(!randomMode ? AQUA : 0x36FFFFFF, alpha)
        );
        guiGraphics.drawString(
                this.font,
                "指定",
                selectedLeft + (modeWidth - this.font.width("指定")) / 2,
                y + 17,
                withAlpha(!randomMode ? BRIGHT_GOLD : selectedHovered ? TEXT : MUTED_TEXT, alpha)
        );
        Render2DUtility.drawPill(
                guiGraphics,
                targetX,
                y + 6,
                targetWidth,
                32.0F,
                withAlpha(targetHovered ? 0xFF3A2C1D : 0xFF11100E, alpha),
                withAlpha(targetHovered ? GOLD : 0x26FFFFFF, alpha)
        );
        guiGraphics.drawString(
                this.font,
                trimTextToWidth(targetName, Math.max(16, targetWidth - 24)),
                targetX + 12,
                y + 17,
                withAlpha(targetHovered ? BRIGHT_GOLD : TEXT, alpha)
        );
    }

    private boolean handleSettingsClick(double mouseX, double mouseY) {
        if (!this.snapshot.settingsAvailable()) {
            return false;
        }
        if (mouseY < getSettingsViewportTop() || mouseY > getSettingsViewportBottom()) {
            return false;
        }
        int columnWidth = getSettingsColumnWidth();
        for (SettingsSection section : SettingsSection.values()) {
            int x = getSettingsSectionX(section);
            int y = getSettingsSectionY(section);
            SettingToggle[] toggles = getSettingsToggles(section);
            for (int index = 0; index < toggles.length; index++) {
                int rowY = y
                        + SETTINGS_SECTION_TITLE_HEIGHT
                        + index * SETTINGS_ROW_HEIGHT;
                if (isInside(mouseX, mouseY, x, rowY, columnWidth, SETTINGS_ROW_FILL_HEIGHT)) {
                    toggleSetting(toggles[index]);
                    return true;
                }
            }
            if (section.hasTarget) {
                int targetY = getSettingsTargetY(section);
                if (isInside(
                        mouseX,
                        mouseY,
                        x,
                        targetY,
                        columnWidth,
                        SETTINGS_TARGET_HEIGHT
                )) {
                    if (mouseX < x + 276) {
                        cycleTargetMode(section == SettingsSection.TRANSFER);
                    } else {
                        cycleTarget(section == SettingsSection.TRANSFER);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void toggleSettingsView() {
        if (this.mode == ViewMode.SETTINGS) {
            this.mode = this.modeBeforeSettings;
        } else {
            this.modeBeforeSettings = this.mode;
            this.mode = ViewMode.SETTINGS;
            this.settingsScrollOffset = 0;
        }
        this.viewStartedAt = Util.getMillis();
    }

    private boolean isSettingsButtonAt(double mouseX, double mouseY) {
        int buttonLeft = getSettingsButtonLeft();
        String settingsText = this.mode == ViewMode.SETTINGS ? "返回推衍" : "术式设置";
        int buttonWidth = this.font.width(settingsText) + 24;
        return isInside(mouseX, mouseY, buttonLeft, 16, buttonWidth, 28);
    }

    private void cycleTargetMode(boolean transfer) {
        GreatCausalityTargetMode currentMode = transfer
                ? this.settings.transferTargetMode()
                : this.settings.tamperTargetMode();
        GreatCausalityTargetMode nextMode = currentMode == GreatCausalityTargetMode.RANDOM
                ? GreatCausalityTargetMode.SELECTED
                : GreatCausalityTargetMode.RANDOM;
        UUID targetId = transfer ? this.settings.transferTargetId() : this.settings.tamperTargetId();
        if (nextMode == GreatCausalityTargetMode.SELECTED
                && targetId == null
                && !this.snapshot.settingsTargets().isEmpty()) {
            targetId = this.snapshot.settingsTargets().get(0).playerId();
        }
        updateSettings(new GreatCausalitySettings(
                this.settings.transferAttack(),
                this.settings.transferHate(),
                this.settings.transferTribulation(),
                this.settings.transferNegativeEffects(),
                this.settings.transferVillagerPrice(),
                this.settings.transferDamage(),
                transfer ? nextMode : this.settings.transferTargetMode(),
                transfer ? targetId : this.settings.transferTargetId(),
                this.settings.randomItem(),
                this.settings.corruptBeneficialEffects(),
                this.settings.transferItems(),
                this.settings.transferAdvancements(),
                this.settings.voidCrafting(),
                transfer ? this.settings.tamperTargetMode() : nextMode,
                transfer ? this.settings.tamperTargetId() : targetId
        ));
    }

    private void cycleTarget(boolean transfer) {
        List<CausalityPlayerSnapshot> targets = this.snapshot.settingsTargets();
        if (targets.isEmpty()) {
            return;
        }
        UUID currentId = transfer ? this.settings.transferTargetId() : this.settings.tamperTargetId();
        int currentIndex = -1;
        for (int index = 0; index < targets.size(); index++) {
            if (targets.get(index).playerId().equals(currentId)) {
                currentIndex = index;
                break;
            }
        }
        UUID nextId = targets.get((currentIndex + 1) % targets.size()).playerId();
        updateSettings(new GreatCausalitySettings(
                this.settings.transferAttack(),
                this.settings.transferHate(),
                this.settings.transferTribulation(),
                this.settings.transferNegativeEffects(),
                this.settings.transferVillagerPrice(),
                this.settings.transferDamage(),
                transfer ? GreatCausalityTargetMode.SELECTED : this.settings.transferTargetMode(),
                transfer ? nextId : this.settings.transferTargetId(),
                this.settings.randomItem(),
                this.settings.corruptBeneficialEffects(),
                this.settings.transferItems(),
                this.settings.transferAdvancements(),
                this.settings.voidCrafting(),
                transfer ? this.settings.tamperTargetMode() : GreatCausalityTargetMode.SELECTED,
                transfer ? this.settings.tamperTargetId() : nextId
        ));
    }

    private void toggleSetting(SettingToggle toggle) {
        boolean transferAttack = this.settings.transferAttack();
        boolean transferHate = this.settings.transferHate();
        boolean transferTribulation = this.settings.transferTribulation();
        boolean transferNegativeEffects = this.settings.transferNegativeEffects();
        boolean transferVillagerPrice = this.settings.transferVillagerPrice();
        boolean transferDamage = this.settings.transferDamage();
        boolean randomItem = this.settings.randomItem();
        boolean corruptBeneficialEffects = this.settings.corruptBeneficialEffects();
        boolean transferItems = this.settings.transferItems();
        boolean transferAdvancements = this.settings.transferAdvancements();
        boolean voidCrafting = this.settings.voidCrafting();
        switch (toggle) {
            case TRANSFER_ATTACK -> transferAttack = !transferAttack;
            case TRANSFER_HATE -> transferHate = !transferHate;
            case TRANSFER_TRIBULATION -> transferTribulation = !transferTribulation;
            case TRANSFER_NEGATIVE_EFFECTS -> transferNegativeEffects = !transferNegativeEffects;
            case TRANSFER_VILLAGER_PRICE -> transferVillagerPrice = !transferVillagerPrice;
            case TRANSFER_DAMAGE -> transferDamage = !transferDamage;
            case RANDOM_ITEM -> randomItem = !randomItem;
            case CORRUPT_BENEFICIAL_EFFECTS -> corruptBeneficialEffects = !corruptBeneficialEffects;
            case TRANSFER_ITEMS -> transferItems = !transferItems;
            case TRANSFER_ADVANCEMENTS -> transferAdvancements = !transferAdvancements;
            case VOID_CRAFTING -> voidCrafting = !voidCrafting;
        }
        updateSettings(new GreatCausalitySettings(
                transferAttack,
                transferHate,
                transferTribulation,
                transferNegativeEffects,
                transferVillagerPrice,
                transferDamage,
                this.settings.transferTargetMode(),
                this.settings.transferTargetId(),
                randomItem,
                corruptBeneficialEffects,
                transferItems,
                transferAdvancements,
                voidCrafting,
                this.settings.tamperTargetMode(),
                this.settings.tamperTargetId()
        ));
    }

    private void updateSettings(GreatCausalitySettings settings) {
        this.settings = settings;
        ModNetwork.sendToServer(new SetGreatCausalitySettingsPacket(settings));
    }

    private boolean isSettingEnabled(SettingToggle toggle) {
        return switch (toggle) {
            case TRANSFER_ATTACK -> this.settings.transferAttack();
            case TRANSFER_HATE -> this.settings.transferHate();
            case TRANSFER_TRIBULATION -> this.settings.transferTribulation();
            case TRANSFER_NEGATIVE_EFFECTS -> this.settings.transferNegativeEffects();
            case TRANSFER_VILLAGER_PRICE -> this.settings.transferVillagerPrice();
            case TRANSFER_DAMAGE -> this.settings.transferDamage();
            case RANDOM_ITEM -> this.settings.randomItem();
            case CORRUPT_BENEFICIAL_EFFECTS -> this.settings.corruptBeneficialEffects();
            case TRANSFER_ITEMS -> this.settings.transferItems();
            case TRANSFER_ADVANCEMENTS -> this.settings.transferAdvancements();
            case VOID_CRAFTING -> this.settings.voidCrafting();
        };
    }

    private float getToggleAnimationProgress(
            SettingToggle toggle,
            boolean enabled,
            long currentTime
    ) {
        ToggleAnimation animation = this.toggleAnimations.get(toggle);
        if (animation == null) {
            animation = new ToggleAnimation(enabled ? 1.0F : 0.0F, currentTime);
            this.toggleAnimations.put(toggle, animation);
        }
        animation.updateTarget(enabled ? 1.0F : 0.0F, currentTime);
        return animation.getValue(currentTime);
    }

    private String getSettingsTargetName(UUID targetId) {
        if (targetId == null) {
            return "未选定";
        }
        return this.snapshot.settingsTargets().stream()
                .filter(target -> target.playerId().equals(targetId))
                .map(CausalityPlayerSnapshot::playerName)
                .findFirst()
                .orElse("目标已离线");
    }

    private int getSettingsSectionY(SettingsSection section) {
        return getSettingsSectionYUnscrolled(section) - this.settingsScrollOffset;
    }

    private int getSettingsSectionYUnscrolled(SettingsSection section) {
        int columnCount = getSettingsColumnCount();
        if (columnCount >= 3) {
            return SETTINGS_TOP;
        }
        if (columnCount == 2) {
            return section == SettingsSection.NO_CAUSE
                    ? SETTINGS_TOP + getSettingsSectionHeight(SettingsSection.TRANSFER)
                    + SETTINGS_SECTION_GAP
                    : SETTINGS_TOP;
        }
        return switch (section) {
            case TRANSFER -> SETTINGS_TOP;
            case NO_CAUSE -> SETTINGS_TOP
                    + getSettingsSectionHeight(SettingsSection.TRANSFER)
                    + SETTINGS_SECTION_GAP;
            case TAMPER -> SETTINGS_TOP
                    + getSettingsSectionHeight(SettingsSection.TRANSFER)
                    + SETTINGS_SECTION_GAP
                    + getSettingsSectionHeight(SettingsSection.NO_CAUSE)
                    + SETTINGS_SECTION_GAP;
        };
    }

    private int getSettingsSectionX(SettingsSection section) {
        int column = switch (section) {
            case TRANSFER -> 0;
            case NO_CAUSE -> getSettingsColumnCount() >= 3 ? 1 : 0;
            case TAMPER -> getSettingsColumnCount() >= 3
                    ? 2
                    : getSettingsColumnCount() == 2 ? 1 : 0;
        };
        return getSettingsContentLeft() + column * (getSettingsColumnWidth() + SETTINGS_COLUMN_GAP);
    }

    private int getSettingsTargetY(SettingsSection section) {
        return getSettingsSectionY(section)
                + SETTINGS_SECTION_TITLE_HEIGHT
                + getSettingsToggles(section).length * SETTINGS_ROW_HEIGHT
                + 8;
    }

    private int getSettingsSectionHeight(SettingsSection section) {
        return SETTINGS_SECTION_TITLE_HEIGHT
                + getSettingsToggles(section).length * SETTINGS_ROW_HEIGHT
                + (section.hasTarget ? 8 + SETTINGS_TARGET_HEIGHT : 0);
    }

    private SettingToggle[] getSettingsToggles(SettingsSection section) {
        return switch (section) {
            case TRANSFER -> new SettingToggle[]{
                    SettingToggle.TRANSFER_ATTACK,
                    SettingToggle.TRANSFER_HATE,
                    SettingToggle.TRANSFER_TRIBULATION,
                    SettingToggle.TRANSFER_NEGATIVE_EFFECTS,
                    SettingToggle.TRANSFER_VILLAGER_PRICE,
                    SettingToggle.TRANSFER_DAMAGE
            };
            case NO_CAUSE -> new SettingToggle[]{
                    SettingToggle.RANDOM_ITEM
            };
            case TAMPER -> new SettingToggle[]{
                    SettingToggle.CORRUPT_BENEFICIAL_EFFECTS,
                    SettingToggle.TRANSFER_ITEMS,
                    SettingToggle.TRANSFER_ADVANCEMENTS,
                    SettingToggle.VOID_CRAFTING
            };
        };
    }

    private int getSettingsColumnCount() {
        if (this.width >= 1_200) {
            return 3;
        }
        return this.width >= 900 ? 2 : 1;
    }

    private int getSettingsColumnWidth() {
        int contentWidth = Math.min(SETTINGS_MAX_WIDTH, Math.max(360, this.width - 80));
        int columnCount = getSettingsColumnCount();
        return Math.max(
                220,
                (contentWidth - SETTINGS_COLUMN_GAP * (columnCount - 1)) / columnCount
        );
    }

    private int getSettingsContentHeight() {
        int maximumBottom = SETTINGS_VIEWPORT_TOP;
        for (SettingsSection section : SettingsSection.values()) {
            maximumBottom = Math.max(
                    maximumBottom,
                    getSettingsSectionYUnscrolled(section) + getSettingsSectionHeight(section)
            );
        }
        return Math.max(
                getSettingsViewportBottom() - getSettingsViewportTop(),
                maximumBottom - getSettingsViewportTop() + 8
        );
    }

    private int getSettingsScrollMax() {
        return Math.max(
                0,
                getSettingsContentHeight() - (getSettingsViewportBottom() - getSettingsViewportTop())
        );
    }

    private int getSettingsViewportTop() {
        return SETTINGS_VIEWPORT_TOP;
    }

    private int getSettingsViewportBottom() {
        return Math.max(
                getSettingsViewportTop() + 80,
                this.height - SETTINGS_VIEWPORT_BOTTOM_MARGIN
        );
    }

    private int getSettingsContentLeft() {
        return getPageContentLeft();
    }

    private int getSettingsButtonLeft() {
        String settingsText = this.mode == ViewMode.SETTINGS ? "返回推衍" : "术式设置";
        return getPageContentRight() - this.font.width(settingsText) - 24;
    }

    private int getPageContentLeft() {
        int contentWidth = Math.min(SETTINGS_MAX_WIDTH, Math.max(360, this.width - 80));
        return Math.max(40, (this.width - contentWidth) / 2);
    }

    private int getPageContentRight() {
        int contentWidth = Math.min(SETTINGS_MAX_WIDTH, Math.max(360, this.width - 80));
        return getPageContentLeft() + contentWidth;
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                && mouseX <= x + width
                && mouseY >= y
                && mouseY <= y + height;
    }

    private static int mixColor(int firstColor, int secondColor, float progress) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        int firstAlpha = firstColor >>> 24 & 0xFF;
        int firstRed = firstColor >>> 16 & 0xFF;
        int firstGreen = firstColor >>> 8 & 0xFF;
        int firstBlue = firstColor & 0xFF;
        int secondAlpha = secondColor >>> 24 & 0xFF;
        int secondRed = secondColor >>> 16 & 0xFF;
        int secondGreen = secondColor >>> 8 & 0xFF;
        int secondBlue = secondColor & 0xFF;
        int alpha = Mth.lerpInt(clampedProgress, firstAlpha, secondAlpha);
        int red = Mth.lerpInt(clampedProgress, firstRed, secondRed);
        int green = Mth.lerpInt(clampedProgress, firstGreen, secondGreen);
        int blue = Mth.lerpInt(clampedProgress, firstBlue, secondBlue);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private void updateSettingsTargetSelection(
            GreatCausalityTargetMode transferMode,
            UUID transferTargetId,
            GreatCausalityTargetMode tamperMode,
            UUID tamperTargetId
    ) {
        updateSettings(new GreatCausalitySettings(
                this.settings.transferAttack(),
                this.settings.transferHate(),
                this.settings.transferTribulation(),
                this.settings.transferNegativeEffects(),
                this.settings.transferVillagerPrice(),
                this.settings.transferDamage(),
                transferMode,
                transferTargetId,
                this.settings.randomItem(),
                this.settings.corruptBeneficialEffects(),
                this.settings.transferItems(),
                this.settings.transferAdvancements(),
                this.settings.voidCrafting(),
                tamperMode,
                tamperTargetId
        ));
    }

    private void drawTimeline(
            GuiGraphics guiGraphics,
            List<CausalityRecord> records,
            long currentTime,
            float lineProgress,
            long animationStartedAt
    ) {
        int nodeCount = getNodeCount();
        float startX = getTimelineStartX();
        float endX = getTimelineEndX();
        float startY = getTimelineStartY();
        float endY = getTimelineEndY();
        float visibleProgress = Mth.clamp(lineProgress, 0.0F, 1.0F);

        guiGraphics.drawString(
                this.font,
                "过去",
                Mth.floor(startX),
                Mth.floor(startY - 34.0F),
                withAlpha(MUTED_TEXT, lineProgress)
        );
        guiGraphics.drawString(
                this.font,
                "现在",
                Mth.floor(endX - 28.0F),
                Mth.floor(endY + 28.0F),
                withAlpha(MUTED_TEXT, lineProgress)
        );
        drawTimelinePath(
                guiGraphics,
                startX,
                startY,
                endX,
                endY,
                visibleProgress,
                9,
                withAlpha(GOLD_LINE, 0.10F)
        );
        drawTimelinePath(
                guiGraphics,
                startX,
                startY,
                endX,
                endY,
                visibleProgress,
                4,
                withAlpha(GOLD_LINE, 0.42F)
        );
        drawTimelinePath(
                guiGraphics,
                startX,
                startY,
                endX,
                endY,
                visibleProgress,
                2,
                withAlpha(BRIGHT_GOLD, 0.96F)
        );

        for (int index = 0; index < nodeCount; index++) {
            float nodeProgress = easeOut(progress(
                    currentTime - animationStartedAt - index * NODE_APPEAR_INTERVAL,
                    NODE_APPEAR_DURATION
            ));
            float nodePosition = getNodePosition(index, nodeCount);
            if (nodePosition > visibleProgress || nodeProgress <= 0.0F) {
                continue;
            }
            float[] point = getTimelinePoint(nodePosition, startX, startY, endX, endY);
            CausalityRecord record = getNodeRecord(records, index, nodeCount);
            drawTimelineNode(guiGraphics, point[0], point[1], nodeProgress, index == this.selectedNodeIndex);
            drawTimelineNodeLabel(
                    guiGraphics,
                    record,
                    index,
                    nodeCount,
                    startX,
                    endX,
                    point[0],
                    point[1],
                    nodeProgress
            );
        }
        guiGraphics.drawString(
                this.font,
                records.isEmpty() ? "起点 · 等待第一条记录" : "起点 · 最早可见记录",
                Mth.floor(startX),
                this.height - 92,
                withAlpha(FAINT_TEXT, lineProgress)
        );
        guiGraphics.drawString(
                this.font,
                records.isEmpty() ? "终点 · 当前观测时刻" : "终点 · 当前推衍边界",
                Mth.floor(Math.max(12.0F, endX - 194.0F)),
                this.height - 92,
                withAlpha(FAINT_TEXT, lineProgress)
        );
        if (records.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal("因果线已展开，等待可见行为写入"),
                    this.width / 2,
                    this.height / 2 - 12,
                    withAlpha(MUTED_TEXT, lineProgress * 0.92F)
            );
        }
    }

    private void drawTimelineNodeLabel(
            GuiGraphics guiGraphics,
            CausalityRecord record,
            int nodeIndex,
            int nodeCount,
            float timelineStartX,
            float timelineEndX,
            float nodeX,
            float nodeY,
            float alpha
    ) {
        float position = getNodePosition(nodeIndex, nodeCount);
        float previousPosition = getNodePosition(Math.max(0, nodeIndex - 1), nodeCount);
        float nextPosition = getNodePosition(Math.min(nodeCount - 1, nodeIndex + 1), nodeCount);
        int cellLeft = nodeIndex == 0
                ? Mth.floor(timelineStartX)
                : Mth.floor(Mth.lerp((previousPosition + position) * 0.5F, timelineStartX, timelineEndX));
        int cellRight = nodeIndex == nodeCount - 1
                ? Mth.floor(timelineEndX)
                : Mth.floor(Mth.lerp((position + nextPosition) * 0.5F, timelineStartX, timelineEndX));
        int labelX = cellLeft + 6;
        int labelWidth = Math.max(42, cellRight - cellLeft - 12);
        int lane = getTimelineLabelLane(nodeIndex);
        boolean above = lane < 2;
        int labelY = getTimelineLabelY(lane);
        int connectorY = above ? labelY + TIMELINE_LABEL_HEIGHT : labelY;
        drawUiPanel(
                guiGraphics,
                labelX - 4,
                labelY - 4,
                labelWidth + 8,
                TIMELINE_LABEL_HEIGHT + 4,
                6.0F,
                0xE6151D2A,
                0xE60B0E16,
                0x30FFFFFF,
                alpha * 0.9F
        );
        guiGraphics.fill(
                Mth.floor(nodeX),
                Math.min(Mth.floor(nodeY), connectorY),
                Mth.floor(nodeX) + 1,
                Math.max(Mth.floor(nodeY), connectorY) + 1,
                withAlpha(GOLD_LINE, alpha * 0.66F)
        );
        String title = record == null
                ? String.format("%02d · 等待记录", nodeIndex + 1)
                : String.format("%02d · %s", nodeIndex + 1, categoryName(record.category()));
        String timestamp = record == null
                ? "等待行为写入"
                : "时刻 " + formatShortTimestamp(record.timestamp()) + " · 第 " + record.gameTime() + " 刻";
        drawTimelineTextLine(guiGraphics, title, labelX, labelY, labelWidth, GOLD, alpha);
        drawTimelineTextLine(guiGraphics, timestamp, labelX, labelY + 15, labelWidth, MUTED_TEXT, alpha);
    }

    private void drawTimelinePath(
            GuiGraphics guiGraphics,
            float startX,
            float startY,
            float endX,
            float endY,
            float progress,
            int thickness,
            int color
    ) {
        if (progress <= 0.0F) {
            return;
        }

        int segmentCount = 384;
        List<float[]> points = new ArrayList<>(segmentCount + 1);
        for (int index = 0; index <= segmentCount; index++) {
            float currentT = Math.min(progress, index / (float) segmentCount);
            points.add(getTimelinePoint(currentT, startX, startY, endX, endY));
            if (currentT >= progress) {
                break;
            }
        }
        drawSmoothPolyline(guiGraphics, points, thickness, color);
    }

    private void drawTimelineNode(
            GuiGraphics guiGraphics,
            float x,
            float y,
            float alpha,
            boolean selected
    ) {
        float pulse = 1.0F + Mth.sin((Util.getMillis() % 1600L) / 1600.0F * Mth.TWO_PI) * 0.12F;
        float outerRadius = Math.max(8.0F, 15.0F * pulse);
        float innerRadius = selected ? 8.0F : 6.0F;
        Render2DUtility.drawCircle(
                guiGraphics,
                x,
                y,
                outerRadius,
                withAlpha(selected ? AQUA : GOLD_LINE, alpha * 0.20F)
        );
        Render2DUtility.drawCircle(
                guiGraphics,
                x,
                y,
                innerRadius,
                withAlpha(selected ? BRIGHT_GOLD : GOLD, alpha)
        );
        Render2DUtility.drawCircle(
                guiGraphics,
                x,
                y,
                2.5F,
                withAlpha(TEXT, alpha)
        );
    }

    private void renderRecordPage(
            GuiGraphics guiGraphics,
            CausalityRecord record,
            int absoluteIndex,
            int left,
            int top,
            int cardWidth,
            int rowHeight,
            long currentTime,
            long animationStartedAt,
            float alpha
    ) {
        left += (absoluteIndex % 3) * 4;
        top += (absoluteIndex % 2) * 3;
        int panelHeight = rowHeight - 12;
        int contentX = left + 16;
        int availableWidth = Math.max(180, cardWidth - 32);
        int bottom = top + panelHeight;
        drawUiPanel(
                guiGraphics,
                left,
                top,
                cardWidth,
                panelHeight,
                CARD_RADIUS,
                PANEL_RAISED,
                PANEL,
                0x2CFFFFFF,
                alpha
        );
        Render2DUtility.drawRoundedRect(
                guiGraphics,
                left + 14,
                top + 14,
                3.0F,
                18.0F,
                1.5F,
                withAlpha(GOLD, alpha)
        );
        Render2DUtility.drawPill(
                guiGraphics,
                left + cardWidth - 70,
                top + 13,
                54.0F,
                20.0F,
                withAlpha(0xFF111928, alpha),
                withAlpha(0x405E4A2C, alpha)
        );
        guiGraphics.drawCenteredString(
                this.font,
                categoryName(record.category()),
                left + cardWidth - 43,
                top + 19,
                withAlpha(GOLD, alpha)
        );
        guiGraphics.drawString(
                this.font,
                String.format("档案 %02d  ·  %s", absoluteIndex + 1, categoryName(record.category())),
                contentX + 10,
                top + 14,
                withAlpha(GOLD, alpha)
        );
        guiGraphics.drawString(
                this.font,
                formatTimestamp(record.timestamp()) + "  /  游戏刻 " + record.gameTime(),
                contentX,
                top + 35,
                withAlpha(TEXT, alpha)
        );
        drawTimelineTextLine(
                guiGraphics,
                "位置：" + record.dimension() + "  [" + record.x() + ", " + record.y() + ", " + record.z() + "]",
                contentX,
                top + 51,
                availableWidth,
                MUTED_TEXT,
                alpha
        );

        List<String> sentences = splitSentences(record.description());
        int sentenceY = top + 70;
        int maximumDescriptionLines = Math.max(1, (bottom - sentenceY - 2) / 12);
        int renderedDescriptionLines = 0;
        for (int index = 0; index < sentences.size(); index++) {
            float sentenceAlpha = easeOut(progress(
                    currentTime - animationStartedAt
                            - (absoluteIndex - getVisibleFileStart()) * RECORD_APPEAR_INTERVAL
                            - index * SENTENCE_APPEAR_INTERVAL,
                    420L
            ));
            if (sentenceAlpha <= 0.0F) {
                continue;
            }
            int sentenceX = left + ((absoluteIndex + index) % 3) * 8;
            int sentenceWidth = Math.max(150, availableWidth - ((absoluteIndex + index) % 3) * 8);
            for (FormattedCharSequence line : wrapText(sentences.get(index), sentenceWidth)) {
                if (renderedDescriptionLines >= maximumDescriptionLines) {
                    break;
                }
                guiGraphics.drawString(
                        this.font,
                        line,
                        sentenceX,
                        sentenceY,
                        withAlpha(TEXT, alpha * sentenceAlpha)
                );
                sentenceY += 12;
                renderedDescriptionLines++;
            }
            if (renderedDescriptionLines >= maximumDescriptionLines) {
                break;
            }
        }
    }

    private boolean isEntryConfigurationAt(double mouseX, double mouseY) {
        return mouseX >= 36.0D
                && mouseX < this.width * 0.5D
                && mouseY >= 142.0D
                && mouseY < this.height - 92.0D;
    }

    private boolean isEntryCausalityAt(double mouseX, double mouseY) {
        return mouseX >= this.width * 0.5D
                && mouseX <= this.width - 36.0D
                && mouseY >= 142.0D
                && mouseY < this.height - 92.0D;
    }

    private int getPlayerIndexAt(double mouseX, double mouseY) {
        int firstPlayerIndex = this.playerScrollOffset;
        int lastPlayerIndex = Math.min(
                this.snapshot.players().size(),
                firstPlayerIndex + getVisiblePlayerRows()
        );
        int contentLeft = getPageContentLeft();
        int contentRight = getPageContentRight();
        int listTop = 120;
        for (int index = firstPlayerIndex; index < lastPlayerIndex; index++) {
            int row = index - firstPlayerIndex;
            int top = listTop + row * PLAYER_ROW_HEIGHT;
            if (isInside(
                    mouseX,
                    mouseY,
                    contentLeft,
                    top,
                    contentRight - contentLeft,
                    PLAYER_ROW_HEIGHT
            )) {
                return index;
            }
        }
        return -1;
    }

    private int getNodeAt(double mouseX, double mouseY) {
        if (this.snapshot.records().isEmpty()) {
            return -1;
        }
        int nodeCount = getNodeCount();
        for (int index = 0; index < nodeCount; index++) {
            float position = getNodePosition(index, nodeCount);
            float[] point = getTimelinePoint(
                    position,
                    getTimelineStartX(),
                    getTimelineStartY(),
                    getTimelineEndX(),
                    getTimelineEndY()
            );
            float deltaX = (float) mouseX - point[0];
            float deltaY = (float) mouseY - point[1];
            if (deltaX * deltaX + deltaY * deltaY <= 22.0F * 22.0F) {
                return index;
            }
        }
        return -1;
    }

    private int getNodeCount() {
        return this.snapshot.records().isEmpty()
                ? EMPTY_LINE_NODES
                : Math.min(MAX_LINE_NODES, this.snapshot.records().size());
    }

    private float getNodePosition(int nodeIndex, int nodeCount) {
        return nodeCount <= 1 ? 0.0F : nodeIndex / (float) (nodeCount - 1);
    }

    private CausalityRecord getNodeRecord(List<CausalityRecord> records, int nodeIndex, int nodeCount) {
        if (records.isEmpty()) {
            return null;
        }
        int recordIndex = nodeCount <= 1
                ? 0
                : Math.round(nodeIndex * (records.size() - 1) / (float) (nodeCount - 1));
        return records.get(Mth.clamp(recordIndex, 0, records.size() - 1));
    }

    private int getPlayerRowCount() {
        return this.snapshot.players().size();
    }

    private int getVisiblePlayerRows() {
        return Math.max(1, (this.height - 198) / PLAYER_ROW_HEIGHT);
    }

    private float getTimelineStartX() {
        return Math.max(48.0F, this.width * 0.08F);
    }

    private float getTimelineEndX() {
        return Math.min(this.width - 48.0F, this.width * 0.92F);
    }

    private float getTimelineStartY() {
        if (this.height < 620) {
            return this.height - 64.0F - TIMELINE_LABEL_HEIGHT - 16.0F;
        }
        float verticalSpan = Math.min(260.0F, Math.max(188.0F, this.height * 0.36F));
        return this.height * 0.5F + verticalSpan * 0.5F;
    }

    private float getTimelineEndY() {
        if (this.height < 620) {
            return HEADER_HEIGHT + TIMELINE_LABEL_HEIGHT + 20.0F;
        }
        float verticalSpan = Math.min(260.0F, Math.max(188.0F, this.height * 0.36F));
        return this.height * 0.5F - verticalSpan * 0.5F;
    }

    private List<CausalityRecord> getVisibleFileRecords() {
        if (this.snapshot.records().isEmpty()) {
            return List.of();
        }
        int start = getVisibleFileStart();
        int end = Math.min(this.snapshot.records().size(), start + getVisibleRecordCount());
        return this.snapshot.records().subList(start, end);
    }

    private int getVisibleFileStart() {
        if (this.snapshot.records().isEmpty()) {
            return 0;
        }
        int nodeCount = getNodeCount();
        int center = nodeCount <= 1
                ? 0
                : Math.round(this.selectedNodeIndex * (this.snapshot.records().size() - 1)
                / (float) (nodeCount - 1));
        int start = Mth.clamp(
                center - 2,
                0,
                Math.max(0, this.snapshot.records().size() - getVisibleRecordCount())
        );
        return Mth.clamp(
                start + this.recordScrollOffset,
                0,
                Math.max(0, this.snapshot.records().size() - getVisibleRecordCount())
        );
    }

    private int getTimelineLabelLane(int nodeIndex) {
        if (this.height < 620) {
            return nodeIndex % 2 == 0 ? 0 : 2;
        }
        return nodeIndex % 4;
    }

    private int getTimelineLabelY(int lane) {
        int minimumTimelineY = Mth.floor(Math.min(getTimelineStartY(), getTimelineEndY()));
        int maximumTimelineY = Mth.floor(Math.max(getTimelineStartY(), getTimelineEndY()));
        int upperHeaderY = HEADER_HEIGHT + 20;
        int upperNearLineY = Math.max(
                upperHeaderY,
                minimumTimelineY - TIMELINE_LABEL_HEIGHT - 16
        );
        int lowerFooterY = this.height - 64 - TIMELINE_LABEL_HEIGHT;
        int lowerNearLineY = Math.min(
                lowerFooterY,
                maximumTimelineY + 16
        );
        return switch (lane) {
            case 0 -> this.height < 620 ? upperNearLineY : upperHeaderY;
            case 1 -> upperNearLineY;
            case 2 -> lowerNearLineY;
            default -> Math.max(lowerNearLineY, lowerFooterY);
        };
    }

    private int getVisibleRecordRows() {
        return Mth.clamp((this.height - 140) / 126, 1, MAX_VISIBLE_RECORDS / 2);
    }

    private int getVisibleRecordCount() {
        return getVisibleRecordRows() * 2;
    }

    private int getRecordRowHeight() {
        return Math.max(108, (this.height - 150) / getVisibleRecordRows());
    }

    private void drawFooter(GuiGraphics guiGraphics, String text) {
        guiGraphics.drawString(this.font, text, getPageContentLeft(), this.height - 38, FAINT_TEXT);
    }

    private static void drawSmoothPolyline(
            GuiGraphics guiGraphics,
            List<float[]> points,
            float thickness,
            int color
    ) {
        if ((color >>> 24) == 0 || points.size() < 2) {
            return;
        }

        guiGraphics.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        drawPolylineLayer(matrix, points, thickness + 4.0F, scaleAlpha(color, 0.12F));
        drawPolylineEdgeFade(matrix, points, thickness, color);
        drawPolylineLayer(matrix, points, Math.max(1.0F, thickness - 0.9F), color);

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void drawPolylineLayer(
            Matrix4f matrix,
            List<float[]> points,
            float thickness,
            int color
    ) {
        if ((color >>> 24) == 0 || thickness <= 0.0F) {
            return;
        }

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int index = 0; index < points.size(); index++) {
            appendPolylineStripVertices(bufferBuilder, matrix, points, index, thickness, color);
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void drawPolylineEdgeFade(
            Matrix4f matrix,
            List<float[]> points,
            float thickness,
            int color
    ) {
        float innerHalfThickness = Math.max(0.35F, thickness * 0.5F - 0.35F);
        float outerHalfThickness = thickness * 0.5F + 1.65F;
        int transparentColor = color & 0x00FFFFFF;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int index = 0; index < points.size() - 1; index++) {
            float[] start = points.get(index);
            float[] end = points.get(index + 1);
            float[] startNormal = getPolylineNormal(points, index);
            float[] endNormal = getPolylineNormal(points, index + 1);

            appendQuad(
                    bufferBuilder,
                    matrix,
                    start[0] + startNormal[0] * outerHalfThickness,
                    start[1] + startNormal[1] * outerHalfThickness,
                    start[0] + startNormal[0] * innerHalfThickness,
                    start[1] + startNormal[1] * innerHalfThickness,
                    end[0] + endNormal[0] * innerHalfThickness,
                    end[1] + endNormal[1] * innerHalfThickness,
                    end[0] + endNormal[0] * outerHalfThickness,
                    end[1] + endNormal[1] * outerHalfThickness,
                    transparentColor,
                    color,
                    color,
                    transparentColor
            );
            appendQuad(
                    bufferBuilder,
                    matrix,
                    start[0] - startNormal[0] * innerHalfThickness,
                    start[1] - startNormal[1] * innerHalfThickness,
                    start[0] - startNormal[0] * outerHalfThickness,
                    start[1] - startNormal[1] * outerHalfThickness,
                    end[0] - endNormal[0] * outerHalfThickness,
                    end[1] - endNormal[1] * outerHalfThickness,
                    end[0] - endNormal[0] * innerHalfThickness,
                    end[1] - endNormal[1] * innerHalfThickness,
                    color,
                    transparentColor,
                    transparentColor,
                    color
            );
        }
        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private static void appendPolylineStripVertices(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            List<float[]> points,
            int pointIndex,
            float thickness,
            int color
    ) {
        float[] point = points.get(pointIndex);
        float[] normal = getPolylineNormal(points, pointIndex);
        float halfThickness = thickness * 0.5F;
        vertex(
                bufferBuilder,
                matrix,
                point[0] + normal[0] * halfThickness,
                point[1] + normal[1] * halfThickness,
                color
        );
        vertex(
                bufferBuilder,
                matrix,
                point[0] - normal[0] * halfThickness,
                point[1] - normal[1] * halfThickness,
                color
        );
    }

    private static float[] getPolylineNormal(List<float[]> points, int pointIndex) {
        float[] previous = points.get(Math.max(0, pointIndex - 1));
        float[] next = points.get(Math.min(points.size() - 1, pointIndex + 1));
        float deltaX = next[0] - previous[0];
        float deltaY = next[1] - previous[1];
        float length = Mth.sqrt(deltaX * deltaX + deltaY * deltaY);
        if (length <= 0.0001F) {
            return new float[]{0.0F, 1.0F};
        }
        return new float[]{-deltaY / length, deltaX / length};
    }

    private static void appendQuad(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float firstX,
            float firstY,
            float secondX,
            float secondY,
            float thirdX,
            float thirdY,
            float fourthX,
            float fourthY,
            int firstColor,
            int secondColor,
            int thirdColor,
            int fourthColor
    ) {
        vertex(bufferBuilder, matrix, firstX, firstY, firstColor);
        vertex(bufferBuilder, matrix, secondX, secondY, secondColor);
        vertex(bufferBuilder, matrix, thirdX, thirdY, thirdColor);
        vertex(bufferBuilder, matrix, fourthX, fourthY, fourthColor);
    }

    private static void vertex(
            BufferBuilder bufferBuilder,
            Matrix4f matrix,
            float x,
            float y,
            int color
    ) {
        int alpha = color >>> 24 & 0xFF;
        int red = color >>> 16 & 0xFF;
        int green = color >>> 8 & 0xFF;
        int blue = color & 0xFF;
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(red, green, blue, alpha).endVertex();
    }

    private static int scaleAlpha(int color, float scale) {
        int alpha = Mth.clamp((int) ((color >>> 24 & 0xFF) * scale), 0, 255);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private static float[] getTimelinePoint(
            float progress,
            float startX,
            float startY,
            float endX,
            float endY
    ) {
        float clampedProgress = Mth.clamp(progress, 0.0F, 1.0F);
        float x = Mth.lerp(clampedProgress, startX, endX);
        float wave = Mth.sin(clampedProgress * Mth.PI) * 0.045F;
        float y = Mth.lerp(clampedProgress, startY, endY)
                + wave * (endY - startY);
        return new float[]{x, y};
    }

    private List<FormattedCharSequence> wrapText(String text, int width) {
        return this.font.split(Component.literal(text), width);
    }

    private String trimTextToWidth(String text, int width) {
        if (this.font.width(text) <= width) {
            return text;
        }
        String suffix = "...";
        int availableWidth = Math.max(0, width - this.font.width(suffix));
        int end = text.length();
        while (end > 0 && this.font.width(text.substring(0, end)) > availableWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }

    private void drawTimelineTextLine(
            GuiGraphics guiGraphics,
            String text,
            int x,
            int y,
            int width,
            int color,
            float alpha
    ) {
        List<FormattedCharSequence> lines = wrapText(text, width);
        if (!lines.isEmpty()) {
            guiGraphics.drawString(this.font, lines.get(0), x, y, withAlpha(color, alpha));
        }
    }

    private static List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            current.append(character);
            if (character == '。'
                    || character == '！'
                    || character == '？'
                    || character == '.'
                    || character == '!') {
                sentences.add(current.toString());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            sentences.add(current.toString());
        }
        return sentences.isEmpty() ? List.of(text) : sentences;
    }

    private static String getDimensionName(List<CausalityPlayerSnapshot> players) {
        return players.isEmpty() ? "当前界域" : players.get(0).dimension();
    }

    private static String categoryName(String category) {
        return switch (category) {
            case "combat" -> "战斗";
            case "interaction" -> "交互";
            case "block" -> "方块";
            case "item" -> "物品";
            case "cultivation" -> "修行";
            case "fishing" -> "垂钓";
            case "loot" -> "战利品";
            case "death" -> "生死";
            case "travel" -> "行迹";
            case "crafting" -> "制作";
            case "login" -> "登录";
            case "logout" -> "离开";
            default -> category;
        };
    }

    private static String formatTimestamp(String timestamp) {
        try {
            return TIMESTAMP_FORMAT.format(Instant.parse(timestamp));
        } catch (DateTimeParseException exception) {
            return timestamp;
        }
    }

    private static String formatShortTimestamp(String timestamp) {
        try {
            return SHORT_TIMESTAMP_FORMAT.format(Instant.parse(timestamp));
        } catch (DateTimeParseException exception) {
            return timestamp.length() > 8 ? timestamp.substring(timestamp.length() - 8) : timestamp;
        }
    }

    private static float progress(long elapsed, long duration) {
        return Mth.clamp(elapsed / (float) Math.max(1L, duration), 0.0F, 1.0F);
    }

    private static float easeOut(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static int withAlpha(int color, float alpha) {
        int sourceAlpha = color >>> 24 & 0xFF;
        int clampedAlpha = Mth.clamp((int) (sourceAlpha * Mth.clamp(alpha, 0.0F, 1.0F)), 0, 255);
        return (clampedAlpha << 24) | (color & 0x00FFFFFF);
    }

    private static final class ToggleAnimation {
        private float startValue;
        private float targetValue;
        private long startedAt;

        private ToggleAnimation(float initialValue, long currentTime) {
            this.startValue = initialValue;
            this.targetValue = initialValue;
            this.startedAt = currentTime;
        }

        private void updateTarget(float nextTarget, long currentTime) {
            if (Mth.equal(this.targetValue, nextTarget)) {
                return;
            }
            this.startValue = getValue(currentTime);
            this.targetValue = nextTarget;
            this.startedAt = currentTime;
        }

        private float getValue(long currentTime) {
            float progress = progress(currentTime - this.startedAt, TOGGLE_ANIMATION_DURATION);
            float easedProgress = BezierUtility.cubicEase(progress, 0.18F, 0.92F);
            return Mth.lerp(easedProgress, this.startValue, this.targetValue);
        }
    }

    private enum SettingsSection {
        TRANSFER(
                "换因果",
                "把已发生的结果转接给另一条因果线",
                true
        ),
        NO_CAUSE(
                "无因得果",
                "无视过程，直接取得一件随机物品",
                false
        ),
        TAMPER(
                "篡改因果",
                "让结果脱离原本的因，改写归属与去向",
                true
        );

        private final String title;
        private final String subtitle;
        private final boolean hasTarget;

        SettingsSection(String title, String subtitle, boolean hasTarget) {
            this.title = title;
            this.subtitle = subtitle;
            this.hasTarget = hasTarget;
        }
    }

    private enum SettingToggle {
        TRANSFER_ATTACK("转移攻击", "受到的攻击转给目标"),
        TRANSFER_HATE("转移仇恨", "怪物的敌意改写归属"),
        TRANSFER_TRIBULATION("转移天劫", "天劫落向另一条因果线"),
        TRANSFER_NEGATIVE_EFFECTS("转移负面效果", "负面状态不再由自己承受"),
        TRANSFER_VILLAGER_PRICE("转移村民价格", "涨价结果转给目标"),
        TRANSFER_DAMAGE("转移伤害", "受到的伤害转给目标"),
        RANDOM_ITEM("随机物品", "无因取得一件随机物品"),
        CORRUPT_BENEFICIAL_EFFECTS("篡改有益效果", "有益状态改写为有弊状态"),
        TRANSFER_ITEMS("转移物品", "获得的物品转移到自己"),
        TRANSFER_ADVANCEMENTS("转移进度", "获取进度改写为自己获取"),
        VOID_CRAFTING("虚空合成", "合成结果归于虚空");

        private final String displayName;
        private final String description;

        SettingToggle(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
    }

    private enum ViewMode {
        ENTRY,
        PLAYER_LIST,
        CAUSALITY_LINE,
        RECORD_FILE,
        SETTINGS
    }
}
