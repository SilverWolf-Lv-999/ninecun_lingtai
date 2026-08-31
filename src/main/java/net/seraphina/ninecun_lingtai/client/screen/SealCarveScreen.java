package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenSealCarveScreenPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SealCarvePacket;
import net.seraphina.ninecun_lingtai.common.seal.SealKind;
import net.seraphina.ninecun_lingtai.common.seal.SealManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

/**
 * 刻印界面（M4 扩至六种）：认主/传讯/隐匿/反噬/自毁/遗念 + 自毁阈值档 +
 * 遗念遗言输入 + 废除最外层。只展示服务端下发的主人视角信息；
 * 确认后 30 秒潜行仪式由服务端驱动。新三种文案带中文回退（无 lang 可跑）。
 */
public final class SealCarveScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int KIND_BUTTON_WIDTH = 98;
    private static final int BUTTON_HEIGHT = 20;
    private static final int[] THRESHOLD_STEPS = {50, 60, 70, 80, 90};
    private static final SealKind[] KIND_ORDER = {
            SealKind.OWNERSHIP, SealKind.MESSAGE,
            SealKind.CONCEAL, SealKind.BACKLASH,
            SealKind.DETONATE, SealKind.LEGACY
    };
    private static final String[] KIND_FALLBACK_NAMES = {
            "认主禁（一阶）", "传讯禁（一阶）",
            "隐匿禁（二阶）", "反噬禁（三阶）",
            "自毁禁（三阶）", "遗念禁（四阶）"
    };

    private final OpenSealCarveScreenPacket data;
    private int thresholdIndex = 2;
    private Button thresholdButton;
    private EditBox legacyMessageBox;

    private SealCarveScreen(OpenSealCarveScreenPacket data) {
        super(Component.translatable("screen.ninecun_lingtai.seal_carve.title"));
        this.data = data;
    }

    public static void open(OpenSealCarveScreenPacket packet) {
        Minecraft.getInstance().setScreen(new SealCarveScreen(packet));
    }

    @Override
    protected void init() {
        int startX = (this.width - BUTTON_WIDTH) / 2;
        int y = this.height / 2 - 62;
        boolean anchorsLeft = this.data.availableAnchors() - this.data.usedAnchors() >= 1
                && this.data.ownLayerKindOrdinals().size() < this.data.layerCap();
        for (int index = 0; index < KIND_ORDER.length; index++) {
            SealKind kind = KIND_ORDER[index];
            int column = index % 2;
            int row = index / 2;
            Button button = Button.builder(
                            Component.translatableWithFallback(
                                    "seal.ninecun_lingtai." + kind.getSerializedName() + ".name",
                                    KIND_FALLBACK_NAMES[index]),
                            pressed -> carve(kind))
                    .bounds(startX + column * (KIND_BUTTON_WIDTH + 4),
                            y + row * 24, KIND_BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            button.active = anchorsLeft;
            addRenderableWidget(button);
        }
        y += 3 * 24;
        this.thresholdButton = Button.builder(thresholdLabel(), pressed -> {
                    this.thresholdIndex = (this.thresholdIndex + 1) % THRESHOLD_STEPS.length;
                    this.thresholdButton.setMessage(thresholdLabel());
                })
                .bounds(startX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        addRenderableWidget(this.thresholdButton);
        y += 24;
        this.legacyMessageBox = new EditBox(
                this.font, startX, y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatableWithFallback(
                        "screen.ninecun_lingtai.seal_carve.legacy_message", "遗念禁遗言"));
        this.legacyMessageBox.setMaxLength(SealManager.LEGACY_MESSAGE_MAX_LENGTH);
        this.legacyMessageBox.setHint(Component.translatableWithFallback(
                "screen.ninecun_lingtai.seal_carve.legacy_hint",
                "刻遗念禁前，在此录下一句遗言…"));
        addRenderableWidget(this.legacyMessageBox);
        y += 28;
        Button revokeButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.seal_carve.revoke"),
                        pressed -> {
                            ModNetwork.sendToServer(new SealCarvePacket(
                                    this.data.hand() == InteractionHand.MAIN_HAND, -1, 0, ""));
                            onClose();
                        })
                .bounds(startX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        revokeButton.active = !this.data.ownLayerKindOrdinals().isEmpty();
        addRenderableWidget(revokeButton);
    }

    private Component thresholdLabel() {
        return Component.translatable(
                "screen.ninecun_lingtai.seal_carve.threshold", THRESHOLD_STEPS[this.thresholdIndex]);
    }

    private void carve(SealKind kind) {
        String legacyMessage = this.legacyMessageBox == null ? "" : this.legacyMessageBox.getValue();
        if (kind == SealKind.LEGACY && legacyMessage.isBlank()) {
            this.legacyMessageBox.setFocused(true);
            return;
        }
        ModNetwork.sendToServer(new SealCarvePacket(
                this.data.hand() == InteractionHand.MAIN_HAND,
                kind.ordinal(),
                THRESHOLD_STEPS[this.thresholdIndex],
                kind == SealKind.LEGACY ? legacyMessage : ""
        ));
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFFFFF);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.ninecun_lingtai.seal_carve.anchors",
                        this.data.usedAnchors(),
                        this.data.availableAnchors(),
                        this.data.totalAnchors()),
                this.width / 2, 40, 0xFFB8C4D8);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.ninecun_lingtai.seal_carve.layers",
                        this.data.ownLayerKindOrdinals().size(),
                        this.data.layerCap()),
                this.width / 2, 52, 0xFFB8C4D8);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.seal_carve.hint"),
                this.width / 2, this.height / 2 + 94, 0xFF8890A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
