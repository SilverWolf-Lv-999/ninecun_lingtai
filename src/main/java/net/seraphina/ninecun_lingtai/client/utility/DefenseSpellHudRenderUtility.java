package net.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import net.seraphina.ninecun_lingtai.client.player.ClientDefenseSpellManager;
import net.seraphina.ninecun_lingtai.common.player.DefenseSpellSnapshot;
import net.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import net.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.seraphina.ninecun_lingtai.utility.DefenseSpellUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.List;

public final class DefenseSpellHudRenderUtility {
    private static final int LEFT = 6;
    private static final int TOP = 6;
    private static final int CARD_WIDTH = 148;
    private static final int CARD_HEIGHT = 34;
    private static final int CARD_GAP = 4;
    private static final int FRAME_COLOR = 0xC0151820;
    private static final int INNER_COLOR = 0xD0262B35;
    private static final int TRACK_COLOR = 0xC0080B10;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
            return;
        }
        List<DefenseSpellSnapshot> activeSpells = ClientDefenseSpellManager.getActiveSpells(
                minecraft.player.getId()
        );
        if (activeSpells.isEmpty()) {
            return;
        }
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int top = TOP;
        for (DefenseSpellSnapshot snapshot : activeSpells) {
            renderSpellCard(guiGraphics, minecraft.font, snapshot, LEFT, top);
            top += CARD_HEIGHT + CARD_GAP;
        }
    }

    private static void renderSpellCard(
            GuiGraphics guiGraphics,
            Font font,
            DefenseSpellSnapshot snapshot,
            int left,
            int top
    ) {
        int right = left + CARD_WIDTH;
        int bottom = top + CARD_HEIGHT;
        int spellColor = DefenseSpellUtility.getColor(snapshot.kind(), snapshot.elementMask());
        guiGraphics.fill(left, top, right, bottom, FRAME_COLOR);
        guiGraphics.fill(left + 1, top + 1, right - 1, bottom - 1, INNER_COLOR);
        guiGraphics.fill(left + 2, top + 2, left + 4, bottom - 2, 0xFF000000 | spellColor);

        drawFittedText(
                guiGraphics,
                font,
                Component.literal(snapshot.kind().getChineseName()),
                left + 8,
                top + 4,
                CARD_WIDTH - 14,
                PRIMARY_TEXT_COLOR
        );
        String detailText = "余时 "
                + ChineseNumberUtility.formatDurationTicks(snapshot.remainingTicks())
                + "　护御 "
                + ChineseNumberUtility.toCompactChinese(snapshot.current(), 1)
                + "／"
                + ChineseNumberUtility.toCompactChinese(snapshot.maximum(), 1)
                + "点";
        drawFittedText(
                guiGraphics,
                font,
                Component.literal(detailText),
                left + 8,
                top + 15,
                CARD_WIDTH - 14,
                SECONDARY_TEXT_COLOR
        );

        int barLeft = left + 8;
        int barRight = right - 6;
        int barTop = bottom - 7;
        int barBottom = bottom - 4;
        guiGraphics.fill(barLeft, barTop, barRight, barBottom, TRACK_COLOR);
        double coverage = Mth.clamp(snapshot.ratio(), 0.0D, 1.0D);
        int fillRight = barLeft + (int) Math.round((barRight - barLeft) * coverage);
        if (fillRight > barLeft) {
            guiGraphics.fill(
                    barLeft,
                    barTop,
                    fillRight,
                    barBottom,
                    ColorUtility.withAlpha(spellColor, 0.90F)
            );
            guiGraphics.fill(barLeft, barTop, fillRight, barTop + 1, 0x42FFFFFF);
        }
    }

    private static void drawFittedText(
            GuiGraphics guiGraphics,
            Font font,
            Component text,
            int left,
            int top,
            int maximumWidth,
            int color
    ) {
        int textWidth = Math.max(1, font.width(text));
        float scale = Math.min(1.0F, maximumWidth / (float) textWidth);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(left, top, 0.0F);
        poseStack.scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, color, false);
        poseStack.popPose();
    }
}
