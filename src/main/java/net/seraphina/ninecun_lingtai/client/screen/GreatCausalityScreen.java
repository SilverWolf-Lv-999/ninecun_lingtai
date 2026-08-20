package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.client.utility.CausalityBlurUtility;
import net.seraphina.ninecun_lingtai.common.causality.CausalityPlayerSnapshot;
import net.seraphina.ninecun_lingtai.common.causality.CausalityRecord;
import net.seraphina.ninecun_lingtai.common.causality.CausalitySnapshot;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestGreatCausalityPacket;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class GreatCausalityScreen extends Screen {
    private static final int VOID_BACKGROUND = 0xA9080C14;
    private static final int VOID_GRID = 0x3A5F4B35;
    private static final int GOLD = 0xFFF5D37C;
    private static final int BRIGHT_GOLD = 0xFFFFF0B5;
    private static final int GOLD_LINE = 0xFFE0A63A;
    private static final int TEXT = 0xFFF0ECE2;
    private static final int MUTED_TEXT = 0xFFB8B5AF;
    private static final int FAINT_TEXT = 0xFF777B82;
    private static final int MAX_LINE_NODES = 12;
    private static final int EMPTY_LINE_NODES = 5;
    private static final int MAX_VISIBLE_RECORDS = 8;
    private static final int HEADER_HEIGHT = 84;
    private static final long INTRO_DURATION = 1_250L;
    private static final long NODE_APPEAR_INTERVAL = 95L;
    private static final long NODE_APPEAR_DURATION = 480L;
    private static final long RECORD_APPEAR_INTERVAL = 280L;
    private static final long SENTENCE_APPEAR_INTERVAL = 155L;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss"
    ).withZone(ZoneId.systemDefault());

    private final CausalitySnapshot snapshot;
    private ViewMode mode;
    private long openedAt;
    private long viewStartedAt;
    private int selectedNodeIndex = -1;
    private int recordScrollOffset;
    private int playerScrollOffset;

    public GreatCausalityScreen(CausalitySnapshot snapshot) {
        super(Component.literal("大因果术"));
        this.snapshot = snapshot;
        this.mode = snapshot.playerListMode() ? ViewMode.PLAYER_LIST : ViewMode.CAUSALITY_LINE;
    }

    @Override
    public void added() {
        super.added();
        this.openedAt = Util.getMillis();
        this.viewStartedAt = this.openedAt;
        CausalityBlurUtility.install();
    }

    @Override
    public void removed() {
        CausalityBlurUtility.restore();
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
        CausalityBlurUtility.restore();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        long currentTime = Util.getMillis();
        CausalityBlurUtility.ensureInstalled();
        renderFullScreenAtmosphere(guiGraphics, currentTime);

        if (this.mode == ViewMode.PLAYER_LIST) {
            renderPlayerList(guiGraphics, currentTime, mouseX, mouseY);
        } else if (this.mode == ViewMode.CAUSALITY_LINE) {
            renderCausalityLine(guiGraphics, currentTime);
        } else {
            renderRecordFile(guiGraphics, currentTime);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (this.mode == ViewMode.PLAYER_LIST) {
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
                this.mode = ViewMode.RECORD_FILE;
                this.viewStartedAt = Util.getMillis();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
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
            int maximumOffset = Math.max(0, this.snapshot.records().size() - MAX_VISIBLE_RECORDS);
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
            this.mode = ViewMode.CAUSALITY_LINE;
            this.viewStartedAt = Util.getMillis();
            this.selectedNodeIndex = -1;
            return true;
        }
        onClose();
        return true;
    }

    private void renderFullScreenAtmosphere(GuiGraphics guiGraphics, long currentTime) {
        guiGraphics.fill(0, 0, this.width, this.height, VOID_BACKGROUND);
        for (int x = 0; x < this.width; x += 48) {
            guiGraphics.fill(x, HEADER_HEIGHT + 1, x + 1, this.height - 48, withAlpha(VOID_GRID, 0.34F));
        }
        for (int y = HEADER_HEIGHT + 24; y < this.height - 48; y += 48) {
            guiGraphics.fill(0, y, this.width, y + 1, withAlpha(VOID_GRID, 0.26F));
        }
        guiGraphics.fill(0, 0, this.width, 2, withAlpha(GOLD, 0.62F));
        guiGraphics.fill(0, this.height - 2, this.width, this.height, withAlpha(GOLD, 0.32F));
        guiGraphics.fill(0, 0, 2, this.height, withAlpha(GOLD, 0.3F));
        guiGraphics.fill(this.width - 2, 0, this.width, this.height, withAlpha(GOLD, 0.3F));

        for (int index = 0; index < 28; index++) {
            float x = Mth.frac(index * 0.371F + currentTime * 0.000012F) * this.width;
            float y = Mth.frac(index * 0.173F + currentTime * 0.000008F) * this.height;
            int size = index % 5 == 0 ? 2 : 1;
            guiGraphics.fill(
                    (int) x,
                    (int) y,
                    (int) x + size,
                    (int) y + size,
                    withAlpha(GOLD, 0.1F + (index % 4) * 0.035F)
            );
        }
    }

    private void renderPlayerList(GuiGraphics guiGraphics, long currentTime, int mouseX, int mouseY) {
        float intro = easeOut(progress(currentTime - this.openedAt, INTRO_DURATION));
        drawHeader(
                guiGraphics,
                "大因果术 · 当前界域",
                "左键选择一名玩家，展开他的因果时间线",
                currentTime
        );
        guiGraphics.drawString(
                this.font,
                "界域：" + getDimensionName(this.snapshot.players()),
                52,
                66,
                withAlpha(MUTED_TEXT, intro)
        );
        guiGraphics.drawString(
                this.font,
                "共 " + this.snapshot.players().size() + " 条可观测因果线",
                this.width - 220,
                66,
                withAlpha(MUTED_TEXT, intro)
        );

        List<CausalityPlayerSnapshot> players = this.snapshot.players();
        int firstPlayerIndex = this.playerScrollOffset * 2;
        int lastPlayerIndex = Math.min(players.size(), firstPlayerIndex + getVisiblePlayerRows() * 2);
        for (int index = firstPlayerIndex; index < lastPlayerIndex; index++) {
            long delay = (index - firstPlayerIndex) * 115L;
            float alpha = easeOut(progress(currentTime - this.openedAt - delay, 560L));
            if (alpha <= 0.0F) {
                continue;
            }
            int column = index % 2;
            int row = (index - firstPlayerIndex) / 2;
            int columnWidth = Math.max(220, this.width / 2 - 84);
            int left = 52 + column * (this.width / 2);
            int top = 112 + row * 54;
            boolean hovered = mouseX >= left - 8
                    && mouseX <= left + columnWidth
                    && mouseY >= top - 8
                    && mouseY <= top + 35;
            int accent = hovered ? BRIGHT_GOLD : GOLD;
            guiGraphics.fill(left - 8, top - 5, left - 6, top + 31, withAlpha(accent, alpha));
            guiGraphics.drawString(
                    this.font,
                    String.format("%02d", index + 1),
                    left + 4,
                    top,
                    withAlpha(accent, alpha)
            );
            guiGraphics.drawString(
                    this.font,
                    players.get(index).playerName(),
                    left + 38,
                    top - 1,
                    withAlpha(hovered ? BRIGHT_GOLD : TEXT, alpha)
            );
            guiGraphics.drawString(
                    this.font,
                    players.get(index).cultivation(),
                    left + 38,
                    top + 14,
                    withAlpha(MUTED_TEXT, alpha)
            );
            guiGraphics.drawString(
                    this.font,
                    players.get(index).dimension(),
                    left + 38,
                    top + 29,
                    withAlpha(FAINT_TEXT, alpha)
            );
            guiGraphics.hLine(
                    left + 38,
                    Math.min(this.width - 52, left + columnWidth),
                    top + 38,
                    withAlpha(0x5C6B54, alpha * 0.75F)
            );
        }
        drawFooter(
                guiGraphics,
                players.size() > getVisiblePlayerRows() * 2
                        ? "滚轮翻阅玩家 · 左键展开 · ESC 关闭"
                        : "左键展开玩家因果线 · ESC 关闭"
        );
    }

    private void renderCausalityLine(GuiGraphics guiGraphics, long currentTime) {
        float lineProgress = easeOut(progress(currentTime - this.viewStartedAt, INTRO_DURATION));
        drawHeader(
                guiGraphics,
                "大因果术 · 因果时间线",
                "点击时间节点，查看该节点附近的完整行为记录",
                currentTime
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

        List<CausalityRecord> records = this.snapshot.records();
        drawTimeline(guiGraphics, records, currentTime, lineProgress);
        drawFooter(
                guiGraphics,
                records.isEmpty()
                        ? "当前没有已写入的行为 · ESC 关闭"
                        : "左键时间节点查看完整档案 · ESC 关闭"
        );
    }

    private void renderRecordFile(GuiGraphics guiGraphics, long currentTime) {
        float reveal = easeOut(progress(currentTime - this.viewStartedAt, 780L));
        drawHeader(
                guiGraphics,
                "大因果术 · 因果档案",
                "时间节点附近的行为，按发生顺序逐句显现",
                currentTime
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
        for (int index = 0; index < records.size(); index++) {
            CausalityRecord record = records.get(index);
            int absoluteIndex = windowStart + index;
            int column = index % 2;
            int row = index / 2;
            int left = 58 + column * (this.width / 2);
            int top = 108 + row * 108;
            float recordAlpha = easeOut(progress(
                    currentTime - this.viewStartedAt - index * RECORD_APPEAR_INTERVAL,
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
                    currentTime,
                    recordAlpha * reveal
            );
        }
        drawFooter(guiGraphics, "ESC 返回因果线 · 滚轮翻阅附近记录");
    }

    private void drawHeader(GuiGraphics guiGraphics, String title, String subtitle, long currentTime) {
        float alpha = easeOut(progress(currentTime - this.openedAt, 600L));
        guiGraphics.drawString(this.font, title, 52, 24, withAlpha(GOLD, alpha));
        guiGraphics.drawString(this.font, subtitle, 52, 42, withAlpha(MUTED_TEXT, alpha * 0.9F));
        guiGraphics.hLine(52, this.width - 52, HEADER_HEIGHT, withAlpha(GOLD_LINE, alpha * 0.8F));
        guiGraphics.fill(
                52,
                HEADER_HEIGHT - 2,
                130,
                HEADER_HEIGHT,
                withAlpha(BRIGHT_GOLD, alpha)
        );
    }

    private void drawTimeline(
            GuiGraphics guiGraphics,
            List<CausalityRecord> records,
            long currentTime,
            float lineProgress
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
                withAlpha(GOLD_LINE, 0.12F)
        );
        drawTimelinePath(
                guiGraphics,
                startX,
                startY,
                endX,
                endY,
                visibleProgress,
                4,
                withAlpha(GOLD_LINE, 0.38F)
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
                    currentTime - this.viewStartedAt - index * NODE_APPEAR_INTERVAL,
                    NODE_APPEAR_DURATION
            ));
            float nodePosition = getNodePosition(index, nodeCount);
            if (nodePosition > visibleProgress || nodeProgress <= 0.0F) {
                continue;
            }
            float[] point = getTimelinePoint(nodePosition, startX, startY, endX, endY);
            CausalityRecord record = getNodeRecord(records, index, nodeCount);
            drawTimelineNode(guiGraphics, point[0], point[1], nodeProgress, index == this.selectedNodeIndex);
            drawTimelineNodeLabel(guiGraphics, record, index, point[0], point[1], nodeProgress);
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
            float nodeX,
            float nodeY,
            float alpha
    ) {
        boolean above = nodeIndex % 2 == 0;
        int labelWidth = Mth.clamp(this.width / 5, 148, 220);
        int labelX = Mth.clamp(
                Mth.floor(nodeX - labelWidth * 0.5F),
                12,
                Math.max(12, this.width - labelWidth - 12)
        );
        int labelY = above
                ? Mth.floor(nodeY - 82.0F)
                : Mth.floor(nodeY + 30.0F);
        int connectorY = above ? labelY + 45 : labelY - 8;
        guiGraphics.fill(
                Mth.floor(nodeX),
                Math.min(Mth.floor(nodeY), connectorY),
                Mth.floor(nodeX) + 1,
                Math.max(Mth.floor(nodeY), connectorY) + 1,
                withAlpha(GOLD_LINE, alpha * 0.66F)
        );
        String title = record == null
                ? String.format("节点 %02d · 等待记录", nodeIndex + 1)
                : String.format("节点 %02d · %s", nodeIndex + 1, categoryName(record.category()));
        String timestamp = record == null
                ? "时间：暂无"
                : "时间：" + formatTimestamp(record.timestamp());
        String metadata = record == null
                ? "游戏刻：暂无 · 坐标：暂无"
                : "刻 " + record.gameTime() + " · [" + record.x() + ", " + record.y() + ", " + record.z() + "]";
        guiGraphics.drawString(this.font, title, labelX, labelY, withAlpha(GOLD, alpha));
        guiGraphics.drawString(this.font, timestamp, labelX, labelY + 14, withAlpha(TEXT, alpha));
        guiGraphics.drawString(this.font, metadata, labelX, labelY + 28, withAlpha(MUTED_TEXT, alpha));
        if (record != null) {
            String description = record.description();
            List<FormattedCharSequence> lines = wrapText(description, labelWidth);
            if (!lines.isEmpty()) {
                guiGraphics.drawString(this.font, lines.get(0), labelX, labelY + 42, withAlpha(FAINT_TEXT, alpha));
            }
        }
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
        float previousT = 0.0F;
        float[] previousPoint = getTimelinePoint(0.0F, startX, startY, endX, endY);
        int segmentCount = 96;
        for (int index = 1; index <= segmentCount; index++) {
            float currentT = index / (float) segmentCount;
            if (currentT > progress) {
                currentT = progress;
            }
            float[] currentPoint = getTimelinePoint(currentT, startX, startY, endX, endY);
            drawSegment(
                    guiGraphics,
                    previousPoint[0],
                    previousPoint[1],
                    currentPoint[0],
                    currentPoint[1],
                    thickness,
                    color
            );
            previousPoint = currentPoint;
            previousT = currentT;
            if (previousT >= progress) {
                break;
            }
        }
    }

    private void drawTimelineNode(
            GuiGraphics guiGraphics,
            float x,
            float y,
            float alpha,
            boolean selected
    ) {
        float pulse = 1.0F + Mth.sin((Util.getMillis() % 1600L) / 1600.0F * Mth.TWO_PI) * 0.12F;
        int outerSize = Math.max(8, (int) (15.0F * pulse));
        int innerSize = selected ? 8 : 6;
        guiGraphics.fill(
                (int) x - outerSize,
                (int) y - outerSize,
                (int) x + outerSize + 1,
                (int) y + outerSize + 1,
                withAlpha(GOLD_LINE, alpha * 0.22F)
        );
        guiGraphics.fill(
                (int) x - innerSize,
                (int) y - innerSize,
                (int) x + innerSize + 1,
                (int) y + innerSize + 1,
                withAlpha(selected ? BRIGHT_GOLD : GOLD, alpha)
        );
        guiGraphics.fill(
                (int) x - 2,
                (int) y - 2,
                (int) x + 3,
                (int) y + 3,
                withAlpha(0xFFFFFBDF, alpha)
        );
    }

    private void renderRecordPage(
            GuiGraphics guiGraphics,
            CausalityRecord record,
            int absoluteIndex,
            int left,
            int top,
            long currentTime,
            float alpha
    ) {
        int availableWidth = Math.max(180, this.width / 2 - 104);
        guiGraphics.fill(left - 8, top - 8, left - 5, top + 82, withAlpha(GOLD_LINE, alpha * 0.82F));
        guiGraphics.hLine(left, left + availableWidth, top + 80, withAlpha(0x7B6040, alpha * 0.55F));
        guiGraphics.drawString(
                this.font,
                String.format("档案 %02d  ·  %s", absoluteIndex + 1, categoryName(record.category())),
                left,
                top,
                withAlpha(GOLD, alpha)
        );
        guiGraphics.drawString(
                this.font,
                formatTimestamp(record.timestamp()) + "  /  游戏刻 " + record.gameTime(),
                left,
                top + 16,
                withAlpha(TEXT, alpha)
        );
        guiGraphics.drawString(
                this.font,
                "位置：" + record.dimension() + "  [" + record.x() + ", " + record.y() + ", " + record.z() + "]",
                left,
                top + 32,
                withAlpha(MUTED_TEXT, alpha)
        );

        List<String> sentences = splitSentences(record.description());
        int sentenceY = top + 49;
        for (int index = 0; index < sentences.size(); index++) {
            float sentenceAlpha = easeOut(progress(
                    currentTime - this.viewStartedAt
                            - (absoluteIndex - getVisibleFileStart()) * RECORD_APPEAR_INTERVAL
                            - index * SENTENCE_APPEAR_INTERVAL,
                    420L
            ));
            if (sentenceAlpha <= 0.0F) {
                continue;
            }
            for (FormattedCharSequence line : wrapText(sentences.get(index), availableWidth)) {
                guiGraphics.drawString(
                        this.font,
                        line,
                        left,
                        sentenceY,
                        withAlpha(TEXT, alpha * sentenceAlpha)
                );
                sentenceY += 12;
            }
        }
    }

    private int getPlayerIndexAt(double mouseX, double mouseY) {
        int firstPlayerIndex = this.playerScrollOffset * 2;
        int lastPlayerIndex = Math.min(
                this.snapshot.players().size(),
                firstPlayerIndex + getVisiblePlayerRows() * 2
        );
        for (int index = firstPlayerIndex; index < lastPlayerIndex; index++) {
            int column = index % 2;
            int row = (index - firstPlayerIndex) / 2;
            int columnWidth = Math.max(220, this.width / 2 - 84);
            int left = 52 + column * (this.width / 2);
            int top = 112 + row * 54;
            if (mouseX >= left - 8
                    && mouseX <= left + columnWidth
                    && mouseY >= top - 8
                    && mouseY <= top + 35) {
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
        return (this.snapshot.players().size() + 1) / 2;
    }

    private int getVisiblePlayerRows() {
        return Math.max(1, (this.height - 156) / 54);
    }

    private float getTimelineStartX() {
        return Math.max(48.0F, this.width * 0.08F);
    }

    private float getTimelineEndX() {
        return Math.min(this.width - 48.0F, this.width * 0.92F);
    }

    private float getTimelineStartY() {
        return Math.max(142.0F, this.height * 0.24F);
    }

    private float getTimelineEndY() {
        return Math.min(this.height - 174.0F, this.height * 0.76F);
    }

    private List<CausalityRecord> getVisibleFileRecords() {
        if (this.snapshot.records().isEmpty()) {
            return List.of();
        }
        int start = getVisibleFileStart();
        int end = Math.min(this.snapshot.records().size(), start + MAX_VISIBLE_RECORDS);
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
                Math.max(0, this.snapshot.records().size() - MAX_VISIBLE_RECORDS)
        );
        return Mth.clamp(
                start + this.recordScrollOffset,
                0,
                Math.max(0, this.snapshot.records().size() - MAX_VISIBLE_RECORDS)
        );
    }

    private void drawFooter(GuiGraphics guiGraphics, String text) {
        guiGraphics.drawString(this.font, text, 52, this.height - 38, FAINT_TEXT);
    }

    private static void drawSegment(
            GuiGraphics guiGraphics,
            float x1,
            float y1,
            float x2,
            float y2,
            int thickness,
            int color
    ) {
        int halfThickness = Math.max(1, thickness / 2);
        int steps = Math.max(1, (int) Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1)));
        for (int index = 0; index <= steps; index++) {
            float progress = index / (float) steps;
            int x = Mth.floor(Mth.lerp(progress, x1, x2));
            int y = Mth.floor(Mth.lerp(progress, y1, y2));
            guiGraphics.fill(
                    x - halfThickness,
                    y - halfThickness,
                    x + halfThickness + 1,
                    y + halfThickness + 1,
                    color
            );
        }
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
        float wave = Mth.sin(clampedProgress * Mth.PI * 4.0F) * 0.055F;
        float y = Mth.lerp(clampedProgress, startY, endY)
                + wave * (endY - startY);
        return new float[]{x, y};
    }

    private List<FormattedCharSequence> wrapText(String text, int width) {
        return this.font.split(Component.literal(text), width);
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

    private static float progress(long elapsed, long duration) {
        return Mth.clamp(elapsed / (float) Math.max(1L, duration), 0.0F, 1.0F);
    }

    private static float easeOut(float value) {
        float inverse = 1.0F - value;
        return 1.0F - inverse * inverse * inverse;
    }

    private static int withAlpha(int color, float alpha) {
        int clampedAlpha = Mth.clamp((int) (alpha * 255.0F), 0, 255);
        return (clampedAlpha << 24) | (color & 0x00FFFFFF);
    }

    private enum ViewMode {
        PLAYER_LIST,
        CAUSALITY_LINE,
        RECORD_FILE
    }
}
