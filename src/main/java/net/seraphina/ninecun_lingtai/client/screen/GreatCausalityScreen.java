package net.seraphina.ninecun_lingtai.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.seraphina.ninecun_lingtai.common.causality.CausalityPlayerSnapshot;
import net.seraphina.ninecun_lingtai.common.causality.CausalityRecord;
import net.seraphina.ninecun_lingtai.common.causality.CausalitySnapshot;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestGreatCausalityPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class GreatCausalityScreen extends Screen {
    private static final int BACKGROUND = 0xD90B0C12;
    private static final int PANEL = 0xCC151725;
    private static final int GOLD = 0xFFE4B866;
    private static final int MUTED_GOLD = 0xFF9B8154;
    private static final int TEXT = 0xFFE5E0D4;
    private static final int MUTED_TEXT = 0xFFAAA5A0;
    private static final int LINE = 0xFFB78D35;
    private static final int MAX_VISIBLE_ROWS = 10;
    private static final int ROW_HEIGHT = 24;

    private final CausalitySnapshot snapshot;
    private int scrollOffset;

    public GreatCausalityScreen(CausalitySnapshot snapshot) {
        super(Component.literal("大因果术"));
        this.snapshot = snapshot;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, BACKGROUND);
        int left = Math.max(18, this.width / 2 - 210);
        int right = Math.min(this.width - 18, this.width / 2 + 210);
        guiGraphics.fill(left, 16, right, this.height - 16, PANEL);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 28, GOLD);
        if (this.snapshot.playerListMode()) {
            renderPlayerList(guiGraphics, left, right, mouseX, mouseY);
        } else {
            renderHistory(guiGraphics, left, right);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int left = Math.max(18, this.width / 2 - 210);
        int right = Math.min(this.width - 18, this.width / 2 + 210);
        if (this.snapshot.playerListMode()) {
            int row = getRowAt(mouseY, 54);
            if (row >= 0 && row < this.snapshot.players().size()) {
                ModNetwork.sendToServer(new RequestGreatCausalityPacket(
                        this.snapshot.players().get(row).playerId()
                ));
                return true;
            }
        } else if (mouseX >= left + 12 && mouseX <= left + 62 && mouseY >= 24 && mouseY <= 45) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.snapshot.playerListMode()) {
            this.scrollOffset = Mth.clamp(
                    this.scrollOffset - (int) Math.signum(delta),
                    0,
                    Math.max(0, this.snapshot.records().size() - MAX_VISIBLE_ROWS)
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!this.snapshot.playerListMode()) {
                onClose();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void renderPlayerList(
            GuiGraphics guiGraphics,
            int left,
            int right,
            int mouseX,
            int mouseY
    ) {
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("当前界域 · " + this.snapshot.players().size() + "人"),
                this.width / 2,
                42,
                MUTED_GOLD
        );
        List<CausalityPlayerSnapshot> players = this.snapshot.players();
        for (int index = 0; index < players.size(); index++) {
            int top = 54 + index * ROW_HEIGHT;
            if (top + ROW_HEIGHT > this.height - 28) {
                break;
            }
            boolean hovered = mouseX >= left + 18 && mouseX <= right - 18
                    && mouseY >= top && mouseY < top + ROW_HEIGHT;
            if (hovered) {
                guiGraphics.fill(left + 14, top - 1, right - 14, top + ROW_HEIGHT - 2, 0x553A3042);
            }
            guiGraphics.drawString(this.font, players.get(index).playerName(), left + 24, top + 5, hovered ? GOLD : TEXT);
            guiGraphics.drawString(
                    this.font,
                    players.get(index).cultivation(),
                    right - 150,
                    top + 5,
                    MUTED_TEXT
            );
            guiGraphics.hLine(left + 18, right - 18, top + ROW_HEIGHT - 3, 0x553F3D46);
        }
    }

    private void renderHistory(GuiGraphics guiGraphics, int left, int right) {
        if (this.snapshot.target() != null) {
            guiGraphics.drawString(
                    this.font,
                    this.snapshot.target().playerName() + " · " + this.snapshot.target().cultivation(),
                    left + 64,
                    38,
                    TEXT
            );
            guiGraphics.drawString(
                    this.font,
                    this.snapshot.fullHistory() ? "因果尽显" : "因果受限",
                    right - 86,
                    38,
                    this.snapshot.fullHistory() ? GOLD : MUTED_GOLD
            );
        }
        guiGraphics.hLine(left + 30, right - 30, 58, LINE);
        List<CausalityRecord> records = this.snapshot.records();
        int start = Math.min(this.scrollOffset, Math.max(0, records.size() - MAX_VISIBLE_ROWS));
        for (int index = start; index < records.size() && index < start + MAX_VISIBLE_ROWS; index++) {
            int row = index - start;
            int centerX = left + 42 + (row % 2) * 28;
            int y = 78 + row * 30;
            guiGraphics.fill(centerX - 3, y - 3, centerX + 4, y + 4, GOLD);
            guiGraphics.hLine(centerX + 5, right - 24, y, 0x664B4437);
            CausalityRecord record = records.get(index);
            String text = record.description();
            String visibleText = this.font.plainSubstrByWidth(text, right - centerX - 44);
            guiGraphics.drawString(this.font, visibleText, centerX + 14, y - 5, TEXT);
            guiGraphics.drawString(
                    this.font,
                    record.category() + " · " + record.dimension(),
                    centerX + 14,
                    y + 8,
                    MUTED_TEXT
            );
        }
        if (records.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.literal("尚未留下可见因果"), this.width / 2, 100, MUTED_TEXT);
        }
    }

    private int getRowAt(double mouseY, int top) {
        int row = (int) ((mouseY - top) / ROW_HEIGHT);
        return mouseY >= top && row >= 0 ? row : -1;
    }
}
