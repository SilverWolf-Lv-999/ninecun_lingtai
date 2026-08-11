package com.seraphina.ninecun_lingtai.client.utility;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import com.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;
import com.seraphina.ninecun_lingtai.utility.ChineseNumberUtility;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SpiritualEnergyHudRenderUtility {
    private static final int LEFT = 6;
    private static final int BOTTOM_MARGIN = 6;
    private static final int BAR_WIDTH = 148;
    private static final int BAR_HEIGHT = 7;
    private static final int FRAME_COLOR = 0xC0151820;
    private static final int INNER_COLOR = 0xD02C3038;
    private static final int TRACK_COLOR = 0xC004080D;
    private static final int TEXT_COLOR = 0xFFF0E5CA;
    private static final List<ElementType> ELEMENT_DISPLAY_ORDER = List.of(
            ElementType.WATER,
            ElementType.WOOD,
            ElementType.FIRE,
            ElementType.EARTH,
            ElementType.METAL,
            ElementType.YIN_YANG,
            ElementType.THUNDER,
            ElementType.CHAOS
    );

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
            return;
        }
        Optional<PlayerCultivationInfo> cultivationInfoOptional =
                ClientCultivationInfoManager.getCultivationInfo();
        if (cultivationInfoOptional.isEmpty() || cultivationInfoOptional.get().spiritualRoot().isEmpty()) {
            return;
        }
        PlayerCultivationInfo cultivationInfo = cultivationInfoOptional.get();
        Set<ElementType> rootElements = SpiritualRootUtility.getIndependentRootElements(
                cultivationInfo.spiritualRoot().orElseThrow()
        );
        if (rootElements.isEmpty()) {
            return;
        }

        double totalEnergy = rootElements.stream()
                .mapToDouble(cultivationInfo::getSpiritualEnergy)
                .sum();

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int bottom = event.getWindow().getGuiScaledHeight() - BOTTOM_MARGIN;
        int top = bottom - BAR_HEIGHT;
        String label = "灵力储量　"
                + ChineseNumberUtility.toCompactChinese(totalEnergy, 1)
                + "点";
        drawFittedText(guiGraphics, minecraft.font, Component.literal(label), LEFT, top - 11, BAR_WIDTH, TEXT_COLOR);

        guiGraphics.fill(LEFT - 2, top - 2, LEFT + BAR_WIDTH + 2, bottom + 2, FRAME_COLOR);
        guiGraphics.fill(LEFT - 1, top - 1, LEFT + BAR_WIDTH + 1, bottom + 1, INNER_COLOR);
        guiGraphics.fill(LEFT, top, LEFT + BAR_WIDTH, bottom, TRACK_COLOR);
        renderEnergySegments(guiGraphics, cultivationInfo, rootElements, totalEnergy, top, bottom);
    }

    private static void renderEnergySegments(
            GuiGraphics guiGraphics,
            PlayerCultivationInfo cultivationInfo,
            Set<ElementType> rootElements,
            double totalEnergy,
            int top,
            int bottom
    ) {
        if (totalEnergy <= 0.0D) {
            return;
        }
        double accumulatedEnergy = 0.0D;
        int segmentLeft = LEFT;
        for (ElementType element : ELEMENT_DISPLAY_ORDER) {
            if (!rootElements.contains(element)) {
                continue;
            }
            double elementEnergy = cultivationInfo.getSpiritualEnergy(element);
            if (elementEnergy <= 0.0D) {
                continue;
            }
            accumulatedEnergy += elementEnergy;
            int segmentRight = LEFT + (int) Math.round(BAR_WIDTH * accumulatedEnergy / totalEnergy);
            guiGraphics.fill(
                    segmentLeft,
                    top,
                    segmentRight,
                    bottom,
                    ColorUtility.withAlpha(SpiritualRootUtility.getElementColor(element), 0.90F)
            );
            segmentLeft = segmentRight;
        }
        guiGraphics.fill(LEFT, top, LEFT + BAR_WIDTH, top + 1, 0x4AFFFFFF);
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
