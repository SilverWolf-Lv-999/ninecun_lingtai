package com.seraphina.ninecun_lingtai.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.seraphina.ninecun_lingtai.client.utility.TechniqueImprintRenderUtility;
import com.seraphina.ninecun_lingtai.common.font.CustomFontConstants;
import com.seraphina.ninecun_lingtai.utility.ColorUtility;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * A text input styled for the cloud-and-gold cultivation screens. The vanilla {@link
 * net.minecraft.client.gui.components.EditBox} draws glyphs unscaled, which does not work with
 * the 48-unit technique font, so this input renders the calligraphy font scaled down by hand.
 */
public final class CultivationTextInput {
    private static final float TEXT_SCALE = 0.3F;
    private static final float CUSTOM_FONT_RENDER_HEIGHT = 48.0F;
    private static final int HORIZONTAL_PADDING = 6;

    private final Font font;
    private final Component hint;
    private final int maximumLength;

    private String value = "";
    private int cursorPosition;
    private int scrollOffset;
    private boolean focused;
    private boolean editable = true;
    private int left;
    private int top;
    private int width;
    private int height;

    public CultivationTextInput(Font font, Component hint, int maximumLength) {
        this.font = font;
        this.hint = hint;
        this.maximumLength = Math.max(1, maximumLength);
    }

    public void setBounds(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        this.cursorPosition = Mth.clamp(this.cursorPosition, 0, this.value.length());
        ensureCursorVisible();
    }

    public String getValue() {
        return this.value;
    }

    public void setValue(String updatedValue) {
        this.value = sanitize(updatedValue);
        this.cursorPosition = this.value.length();
        this.scrollOffset = 0;
        ensureCursorVisible();
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return TechniqueImprintRenderUtility.isInside(
                mouseX,
                mouseY,
                this.left,
                this.top,
                this.width,
                this.height
        );
    }

    /** Updates focus from a click and reports whether the click landed inside the input. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        boolean inside = isMouseOver(mouseX, mouseY);
        this.focused = inside && this.editable;
        if (inside) {
            this.cursorPosition = findClickedPosition(mouseX);
            ensureCursorVisible();
        }
        return inside;
    }

    public boolean keyPressed(int keyCode, int modifiers) {
        if (!this.focused || !this.editable) {
            return false;
        }
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0
                || (modifiers & GLFW.GLFW_MOD_SUPER) != 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> {
                moveCursor(-1, control);
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                moveCursor(1, control);
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                this.cursorPosition = 0;
                ensureCursorVisible();
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                this.cursorPosition = this.value.length();
                ensureCursorVisible();
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                deleteBeforeCursor();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                deleteAfterCursor();
                return true;
            }
            default -> {
                if (control && keyCode == GLFW.GLFW_KEY_V) {
                    insertText(Minecraft.getInstance().keyboardHandler.getClipboard());
                    return true;
                }
                if (control && keyCode == GLFW.GLFW_KEY_C) {
                    Minecraft.getInstance().keyboardHandler.setClipboard(this.value);
                    return true;
                }
                return false;
            }
        }
    }

    public boolean charTyped(char typedCharacter) {
        if (!this.focused || !this.editable) {
            return false;
        }
        if (!SharedConstants.isAllowedChatCharacter(typedCharacter)) {
            return false;
        }
        insertText(Character.toString(typedCharacter));
        return true;
    }

    public void render(GuiGraphics guiGraphics, long currentTime, float alpha) {
        if (alpha <= 0.0F) {
            return;
        }
        int innerLeft = this.left + HORIZONTAL_PADDING;
        int innerWidth = this.width - HORIZONTAL_PADDING * 2;
        float textCenterY = this.top + this.height * 0.5F;
        if (this.value.isEmpty() && !this.focused) {
            drawScaledText(
                    guiGraphics,
                    Component.empty().append(this.hint).withStyle(
                            style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
                    ),
                    innerLeft,
                    textCenterY,
                    ColorUtility.withAlpha(TechniqueImprintRenderUtility.MUTED_GOLD_COLOR, alpha * 0.85F)
            );
            return;
        }

        ensureCursorVisible();
        int visibleEnd = findVisibleEnd(this.scrollOffset, innerWidth);
        String visibleText = this.value.substring(this.scrollOffset, visibleEnd);
        if (!visibleText.isEmpty()) {
            drawScaledText(
                    guiGraphics,
                    styled(visibleText),
                    innerLeft,
                    textCenterY,
                    ColorUtility.withAlpha(TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR, alpha)
            );
        }

        if (this.focused && this.editable && (currentTime / 500L) % 2L == 0L) {
            String beforeCursor = this.value.substring(
                    this.scrollOffset,
                    Mth.clamp(this.cursorPosition, this.scrollOffset, visibleEnd)
            );
            float cursorX = innerLeft + getStyledWidth(beforeCursor) * TEXT_SCALE;
            guiGraphics.fill(
                    (int) cursorX,
                    this.top + 3,
                    (int) cursorX + 1,
                    this.top + this.height - 3,
                    ColorUtility.withAlpha(TechniqueImprintRenderUtility.BRIGHT_GOLD_COLOR, alpha)
            );
        }
    }

    private void moveCursor(int direction, boolean toEdge) {
        if (toEdge) {
            this.cursorPosition = direction < 0 ? 0 : this.value.length();
        } else if (direction < 0 && this.cursorPosition > 0) {
            this.cursorPosition = this.value.offsetByCodePoints(this.cursorPosition, -1);
        } else if (direction > 0 && this.cursorPosition < this.value.length()) {
            this.cursorPosition = this.value.offsetByCodePoints(this.cursorPosition, 1);
        }
        ensureCursorVisible();
    }

    private void deleteBeforeCursor() {
        if (this.cursorPosition <= 0) {
            return;
        }
        int previousPosition = this.value.offsetByCodePoints(this.cursorPosition, -1);
        this.value = this.value.substring(0, previousPosition)
                + this.value.substring(this.cursorPosition);
        this.cursorPosition = previousPosition;
        ensureCursorVisible();
    }

    private void deleteAfterCursor() {
        if (this.cursorPosition >= this.value.length()) {
            return;
        }
        int nextPosition = this.value.offsetByCodePoints(this.cursorPosition, 1);
        this.value = this.value.substring(0, this.cursorPosition)
                + this.value.substring(nextPosition);
        ensureCursorVisible();
    }

    private void insertText(String insertedText) {
        String sanitizedText = sanitize(insertedText);
        int availableLength = this.maximumLength - this.value.length();
        if (availableLength <= 0 || sanitizedText.isEmpty()) {
            return;
        }
        if (sanitizedText.length() > availableLength) {
            int cutLength = availableLength;
            if (cutLength > 0 && Character.isHighSurrogate(sanitizedText.charAt(cutLength - 1))) {
                cutLength--;
            }
            sanitizedText = sanitizedText.substring(0, cutLength);
        }
        this.value = this.value.substring(0, this.cursorPosition)
                + sanitizedText
                + this.value.substring(this.cursorPosition);
        this.cursorPosition += sanitizedText.length();
        ensureCursorVisible();
    }

    private static String sanitize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sanitizedText = new StringBuilder(text.length());
        for (char character : text.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').toCharArray()) {
            if (SharedConstants.isAllowedChatCharacter(character)
                    || Character.isHighSurrogate(character)
                    || Character.isLowSurrogate(character)) {
                sanitizedText.append(character);
            }
        }
        return sanitizedText.toString();
    }

    private void ensureCursorVisible() {
        this.cursorPosition = Mth.clamp(this.cursorPosition, 0, this.value.length());
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, this.value.length());
        if (this.cursorPosition < this.scrollOffset) {
            this.scrollOffset = this.cursorPosition;
            return;
        }
        int innerWidth = Math.max(8, this.width - HORIZONTAL_PADDING * 2);
        while (this.scrollOffset < this.cursorPosition
                && getStyledWidth(this.value.substring(this.scrollOffset, this.cursorPosition))
                * TEXT_SCALE > innerWidth) {
            this.scrollOffset = this.value.offsetByCodePoints(this.scrollOffset, 1);
        }
    }

    private int findVisibleEnd(int startPosition, int innerWidth) {
        int endPosition = startPosition;
        while (endPosition < this.value.length()) {
            int nextPosition = this.value.offsetByCodePoints(endPosition, 1);
            if (getStyledWidth(this.value.substring(startPosition, nextPosition)) * TEXT_SCALE
                    > innerWidth) {
                break;
            }
            endPosition = nextPosition;
        }
        return endPosition;
    }

    private int findClickedPosition(double mouseX) {
        double relativeX = mouseX - (this.left + HORIZONTAL_PADDING);
        int position = this.scrollOffset;
        while (position < this.value.length()) {
            int nextPosition = this.value.offsetByCodePoints(position, 1);
            float widthToNext = getStyledWidth(this.value.substring(this.scrollOffset, nextPosition))
                    * TEXT_SCALE;
            if (widthToNext > relativeX) {
                break;
            }
            position = nextPosition;
        }
        return position;
    }

    private float getStyledWidth(String text) {
        return this.font.width(styled(text));
    }

    private static Component styled(String text) {
        return Component.literal(text).withStyle(
                style -> style.withFont(CustomFontConstants.TECHNIQUE_FONT_ID)
        );
    }

    private void drawScaledText(
            GuiGraphics guiGraphics,
            Component text,
            float textLeft,
            float centerY,
            int color
    ) {
        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(
                textLeft,
                centerY - CUSTOM_FONT_RENDER_HEIGHT * TEXT_SCALE * 0.5F,
                221.0F
        );
        poseStack.scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        guiGraphics.drawString(this.font, text, 0, 0, color, false);
        poseStack.popPose();
    }
}
