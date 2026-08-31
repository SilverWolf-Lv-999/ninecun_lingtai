package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.common.death.DeathRoute;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.ChooseDeathRoutePacket;
import net.seraphina.ninecun_lingtai.common.network.packet.OpenDeathRouteScreenPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 死亡路线选择界面：倒计时结束由服务端按默认路线结算；无资格路线灰显并
 * 悬浮标注缺什么。ESC 关不掉——生死大事，躲是躲不掉的。
 */
public final class DeathRouteScreen extends Screen {
    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 44;

    private final OpenDeathRouteScreenPacket data;
    private int remainingTicks;
    private boolean chosen;

    private DeathRouteScreen(OpenDeathRouteScreenPacket data) {
        super(Component.translatable("screen.ninecun_lingtai.death_route.title"));
        this.data = data;
        this.remainingTicks = data.remainingSeconds() * 20;
    }

    public static void open(OpenDeathRouteScreenPacket packet) {
        Minecraft.getInstance().setScreen(new DeathRouteScreen(packet));
    }

    @Override
    protected void init() {
        int startX = (this.width - BUTTON_WIDTH) / 2;
        int startY = this.height / 2 - DeathRoute.values().length * BUTTON_SPACING / 2 + 10;
        for (DeathRoute route : DeathRoute.values()) {
            boolean available = (this.data.availableMask() & (1 << route.ordinal())) != 0;
            String keyBase = "screen.ninecun_lingtai.death_route."
                    + route.name().toLowerCase(java.util.Locale.ROOT);
            Button button = Button.builder(
                            Component.translatable(keyBase + ".name"),
                            pressed -> choose(route))
                    .bounds(startX, startY + route.ordinal() * BUTTON_SPACING,
                            BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            button.active = available;
            if (!available && route.ordinal() < this.data.lockedReasonKeys().size()) {
                String reasonKey = this.data.lockedReasonKeys().get(route.ordinal());
                if (!reasonKey.isEmpty()) {
                    button.setTooltip(Tooltip.create(Component.translatable(reasonKey)));
                }
            }
            addRenderableWidget(button);
        }
    }

    private void choose(DeathRoute route) {
        if (this.chosen) {
            return;
        }
        this.chosen = true;
        ModNetwork.sendToServer(new ChooseDeathRoutePacket(this.data.sessionNonce(), route.ordinal()));
        onClose();
    }

    @Override
    public void tick() {
        if (--this.remainingTicks <= 0 && !this.chosen) {
            // 超时结算在服务端，客户端只需收屏
            this.chosen = true;
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFE05555);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.ninecun_lingtai.death_route.countdown",
                        Math.max(0, this.remainingTicks / 20)),
                this.width / 2, 46, 0xFFB8C4D8);
        int startY = this.height / 2 - DeathRoute.values().length * BUTTON_SPACING / 2 + 10;
        for (DeathRoute route : DeathRoute.values()) {
            String keyBase = "screen.ninecun_lingtai.death_route."
                    + route.name().toLowerCase(java.util.Locale.ROOT);
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable(keyBase + ".desc"),
                    this.width / 2,
                    startY + route.ordinal() * BUTTON_SPACING + BUTTON_HEIGHT + 3,
                    0xFF8890A0);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
