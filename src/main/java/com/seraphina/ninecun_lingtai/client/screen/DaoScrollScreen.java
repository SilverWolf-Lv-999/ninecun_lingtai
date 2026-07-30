package com.seraphina.ninecun_lingtai.client.screen;

import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.debug.DaoScrollSnapshot;
import com.seraphina.ninecun_lingtai.common.debug.DaoScrollTechniqueData;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.network.packet.SubmitDaoScrollPacket;
import com.seraphina.ninecun_lingtai.utility.SpiritualRootUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class DaoScrollScreen extends Screen {
    private static final int PANEL_COLOR = 0xF2181B24;
    private static final int PANEL_BORDER_COLOR = 0xFF75644D;
    private static final int CARD_COLOR = 0xD9282C38;
    private static final int PRIMARY_TEXT_COLOR = 0xFFF0E5CA;
    private static final int SECONDARY_TEXT_COLOR = 0xFFB9B2A4;
    private static final int MUTED_TEXT_COLOR = 0xFF817D75;
    private static final int ERROR_TEXT_COLOR = 0xFFFF7777;
    private static final int STATUS_TEXT_COLOR = 0xFF8FD694;
    private static final int CONTENT_TOP = 54;
    private static final int FOOTER_HEIGHT = 28;
    private static final int WIDGET_HEIGHT = 20;
    private static final int ROW_HEIGHT = 28;
    private static final int MAXIMUM_PANEL_WIDTH = 560;
    private static final int MAXIMUM_PANEL_HEIGHT = 420;
    private static final List<ElementType> EDITABLE_ROOT_ELEMENTS = List.of(
            ElementType.METAL,
            ElementType.WOOD,
            ElementType.WATER,
            ElementType.FIRE,
            ElementType.EARTH,
            ElementType.YIN_YANG
    );
    private static final List<ElementType> ENERGY_ELEMENTS = List.of(ElementType.values());

    private final DaoScrollSnapshot initialSnapshot;
    private final boolean editingSelf;
    private final EnumSet<ElementType> selectedRootElements = EnumSet.noneOf(ElementType.class);
    private final EnumMap<ElementType, Button> rootElementButtons = new EnumMap<>(ElementType.class);
    private final EnumMap<ElementType, EditBox> energyBoxes = new EnumMap<>(ElementType.class);
    private final List<ScrollPlacement> scrollPlacements = new ArrayList<>();
    private final List<EditBox> editBoxes = new ArrayList<>();
    private final List<TechniqueDraft> techniqueDrafts = new ArrayList<>();
    private final List<TechniqueControls> techniqueControls = new ArrayList<>();
    private final List<LabelPlacement> labels = new ArrayList<>();

    private boolean stageEnabled;
    private CultivationRealm realmDraft;
    private boolean rootEnabled;
    private int soulStars;
    private Page currentPage = Page.GENERAL;
    private double scrollOffset;
    private int generalContentHeight;
    private int techniqueContentHeight;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int viewportTop;
    private int viewportBottom;
    private Button generalTab;
    private Button techniqueTab;
    private Button saveButton;
    private Button cancelButton;
    private CycleButton<Boolean> stageToggle;
    private CycleButton<CultivationRealm> realmButton;
    private CycleButton<Boolean> rootToggle;
    private CycleButton<Integer> soulButton;
    private EditBox layerBox;
    private EditBox rootLengthBox;
    private EditBox ageBox;
    private EditBox defenseBox;
    private Component statusMessage = Component.empty();
    private boolean statusError;

    public DaoScrollScreen(DaoScrollSnapshot snapshot, boolean editingSelf) {
        super(Component.translatable("screen.ninecun_lingtai.dao_scroll.title"));
        this.initialSnapshot = snapshot;
        this.editingSelf = editingSelf;
        this.stageEnabled = snapshot.cultivationStage().isPresent();
        this.realmDraft = snapshot.cultivationStage()
                .map(CultivationStage::realm)
                .orElse(CultivationStage.first().realm());
        this.rootEnabled = snapshot.spiritualRoot().isPresent();
        snapshot.spiritualRoot().ifPresent(root -> this.selectedRootElements.addAll(root.elements()));
        this.soulStars = snapshot.soulStrength()
                .map(SoulStrength::illuminatedStars)
                .orElse(SoulStrength.MINIMUM_ILLUMINATED_STARS);
        for (DaoScrollTechniqueData technique : snapshot.techniques()) {
            this.techniqueDrafts.add(new TechniqueDraft(technique));
        }
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(MAXIMUM_PANEL_WIDTH, Math.max(360, this.width - 20));
        this.panelHeight = Math.min(MAXIMUM_PANEL_HEIGHT, Math.max(240, this.height - 20));
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;
        this.viewportTop = this.panelTop + CONTENT_TOP;
        this.viewportBottom = this.panelTop + this.panelHeight - FOOTER_HEIGHT;
        this.scrollPlacements.clear();
        this.editBoxes.clear();
        this.techniqueControls.clear();
        this.rootElementButtons.clear();
        this.energyBoxes.clear();
        this.labels.clear();
        buildGeneralWidgets();
        buildTechniqueWidgets();
        buildFixedWidgets();
        updateLayout();
    }

    @Override
    public void tick() {
        for (EditBox editBox : this.editBoxes) {
            editBox.tick();
        }
    }

    @Override
    public void render(
            GuiGraphics guiGraphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        updateLayout();
        this.renderBackground(guiGraphics);
        int panelRight = this.panelLeft + this.panelWidth;
        int panelBottom = this.panelTop + this.panelHeight;
        guiGraphics.fill(this.panelLeft, this.panelTop, panelRight, panelBottom, PANEL_BORDER_COLOR);
        guiGraphics.fill(
                this.panelLeft + 1,
                this.panelTop + 1,
                panelRight - 1,
                panelBottom - 1,
                PANEL_COLOR
        );
        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                this.panelTop + 8,
                PRIMARY_TEXT_COLOR
        );
        Component targetText = Component.translatable(
                this.editingSelf
                        ? "screen.ninecun_lingtai.dao_scroll.editing_self"
                        : "screen.ninecun_lingtai.dao_scroll.target",
                this.initialSnapshot.getEntityNameComponent()
        );
        guiGraphics.drawCenteredString(
                this.font,
                targetText,
                this.width / 2,
                this.panelTop + 22,
                SECONDARY_TEXT_COLOR
        );
        renderContent(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseY >= this.viewportTop && mouseY < this.viewportBottom) {
            int maximumScroll = getMaximumScroll();
            if (maximumScroll > 0) {
                this.scrollOffset = Mth.clamp(
                        this.scrollOffset - delta * 22.0D,
                        0.0D,
                        maximumScroll
                );
                updateLayout();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    private void buildFixedWidgets() {
        int tabWidth = (this.panelWidth - 28) / 2;
        this.generalTab = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.tab.general"),
                        button -> selectPage(Page.GENERAL)
                )
                .bounds(this.panelLeft + 10, this.panelTop + 31, tabWidth, WIDGET_HEIGHT)
                .build();
        this.techniqueTab = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.tab.techniques"),
                        button -> selectPage(Page.TECHNIQUES)
                )
                .bounds(this.panelLeft + 18 + tabWidth, this.panelTop + 31, tabWidth, WIDGET_HEIGHT)
                .build();
        this.addRenderableWidget(this.generalTab);
        this.addRenderableWidget(this.techniqueTab);

        int footerTop = this.panelTop + this.panelHeight - 24;
        int footerWidth = Math.min(260, this.panelWidth - 24);
        int buttonWidth = (footerWidth - 4) / 2;
        int footerLeft = this.panelLeft + (this.panelWidth - footerWidth) / 2;
        this.cancelButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.cancel"),
                        button -> onClose()
                )
                .bounds(footerLeft, footerTop, buttonWidth, WIDGET_HEIGHT)
                .build();
        this.saveButton = Button.builder(
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.save"),
                        button -> saveDraft()
                )
                .bounds(footerLeft + buttonWidth + 4, footerTop, buttonWidth, WIDGET_HEIGHT)
                .build();
        this.addRenderableWidget(this.cancelButton);
        this.addRenderableWidget(this.saveButton);
    }

    private void buildGeneralWidgets() {
        int y = 8;
        int fieldLeft = 145;
        int fieldWidth = Math.max(80, this.panelWidth - fieldLeft - 12);
        addLabel(Page.GENERAL, 10, y + 6, 130, "screen.ninecun_lingtai.dao_scroll.field.stage");
        this.stageToggle = CycleButton.<Boolean>builder(value -> Component.translatable(
                        value
                                ? "screen.ninecun_lingtai.dao_scroll.enabled"
                                : "screen.ninecun_lingtai.dao_scroll.disabled"
                ))
                .withValues(List.of(false, true))
                .withInitialValue(this.stageEnabled)
                .displayOnlyValue()
                .create(
                        0,
                        0,
                        110,
                        WIDGET_HEIGHT,
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.field.stage"),
                        (button, value) -> {
                            this.stageEnabled = value;
                            updateWidgetState();
                        }
                );
        addScrollWidget(this.stageToggle, fieldLeft, y, 110, WIDGET_HEIGHT, Page.GENERAL);
        this.realmButton = CycleButton.<CultivationRealm>builder(
                        value -> Component.literal(value.getChineseName())
                )
                .withValues(CultivationRealm.values())
                .withInitialValue(this.realmDraft)
                .displayOnlyValue()
                .create(260, 0, 100, WIDGET_HEIGHT, Component.empty(), (button, value) -> {
                    this.realmDraft = value;
                });
        addScrollWidget(this.realmButton, 260, y, 100, WIDGET_HEIGHT, Page.GENERAL);
        this.layerBox = createIntegerBox(
                "screen.ninecun_lingtai.dao_scroll.field.layer",
                String.valueOf(this.initialSnapshot.cultivationStage().map(CultivationStage::layer).orElse(1)),
                2
        );
        addScrollWidget(this.layerBox, 370, y, 48, WIDGET_HEIGHT, Page.GENERAL);
        y += 30;

        addLabel(Page.GENERAL, 10, y + 6, 130, "screen.ninecun_lingtai.dao_scroll.field.root");
        this.rootToggle = CycleButton.<Boolean>builder(value -> Component.translatable(
                        value
                                ? "screen.ninecun_lingtai.dao_scroll.enabled"
                                : "screen.ninecun_lingtai.dao_scroll.disabled"
                ))
                .withValues(List.of(false, true))
                .withInitialValue(this.rootEnabled)
                .displayOnlyValue()
                .create(
                        0,
                        0,
                        110,
                        WIDGET_HEIGHT,
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.field.root"),
                        (button, value) -> {
                            this.rootEnabled = value;
                            updateWidgetState();
                        }
                );
        addScrollWidget(this.rootToggle, fieldLeft, y, 110, WIDGET_HEIGHT, Page.GENERAL);
        this.rootLengthBox = createDecimalBox(
                "screen.ninecun_lingtai.dao_scroll.field.root_length",
                String.format(
                        Locale.ROOT,
                        "%.1f",
                        this.initialSnapshot.spiritualRoot()
                                .map(SpiritualRoot::lengthInches)
                                .orElse(SpiritualRoot.MINIMUM_LENGTH_INCHES)
                ),
                4
        );
        addScrollWidget(this.rootLengthBox, 260, y, 70, WIDGET_HEIGHT, Page.GENERAL);
        y += 28;

        int elementButtonWidth = Math.max(48, (this.panelWidth - 155) / 3 - 4);
        int elementButtonX = 145;
        for (int elementIndex = 0; elementIndex < EDITABLE_ROOT_ELEMENTS.size(); elementIndex++) {
            ElementType element = EDITABLE_ROOT_ELEMENTS.get(elementIndex);
            int row = elementIndex / 3;
            int column = elementIndex % 3;
            Button elementButton = Button.builder(
                            Component.empty(),
                            button -> {
                                if (this.selectedRootElements.contains(element)) {
                                    this.selectedRootElements.remove(element);
                                } else {
                                    this.selectedRootElements.add(element);
                                }
                                updateRootElementButtonMessages();
                            }
                    )
                    .bounds(0, 0, elementButtonWidth, WIDGET_HEIGHT)
                    .build();
            this.rootElementButtons.put(element, elementButton);
            addScrollWidget(
                    elementButton,
                    elementButtonX + column * (elementButtonWidth + 4),
                    y + row * 24,
                    elementButtonWidth,
                    WIDGET_HEIGHT,
                    Page.GENERAL
            );
        }
        y += 52;

        if (this.initialSnapshot.player()) {
            addLabel(Page.GENERAL, 10, y + 6, 130, "screen.ninecun_lingtai.dao_scroll.field.soul_strength");
            this.soulButton = CycleButton.<Integer>builder(value -> Component.literal(value + " " + Component.translatable(
                            "screen.ninecun_lingtai.dao_scroll.stars"
                    ).getString()))
                    .withValues(List.of(1, 2, 3, 4, 5, 6, 7))
                    .withInitialValue(this.soulStars)
                    .displayOnlyValue()
                    .create(0, 0, 90, WIDGET_HEIGHT, Component.empty(), (button, value) -> {
                        this.soulStars = value;
                    });
            addScrollWidget(this.soulButton, fieldLeft, y, 90, WIDGET_HEIGHT, Page.GENERAL);
            y += 28;
        }

        addLabel(Page.GENERAL, 10, y + 6, 130, "screen.ninecun_lingtai.dao_scroll.field.age");
        this.ageBox = createIntegerBox(
                "screen.ninecun_lingtai.dao_scroll.field.age",
                String.valueOf(this.initialSnapshot.ageTicks()),
                20
        );
        addScrollWidget(this.ageBox, fieldLeft, y, fieldWidth, WIDGET_HEIGHT, Page.GENERAL);
        y += 28;

        addLabel(Page.GENERAL, 10, y + 6, 130, "screen.ninecun_lingtai.dao_scroll.field.defense");
        this.defenseBox = createIntegerBox(
                "screen.ninecun_lingtai.dao_scroll.field.defense",
                String.valueOf(this.initialSnapshot.defenseCurrent()),
                12
        );
        addScrollWidget(this.defenseBox, fieldLeft, y, fieldWidth, WIDGET_HEIGHT, Page.GENERAL);
        y += 34;

        if (this.initialSnapshot.player()) {
            addLabel(Page.GENERAL, 10, y, this.panelWidth - 20, "screen.ninecun_lingtai.dao_scroll.field.energy");
            y += 20;
            for (ElementType element : ENERGY_ELEMENTS) {
                addLabel(
                        Page.GENERAL,
                        16,
                        y + 6,
                        120,
                        Component.literal(SpiritualRootUtility.getElementName(element))
                );
                EditBox energyBox = createDecimalBox(
                        "screen.ninecun_lingtai.dao_scroll.field.energy",
                        formatDouble(this.initialSnapshot.getEnergy(element)),
                        24
                );
                this.energyBoxes.put(element, energyBox);
                addScrollWidget(energyBox, fieldLeft, y, fieldWidth, WIDGET_HEIGHT, Page.GENERAL);
                y += 25;
            }
        }
        this.generalContentHeight = y + 8;
        updateRootElementButtonMessages();
    }

    private void buildTechniqueWidgets() {
        int y = 8;
        addLabel(
                Page.TECHNIQUES,
                10,
                y,
                this.panelWidth - 20,
                "screen.ninecun_lingtai.dao_scroll.field.techniques"
        );
        y += 24;
        if (this.techniqueDrafts.isEmpty()) {
            addLabel(
                    Page.TECHNIQUES,
                    10,
                    y + 4,
                    this.panelWidth - 20,
                    "screen.ninecun_lingtai.dao_scroll.no_techniques"
            );
            this.techniqueContentHeight = y + 28;
            return;
        }

        int controlLeft = 145;
        int labelWidth = Math.max(80, controlLeft - 18);
        int learnedWidth = 68;
        int scoreWidth = 54;
        int proficiencyWidth = Math.max(70, this.panelWidth - controlLeft - learnedWidth - scoreWidth - 84);
        int imprintLeft = controlLeft + learnedWidth + scoreWidth + proficiencyWidth + 12;
        int imprintWidth = Math.max(52, this.panelWidth - imprintLeft - 10);
        for (TechniqueDraft draft : this.techniqueDrafts) {
            addLabel(
                    Page.TECHNIQUES,
                    10,
                    y + 5,
                    labelWidth,
                    Component.literal(draft.name)
            );
            Button learnedButton = Button.builder(
                            Component.empty(),
                            button -> {
                                draft.learned = !draft.learned;
                                updateTechniqueButtonMessages();
                            }
                    )
                    .bounds(0, 0, learnedWidth, WIDGET_HEIGHT)
                    .build();
            EditBox scoreBox = createIntegerBox(
                    "screen.ninecun_lingtai.dao_scroll.field.comprehension",
                    String.valueOf(draft.comprehensionScore),
                    3
            );
            EditBox proficiencyBox = createIntegerBox(
                    "screen.ninecun_lingtai.dao_scroll.field.proficiency",
                    String.valueOf(draft.proficiency),
                    20
            );
            Button imprintButton = Button.builder(
                            Component.empty(),
                            button -> {
                                draft.imprinted = !draft.imprinted;
                                updateTechniqueButtonMessages();
                            }
                    )
                    .bounds(0, 0, imprintWidth, WIDGET_HEIGHT)
                    .build();
            TechniqueControls controls = new TechniqueControls(
                    draft,
                    learnedButton,
                    scoreBox,
                    proficiencyBox,
                    imprintButton
            );
            this.techniqueControls.add(controls);
            addScrollWidget(learnedButton, controlLeft, y, learnedWidth, WIDGET_HEIGHT, Page.TECHNIQUES);
            addScrollWidget(
                    scoreBox,
                    controlLeft + learnedWidth + 4,
                    y,
                    scoreWidth,
                    WIDGET_HEIGHT,
                    Page.TECHNIQUES
            );
            addScrollWidget(
                    proficiencyBox,
                    controlLeft + learnedWidth + scoreWidth + 8,
                    y,
                    proficiencyWidth,
                    WIDGET_HEIGHT,
                    Page.TECHNIQUES
            );
            addScrollWidget(
                    imprintButton,
                    imprintLeft,
                    y,
                    imprintWidth,
                    WIDGET_HEIGHT,
                    Page.TECHNIQUES
            );
            y += ROW_HEIGHT;
        }
        this.techniqueContentHeight = y + 8;
        updateTechniqueButtonMessages();
    }

    private void updateLayout() {
        if (this.panelWidth <= 0) {
            return;
        }
        this.scrollOffset = Mth.clamp(
                this.scrollOffset,
                0.0D,
                getMaximumScroll()
        );
        for (ScrollPlacement placement : this.scrollPlacements) {
            int widgetY = this.viewportTop + placement.contentY - (int) Math.round(this.scrollOffset);
            placement.widget.setX(this.panelLeft + placement.xOffset);
            placement.widget.setY(widgetY);
            boolean pageVisible = placement.page == this.currentPage;
            placement.widget.visible = pageVisible
                    && widgetY >= this.viewportTop
                    && widgetY + placement.height <= this.viewportBottom;
        }
        updateWidgetState();
        this.generalTab.active = this.currentPage != Page.GENERAL;
        this.techniqueTab.active = this.currentPage != Page.TECHNIQUES;
    }

    private void updateWidgetState() {
        if (this.stageToggle != null) {
            this.realmButton.active = this.stageEnabled;
            this.layerBox.active = this.stageEnabled;
        }
        if (this.rootToggle != null) {
            this.rootLengthBox.active = this.rootEnabled;
            for (Button button : this.rootElementButtons.values()) {
                button.active = this.rootEnabled;
            }
        }
    }

    private void selectPage(Page page) {
        this.currentPage = page;
        this.scrollOffset = 0.0D;
        updateLayout();
    }

    private int getMaximumScroll() {
        int contentHeight = this.currentPage == Page.GENERAL
                ? this.generalContentHeight
                : this.techniqueContentHeight;
        return Math.max(0, contentHeight - (this.viewportBottom - this.viewportTop));
    }

    private void renderContent(GuiGraphics guiGraphics) {
        guiGraphics.enableScissor(
                this.panelLeft + 4,
                this.viewportTop,
                this.panelLeft + this.panelWidth - 4,
                this.viewportBottom
        );
        for (LabelPlacement label : this.labels) {
            if (label.page != this.currentPage) {
                continue;
            }
            int y = this.viewportTop + label.contentY - (int) Math.round(this.scrollOffset);
            if (y + 12 < this.viewportTop || y > this.viewportBottom) {
                continue;
            }
            String text = label.text.getString();
            if (this.font.width(text) > label.width) {
                text = this.font.plainSubstrByWidth(
                        text,
                        Math.max(0, label.width - this.font.width("..."))
                ) + "...";
            }
            guiGraphics.drawString(this.font, text, this.panelLeft + label.xOffset, y, label.color, false);
        }
        if (this.currentPage == Page.TECHNIQUES && !this.techniqueDrafts.isEmpty()) {
            int headingY = this.viewportTop + 26 - (int) Math.round(this.scrollOffset);
            if (headingY >= this.viewportTop - 12 && headingY <= this.viewportBottom) {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.column.learned"),
                        this.panelLeft + 145,
                        headingY,
                        MUTED_TEXT_COLOR,
                        false
                );
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.column.score"),
                        this.panelLeft + 217,
                        headingY,
                        MUTED_TEXT_COLOR,
                        false
                );
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.ninecun_lingtai.dao_scroll.column.proficiency"),
                        this.panelLeft + 275,
                        headingY,
                        MUTED_TEXT_COLOR,
                        false
                );
            }
        }
        guiGraphics.disableScissor();

        if (!this.statusMessage.getString().isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    this.statusMessage,
                    this.width / 2,
                    this.panelTop + this.panelHeight - 39,
                    this.statusError ? ERROR_TEXT_COLOR : STATUS_TEXT_COLOR
            );
        }
        renderScrollBar(guiGraphics);
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int maximumScroll = getMaximumScroll();
        if (maximumScroll <= 0) {
            return;
        }
        int barLeft = this.panelLeft + this.panelWidth - 8;
        int trackHeight = this.viewportBottom - this.viewportTop;
        int thumbHeight = Math.max(16, trackHeight * trackHeight
                / Math.max(trackHeight, trackHeight + maximumScroll));
        int thumbTravel = trackHeight - thumbHeight;
        int thumbTop = this.viewportTop + (int) Math.round(
                thumbTravel * this.scrollOffset / maximumScroll
        );
        guiGraphics.fill(barLeft, this.viewportTop, barLeft + 2, this.viewportBottom, 0xFF2B2D35);
        guiGraphics.fill(barLeft, thumbTop, barLeft + 2, thumbTop + thumbHeight, 0xFF9C8768);
    }

    private void saveDraft() {
        Integer layer = this.stageEnabled
                ? parseInteger(
                this.layerBox,
                "screen.ninecun_lingtai.dao_scroll.field.layer",
                CultivationStage.MINIMUM_LAYER,
                CultivationStage.MAXIMUM_LAYER
        )
                : null;
        if (this.stageEnabled && layer == null) {
            return;
        }
        Double rootLength = this.rootEnabled
                ? parseDouble(
                this.rootLengthBox,
                "screen.ninecun_lingtai.dao_scroll.field.root_length",
                SpiritualRoot.MINIMUM_LENGTH_INCHES,
                SpiritualRoot.MAXIMUM_LENGTH_INCHES
        )
                : null;
        if (this.rootEnabled && rootLength == null) {
            return;
        }
        Long age = parseLong(
                this.ageBox,
                "screen.ninecun_lingtai.dao_scroll.field.age",
                0L,
                Long.MAX_VALUE
        );
        if (age == null) {
            return;
        }
        Integer defense = parseInteger(
                this.defenseBox,
                "screen.ninecun_lingtai.dao_scroll.field.defense",
                0,
                Integer.MAX_VALUE
        );
        if (defense == null) {
            return;
        }

        Optional<CultivationStage> cultivationStage = Optional.empty();
        if (this.stageEnabled) {
            cultivationStage = Optional.of(new CultivationStage(this.realmDraft, layer));
        }
        Optional<SpiritualRoot> spiritualRoot = Optional.empty();
        if (this.rootEnabled) {
            if (this.selectedRootElements.isEmpty()) {
                setError("screen.ninecun_lingtai.dao_scroll.error.no_root_elements");
                return;
            }
            try {
                spiritualRoot = Optional.of(new SpiritualRoot(this.selectedRootElements, rootLength.floatValue()));
            } catch (IllegalArgumentException exception) {
                setError("screen.ninecun_lingtai.dao_scroll.error.invalid_root");
                return;
            }
        }

        EnumMap<ElementType, Double> energy = new EnumMap<>(ElementType.class);
        if (this.initialSnapshot.player()) {
            for (Map.Entry<ElementType, EditBox> entry : this.energyBoxes.entrySet()) {
                Double value = parseDouble(
                        entry.getValue(),
                        "screen.ninecun_lingtai.dao_scroll.field.energy",
                        0.0D,
                        Double.MAX_VALUE
                );
                if (value == null) {
                    return;
                }
                if (value > 0.0D) {
                    energy.put(entry.getKey(), value);
                }
            }
        }

        List<DaoScrollTechniqueData> techniques = new ArrayList<>();
        for (TechniqueControls controls : this.techniqueControls) {
            Integer score = parseInteger(
                    controls.scoreBox,
                    "screen.ninecun_lingtai.dao_scroll.field.comprehension",
                    0,
                    100
            );
            if (score == null) {
                return;
            }
            Long proficiency = parseLong(
                    controls.proficiencyBox,
                    "screen.ninecun_lingtai.dao_scroll.field.proficiency",
                    0L,
                    Long.MAX_VALUE
            );
            if (proficiency == null) {
                return;
            }
            techniques.add(new DaoScrollTechniqueData(
                    controls.draft.id,
                    controls.draft.name,
                    controls.draft.learned,
                    score,
                    proficiency,
                    controls.draft.imprinted
            ));
        }

        Optional<SoulStrength> soulStrength = this.initialSnapshot.player()
                ? Optional.of(new SoulStrength(this.soulStars))
                : Optional.empty();
        DaoScrollSnapshot updatedSnapshot = new DaoScrollSnapshot(
                this.initialSnapshot.entityId(),
                this.initialSnapshot.entityName(),
                this.initialSnapshot.player(),
                cultivationStage,
                spiritualRoot,
                soulStrength,
                age,
                defense,
                energy,
                techniques
        );
        ModNetwork.sendToServer(new SubmitDaoScrollPacket(updatedSnapshot, this.editingSelf));
        onClose();
    }

    private Integer parseInteger(EditBox box, String labelKey, int minimum, int maximum) {
        try {
            int value = Integer.parseInt(box.getValue());
            if (value < minimum || value > maximum) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            setError(
                    "screen.ninecun_lingtai.dao_scroll.error.invalid_number",
                    Component.translatable(labelKey),
                    minimum,
                    maximum
            );
            return null;
        }
    }

    private Long parseLong(EditBox box, String labelKey, long minimum, long maximum) {
        try {
            long value = Long.parseLong(box.getValue());
            if (value < minimum || value > maximum) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            setError(
                    "screen.ninecun_lingtai.dao_scroll.error.invalid_number",
                    Component.translatable(labelKey),
                    minimum,
                    maximum
            );
            return null;
        }
    }

    private Double parseDouble(EditBox box, String labelKey, double minimum, double maximum) {
        try {
            double value = Double.parseDouble(box.getValue());
            if (!Double.isFinite(value) || value < minimum || value > maximum) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException exception) {
            setError(
                    "screen.ninecun_lingtai.dao_scroll.error.invalid_number",
                    Component.translatable(labelKey),
                    minimum,
                    maximum
            );
            return null;
        }
    }

    private EditBox createIntegerBox(String labelKey, String value, int maximumLength) {
        EditBox editBox = new EditBox(this.font, 0, 0, 100, WIDGET_HEIGHT, Component.translatable(labelKey));
        editBox.setMaxLength(maximumLength);
        editBox.setFilter(input -> input.isEmpty() || input.matches("\\d+"));
        editBox.setValue(value);
        this.editBoxes.add(editBox);
        return editBox;
    }

    private EditBox createDecimalBox(String labelKey, String value, int maximumLength) {
        EditBox editBox = new EditBox(this.font, 0, 0, 100, WIDGET_HEIGHT, Component.translatable(labelKey));
        editBox.setMaxLength(maximumLength);
        editBox.setFilter(input -> input.isEmpty() || input.matches("\\d*(\\.\\d*)?"));
        editBox.setValue(value);
        this.editBoxes.add(editBox);
        return editBox;
    }

    private void addScrollWidget(
            AbstractWidget widget,
            int xOffset,
            int contentY,
            int width,
            int height,
            Page page
    ) {
        this.scrollPlacements.add(new ScrollPlacement(widget, xOffset, contentY, width, height, page));
        this.addRenderableWidget(widget);
    }

    private void addLabel(Page page, int xOffset, int contentY, int width, String translationKey) {
        this.labels.add(new LabelPlacement(
                page,
                xOffset,
                contentY,
                width,
                Component.translatable(translationKey),
                PRIMARY_TEXT_COLOR
        ));
    }

    private void addLabel(
            Page page,
            int xOffset,
            int contentY,
            int width,
            Component text
    ) {
        this.labels.add(new LabelPlacement(page, xOffset, contentY, width, text, SECONDARY_TEXT_COLOR));
    }

    private void updateRootElementButtonMessages() {
        for (Map.Entry<ElementType, Button> entry : this.rootElementButtons.entrySet()) {
            boolean selected = this.selectedRootElements.contains(entry.getKey());
            String marker = selected ? "[x] " : "[ ] ";
            entry.getValue().setMessage(Component.literal(
                    marker + SpiritualRootUtility.getElementName(entry.getKey())
            ));
        }
    }

    private void updateTechniqueButtonMessages() {
        for (TechniqueControls controls : this.techniqueControls) {
            controls.learnedButton.setMessage(Component.translatable(
                    controls.draft.learned
                            ? "screen.ninecun_lingtai.dao_scroll.learned"
                            : "screen.ninecun_lingtai.dao_scroll.not_learned"
            ));
            controls.imprintButton.setMessage(Component.translatable(
                    controls.draft.imprinted
                            ? "screen.ninecun_lingtai.dao_scroll.imprinted"
                            : "screen.ninecun_lingtai.dao_scroll.not_imprinted"
            ));
        }
    }

    private void setError(String translationKey, Object... arguments) {
        this.statusMessage = Component.translatable(translationKey, arguments);
        this.statusError = true;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private enum Page {
        GENERAL,
        TECHNIQUES
    }

    private record ScrollPlacement(
            AbstractWidget widget,
            int xOffset,
            int contentY,
            int width,
            int height,
            Page page
    ) {
    }

    private record LabelPlacement(
            Page page,
            int xOffset,
            int contentY,
            int width,
            Component text,
            int color
    ) {
    }

    private static final class TechniqueDraft {
        private final ResourceLocation id;
        private final String name;
        private boolean learned;
        private final int comprehensionScore;
        private final String proficiency;
        private boolean imprinted;

        private TechniqueDraft(DaoScrollTechniqueData technique) {
            this.id = technique.id();
            this.name = technique.name();
            this.learned = technique.learned();
            this.comprehensionScore = technique.comprehensionScore();
            this.proficiency = String.valueOf(technique.proficiency());
            this.imprinted = technique.imprinted();
        }
    }

    private record TechniqueControls(
            TechniqueDraft draft,
            Button learnedButton,
            EditBox scoreBox,
            EditBox proficiencyBox,
            Button imprintButton
    ) {
    }
}
