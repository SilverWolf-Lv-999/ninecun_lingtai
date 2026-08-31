package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.CancelBreakthroughRitualPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * 引灵仪式 HUD（进度条 + "约 X 成把握"文案 + 拖后腿提示）。客户端状态由
 * {@code SyncBreakthroughRitualPacket} 驱动（约每秒一次），渲染时按本地刻外插平滑。
 *
 * <p>静态可挂渲染方法：{@code BreakthroughRitualHud.render(guiGraphics, screenWidth, screenHeight)}。
 * TODO(接线): 由主控挂到 ClientForgeEventHandler 的 HUD 绘制事件
 * （RenderGuiOverlayEvent.Post，挂点代码见开发日志"需集中接线"）。</p>
 */
public final class BreakthroughRitualHud {

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int BAR_BOTTOM_OFFSET = 66;
    private static final int BAR_BACKGROUND_COLOR = 0xC0101018;
    private static final int BAR_FILL_COLOR = 0xFF7FD8FF;
    private static final int BAR_BORDER_COLOR = 0xFF3A5A78;
    private static final int TEXT_COLOR = 0xFFE8F4FF;
    private static final int DRAGGING_TEXT_COLOR = 0xFFF0A860;

    private static boolean active;
    private static int remainingTicks;
    private static int totalTicks;
    private static String graspBucketKey = "";
    private static String draggingModifierId = "";
    /** 收到同步时的本地 tickCount，用于两次同步之间的外插。 */
    private static int syncedAtClientTick;

    private BreakthroughRitualHud() {
    }

    /** SyncBreakthroughRitualPacket 的客户端入口。 */
    public static void applySync(
            boolean syncedActive,
            int syncedRemainingTicks,
            int syncedTotalTicks,
            String syncedGraspBucketKey,
            String syncedDraggingModifierId
    ) {
        active = syncedActive;
        remainingTicks = syncedRemainingTicks;
        totalTicks = Math.max(1, syncedTotalTicks);
        graspBucketKey = syncedGraspBucketKey == null ? "" : syncedGraspBucketKey;
        draggingModifierId = syncedDraggingModifierId == null ? "" : syncedDraggingModifierId;
        Minecraft minecraft = Minecraft.getInstance();
        syncedAtClientTick = minecraft.player == null ? 0 : minecraft.player.tickCount;
    }

    public static boolean isActive() {
        return active;
    }

    /** 主动取消入口（供取消按键/按钮调用）：发 CancelBreakthroughRitualPacket。 */
    public static void requestCancel() {
        if (active) {
            ModNetwork.sendToServer(CancelBreakthroughRitualPacket.INSTANCE);
        }
    }

    /** 静态可挂渲染方法：进度条居中悬于快捷栏上方。 */
    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!active) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        int extrapolated = Math.max(
                0,
                remainingTicks - Math.max(0, minecraft.player.tickCount - syncedAtClientTick)
        );
        float progress = Mth.clamp(1.0F - (float) extrapolated / totalTicks, 0.0F, 1.0F);

        int barLeft = (screenWidth - BAR_WIDTH) / 2;
        int barTop = screenHeight - BAR_BOTTOM_OFFSET;
        guiGraphics.fill(
                barLeft - 1,
                barTop - 1,
                barLeft + BAR_WIDTH + 1,
                barTop + BAR_HEIGHT + 1,
                BAR_BORDER_COLOR
        );
        guiGraphics.fill(barLeft, barTop, barLeft + BAR_WIDTH, barTop + BAR_HEIGHT, BAR_BACKGROUND_COLOR);
        guiGraphics.fill(
                barLeft,
                barTop,
                barLeft + Math.round(BAR_WIDTH * progress),
                barTop + BAR_HEIGHT,
                BAR_FILL_COLOR
        );

        Font font = minecraft.font;
        Component title = Component.translatableWithFallback(
                "message.ninecun_lingtai.breakthrough_ritual_hud",
                "引灵仪式 · %s秒",
                (extrapolated + 19) / 20
        );
        Component grasp = graspBucketKey.isEmpty()
                ? Component.empty()
                : Component.translatableWithFallback(
                        "message.ninecun_lingtai.breakthrough_" + graspBucketKey,
                        graspBucketKey
                );
        guiGraphics.drawCenteredString(
                font,
                Component.empty().append(title).append(" · ").append(grasp),
                screenWidth / 2,
                barTop - 12,
                TEXT_COLOR
        );
        if (!draggingModifierId.isEmpty()) {
            guiGraphics.drawCenteredString(
                    font,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.breakthrough_dragging_" + draggingModifierId,
                            "拖后腿：" + draggingModifierId
                    ),
                    screenWidth / 2,
                    barTop + BAR_HEIGHT + 4,
                    DRAGGING_TEXT_COLOR
            );
        }
    }
}
