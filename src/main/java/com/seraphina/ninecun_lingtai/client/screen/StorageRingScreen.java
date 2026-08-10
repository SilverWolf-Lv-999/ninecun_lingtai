package com.seraphina.ninecun_lingtai.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.Window;
import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.client.render.StorageRingProjectionRenderMeshManager;
import com.seraphina.ninecun_lingtai.client.ring.ClientStorageRingProjectionManager;
import com.seraphina.ninecun_lingtai.common.menu.StorageRingMenu;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.CloseStorageRingPacket;
import com.seraphina.ninecun_lingtai.common.network.packet.StorageRingInteractionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

public final class StorageRingScreen extends AbstractContainerScreen<StorageRingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ModMain.MOD_ID,
            "textures/gui/storage_ring.png"
    );

    private boolean closePacketSent;
    private boolean movingForward;
    private boolean movingBackward;
    private boolean movingLeft;
    private boolean movingRight;
    private boolean movingUp;
    private boolean movingDown;
    private boolean thisRotatingCamera;

    public StorageRingScreen(
            StorageRingMenu menu,
            net.minecraft.world.entity.player.Inventory playerInventory,
            Component title
    ) {
        super(menu, playerInventory, title);
        this.imageWidth = StorageRingMenu.SCREEN_WIDTH;
        this.imageHeight = StorageRingMenu.SCREEN_HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(
                TEXTURE,
                this.leftPos,
                this.topPos,
                0,
                0,
                this.imageWidth,
                this.imageHeight,
                StorageRingMenu.SCREEN_WIDTH,
                StorageRingMenu.SCREEN_HEIGHT
        );
        renderProjection(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ClientStorageRingProjectionManager.sendCameraInput(
                this.menu.getRingId(),
                getLeftImpulse(),
                getForwardImpulse(),
                this.movingUp,
                this.movingDown
        );
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (updateMovementState(keyCode, scanCode, true)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (updateMovementState(keyCode, scanCode, false)) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isInsideViewport(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            interactWithProjection(false, mouseX, mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            interactWithProjection(true, mouseX, mouseY);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            this.thisRotatingCamera = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && this.thisRotatingCamera) {
            ClientStorageRingProjectionManager.rotate(this.menu.getRingId(), dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && this.thisRotatingCamera) {
            this.thisRotatingCamera = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (!this.closePacketSent) {
            this.closePacketSent = true;
            if (ClientStorageRingProjectionManager.isActiveFor(this.menu.getRingId())) {
                ClientStorageRingProjectionManager.updateCameraMotion();
                ModNetwork.sendToServer(CloseStorageRingPacket.withCameraState(
                        ClientStorageRingProjectionManager.getCameraPosition(),
                        ClientStorageRingProjectionManager.getCameraYRot(),
                        ClientStorageRingProjectionManager.getCameraXRot()
                ));
            } else {
                ModNetwork.sendToServer(CloseStorageRingPacket.INSTANCE);
            }
        }
        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isInsideViewport(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + StorageRingMenu.VIEWPORT_LEFT
                && mouseX < this.leftPos + StorageRingMenu.VIEWPORT_LEFT + StorageRingMenu.VIEWPORT_WIDTH
                && mouseY >= this.topPos + StorageRingMenu.VIEWPORT_TOP
                && mouseY < this.topPos + StorageRingMenu.VIEWPORT_TOP + StorageRingMenu.VIEWPORT_HEIGHT;
    }

    private void renderProjection(GuiGraphics guiGraphics) {
        if (!ClientStorageRingProjectionManager.isActiveFor(this.menu.getRingId())) {
            return;
        }
        ClientStorageRingProjectionManager.updateCameraMotion();

        int viewportLeft = this.leftPos + StorageRingMenu.VIEWPORT_LEFT;
        int viewportTop = this.topPos + StorageRingMenu.VIEWPORT_TOP;
        int viewportRight = viewportLeft + StorageRingMenu.VIEWPORT_WIDTH;
        int viewportBottom = viewportTop + StorageRingMenu.VIEWPORT_HEIGHT;
        guiGraphics.enableScissor(viewportLeft, viewportTop, viewportRight, viewportBottom);
        guiGraphics.flush();
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        int viewportX = toFramebufferCoordinate(
                viewportLeft,
                window.getGuiScaledWidth(),
                window.getWidth()
        );
        int viewportY = window.getHeight() - toFramebufferCoordinate(
                viewportBottom,
                window.getGuiScaledHeight(),
                window.getHeight()
        );
        int viewportWidth = Math.max(
                1,
                toFramebufferCoordinate(viewportRight, window.getGuiScaledWidth(), window.getWidth()) - viewportX
        );
        int viewportHeight = Math.max(
                1,
                toFramebufferCoordinate(viewportBottom, window.getGuiScaledHeight(), window.getHeight())
                        - toFramebufferCoordinate(viewportTop, window.getGuiScaledHeight(), window.getHeight())
        );
        RenderSystem.viewport(viewportX, viewportY, viewportWidth, viewportHeight);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        StorageRingProjectionRenderMeshManager.render(
                Minecraft.getInstance().getBlockRenderer(),
                ClientStorageRingProjectionManager.getBlockStates(),
                ClientStorageRingProjectionManager.getSnapshotVersion(),
                ClientStorageRingProjectionManager.getCameraPosition(),
                ClientStorageRingProjectionManager.getCameraYRot(),
                ClientStorageRingProjectionManager.getCameraXRot(),
                viewportWidth,
                viewportHeight
        );
        RenderSystem.disableDepthTest();
        RenderSystem.viewport(0, 0, window.getWidth(), window.getHeight());
        guiGraphics.disableScissor();
    }

    private void interactWithProjection(boolean use, double mouseX, double mouseY) {
        ClientStorageRingProjectionManager.ProjectionTarget target = ClientStorageRingProjectionManager.pick(
                this.menu.getRingId(),
                this.leftPos + StorageRingMenu.VIEWPORT_LEFT,
                this.topPos + StorageRingMenu.VIEWPORT_TOP,
                StorageRingMenu.VIEWPORT_WIDTH,
                StorageRingMenu.VIEWPORT_HEIGHT,
                mouseX,
                mouseY
        );
        if (target != null) {
            if (!use) {
                ClientStorageRingProjectionManager.removeBlock(
                        this.menu.getRingId(),
                        target.localPosition()
                );
            }
            ModNetwork.sendToServer(new StorageRingInteractionPacket(
                    target.localPosition(),
                    target.direction(),
                    target.localHitLocation(),
                    use
            ));
        }
    }

    private boolean updateMovementState(int keyCode, int scanCode, boolean pressed) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.keyUp.matches(keyCode, scanCode)) {
            this.movingForward = pressed;
        } else if (minecraft.options.keyDown.matches(keyCode, scanCode)) {
            this.movingBackward = pressed;
        } else if (minecraft.options.keyLeft.matches(keyCode, scanCode)) {
            this.movingLeft = pressed;
        } else if (minecraft.options.keyRight.matches(keyCode, scanCode)) {
            this.movingRight = pressed;
        } else if (minecraft.options.keyJump.matches(keyCode, scanCode)) {
            this.movingUp = pressed;
        } else if (minecraft.options.keyShift.matches(keyCode, scanCode)) {
            this.movingDown = pressed;
        } else {
            return false;
        }
        return true;
    }

    private float getLeftImpulse() {
        return (this.movingLeft ? 1.0F : 0.0F) - (this.movingRight ? 1.0F : 0.0F);
    }

    private float getForwardImpulse() {
        return (this.movingForward ? 1.0F : 0.0F) - (this.movingBackward ? 1.0F : 0.0F);
    }

    private static int toFramebufferCoordinate(int coordinate, int guiSize, int framebufferSize) {
        return (int) Math.round((double) coordinate * framebufferSize / guiSize);
    }
}
