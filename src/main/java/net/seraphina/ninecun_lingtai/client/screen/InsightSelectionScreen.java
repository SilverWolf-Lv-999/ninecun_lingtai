package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.SelectInsightPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 悟道印记三选一界面（T14）。候选 id 由 OpenInsightSelectionPacket 下发，
 * 名称/描述走语言键 insight.ninecun_lingtai.&lt;path 以 . 连接&gt;.name / .desc。
 * 关界面不选不作废：候选保留至过期（服务端记账），可由背包界面提醒再次打开（后续接线）。
 */
public final class InsightSelectionScreen extends Screen {
    private static final int BUTTON_WIDTH = 240;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 46;
    private static final int DESCRIPTION_COLOR = 0xFFB8C4D8;

    private final List<ResourceLocation> candidateIds;

    public InsightSelectionScreen(List<ResourceLocation> candidateIds) {
        super(Component.translatableWithFallback(
                "message.ninecun_lingtai.insight_selection_title",
                "悟道印记 · 三择其一，落子无悔"
        ));
        this.candidateIds = List.copyOf(candidateIds);
    }

    @Override
    protected void init() {
        int startX = (this.width - BUTTON_WIDTH) / 2;
        int startY = (this.height - this.candidateIds.size() * BUTTON_SPACING) / 2 + 12;
        for (int index = 0; index < this.candidateIds.size(); index++) {
            ResourceLocation candidateId = this.candidateIds.get(index);
            addRenderableWidget(Button.builder(
                            Component.translatable(getTranslationKeyBase(candidateId) + ".name"),
                            pressed -> select(candidateId)
                    )
                    .bounds(startX, startY + index * BUTTON_SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
    }

    private void select(ResourceLocation candidateId) {
        ModNetwork.sendToServer(new SelectInsightPacket(candidateId));
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 28, 0xFFFFFF);
        int startX = this.width / 2;
        int startY = (this.height - this.candidateIds.size() * BUTTON_SPACING) / 2 + 12;
        for (int index = 0; index < this.candidateIds.size(); index++) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable(getTranslationKeyBase(this.candidateIds.get(index)) + ".desc"),
                    startX,
                    startY + index * BUTTON_SPACING + BUTTON_HEIGHT + 4,
                    DESCRIPTION_COLOR
            );
        }
    }

    private static String getTranslationKeyBase(ResourceLocation insightId) {
        return "insight." + ModMain.MOD_ID + "." + insightId.getPath().replace('/', '.');
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
