package net.seraphina.ninecun_lingtai.client.space;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.seraphina.ninecun_lingtai.client.input.ModKeyMappings;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.CreateSpaceRiftPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.RequestSpaceRiftGuiPacket;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ClientSpaceRiftManager {
    private static List<ResourceLocation> dimensions = List.of();
    private static int selectedIndex;
    private static int rightHoldTicks;
    private static boolean active;
    private static boolean rightHeld;

    private ClientSpaceRiftManager() {
    }

    public static void begin() {
        if (active || Minecraft.getInstance().screen != null) {
            return;
        }
        active = true;
        rightHoldTicks = 0;
        rightHeld = false;
        selectedIndex = 0;
        ModNetwork.sendToServer(RequestSpaceRiftGuiPacket.INSTANCE);
    }

    public static void open(List<ResourceLocation> values) {
        dimensions = List.copyOf(values);
        selectedIndex = dimensions.isEmpty() ? 0 : Math.min(selectedIndex, dimensions.size() - 1);
        if (dimensions.isEmpty()) {
            active = false;
        }
    }

    public static void tick() {
        if (!active) {
            return;
        }
        if (!rightHeld) {
            rightHoldTicks = 0;
            return;
        }
        rightHoldTicks++;
        if (rightHoldTicks >= 60 && !dimensions.isEmpty()) {
            ResourceLocation selected = dimensions.get(selectedIndex);
            ModNetwork.sendToServer(new CreateSpaceRiftPacket(selected));
            reset();
        }
    }

    public static boolean onKeyInput(int key, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return active;
        }
        if (!active) {
            if (key == GLFW.GLFW_KEY_Z) {
                begin();
                return true;
            }
            return false;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            reset();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT && !dimensions.isEmpty()) {
            selectedIndex = (selectedIndex + 1) % dimensions.size();
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT && !dimensions.isEmpty()) {
            selectedIndex = (selectedIndex - 1 + dimensions.size()) % dimensions.size();
            return true;
        }
        return active;
    }

    public static boolean onMouseButton(int button, int action) {
        if (!active) {
            return false;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS) {
            reset();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            rightHeld = action != GLFW.GLFW_RELEASE;
            if (action == GLFW.GLFW_RELEASE) {
                rightHoldTicks = 0;
            }
            return true;
        }
        return true;
    }

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!active || dimensions.isEmpty() || event.getOverlay() != net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        int cardWidth = 132;
        int cardHeight = 54;
        int gap = 10;
        int visible = Math.min(5, dimensions.size());
        int left = width / 2 - (visible * cardWidth + (visible - 1) * gap) / 2;
        int top = height / 2 - cardHeight / 2;
        double mouseX = Minecraft.getInstance().mouseHandler.xpos() * width / Minecraft.getInstance().getWindow().getScreenWidth();
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() * height / Minecraft.getInstance().getWindow().getScreenHeight();
        for (int slot = 0; slot < visible; slot++) {
            int index = (selectedIndex - visible / 2 + slot + dimensions.size()) % dimensions.size();
            int cardLeft = left + slot * (cardWidth + gap);
            boolean hovered = mouseX >= cardLeft && mouseX < cardLeft + cardWidth && mouseY >= top && mouseY < top + cardHeight;
            if (hovered) {
                selectedIndex = index;
            }
            boolean selected = index == selectedIndex;
            graphics.fill(cardLeft, top, cardLeft + cardWidth, top + cardHeight, selected ? 0xEE315B78 : 0xCC111A28);
            graphics.renderOutline(cardLeft, top, cardWidth, cardHeight, selected ? 0xFFB9E7FF : 0xFF41516A);
            graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal(dimensions.get(index).toString()), cardLeft + cardWidth / 2, top + 16, 0xFFE8F5FF);
            if (selected) {
                graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("「" + Math.round(rightHoldTicks / 60.0F * 100.0F) + "%」"), cardLeft + cardWidth / 2, top + 35, 0xFF9DE5FF);
            }
        }
        graphics.drawCenteredString(Minecraft.getInstance().font, Component.literal("按住右键撕裂空间　左键取消"), width / 2, top - 24, 0xFFE8F5FF);
    }

    public static void reset() {
        dimensions = List.of();
        selectedIndex = 0;
        rightHoldTicks = 0;
        rightHeld = false;
        active = false;
    }
}
