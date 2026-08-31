package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.client.player.ClientSoulInjuryData;
import net.seraphina.ninecun_lingtai.utility.SoulInjuryUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * 七魄星图 HUD：默认收起为屏幕右上角小星阵，星的明暗来自服务端下发的定性星图。
 * 渲染方法为静态可挂式——需集中接线：在 ClientForgeEventHandler.onRenderGuiOverlay
 * 里追加 SoulConstellationOverlay.render(event)（设计文档 R7：与现有 HUD 并列，避免拥挤）。
 * TODO(M4.5): 按键/修炼界面内页展开大图。
 */
public final class SoulConstellationOverlay {
    private static final int RIGHT_MARGIN = 8;
    private static final int TOP_MARGIN = 40;
    private static final int STAR_SIZE = 2;
    /** 七魄星阵相对坐标（斗形排布）。 */
    private static final int[][] STAR_OFFSETS = {
            {0, 9}, {5, 5}, {11, 3}, {17, 5}, {21, 10}, {24, 15}, {28, 19}
    };
    private static final int CONSTELLATION_WIDTH = 30;
    /** PERIL 档体感层：星图整体以此周期呼吸失真。 */
    private static final float PERIL_PULSE_PERIOD_TICKS = 30.0F;

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null || minecraft.level == null) {
            return;
        }
        SoulInjuryUtility.InjuryTier tier = ClientSoulInjuryData.getTier();
        if (tier == SoulInjuryUtility.InjuryTier.NONE) {
            return;
        }
        GuiGraphics guiGraphics = event.getGuiGraphics();
        int left = event.getWindow().getGuiScaledWidth() - RIGHT_MARGIN - CONSTELLATION_WIDTH;
        float pulse = tier == SoulInjuryUtility.InjuryTier.PERIL
                ? 0.65F + 0.35F * Mth.sin(
                        (minecraft.player.tickCount + (float) event.getPartialTick())
                                * ((float) Math.PI * 2.0F / PERIL_PULSE_PERIOD_TICKS))
                : 1.0F;
        for (int index = 0; index < STAR_OFFSETS.length; index++) {
            int brightness = (int) (ClientSoulInjuryData.getStarBrightness(index) * pulse);
            int starLeft = left + STAR_OFFSETS[index][0];
            int starTop = TOP_MARGIN + STAR_OFFSETS[index][1];
            // 暗淡的魄用暗红底衬出"受创"体感，明亮的魄呈月白
            int color = brightness < 96
                    ? packColor(Math.max(48, brightness + 32), brightness / 2, brightness / 2)
                    : packColor(
                            (int) (brightness * 0.85F),
                            (int) (brightness * 0.92F),
                            brightness
                    );
            guiGraphics.fill(
                    starLeft,
                    starTop,
                    starLeft + STAR_SIZE,
                    starTop + STAR_SIZE,
                    color
            );
        }
    }

    private static int packColor(int red, int green, int blue) {
        return 0xFF000000
                | (Mth.clamp(red, 0, 255) << 16)
                | (Mth.clamp(green, 0, 255) << 8)
                | Mth.clamp(blue, 0, 255);
    }
}
