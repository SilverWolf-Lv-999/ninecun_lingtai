package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridClickPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridExitPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridFlowPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.LockGridResultPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenLockGridPacket;
import net.seraphina.ninecun_lingtai.common.seal.lockgrid.LockGridBoard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 锁纹对弈盘面（纯渲染 + 输入回传，游戏不暂停）。
 * 星轨节点/邻接边/雾区来自开屏包；真假流光是服务端下发的渲染帧事件，
 * 客户端只做补间动画——本类不知道也不可能知道主径答案。
 * 音高进度反馈：每命中一节半音递升。
 */
public final class LockGridScreen extends Screen {
    private static final int[] ELEMENT_COLORS = {
            0xFFE8D982, // 金
            0xFF7CCB78, // 木
            0xFF6FB6E8, // 水
            0xFFE87070, // 火
            0xFFC89B5F  // 土
    };
    private static final int NODE_RADIUS = 9;
    private static final int EDGE_COLOR = 0x40AABBD0;
    private static final int FOG_COLOR = 0xB8141820;

    private final OpenLockGridPacket data;
    private final List<ActiveFlow> flows = new ArrayList<>();
    private final List<Integer> confirmedNodes = new ArrayList<>();
    private final Set<Integer> confirmedSet = new HashSet<>();

    private int clickSequence;
    private int progress;
    private int tolerance;
    private int flashNodeIndex = -1;
    private int flashTicks;
    private int flashColor;
    private long pressStartMillis = -1L;
    private int pressedNodeIndex = -1;
    private int ageTicks;
    private Component statusLine = Component.empty();

    private LockGridScreen(OpenLockGridPacket data) {
        super(Component.translatable("screen.ninecun_lingtai.lock_grid.title"));
        this.data = data;
        this.tolerance = data.tolerance();
    }

    // ------------------------------------------------------------------
    // 静态入口（包处理器调用）
    // ------------------------------------------------------------------

    public static void open(OpenLockGridPacket packet) {
        Minecraft.getInstance().setScreen(new LockGridScreen(packet));
    }

    public static void acceptFlow(LockGridFlowPacket packet) {
        if (Minecraft.getInstance().screen instanceof LockGridScreen screen) {
            screen.flows.add(new ActiveFlow(packet, screen.ageTicks));
        }
    }

    public static void acceptResult(LockGridResultPacket packet) {
        if (Minecraft.getInstance().screen instanceof LockGridScreen screen) {
            screen.handleResult(packet);
        }
    }

    // ------------------------------------------------------------------
    // 结果处理
    // ------------------------------------------------------------------

    private void handleResult(LockGridResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        this.progress = packet.progress();
        this.tolerance = packet.tolerance();
        switch (packet.result()) {
            case HIT, STABLE -> {
                if (packet.nodeIndex() >= 0 && !this.confirmedSet.contains(packet.nodeIndex())) {
                    this.confirmedNodes.add(packet.nodeIndex());
                    this.confirmedSet.add(packet.nodeIndex());
                }
                flash(packet.nodeIndex(), 0xFF9BE8A8);
                // 半音递升：pitch = 2^(n/12)
                float pitch = (float) Math.pow(2.0D, (this.progress % 13) / 12.0D) * 0.7F;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.AMETHYST_BLOCK_CHIME, pitch, 1.0F));
                if (packet.result() == LockGridResultPacket.Result.STABLE) {
                    this.statusLine = Component.translatable(
                            "screen.ninecun_lingtai.lock_grid.stable");
                }
            }
            case TREMOR -> {
                flash(packet.nodeIndex(), 0xFFE8D982);
                this.statusLine = Component.translatable("screen.ninecun_lingtai.lock_grid.tremor");
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5F, 0.8F));
            }
            case SETBACK -> {
                truncateConfirmed(this.progress);
                flash(packet.nodeIndex(), 0xFFE05555);
                this.statusLine = Component.translatable("screen.ninecun_lingtai.lock_grid.setback");
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.CHAIN_BREAK, 0.7F, 1.0F));
            }
            case LAYER_BROKEN -> {
                this.statusLine = Component.translatable(
                        "screen.ninecun_lingtai.lock_grid.layer_broken");
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 1.0F));
                // 里层盘面随 GAP 后的新开屏包到来
            }
            case FAILED, EXPELLED, COMPLETE, DETONATED -> onClose();
        }
    }

    private void truncateConfirmed(int keep) {
        while (this.confirmedNodes.size() > keep) {
            this.confirmedSet.remove(this.confirmedNodes.remove(this.confirmedNodes.size() - 1));
        }
    }

    private void flash(int nodeIndex, int color) {
        this.flashNodeIndex = nodeIndex;
        this.flashTicks = 12;
        this.flashColor = color;
    }

    // ------------------------------------------------------------------
    // 布局
    // ------------------------------------------------------------------

    @Override
    protected void init() {
        int buttonY = this.height - 26;
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.ninecun_lingtai.lock_grid.retreat"),
                        pressed -> {
                            ModNetwork.sendToServer(LockGridExitPacket.of(
                                    this.data.sessionNonce(), LockGridExitPacket.Mode.RETREAT));
                            onClose();
                        })
                .bounds(this.width / 2 - 130, buttonY, 120, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.ninecun_lingtai.lock_grid.force_break"),
                        pressed -> ModNetwork.sendToServer(LockGridExitPacket.of(
                                this.data.sessionNonce(), LockGridExitPacket.Mode.FORCE_BREAK)))
                .bounds(this.width / 2 + 10, buttonY, 120, 20)
                .build());
    }

    private float boardScale() {
        return Math.min(this.width, this.height - 70) * 0.82F / LockGridBoard.VIRTUAL_SIZE;
    }

    private int toScreenX(int virtualX) {
        float scale = boardScale();
        float offset = (this.width - LockGridBoard.VIRTUAL_SIZE * scale) / 2.0F;
        return Math.round(offset + virtualX * scale);
    }

    private int toScreenY(int virtualY) {
        float scale = boardScale();
        float offset = (this.height - 40 - LockGridBoard.VIRTUAL_SIZE * scale) / 2.0F + 10.0F;
        return Math.round(offset + virtualY * scale);
    }

    // ------------------------------------------------------------------
    // 输入：按住节点 ≥300ms 判定为落子（服务端复核 holdMillis）
    // ------------------------------------------------------------------

    private int findNodeAt(double mouseX, double mouseY) {
        for (int index = 0; index < this.data.nodes().size(); index++) {
            OpenLockGridPacket.NodeEntry node = this.data.nodes().get(index);
            int x = toScreenX(node.x());
            int y = toScreenY(node.y());
            double dx = mouseX - x;
            double dy = mouseY - y;
            if (dx * dx + dy * dy <= (NODE_RADIUS + 3) * (NODE_RADIUS + 3)) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 0) {
            int nodeIndex = findNodeAt(mouseX, mouseY);
            if (nodeIndex >= 0) {
                this.pressedNodeIndex = nodeIndex;
                this.pressStartMillis = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.pressedNodeIndex >= 0) {
            int holdMillis = (int) Math.min(Integer.MAX_VALUE,
                    System.currentTimeMillis() - this.pressStartMillis);
            int releasedNode = findNodeAt(mouseX, mouseY);
            if (releasedNode == this.pressedNodeIndex) {
                ModNetwork.sendToServer(new LockGridClickPacket(
                        this.data.sessionNonce(),
                        this.clickSequence++,
                        this.pressedNodeIndex,
                        holdMillis
                ));
            }
            this.pressedNodeIndex = -1;
            this.pressStartMillis = -1L;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ------------------------------------------------------------------
    // 渲染
    // ------------------------------------------------------------------

    @Override
    public void tick() {
        this.ageTicks++;
        if (this.flashTicks > 0) {
            this.flashTicks--;
        }
        this.flows.removeIf(flow -> this.ageTicks - flow.startTick > flow.packet.runTicks() + 10);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xA8060810);
        // 邻接边
        for (int[] edge : this.data.edges()) {
            OpenLockGridPacket.NodeEntry a = this.data.nodes().get(edge[0]);
            OpenLockGridPacket.NodeEntry b = this.data.nodes().get(edge[1]);
            drawLine(guiGraphics, toScreenX(a.x()), toScreenY(a.y()),
                    toScreenX(b.x()), toScreenY(b.y()), EDGE_COLOR);
        }
        // 流光（真假混发，客户端无从分辨——只有呼吸感标记）
        float partialAge = this.ageTicks + partialTick;
        for (ActiveFlow flow : this.flows) {
            renderFlow(guiGraphics, flow, partialAge);
        }
        // 节点
        for (int index = 0; index < this.data.nodes().size(); index++) {
            renderNode(guiGraphics, index);
        }
        // 抬头信息
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 0xFFE8E8F8);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.ninecun_lingtai.lock_grid.status",
                        this.data.layerOrdinal(),
                        this.progress,
                        Math.max(0, this.tolerance)),
                this.width / 2, 22, 0xFFB8C4D8);
        if (this.data.deathPact()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.ninecun_lingtai.lock_grid.death_pact"),
                    this.width / 2, 34, 0xFFE05555);
        }
        guiGraphics.drawCenteredString(this.font, this.statusLine,
                this.width / 2, this.height - 44, 0xFF9BA8C0);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderNode(GuiGraphics guiGraphics, int index) {
        OpenLockGridPacket.NodeEntry node = this.data.nodes().get(index);
        int x = toScreenX(node.x());
        int y = toScreenY(node.y());
        int color = ELEMENT_COLORS[Mth.clamp(node.elementOrdinal(), 0, ELEMENT_COLORS.length - 1)];
        if (node.fog()) {
            // 雾区：只见轮廓不辨五行
            fillCircle(guiGraphics, x, y, NODE_RADIUS, FOG_COLOR);
            fillCircle(guiGraphics, x, y, 3, 0xFF3A4150);
            return;
        }
        boolean confirmed = this.confirmedSet.contains(index);
        boolean isStart = index == this.data.startNode();
        fillCircle(guiGraphics, x, y, NODE_RADIUS, confirmed ? color : (color & 0x00FFFFFF) | 0x66000000);
        fillCircle(guiGraphics, x, y, 4, color);
        if (isStart && !confirmed) {
            // 引位常亮光环
            fillCircle(guiGraphics, x, y, NODE_RADIUS + 3, (color & 0x00FFFFFF) | 0x30000000);
        }
        if (this.flashTicks > 0 && this.flashNodeIndex == index) {
            int alpha = Math.min(0xC0, this.flashTicks * 16);
            fillCircle(guiGraphics, x, y, NODE_RADIUS + 4,
                    (this.flashColor & 0x00FFFFFF) | (alpha << 24));
        }
        if (this.pressedNodeIndex == index) {
            fillCircle(guiGraphics, x, y, NODE_RADIUS + 2, 0x50FFFFFF);
        }
    }

    /** 流光：光点沿路径 easeInOut 补间；呼吸感 = 亮度随相位脉动（真流光的破绽）。 */
    private void renderFlow(GuiGraphics guiGraphics, ActiveFlow flow, float partialAge) {
        List<Integer> nodeIndices = flow.packet.nodeIndices();
        if (nodeIndices.size() < 2) {
            return;
        }
        float t = (partialAge - flow.startTick) / flow.packet.runTicks();
        if (t < 0.0F || t > 1.0F) {
            return;
        }
        float eased = t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2) / 2.0F;
        float position = eased * (nodeIndices.size() - 1);
        int segment = Math.min(nodeIndices.size() - 2, (int) position);
        float segmentT = position - segment;
        OpenLockGridPacket.NodeEntry a = this.data.nodes().get(nodeIndices.get(segment));
        OpenLockGridPacket.NodeEntry b = this.data.nodes().get(nodeIndices.get(segment + 1));
        int x = Math.round(Mth.lerp(segmentT, toScreenX(a.x()), toScreenX(b.x())));
        int y = Math.round(Mth.lerp(segmentT, toScreenY(a.y()), toScreenY(b.y())));
        int alpha = 0xB0;
        if (flow.packet.breathing()) {
            alpha = (int) (0x60 + 0x60 * (0.5F + 0.5F * Mth.sin(partialAge * 0.35F)));
        }
        fillCircle(guiGraphics, x, y, 5, (alpha << 24) | 0x00D8E8FF);
        fillCircle(guiGraphics, x, y, 2, 0xFFFFFFFF);
    }

    private static void fillCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int halfWidth = (int) Math.floor(Math.sqrt(radius * radius - dy * dy));
            guiGraphics.fill(centerX - halfWidth, centerY + dy,
                    centerX + halfWidth + 1, centerY + dy + 1, color);
        }
    }

    private static void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        if (steps == 0) {
            return;
        }
        for (int step = 0; step <= steps; step++) {
            int x = x1 + (x2 - x1) * step / steps;
            int y = y1 + (y2 - y1) * step / steps;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // ESC = 主动收手（惊扰 +1），不给白嫖退出
        ModNetwork.sendToServer(LockGridExitPacket.of(
                this.data.sessionNonce(), LockGridExitPacket.Mode.RETREAT));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ActiveFlow {
        private final LockGridFlowPacket packet;
        private final int startTick;

        private ActiveFlow(LockGridFlowPacket packet, int startTick) {
            this.packet = packet;
            this.startTick = startTick;
        }
    }
}
