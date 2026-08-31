package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.common.inner_demon.MindTrialPhase;
import net.seraphina.ninecun_lingtai.common.network.packet.SyncMindTrialPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.ArrayList;
import java.util.List;

/**
 * 心魔试炼 HUD（心魔即己 §7）：阶段字幕/台词/计时/心魔血量条/心潮红闪。
 * 静态可挂式：已在 {@code ClientModEventHandler.onRegisterGuiOverlays} 挂到
 * Forge overlay 链；数据源是 {@link SyncMindTrialPacket}（该包尚未在 ModNetwork
 * 注册，注册前本 Overlay 处于闲置状态，阶段信息暂经通知/聊天下发）。
 *
 * <p>决念三选一输入：注册前经聊天可点按钮（{@code /mindtrial resolve <choice>}），
 * 注册后可改为 {@code SubmitResolveChoicePacket}。</p>
 */
public final class MindTrialOverlay implements IGuiOverlay {
    public static final MindTrialOverlay INSTANCE = new MindTrialOverlay();

    private static final int PHASE_COLOR = 0xFFE7D7FF;
    private static final int TIMER_COLOR = 0xFFB0A4C9;
    private static final int DEMON_BAR_BACKGROUND = 0xAA1A0F26;
    private static final int DEMON_BAR_FILL = 0xEE8A3FC9;
    private static final int ECHO_FLASH_COLOR = 0x55FF2020;
    private static final int ECHO_FLASH_DURATION_TICKS = 12;

    private static volatile boolean active;
    private static volatile MindTrialPhase phase = MindTrialPhase.REVEAL;
    private static volatile int remainingTicks;
    private static volatile float demonHealthPct = 1.0F;
    private static final List<String> LINE_KEYS = new ArrayList<>();
    private static volatile int echoFlashTicks;

    private MindTrialOverlay() {
    }

    /** 由 SyncMindTrialPacket.handle 调用（客户端主线程）。 */
    public static void accept(SyncMindTrialPacket packet) {
        MindTrialPhase[] phases = MindTrialPhase.values();
        if (packet.phaseOrdinal() < 0 || packet.phaseOrdinal() >= phases.length) {
            reset();
            return;
        }
        active = true;
        phase = phases[packet.phaseOrdinal()];
        remainingTicks = packet.remainingTicks();
        demonHealthPct = Mth.clamp(packet.demonHealthPct(), 0.0F, 1.0F);
        if (!packet.lineKeys().isEmpty()) {
            synchronized (LINE_KEYS) {
                LINE_KEYS.clear();
                LINE_KEYS.addAll(packet.lineKeys());
            }
        }
        if (packet.bodyEchoFlash()) {
            echoFlashTicks = ECHO_FLASH_DURATION_TICKS;
        }
    }

    public static void reset() {
        active = false;
        remainingTicks = 0;
        demonHealthPct = 1.0F;
        echoFlashTicks = 0;
        synchronized (LINE_KEYS) {
            LINE_KEYS.clear();
        }
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        // 心潮震荡红闪（bodyEchoFlash）。
        if (echoFlashTicks > 0) {
            echoFlashTicks--;
            guiGraphics.fill(0, 0, width, height, ECHO_FLASH_COLOR);
        }
        int centerX = width / 2;
        int top = height / 5;
        guiGraphics.drawCenteredString(
                minecraft.font,
                phaseTitle(phase),
                centerX,
                top,
                PHASE_COLOR
        );
        int seconds = remainingTicks / 20;
        guiGraphics.drawCenteredString(
                minecraft.font,
                Component.literal(String.format("%d:%02d", seconds / 60, seconds % 60)),
                centerX,
                top + 12,
                TIMER_COLOR
        );
        // 心魔血量条（REVEAL 阶段心魔未现，不画）。
        if (phase != MindTrialPhase.REVEAL) {
            int barWidth = 120;
            int barX = centerX - barWidth / 2;
            int barY = top + 26;
            guiGraphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 5, DEMON_BAR_BACKGROUND);
            guiGraphics.fill(
                    barX,
                    barY,
                    barX + Math.round(barWidth * demonHealthPct),
                    barY + 4,
                    DEMON_BAR_FILL
            );
        }
        // REVEAL 台词滚动。
        if (phase == MindTrialPhase.REVEAL) {
            int lineY = top + 30;
            synchronized (LINE_KEYS) {
                for (String lineKey : LINE_KEYS) {
                    Component line = lineKey.startsWith("message.")
                            ? Component.translatable(lineKey)
                            : Component.literal(lineKey);
                    guiGraphics.drawCenteredString(minecraft.font, line, centerX, lineY, TIMER_COLOR);
                    lineY += 11;
                }
            }
        }
        if (phase == MindTrialPhase.RESOLVE_CHOICE) {
            guiGraphics.drawCenteredString(
                    minecraft.font,
                    Component.translatableWithFallback(
                            "message.ninecun_lingtai.mind_trial_resolve_overlay_hint",
                            "斩念 / 守念 / 认念——点击聊天栏按钮下决断"),
                    centerX,
                    top + 44,
                    PHASE_COLOR
            );
        }
    }

    private static Component phaseTitle(MindTrialPhase phase) {
        return switch (phase) {
            case REVEAL -> Component.translatableWithFallback(
                    "message.ninecun_lingtai.mind_trial_phase_reveal", "照见");
            case SPAR -> Component.translatableWithFallback(
                    "message.ninecun_lingtai.mind_trial_phase_spar", "试锋");
            case REFLECT -> Component.translatableWithFallback(
                    "message.ninecun_lingtai.mind_trial_phase_reflect", "反照");
            case RESOLVE_CHOICE -> Component.translatableWithFallback(
                    "message.ninecun_lingtai.mind_trial_phase_resolve", "决念");
            case SEVER -> Component.translatableWithFallback(
                    "message.ninecun_lingtai.mind_trial_resolve_sever", "斩念");
            case HOLD -> Component.translatableWithFallback(
                    "message.ninecun_lingtai.mind_trial_resolve_hold", "守念");
        };
    }
}
