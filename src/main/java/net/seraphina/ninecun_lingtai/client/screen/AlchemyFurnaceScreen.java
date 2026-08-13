package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyFeedStep;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredientSlot;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatBand;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatControlMode;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatStage;
import net.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.client.player.ClientCultivationInfoManager;
import net.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility;
import net.seraphina.ninecun_lingtai.client.utility.AptitudeRenderUtility.ScreenPoint;
import net.seraphina.ninecun_lingtai.common.menu.AlchemyFurnaceMenu;
import net.seraphina.ninecun_lingtai.common.network.ModNetwork;
import net.seraphina.ninecun_lingtai.common.network.packet.AlchemyFurnaceActionPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.MaintainHeatPacket;
import net.seraphina.ninecun_lingtai.common.network.packet.SelectAlchemyRecipePacket;
import net.seraphina.ninecun_lingtai.common.player.PlayerCultivationInfo;
import net.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AlchemyFurnaceScreen extends AbstractContainerScreen<AlchemyFurnaceMenu> {
    private static final int PREFERRED_WIDTH = 324;
    private static final int MINIMUM_WIDTH = PREFERRED_WIDTH;
    private static final int IMAGE_HEIGHT = 250;
    private static final int PRIMARY_TEXT_COLOR = 0xFFE6D5A8;
    private static final int PANEL_COLOR = 0xF2181612;
    private static final int PANEL_BORDER_COLOR = 0xFF8E7645;
    private static final int DIAGRAM_COLOR = 0x705E4A28;
    private static final int SLOT_BORDER_COLOR = 0xFF776B56;
    private static final int OUTPUT_SLOT_BORDER_COLOR = 0xFFC5A35B;
    private static final int SLOT_FILL_COLOR = 0xEE0E0E0D;
    private static final int SLOT_INNER_FILL_COLOR = 0xFF171512;
    private static final int START_BUTTON_WIDTH = 124;
    private static final int START_BUTTON_LEFT = (PREFERRED_WIDTH - START_BUTTON_WIDTH) / 2;
    private static final int START_BUTTON_TOP = 137;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private static final int SLOT_CENTER_OFFSET = 8;
    private static final int SLOT_FRAME_OFFSET = 1;
    private static final int RECIPE_PANEL_LEFT = 8;
    private static final int RECIPE_PANEL_TOP = 20;
    private static final int RECIPE_PANEL_WIDTH = 108;
    private static final int RECIPE_PANEL_HEIGHT = 36;
    private static final int RECIPE_ARROW_WIDTH = 14;
    private static final int CONTROL_BUTTON_LEFT = 244;
    private static final int CONTROL_BUTTON_WIDTH = 32;
    private static final int CONTROL_BUTTON_HEIGHT = 20;
    private static final int STOKE_BUTTON_TOP = 23;
    private static final int VENT_BUTTON_TOP = 56;
    private static final int MAINTAIN_BUTTON_TOP = 89;

    private static final int HEAT_BAR_LEFT = 284;
    private static final int HEAT_BAR_TOP = 23;
    private static final int HEAT_BAR_WIDTH = 12;
    private static final int HEAT_BAR_HEIGHT = 112;
    private static final int[] HEAT_GRADIENT_LEVELS = {
            0, 100, 200, 300, 400, 500, 600
    };
    private static final int[] HEAT_GRADIENT_COLORS = {
            0xFF251E25,
            0xFF5B2C35,
            0xFFA13B32,
            0xFFD15A2D,
            0xFFE88832,
            0xFFF3BD4E,
            0xFFFFED9A
    };
    private static final int EXTINGUISHED_TINT_COLOR = 0xFF353331;
    private static final double EXTINGUISHED_TINT_STRENGTH = 0.72D;
    private static final int HEAT_SCALE_MAJOR_COLOR = 0xFFD8C89E;
    private static final int HEAT_SCALE_MINOR_COLOR = 0x887C705B;
    private static final int HEAT_POINTER_COLOR = 0xFFFFFFFF;
    private static final int TARGET_BAND_FILL_COLOR = 0x55FFE9A0;
    private static final int TARGET_BAND_FILL_FADED_COLOR = 0x00FFE9A0;
    private static final int TARGET_BAND_FILL_BLINK_COLOR = 0x1EFFE9A0;
    private static final int TARGET_BAND_OUTLINE_COLOR = 0xFFFFE9A0;
    private static final int TARGET_BAND_OUTLINE_BLINK_COLOR = 0x50FFE9A0;
    private static final int TARGET_BAND_UNKNOWN_COLOR = 0xF6E3B4;
    private static final int MAINTAIN_GLOW_INNER_COLOR = 0xB466B4FF;
    private static final int MAINTAIN_GLOW_OUTER_COLOR = 0x5A66B4FF;
    private static final int DISABLED_BUTTON_FILL_COLOR = 0xFF231F19;
    private static final int DISABLED_BUTTON_BORDER_COLOR = 0xFF4C4335;
    private static final int DISABLED_BUTTON_TEXT_COLOR = 0xFF6E6350;
    private static final int SEALED_SLOT_FILL_COLOR = 0xCC251B31;
    private static final int OPEN_SLOT_FILL_COLOR = 0x663C3215;
    private static final int OPEN_SLOT_BORDER_COLOR = 0xFFFFD45C;
    private static final int MISSED_SLOT_FILL_COLOR = 0xBB4D1515;
    private static final int MISSED_SLOT_BORDER_COLOR = 0xFFFF5555;
    private static final int ENVIRONMENT_POSITIVE_COLOR = 0xFF55FF55;
    private static final int ENVIRONMENT_NEGATIVE_COLOR = 0xFFFF5555;
    private static final int ENVIRONMENT_DEAD_COLOR = 0xFF555555;

    private static final String ENVIRONMENT_BADGE_KEY = "screen.ninecun_lingtai.alchemy_furnace.environment_badge";
    private static final String ENVIRONMENT_DEAD_KEY = "screen.ninecun_lingtai.alchemy_furnace.environment_dead";

    private static final int MINIMUM_STARS_FOR_TARGET_BAND = 3;
    private static final int MINIMUM_STARS_FOR_PRECISE_BAND = 5;
    private static final int FUZZY_EDGE_HEAT = 40;
    private static final int STAGE_SWITCH_WARNING_TICKS = 40;
    private static final int BLINK_INTERVAL_TICKS = 5;
    private static final int INITIAL_STATE_SAMPLE_DELAY_TICKS = 2;
    private static final double MINIMUM_ENVIRONMENT_SPEED_MULTIPLIER = 0.5D;
    private static final double MAXIMUM_ENVIRONMENT_SPEED_MULTIPLIER = 1.5D;

    private boolean initialRefiningStateSampled;
    private int initialStateSampleTicks = INITIAL_STATE_SAMPLE_DELAY_TICKS;
    private boolean wasRefining;
    private boolean localTimingValid;
    private int localRefiningTicks;
    @Nullable
    private AlchemyRecipeClientData trackedRecipe;
    @Nullable
    private ResourceLocation selectedRecipeId;
    private int selectionPendingTicks;
    private HeatControlMode heldControl = HeatControlMode.NONE;

    public AlchemyFurnaceScreen(AlchemyFurnaceMenu menu, net.minecraft.world.entity.player.Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PREFERRED_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        if (ClientCultivationInfoManager.getCultivationInfo().isEmpty()) {
            ClientCultivationInfoManager.requestRefresh();
        }
        if (!ClientAlchemyRecipeManager.isInitialized()) {
            ClientAlchemyRecipeManager.requestRefresh();
        }
    }

    @Override
    protected void init() {
        this.imageWidth = Mth.clamp(this.width - 24, MINIMUM_WIDTH, PREFERRED_WIDTH);
        this.imageHeight = IMAGE_HEIGHT;
        super.init();
        this.selectedRecipeId = this.menu.getSelectedRecipeId();
    }

    @Override
    protected void containerTick() {
        boolean refining = this.menu.isRefining();
        synchronizeSelectedRecipe();
        if (!this.initialRefiningStateSampled) {
            if (--this.initialStateSampleTicks <= 0) {
                this.initialRefiningStateSampled = true;
                this.wasRefining = refining;
            }
            return;
        }
        if (refining && !this.wasRefining) {
            this.localTimingValid = true;
            this.localRefiningTicks = 0;
            this.trackedRecipe = null;
        } else if (!refining && this.wasRefining) {
            this.localTimingValid = false;
            this.trackedRecipe = null;
        }
        if (refining) {
            if (this.trackedRecipe == null) {
                this.trackedRecipe = getSelectedRecipe().orElseGet(
                        () -> findMatchingLearnedRecipe().orElse(null)
                );
            }
            if (this.localTimingValid) {
                this.localRefiningTicks++;
            }
        } else if (this.heldControl != HeatControlMode.NONE) {
            releaseHeatControl();
        }
        this.wasRefining = refining;
    }

    private void synchronizeSelectedRecipe() {
        ResourceLocation synchronizedId = this.menu.getSelectedRecipeId();
        if (this.selectionPendingTicks > 0) {
            this.selectionPendingTicks--;
            if (this.selectedRecipeId != null && this.selectedRecipeId.equals(synchronizedId)) {
                this.selectionPendingTicks = 0;
            }
            return;
        }
        if (!java.util.Objects.equals(this.selectedRecipeId, synchronizedId)) {
            this.selectedRecipeId = synchronizedId;
            this.trackedRecipe = getSelectedRecipe().orElse(null);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderProcessTooltip(guiGraphics, mouseX, mouseY);
        if (isStartBlocked() && isInsideStartButton(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable(ENVIRONMENT_DEAD_KEY),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int panelRight = this.leftPos + this.imageWidth;
        int panelBottom = this.topPos + this.imageHeight;
        guiGraphics.fill(this.leftPos, this.topPos, panelRight, panelBottom, PANEL_COLOR);
        guiGraphics.renderOutline(
                this.leftPos,
                this.topPos,
                this.imageWidth,
                this.imageHeight,
                PANEL_BORDER_COLOR
        );
        guiGraphics.fill(
                this.leftPos + 8,
                this.topPos + 159,
                panelRight - 8,
                this.topPos + 160,
                PANEL_BORDER_COLOR
        );
        renderRefiningDiagram(guiGraphics);
        renderSlotFrames(guiGraphics);
        renderFeedSlotStates(guiGraphics);
        renderHeat(guiGraphics, mouseX, mouseY);
        renderActions(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.imageWidth / 2,
                7,
                PRIMARY_TEXT_COLOR
        );
        renderEnvironmentBadge(guiGraphics);
        renderRecipeSelection(guiGraphics, mouseX, mouseY);
        renderCurrentProcessEvent(guiGraphics);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (!this.menu.isRefining() && isInsidePreviousRecipeButton(mouseX, mouseY)) {
                cycleSelectedRecipe(-1);
                return true;
            }
            if (!this.menu.isRefining() && isInsideNextRecipeButton(mouseX, mouseY)) {
                cycleSelectedRecipe(1);
                return true;
            }
            HeatControlMode clickedControl = getControlAt(mouseX, mouseY);
            if (this.menu.isRefining() && clickedControl != HeatControlMode.NONE) {
                beginHeatControl(clickedControl);
                return true;
            }
            if (isInsideStartButton(mouseX, mouseY)) {
                if (isStartBlocked()) {
                    return true;
                }
                ModNetwork.sendToServer(new AlchemyFurnaceActionPacket(
                        this.menu.getBlockPosition(),
                        this.menu.isRefining()
                                ? AlchemyFurnaceActionPacket.Action.FINISH_REFINING
                                : AlchemyFurnaceActionPacket.Action.START_REFINING
                ));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && this.heldControl != HeatControlMode.NONE) {
            releaseHeatControl();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (this.heldControl != HeatControlMode.NONE
                && getControlAt(mouseX, mouseY) != this.heldControl) {
            releaseHeatControl();
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public void removed() {
        if (this.heldControl != HeatControlMode.NONE) {
            releaseHeatControl();
        }
        super.removed();
    }

    private void beginHeatControl(HeatControlMode mode) {
        this.heldControl = mode;
        ModNetwork.sendToServer(new MaintainHeatPacket(this.menu.getBlockPosition(), mode));
    }

    private void releaseHeatControl() {
        this.heldControl = HeatControlMode.NONE;
        ModNetwork.sendToServer(new MaintainHeatPacket(
                this.menu.getBlockPosition(),
                HeatControlMode.NONE
        ));
    }

    private void renderRefiningDiagram(GuiGraphics guiGraphics) {
        ScreenPoint main = slotCenter(AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT);
        ScreenPoint firstSecondary = slotCenter(AlchemyFurnaceMenu.FIRST_SECONDARY_INGREDIENT_SLOT);
        ScreenPoint secondSecondary = slotCenter(AlchemyFurnaceMenu.SECOND_SECONDARY_INGREDIENT_SLOT);
        ScreenPoint output = slotCenter(AlchemyFurnaceMenu.OUTPUT_SLOT);
        ScreenPoint firstAuxiliary = slotCenter(AlchemyFurnaceMenu.FIRST_AUXILIARY_INGREDIENT_SLOT);
        ScreenPoint secondAuxiliary = slotCenter(AlchemyFurnaceMenu.SECOND_AUXILIARY_INGREDIENT_SLOT);

        drawConnection(guiGraphics, main, firstSecondary, DIAGRAM_COLOR);
        drawConnection(guiGraphics, main, secondSecondary, DIAGRAM_COLOR);
        drawConnection(guiGraphics, firstSecondary, output, DIAGRAM_COLOR);
        drawConnection(guiGraphics, secondSecondary, output, DIAGRAM_COLOR);
        drawConnection(guiGraphics, firstAuxiliary, output, DIAGRAM_COLOR);
        drawConnection(guiGraphics, secondAuxiliary, output, DIAGRAM_COLOR);
    }

    private void renderSlotFrames(GuiGraphics guiGraphics) {
        for (int slotIndex = 0; slotIndex < this.menu.slots.size(); slotIndex++) {
            int frameLeft = this.leftPos + this.menu.slots.get(slotIndex).x - SLOT_FRAME_OFFSET;
            int frameTop = this.topPos + this.menu.slots.get(slotIndex).y - SLOT_FRAME_OFFSET;
            int slotBorder = slotIndex == AlchemyFurnaceMenu.OUTPUT_SLOT
                    ? OUTPUT_SLOT_BORDER_COLOR
                    : SLOT_BORDER_COLOR;
            guiGraphics.fill(frameLeft, frameTop, frameLeft + 18, frameTop + 18, slotBorder);
            guiGraphics.fill(frameLeft + 1, frameTop + 1, frameLeft + 17, frameTop + 17, SLOT_FILL_COLOR);
            guiGraphics.fill(frameLeft + 2, frameTop + 2, frameLeft + 16, frameTop + 16, SLOT_INNER_FILL_COLOR);
        }
    }

    private void renderFeedSlotStates(GuiGraphics guiGraphics) {
        int plannedMask = this.menu.getPlannedFeedMask();
        int openMask = this.menu.getOpenFeedMask();
        int missedMask = this.menu.getMissedFeedMask();
        int fedMask = this.menu.getFedFeedMask();
        for (int slotIndex = AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT;
             slotIndex < AlchemyFurnaceMenu.OUTPUT_SLOT;
             slotIndex++) {
            int slotMask = 1 << slotIndex;
            if ((plannedMask & slotMask) == 0 || (fedMask & slotMask) != 0) {
                continue;
            }
            net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
            int slotLeft = this.leftPos + slot.x - SLOT_FRAME_OFFSET;
            int slotTop = this.topPos + slot.y - SLOT_FRAME_OFFSET;
            Component marker;
            int fillColor;
            int borderColor;
            if ((missedMask & slotMask) != 0) {
                marker = Component.translatable("screen.ninecun_lingtai.alchemy.feed_missed");
                fillColor = MISSED_SLOT_FILL_COLOR;
                borderColor = MISSED_SLOT_BORDER_COLOR;
            } else if ((openMask & slotMask) != 0) {
                marker = Component.translatable("screen.ninecun_lingtai.alchemy.feed_open");
                fillColor = OPEN_SLOT_FILL_COLOR;
                borderColor = OPEN_SLOT_BORDER_COLOR;
            } else {
                marker = Component.translatable("screen.ninecun_lingtai.alchemy.feed_sealed");
                fillColor = SEALED_SLOT_FILL_COLOR;
                borderColor = PANEL_BORDER_COLOR;
            }
            guiGraphics.fill(slotLeft + 1, slotTop + 1, slotLeft + 17, slotTop + 17, fillColor);
            guiGraphics.renderOutline(slotLeft, slotTop, 18, 18, borderColor);
            guiGraphics.drawCenteredString(
                    this.font,
                    marker,
                    slotLeft + 9,
                    slotTop + 5,
                    borderColor
            );
        }
    }

    private void renderHeat(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int barLeft = this.leftPos + HEAT_BAR_LEFT;
        int barTop = this.topPos + HEAT_BAR_TOP;
        int innerLeft = barLeft + 1;
        int innerRight = barLeft + HEAT_BAR_WIDTH - 1;
        boolean extinguished = this.menu.getHeat() <= 0;

        renderHeatGradient(guiGraphics, innerLeft, innerRight, extinguished);
        renderTargetBand(guiGraphics, barLeft, barTop);

        if (!extinguished) {
            int pointerY = Mth.clamp(
                    heatToY(this.menu.getHeat()),
                    barTop + 1,
                    barTop + HEAT_BAR_HEIGHT - 2
            );
            int pointerHeatColor = getHeatGradientColor(this.menu.getHeat());
            int pointerAccentColor = blendArgb(
                    pointerHeatColor,
                    HEAT_POINTER_COLOR,
                    0.58D
            );
            guiGraphics.fill(
                    innerLeft,
                    Math.max(barTop + 1, pointerY - 2),
                    innerRight,
                    Math.min(barTop + HEAT_BAR_HEIGHT - 1, pointerY + 3),
                    withAlpha(pointerHeatColor, 0x38)
            );
            guiGraphics.fill(
                    innerLeft,
                    Math.max(barTop + 1, pointerY - 1),
                    innerRight,
                    Math.min(barTop + HEAT_BAR_HEIGHT - 1, pointerY + 2),
                    withAlpha(pointerAccentColor, 0x78)
            );
            guiGraphics.fill(innerLeft, pointerY, innerRight, pointerY + 1, HEAT_POINTER_COLOR);
            guiGraphics.fill(barLeft - 5, pointerY - 2, barLeft - 4, pointerY + 3, pointerAccentColor);
            guiGraphics.fill(barLeft - 4, pointerY - 1, barLeft - 3, pointerY + 2, pointerAccentColor);
            guiGraphics.fill(barLeft - 3, pointerY, barLeft - 2, pointerY + 1, pointerAccentColor);
        }

        guiGraphics.renderOutline(barLeft, barTop, HEAT_BAR_WIDTH, HEAT_BAR_HEIGHT, SLOT_BORDER_COLOR);
        if (this.menu.isRefining() || isInsideHeatZone(mouseX, mouseY)) {
            renderHeatScale(guiGraphics, barLeft);
        }
        renderCurrentHeatValue(guiGraphics, barLeft, barTop, extinguished);

        if ((this.heldControl != HeatControlMode.NONE
                || this.menu.getHeatControlMode() != HeatControlMode.NONE)
                && this.menu.isRefining()) {
            guiGraphics.renderOutline(
                    barLeft - 1,
                    barTop - 1,
                    HEAT_BAR_WIDTH + 2,
                    HEAT_BAR_HEIGHT + 2,
                    MAINTAIN_GLOW_INNER_COLOR
            );
            guiGraphics.renderOutline(
                    barLeft - 2,
                    barTop - 2,
                    HEAT_BAR_WIDTH + 4,
                    HEAT_BAR_HEIGHT + 4,
                    MAINTAIN_GLOW_OUTER_COLOR
            );
        }
    }

    private void renderHeatGradient(
            GuiGraphics guiGraphics,
            int innerLeft,
            int innerRight,
            boolean extinguished
    ) {
        for (int index = 0; index < HEAT_GRADIENT_LEVELS.length - 1; index++) {
            int lowerHeat = HEAT_GRADIENT_LEVELS[index];
            int upperHeat = HEAT_GRADIENT_LEVELS[index + 1];
            int lowerColor = HEAT_GRADIENT_COLORS[index];
            int upperColor = HEAT_GRADIENT_COLORS[index + 1];
            if (extinguished) {
                lowerColor = blendArgb(
                        lowerColor,
                        EXTINGUISHED_TINT_COLOR,
                        EXTINGUISHED_TINT_STRENGTH
                );
                upperColor = blendArgb(
                        upperColor,
                        EXTINGUISHED_TINT_COLOR,
                        EXTINGUISHED_TINT_STRENGTH
                );
            }
            guiGraphics.fillGradient(
                    innerLeft,
                    heatToY(upperHeat),
                    innerRight,
                    heatToY(lowerHeat) + 1,
                    upperColor,
                    lowerColor
            );
        }
    }

    private void renderHeatScale(GuiGraphics guiGraphics, int barLeft) {
        int scaleLeft = barLeft + HEAT_BAR_WIDTH;
        int maximumHeat = getMaximumHeat();
        for (int heat = 0; heat <= maximumHeat; heat += 50) {
            int y = heatToY(heat);
            boolean labeled = heat % 200 == 0;
            boolean mediumTick = heat % 100 == 0;
            int tickLength = labeled ? 5 : mediumTick ? 3 : 2;
            int tickColor = labeled ? HEAT_SCALE_MAJOR_COLOR : HEAT_SCALE_MINOR_COLOR;
            guiGraphics.fill(scaleLeft, y, scaleLeft + tickLength, y + 1, tickColor);
            if (labeled) {
                guiGraphics.drawString(
                        this.font,
                        Component.literal(Integer.toString(heat)),
                        scaleLeft + 6,
                        y - this.font.lineHeight / 2,
                        HEAT_SCALE_MAJOR_COLOR
                );
            }
        }
    }

    private void renderCurrentHeatValue(
            GuiGraphics guiGraphics,
            int barLeft,
            int barTop,
            boolean extinguished
    ) {
        int heat = this.menu.getHeat();
        int valueColor = extinguished
                ? HEAT_SCALE_MINOR_COLOR
                : blendArgb(getHeatGradientColor(heat), HEAT_POINTER_COLOR, 0.35D);
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal(heat + "°"),
                barLeft + HEAT_BAR_WIDTH / 2,
                barTop + HEAT_BAR_HEIGHT + 4,
                valueColor
        );
    }

    private void renderTargetBand(GuiGraphics guiGraphics, int barLeft, int barTop) {
        if (!this.menu.isRefining()) {
            return;
        }
        int illuminatedStars = getIlluminatedStars();
        if (illuminatedStars < MINIMUM_STARS_FOR_TARGET_BAND) {
            return;
        }
        AlchemyRecipeClientData recipe = this.trackedRecipe;
        if (recipe == null) {
            renderUnknownTargetBand(guiGraphics, barLeft, barTop);
            return;
        }
        List<HeatStage> stages = recipe.heatStages();
        if (stages.isEmpty()) {
            return;
        }
        if (!this.localTimingValid) {
            renderUnknownTargetBand(guiGraphics, barLeft, barTop);
            return;
        }
        double effectiveDuration = getEffectiveBrewingDuration(recipe);
        double progress = this.localRefiningTicks / Math.max(1.0D, effectiveDuration);
        HeatBand targetBand = HeatStage.getTargetBand(stages, progress);
        int bandTop = heatToY(targetBand.getMaximumHeat());
        int bandBottom = heatToY(targetBand.getMinimumHeat() - 1);
        int innerLeft = barLeft + 1;
        int innerRight = barLeft + HEAT_BAR_WIDTH - 1;
        if (illuminatedStars < MINIMUM_STARS_FOR_PRECISE_BAND) {
            renderFuzzyTargetBand(guiGraphics, innerLeft, innerRight, barTop, bandTop, bandBottom);
            return;
        }
        boolean blinkDimmed = isStageSwitchImminent(stages, progress, effectiveDuration)
                && (this.localRefiningTicks / BLINK_INTERVAL_TICKS) % 2 == 1;
        guiGraphics.fill(
                innerLeft,
                bandTop,
                innerRight,
                bandBottom,
                blinkDimmed ? TARGET_BAND_FILL_BLINK_COLOR : TARGET_BAND_FILL_COLOR
        );
        guiGraphics.renderOutline(
                innerLeft,
                bandTop,
                innerRight - innerLeft,
                bandBottom - bandTop,
                blinkDimmed ? TARGET_BAND_OUTLINE_BLINK_COLOR : TARGET_BAND_OUTLINE_COLOR
        );
    }

    private void renderFuzzyTargetBand(
            GuiGraphics guiGraphics,
            int innerLeft,
            int innerRight,
            int barTop,
            int bandTop,
            int bandBottom
    ) {
        int innerTop = barTop + 1;
        int innerBottom = barTop + HEAT_BAR_HEIGHT - 1;
        int fuzzPixels = Math.max(
                2,
                (int) Math.round(FUZZY_EDGE_HEAT / (double) getMaximumHeat() * (HEAT_BAR_HEIGHT - 2))
        );
        int fadeTop = Math.max(innerTop, bandTop - fuzzPixels);
        int coreTop = Math.min(innerBottom, bandTop + fuzzPixels);
        int coreBottom = Math.max(innerTop, bandBottom - fuzzPixels);
        int fadeBottom = Math.min(innerBottom, bandBottom + fuzzPixels);
        if (fadeTop < coreTop) {
            guiGraphics.fillGradient(
                    innerLeft,
                    fadeTop,
                    innerRight,
                    coreTop,
                    TARGET_BAND_FILL_FADED_COLOR,
                    TARGET_BAND_FILL_COLOR
            );
        }
        if (coreTop < coreBottom) {
            guiGraphics.fill(innerLeft, coreTop, innerRight, coreBottom, TARGET_BAND_FILL_COLOR);
        }
        if (coreBottom < fadeBottom) {
            guiGraphics.fillGradient(
                    innerLeft,
                    coreBottom,
                    innerRight,
                    fadeBottom,
                    TARGET_BAND_FILL_COLOR,
                    TARGET_BAND_FILL_FADED_COLOR
            );
        }
    }

    private void renderUnknownTargetBand(GuiGraphics guiGraphics, int barLeft, int barTop) {
        AptitudeRenderUtility.drawCenteredText(
                guiGraphics,
                this.font,
                Component.literal("?"),
                barLeft + HEAT_BAR_WIDTH / 2.0F,
                barTop + HEAT_BAR_HEIGHT / 2.0F,
                1.6F,
                TARGET_BAND_UNKNOWN_COLOR,
                0x000000,
                0.85F
        );
    }

    private void renderEnvironmentBadge(GuiGraphics guiGraphics) {
        int primaryElementOrdinal = this.menu.getPrimaryElementOrdinal();
        if (primaryElementOrdinal < 1 || primaryElementOrdinal > ElementType.values().length) {
            return;
        }
        ElementType primaryElement = ElementType.values()[primaryElementOrdinal - 1];
        int multiplierX100 = this.menu.getEnvironmentMultiplierX100();
        int badgeColor = multiplierX100 <= 0
                ? ENVIRONMENT_DEAD_COLOR
                : multiplierX100 >= 100 ? ENVIRONMENT_POSITIVE_COLOR : ENVIRONMENT_NEGATIVE_COLOR;
        guiGraphics.drawString(
                this.font,
                Component.translatable(
                        ENVIRONMENT_BADGE_KEY,
                        SpiritualRootUtility.getElementName(primaryElement),
                        String.format(Locale.ROOT, "%.2f", multiplierX100 / 100.0D)
                ),
                10,
                7,
                badgeColor
        );
    }

    private void renderRecipeSelection(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.fill(
                RECIPE_PANEL_LEFT,
                RECIPE_PANEL_TOP,
                RECIPE_PANEL_LEFT + RECIPE_PANEL_WIDTH,
                RECIPE_PANEL_TOP + RECIPE_PANEL_HEIGHT,
                0xCC17130F
        );
        guiGraphics.renderOutline(
                RECIPE_PANEL_LEFT,
                RECIPE_PANEL_TOP,
                RECIPE_PANEL_WIDTH,
                RECIPE_PANEL_HEIGHT,
                this.menu.isRefining() ? DISABLED_BUTTON_BORDER_COLOR : PANEL_BORDER_COLOR
        );
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.ninecun_lingtai.alchemy.recipe"),
                RECIPE_PANEL_LEFT + 4,
                RECIPE_PANEL_TOP + 4,
                PRIMARY_TEXT_COLOR
        );
        if (!this.menu.isRefining() && isInsideRecipePanel(mouseX, mouseY)) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal("<"),
                    RECIPE_PANEL_LEFT + RECIPE_ARROW_WIDTH / 2,
                    RECIPE_PANEL_TOP + 22,
                    PRIMARY_TEXT_COLOR
            );
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal(">"),
                    RECIPE_PANEL_LEFT + RECIPE_PANEL_WIDTH - RECIPE_ARROW_WIDTH / 2,
                    RECIPE_PANEL_TOP + 22,
                    PRIMARY_TEXT_COLOR
            );
        }
        String selectedName = getSelectedRecipe()
                .map(AlchemyRecipeClientData::name)
                .orElse(Component.translatable(
                        "screen.ninecun_lingtai.alchemy.recipe_unselected"
                ).getString());
        selectedName = this.font.plainSubstrByWidth(
                selectedName,
                RECIPE_PANEL_WIDTH - RECIPE_ARROW_WIDTH * 2 - 4
        );
        guiGraphics.drawCenteredString(
                this.font,
                selectedName,
                RECIPE_PANEL_LEFT + RECIPE_PANEL_WIDTH / 2,
                RECIPE_PANEL_TOP + 22,
                PRIMARY_TEXT_COLOR
        );
    }

    private void renderCurrentProcessEvent(GuiGraphics guiGraphics) {
        if (!this.menu.isRefining()) {
            return;
        }
        Component event = this.menu.getMissedFeedMask() != 0
                ? Component.translatable("screen.ninecun_lingtai.alchemy.process.missed")
                : this.menu.getOpenFeedMask() != 0
                ? Component.translatable("screen.ninecun_lingtai.alchemy.process.feed")
                : Component.translatable("screen.ninecun_lingtai.alchemy.process.wait");
        guiGraphics.drawString(this.font, event, 10, 146, PRIMARY_TEXT_COLOR);
    }

    private void cycleSelectedRecipe(int direction) {
        List<AlchemyRecipeClientData> learnedRecipes =
                ClientAlchemyRecipeManager.getLearnedRecipes();
        if (learnedRecipes.isEmpty()) {
            ClientAlchemyRecipeManager.requestRefresh();
            return;
        }
        int currentIndex = -1;
        for (int index = 0; index < learnedRecipes.size(); index++) {
            if (learnedRecipes.get(index).id().equals(this.selectedRecipeId)) {
                currentIndex = index;
                break;
            }
        }
        int nextIndex = currentIndex < 0
                ? (direction < 0 ? learnedRecipes.size() - 1 : 0)
                : Math.floorMod(currentIndex + direction, learnedRecipes.size());
        AlchemyRecipeClientData selectedRecipe = learnedRecipes.get(nextIndex);
        this.selectedRecipeId = selectedRecipe.id();
        this.trackedRecipe = selectedRecipe;
        this.selectionPendingTicks = 20;
        ModNetwork.sendToServer(new SelectAlchemyRecipePacket(
                this.menu.getBlockPosition(),
                selectedRecipe.id()
        ));
    }

    private Optional<AlchemyRecipeClientData> getSelectedRecipe() {
        return this.selectedRecipeId == null
                ? Optional.empty()
                : ClientAlchemyRecipeManager.getRecipe(this.selectedRecipeId);
    }

    private void renderProcessTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isInsideRecipePanel(mouseX, mouseY)) {
            getSelectedRecipe().ifPresent(recipe -> {
                java.util.ArrayList<Component> lines = new java.util.ArrayList<>();
                lines.add(Component.literal(recipe.name()).withStyle(recipe.grade().getFormatting()));
                if (recipe.feedPlan().isEmpty()) {
                    lines.add(Component.translatable(
                            "screen.ninecun_lingtai.alchemy.process.initial_all"
                    ));
                } else {
                    for (AlchemyFeedStep step : recipe.feedPlan()) {
                        HeatBand band = recipe.heatStages().get(step.stageIndex()).band();
                        lines.add(Component.translatable(
                                "screen.ninecun_lingtai.alchemy.process.step",
                                step.stageIndex() + 1,
                                band.getChineseName(),
                                getSlotName(step.slot())
                        ));
                    }
                }
                guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            });
            return;
        }
        HeatControlMode hoveredControl = getControlAt(mouseX, mouseY);
        if (hoveredControl != HeatControlMode.NONE) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.translatable(switch (hoveredControl) {
                        case STOKE -> "tooltip.ninecun_lingtai.alchemy.control.stoke";
                        case VENT -> "tooltip.ninecun_lingtai.alchemy.control.vent";
                        case MAINTAIN -> "tooltip.ninecun_lingtai.alchemy.control.maintain";
                        case NONE -> throw new IllegalStateException();
                    }),
                    mouseX,
                    mouseY
            );
        }
    }

    private static Component getSlotName(AlchemyIngredientSlot slot) {
        return Component.translatable(
                "screen.ninecun_lingtai.alchemy.slot." + slot.getSerializedName()
        );
    }

    private void renderActions(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.isRefining()) {
            renderControlButton(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    HeatControlMode.STOKE,
                    STOKE_BUTTON_TOP,
                    "screen.ninecun_lingtai.alchemy.control.stoke"
            );
            renderControlButton(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    HeatControlMode.VENT,
                    VENT_BUTTON_TOP,
                    "screen.ninecun_lingtai.alchemy.control.vent"
            );
            renderControlButton(
                    guiGraphics,
                    mouseX,
                    mouseY,
                    HeatControlMode.MAINTAIN,
                    MAINTAIN_BUTTON_TOP,
                    "screen.ninecun_lingtai.alchemy.control.maintain"
            );
        }
        boolean startBlocked = isStartBlocked();
        boolean startHovered = !startBlocked && isInsideStartButton(mouseX, mouseY);
        int buttonLeft = this.leftPos + START_BUTTON_LEFT;
        int buttonTop = this.topPos + START_BUTTON_TOP;
        int buttonFill = startBlocked
                ? DISABLED_BUTTON_FILL_COLOR
                : startHovered ? 0xFF3C3120 : 0xFF29231A;
        int buttonBorder = startBlocked
                ? DISABLED_BUTTON_BORDER_COLOR
                : startHovered ? OUTPUT_SLOT_BORDER_COLOR : SLOT_BORDER_COLOR;
        guiGraphics.fill(
                buttonLeft,
                buttonTop,
                buttonLeft + START_BUTTON_WIDTH,
                buttonTop + ACTION_BUTTON_HEIGHT,
                buttonFill
        );
        guiGraphics.renderOutline(
                buttonLeft,
                buttonTop,
                START_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT,
                buttonBorder
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(this.menu.isRefining()
                        ? "screen.ninecun_lingtai.alchemy.stop"
                        : "screen.ninecun_lingtai.alchemy.start"),
                buttonLeft + START_BUTTON_WIDTH / 2,
                buttonTop + 6,
                startBlocked ? DISABLED_BUTTON_TEXT_COLOR : PRIMARY_TEXT_COLOR
        );
    }

    private void renderControlButton(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            HeatControlMode mode,
            int top,
            String translationKey
    ) {
        boolean active = this.heldControl == mode || this.menu.getHeatControlMode() == mode;
        boolean hovered = isInsideControlButton(mouseX, mouseY, top);
        int left = this.leftPos + CONTROL_BUTTON_LEFT;
        int absoluteTop = this.topPos + top;
        int fill = active ? 0xFF314867 : hovered ? 0xFF3C3120 : 0xFF29231A;
        int border = active ? MAINTAIN_GLOW_INNER_COLOR
                : hovered ? OUTPUT_SLOT_BORDER_COLOR : SLOT_BORDER_COLOR;
        guiGraphics.fill(
                left,
                absoluteTop,
                left + CONTROL_BUTTON_WIDTH,
                absoluteTop + CONTROL_BUTTON_HEIGHT,
                fill
        );
        guiGraphics.renderOutline(
                left,
                absoluteTop,
                CONTROL_BUTTON_WIDTH,
                CONTROL_BUTTON_HEIGHT,
                border
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(translationKey),
                left + CONTROL_BUTTON_WIDTH / 2,
                absoluteTop + 6,
                PRIMARY_TEXT_COLOR
        );
    }

    private boolean isStartBlocked() {
        return !this.menu.isRefining()
                && this.menu.getPrimaryElementOrdinal() != 0
                && this.menu.getEnvironmentMultiplierX100() == 0;
    }

    private int getIlluminatedStars() {
        return ClientCultivationInfoManager.getCultivationInfo()
                .flatMap(PlayerCultivationInfo::soulStrength)
                .map(SoulStrength::illuminatedStars)
                .orElse(0);
    }

    private double getEffectiveBrewingDuration(AlchemyRecipeClientData recipe) {
        double environmentMultiplier = this.menu.getEnvironmentMultiplierX100() / 100.0D;
        return recipe.brewingDuration() / Mth.clamp(
                environmentMultiplier,
                MINIMUM_ENVIRONMENT_SPEED_MULTIPLIER,
                MAXIMUM_ENVIRONMENT_SPEED_MULTIPLIER
        );
    }

    private boolean isStageSwitchImminent(List<HeatStage> stages, double progress, double effectiveDuration) {
        if (progress > 1.0D) {
            return false;
        }
        double totalPortion = stages.stream().mapToDouble(HeatStage::portion).sum();
        double accumulated = 0.0D;
        for (int stageIndex = 0; stageIndex < stages.size(); stageIndex++) {
            accumulated += stages.get(stageIndex).portion() / totalPortion;
            if (progress <= accumulated) {
                if (stageIndex + 1 >= stages.size()
                        || stages.get(stageIndex + 1).band() == stages.get(stageIndex).band()) {
                    return false;
                }
                double remainingTicks = accumulated * effectiveDuration - this.localRefiningTicks;
                return remainingTicks >= 0.0D && remainingTicks <= STAGE_SWITCH_WARNING_TICKS;
            }
        }
        return false;
    }

    private Optional<AlchemyRecipeClientData> findMatchingLearnedRecipe() {
        return ClientAlchemyRecipeManager.getLearnedJeiRecipes().stream()
                .filter(this::matchesFurnaceContents)
                .findFirst()
                .flatMap(recipe -> ClientAlchemyRecipeManager.getRecipe(recipe.id()));
    }

    private boolean matchesFurnaceContents(AlchemyRecipeJeiData recipe) {
        return matchesIngredient(AlchemyFurnaceMenu.MAIN_INGREDIENT_SLOT, recipe.primaryIngredient())
                && matchesIngredient(
                        AlchemyFurnaceMenu.FIRST_SECONDARY_INGREDIENT_SLOT,
                        recipe.secondaryIngredients().get(0)
                )
                && matchesIngredient(
                        AlchemyFurnaceMenu.SECOND_SECONDARY_INGREDIENT_SLOT,
                        recipe.secondaryIngredients().get(1)
                )
                && matchesIngredient(
                        AlchemyFurnaceMenu.FIRST_AUXILIARY_INGREDIENT_SLOT,
                        recipe.auxiliaryIngredients().get(0)
                )
                && matchesIngredient(
                        AlchemyFurnaceMenu.SECOND_AUXILIARY_INGREDIENT_SLOT,
                        recipe.auxiliaryIngredients().get(1)
                );
    }

    private boolean matchesIngredient(int slotIndex, AlchemyIngredient ingredient) {
        ItemStack itemStack = this.menu.slots.get(slotIndex).getItem();
        return itemStack.getCount() >= ingredient.count()
                && BuiltInRegistries.ITEM.getKey(itemStack.getItem()).equals(ingredient.item());
    }

    private static int getHeatGradientColor(int heat) {
        int clampedHeat = Mth.clamp(heat, 0, getMaximumHeat());
        for (int index = 0; index < HEAT_GRADIENT_LEVELS.length - 1; index++) {
            int lowerHeat = HEAT_GRADIENT_LEVELS[index];
            int upperHeat = HEAT_GRADIENT_LEVELS[index + 1];
            if (clampedHeat <= upperHeat) {
                double ratio = (clampedHeat - lowerHeat) / (double) (upperHeat - lowerHeat);
                return blendArgb(
                        HEAT_GRADIENT_COLORS[index],
                        HEAT_GRADIENT_COLORS[index + 1],
                        ratio
                );
            }
        }
        return HEAT_GRADIENT_COLORS[HEAT_GRADIENT_COLORS.length - 1];
    }

    private static int blendArgb(int fromColor, int toColor, double amount) {
        double clampedAmount = Mth.clamp(amount, 0.0D, 1.0D);
        int alpha = interpolateColorChannel(
                fromColor >>> 24,
                toColor >>> 24,
                clampedAmount
        );
        int red = interpolateColorChannel(
                fromColor >>> 16 & 0xFF,
                toColor >>> 16 & 0xFF,
                clampedAmount
        );
        int green = interpolateColorChannel(
                fromColor >>> 8 & 0xFF,
                toColor >>> 8 & 0xFF,
                clampedAmount
        );
        int blue = interpolateColorChannel(
                fromColor & 0xFF,
                toColor & 0xFF,
                clampedAmount
        );
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static int interpolateColorChannel(int from, int to, double amount) {
        return Mth.clamp((int) Math.round(from + (to - from) * amount), 0, 255);
    }

    private static int withAlpha(int color, int alpha) {
        return Mth.clamp(alpha, 0, 255) << 24 | color & 0x00FFFFFF;
    }

    private int heatToY(int heat) {
        int innerBottom = this.topPos + HEAT_BAR_TOP + HEAT_BAR_HEIGHT - 1;
        double heatRatio = Mth.clamp(heat / (double) getMaximumHeat(), 0.0D, 1.0D);
        return innerBottom - (int) Math.round(heatRatio * (HEAT_BAR_HEIGHT - 2));
    }

    private static int getMaximumHeat() {
        return HeatBand.FIERCE.getMaximumHeat();
    }

    private boolean isInsideRecipePanel(double mouseX, double mouseY) {
        return isInside(
                mouseX,
                mouseY,
                this.leftPos + RECIPE_PANEL_LEFT,
                this.topPos + RECIPE_PANEL_TOP,
                RECIPE_PANEL_WIDTH,
                RECIPE_PANEL_HEIGHT
        );
    }

    private boolean isInsidePreviousRecipeButton(double mouseX, double mouseY) {
        return isInside(
                mouseX,
                mouseY,
                this.leftPos + RECIPE_PANEL_LEFT,
                this.topPos + RECIPE_PANEL_TOP + 17,
                RECIPE_ARROW_WIDTH,
                RECIPE_PANEL_HEIGHT - 17
        );
    }

    private boolean isInsideNextRecipeButton(double mouseX, double mouseY) {
        return isInside(
                mouseX,
                mouseY,
                this.leftPos + RECIPE_PANEL_LEFT + RECIPE_PANEL_WIDTH - RECIPE_ARROW_WIDTH,
                this.topPos + RECIPE_PANEL_TOP + 17,
                RECIPE_ARROW_WIDTH,
                RECIPE_PANEL_HEIGHT - 17
        );
    }

    private HeatControlMode getControlAt(double mouseX, double mouseY) {
        if (!this.menu.isRefining()) {
            return HeatControlMode.NONE;
        }
        if (isInsideControlButton(mouseX, mouseY, STOKE_BUTTON_TOP)) {
            return HeatControlMode.STOKE;
        }
        if (isInsideControlButton(mouseX, mouseY, VENT_BUTTON_TOP)) {
            return HeatControlMode.VENT;
        }
        if (isInsideControlButton(mouseX, mouseY, MAINTAIN_BUTTON_TOP)) {
            return HeatControlMode.MAINTAIN;
        }
        return HeatControlMode.NONE;
    }

    private boolean isInsideHeatZone(double mouseX, double mouseY) {
        int zoneLeft = HEAT_BAR_LEFT - 8;
        return isInside(
                mouseX,
                mouseY,
                this.leftPos + zoneLeft,
                this.topPos + HEAT_BAR_TOP - 4,
                this.imageWidth - zoneLeft - 2,
                HEAT_BAR_HEIGHT + 20
        );
    }

    private boolean isInsideControlButton(double mouseX, double mouseY, int top) {
        return isInside(
                mouseX,
                mouseY,
                this.leftPos + CONTROL_BUTTON_LEFT,
                this.topPos + top,
                CONTROL_BUTTON_WIDTH,
                CONTROL_BUTTON_HEIGHT
        );
    }

    private boolean isInsideStartButton(double mouseX, double mouseY) {
        return isInside(
                mouseX,
                mouseY,
                this.leftPos + START_BUTTON_LEFT,
                this.topPos + START_BUTTON_TOP,
                START_BUTTON_WIDTH,
                ACTION_BUTTON_HEIGHT
        );
    }

    private ScreenPoint point(float x, float y) {
        return new ScreenPoint(this.leftPos + x, this.topPos + y);
    }

    private ScreenPoint slotCenter(int slotIndex) {
        net.minecraft.world.inventory.Slot slot = this.menu.slots.get(slotIndex);
        return point(slot.x + SLOT_CENTER_OFFSET, slot.y + SLOT_CENTER_OFFSET);
    }

    private static void drawConnection(GuiGraphics guiGraphics, ScreenPoint start, ScreenPoint end, int color) {
        AptitudeRenderUtility.drawLine(guiGraphics, start, end, 0.65F, color);
    }

    private static boolean isInside(
            double mouseX,
            double mouseY,
            int left,
            int top,
            int width,
            int height
    ) {
        return mouseX >= left
                && mouseX < left + width
                && mouseY >= top
                && mouseY < top + height;
    }
}
