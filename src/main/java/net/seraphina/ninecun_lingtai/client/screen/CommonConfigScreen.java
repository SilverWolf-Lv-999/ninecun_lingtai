package net.seraphina.ninecun_lingtai.client.screen;

import net.seraphina.ninecun_lingtai.api.ai.AiProvider;
import net.seraphina.ninecun_lingtai.client.utility.ConfigScreenUtility;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;

public final class CommonConfigScreen extends Screen {
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFE0E0E0;
    private static final int SECONDARY_TEXT_COLOR = 0xFFA0A0A0;
    private static final int ERROR_TEXT_COLOR = 0xFFFF5555;
    private static final int VALID_INPUT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_INPUT_COLOR = 0xFFFF7777;
    private static final int CATEGORY_BUTTON_HEIGHT = 20;
    private static final int INPUT_HEIGHT = 20;
    private static final int ROW_SPACING = 23;
    private static final int CONTENT_TOP = 70;

    private static final List<DoubleConfigEntry> ELEMENT_DAMAGE_ENTRIES = List.of(
            new DoubleConfigEntry(
                    "generation_weakening_multiplier",
                    CommonConfig.GENERATION_WEAKENING_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "restraint_enhancement_multiplier",
                    CommonConfig.RESTRAINT_ENHANCEMENT_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "suppressed_multiplier",
                    CommonConfig.SUPPRESSED_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "natural_restraint_advantage",
                    CommonConfig.NATURAL_RESTRAINT_ADVANTAGE,
                    CommonConfig.MINIMUM_NATURAL_RESTRAINT_ADVANTAGE,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "chaos_enhancement_multiplier",
                    CommonConfig.CHAOS_ENHANCEMENT_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "chaos_suppressed_multiplier",
                    CommonConfig.CHAOS_SUPPRESSED_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            )
    );
    private static final List<DoubleConfigEntry> MEDITATION_ENTRIES = List.of(
            new DoubleConfigEntry(
                    "meditation_base_absorption_per_second",
                    CommonConfig.MEDITATION_BASE_ABSORPTION_PER_SECOND,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_BASE_ABSORPTION_PER_SECOND
            ),
            new DoubleConfigEntry(
                    "meditation_defense_resource_repair_percent_per_second",
                    CommonConfig.MEDITATION_DEFENSE_RESOURCE_REPAIR_PERCENT_PER_SECOND,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "meditation_generates_multiplier",
                    CommonConfig.MEDITATION_GENERATES_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "meditation_generated_by_multiplier",
                    CommonConfig.MEDITATION_GENERATED_BY_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "meditation_overcomes_multiplier",
                    CommonConfig.MEDITATION_OVERCOMES_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            ),
            new DoubleConfigEntry(
                    "meditation_overcome_by_multiplier",
                    CommonConfig.MEDITATION_OVERCOME_BY_MULTIPLIER,
                    CommonConfig.MINIMUM_MULTIPLIER,
                    CommonConfig.MAXIMUM_MULTIPLIER
            )
    );

    private final Screen parentScreen;
    private final Map<ConfigCategory, List<DoubleConfigEntry>> entriesByCategory = new EnumMap<>(ConfigCategory.class);
    private final Map<ForgeConfigSpec.DoubleValue, String> doubleDraftValues = new LinkedHashMap<>();
    private final List<EditBox> visibleEditBoxes = new ArrayList<>();
    private ConfigCategory selectedCategory = ConfigCategory.ELEMENT_DAMAGE;
    private AiProvider aiProviderDraft;
    private String aiApiKeyDraft;
    private String aiModelDraft;
    private Component validationMessage = Component.empty();

    public CommonConfigScreen(Screen parentScreen) {
        super(Component.translatable("screen.ninecun_lingtai.config.title"));
        this.parentScreen = parentScreen;
        this.entriesByCategory.put(ConfigCategory.ELEMENT_DAMAGE, ELEMENT_DAMAGE_ENTRIES);
        this.entriesByCategory.put(ConfigCategory.MEDITATION, MEDITATION_ENTRIES);
        for (List<DoubleConfigEntry> entries : this.entriesByCategory.values()) {
            for (DoubleConfigEntry entry : entries) {
                this.doubleDraftValues.put(
                        entry.configValue(),
                        ConfigScreenUtility.formatDouble(entry.configValue().get())
                );
            }
        }
        this.aiProviderDraft = CommonConfig.AI_PROVIDER.get();
        this.aiApiKeyDraft = CommonConfig.AI_API_KEY.get();
        this.aiModelDraft = CommonConfig.AI_MODEL.get();
    }

    @Override
    protected void init() {
        this.visibleEditBoxes.clear();
        int contentWidth = Math.min(430, this.width - 24);
        int contentLeft = (this.width - contentWidth) / 2;
        int categoryButtonWidth = (contentWidth - 8) / ConfigCategory.values().length;

        for (int categoryIndex = 0; categoryIndex < ConfigCategory.values().length; categoryIndex++) {
            ConfigCategory category = ConfigCategory.values()[categoryIndex];
            int buttonLeft = contentLeft + categoryIndex * (categoryButtonWidth + 4);
            Button categoryButton = Button.builder(
                            Component.translatable(category.translationKey()),
                            button -> selectCategory(category)
                    )
                    .bounds(buttonLeft, 31, categoryButtonWidth, CATEGORY_BUTTON_HEIGHT)
                    .build();
            categoryButton.active = category != this.selectedCategory;
            this.addRenderableWidget(categoryButton);
        }

        int fieldWidth = Math.min(190, contentWidth / 2);
        int fieldLeft = contentLeft + contentWidth - fieldWidth;
        if (this.selectedCategory == ConfigCategory.AI) {
            addAiWidgets(fieldLeft, fieldWidth);
        } else {
            addDoubleWidgets(this.entriesByCategory.get(this.selectedCategory), fieldLeft, fieldWidth);
        }

        int footerWidth = Math.min(330, this.width - 24);
        int footerLeft = (this.width - footerWidth) / 2;
        int buttonWidth = (footerWidth - 8) / 3;
        int footerTop = this.height - 28;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ninecun_lingtai.config.reset"),
                        button -> resetSelectedCategory()
                )
                .bounds(footerLeft, footerTop, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ninecun_lingtai.config.cancel"),
                        button -> closeWithoutSaving()
                )
                .bounds(footerLeft + buttonWidth + 4, footerTop, buttonWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.ninecun_lingtai.config.save"),
                        button -> saveConfiguration()
                )
                .bounds(footerLeft + (buttonWidth + 4) * 2, footerTop, buttonWidth, 20)
                .build());
    }

    private void addDoubleWidgets(List<DoubleConfigEntry> entries, int fieldLeft, int fieldWidth) {
        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            DoubleConfigEntry entry = entries.get(entryIndex);
            int rowTop = CONTENT_TOP + entryIndex * ROW_SPACING;
            Component fieldLabel = Component.translatable(entry.translationKey());
            EditBox editBox = new EditBox(this.font, fieldLeft, rowTop, fieldWidth, INPUT_HEIGHT, fieldLabel);
            editBox.setMaxLength(24);
            editBox.setFilter(ConfigScreenUtility::isPotentialDecimal);
            editBox.setTooltip(Tooltip.create(Component.translatable(entry.tooltipTranslationKey())));
            editBox.setResponder(value -> {
                this.doubleDraftValues.put(entry.configValue(), value);
                updateDoubleBoxColor(editBox, entry, value);
                clearValidationMessage();
            });
            editBox.setValue(this.doubleDraftValues.get(entry.configValue()));
            updateDoubleBoxColor(editBox, entry, editBox.getValue());
            this.visibleEditBoxes.add(editBox);
            this.addRenderableWidget(editBox);
        }
    }

    private void addAiWidgets(int fieldLeft, int fieldWidth) {
        Component providerLabel = Component.translatable("screen.ninecun_lingtai.config.field.ai_provider");
        CycleButton<AiProvider> providerButton = CycleButton.<AiProvider>builder(provider -> Component.translatable(
                "screen.ninecun_lingtai.config.ai_provider." + provider.name().toLowerCase(Locale.ROOT)
                ))
                .withValues(AiProvider.values())
                .withInitialValue(this.aiProviderDraft)
                .withTooltip(provider -> Tooltip.create(Component.translatable(
                        "screen.ninecun_lingtai.config.field.ai_provider.tooltip"
                )))
                .displayOnlyValue()
                .create(fieldLeft, CONTENT_TOP, fieldWidth, INPUT_HEIGHT, providerLabel, (button, provider) -> {
                    this.aiProviderDraft = provider;
                    clearValidationMessage();
                });
        this.addRenderableWidget(providerButton);

        EditBox apiKeyBox = new EditBox(
                this.font,
                fieldLeft,
                CONTENT_TOP + ROW_SPACING,
                fieldWidth,
                INPUT_HEIGHT,
                Component.translatable("screen.ninecun_lingtai.config.field.ai_api_key")
        );
        apiKeyBox.setMaxLength(512);
        apiKeyBox.setTooltip(Tooltip.create(Component.translatable(
                "screen.ninecun_lingtai.config.field.ai_api_key.tooltip"
        )));
        apiKeyBox.setResponder(value -> {
            this.aiApiKeyDraft = value;
            clearValidationMessage();
        });
        apiKeyBox.setFormatter((value, characterIndex) -> FormattedCharSequence.forward(
                apiKeyBox.isFocused() ? value : "*".repeat(value.length()),
                Style.EMPTY
        ));
        apiKeyBox.setValue(this.aiApiKeyDraft);
        this.visibleEditBoxes.add(apiKeyBox);
        this.addRenderableWidget(apiKeyBox);

        EditBox modelBox = new EditBox(
                this.font,
                fieldLeft,
                CONTENT_TOP + ROW_SPACING * 2,
                fieldWidth,
                INPUT_HEIGHT,
                Component.translatable("screen.ninecun_lingtai.config.field.ai_model")
        );
        modelBox.setMaxLength(128);
        modelBox.setTooltip(Tooltip.create(Component.translatable(
                "screen.ninecun_lingtai.config.field.ai_model.tooltip"
        )));
        modelBox.setResponder(value -> {
            this.aiModelDraft = value;
            clearValidationMessage();
        });
        modelBox.setValue(this.aiModelDraft);
        this.visibleEditBoxes.add(modelBox);
        this.addRenderableWidget(modelBox);
    }

    @Override
    public void tick() {
        for (EditBox editBox : this.visibleEditBoxes) {
            editBox.tick();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, TITLE_COLOR);
        Component secondaryMessage = this.validationMessage.getString().isEmpty()
                ? Component.translatable(this.selectedCategory.descriptionTranslationKey())
                : this.validationMessage;
        int secondaryColor = this.validationMessage.getString().isEmpty()
                ? SECONDARY_TEXT_COLOR
                : ERROR_TEXT_COLOR;
        guiGraphics.drawCenteredString(this.font, secondaryMessage, this.width / 2, 56, secondaryColor);

        int contentWidth = Math.min(430, this.width - 24);
        int contentLeft = (this.width - contentWidth) / 2;
        int fieldWidth = Math.min(190, contentWidth / 2);
        int labelWidth = contentWidth - fieldWidth - 8;
        if (this.selectedCategory == ConfigCategory.AI) {
            drawFieldLabel(guiGraphics, "ai_provider", contentLeft, CONTENT_TOP, labelWidth);
            drawFieldLabel(guiGraphics, "ai_api_key", contentLeft, CONTENT_TOP + ROW_SPACING, labelWidth);
            drawFieldLabel(guiGraphics, "ai_model", contentLeft, CONTENT_TOP + ROW_SPACING * 2, labelWidth);
        } else {
            List<DoubleConfigEntry> entries = this.entriesByCategory.get(this.selectedCategory);
            for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
                drawFieldLabel(
                        guiGraphics,
                        entries.get(entryIndex).translationName(),
                        contentLeft,
                        CONTENT_TOP + entryIndex * ROW_SPACING,
                        labelWidth
                );
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawFieldLabel(
            GuiGraphics guiGraphics,
            String translationName,
            int labelLeft,
            int rowTop,
            int labelWidth
    ) {
        String label = Component.translatable(
                "screen.ninecun_lingtai.config.field." + translationName
        ).getString();
        String visibleLabel = this.font.plainSubstrByWidth(label, labelWidth);
        guiGraphics.drawString(this.font, visibleLabel, labelLeft, rowTop + 6, LABEL_COLOR, false);
    }

    private void selectCategory(ConfigCategory category) {
        this.selectedCategory = category;
        clearValidationMessage();
        this.rebuildWidgets();
    }

    private void resetSelectedCategory() {
        if (this.selectedCategory == ConfigCategory.AI) {
            this.aiProviderDraft = CommonConfig.AI_PROVIDER.getDefault();
            this.aiApiKeyDraft = CommonConfig.AI_API_KEY.getDefault();
            this.aiModelDraft = CommonConfig.AI_MODEL.getDefault();
        } else {
            for (DoubleConfigEntry entry : this.entriesByCategory.get(this.selectedCategory)) {
                this.doubleDraftValues.put(
                        entry.configValue(),
                        ConfigScreenUtility.formatDouble(entry.configValue().getDefault())
                );
            }
        }
        clearValidationMessage();
        this.rebuildWidgets();
    }

    private void saveConfiguration() {
        Map<ForgeConfigSpec.DoubleValue, Double> parsedValues = new LinkedHashMap<>();
        for (Map.Entry<ConfigCategory, List<DoubleConfigEntry>> categoryEntry : this.entriesByCategory.entrySet()) {
            for (DoubleConfigEntry entry : categoryEntry.getValue()) {
                String draftValue = this.doubleDraftValues.get(entry.configValue());
                OptionalDouble parsedValue = ConfigScreenUtility.parseDoubleInRange(
                        draftValue,
                        entry.minimum(),
                        entry.maximum()
                );
                if (parsedValue.isEmpty()) {
                    this.selectedCategory = categoryEntry.getKey();
                    this.validationMessage = Component.translatable(
                            "screen.ninecun_lingtai.config.error.invalid_number",
                            Component.translatable(entry.translationKey()),
                            ConfigScreenUtility.formatDouble(entry.minimum()),
                            ConfigScreenUtility.formatDouble(entry.maximum())
                    );
                    this.rebuildWidgets();
                    return;
                }
                parsedValues.put(entry.configValue(), parsedValue.getAsDouble());
            }
        }

        try {
            parsedValues.forEach(ForgeConfigSpec.ConfigValue::set);
            CommonConfig.AI_PROVIDER.set(this.aiProviderDraft);
            CommonConfig.AI_API_KEY.set(this.aiApiKeyDraft.trim());
            CommonConfig.AI_MODEL.set(this.aiModelDraft.trim());
            CommonConfig.SPEC.save();
            closeWithoutSaving();
        } catch (RuntimeException exception) {
            this.validationMessage = Component.translatable("screen.ninecun_lingtai.config.error.save_failed");
        }
    }

    private void updateDoubleBoxColor(EditBox editBox, DoubleConfigEntry entry, String value) {
        editBox.setTextColor(ConfigScreenUtility.parseDoubleInRange(
                value,
                entry.minimum(),
                entry.maximum()
        ).isPresent() ? VALID_INPUT_COLOR : INVALID_INPUT_COLOR);
    }

    private void clearValidationMessage() {
        this.validationMessage = Component.empty();
    }

    private void closeWithoutSaving() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public void onClose() {
        closeWithoutSaving();
    }

    private enum ConfigCategory {
        ELEMENT_DAMAGE("element_damage"),
        MEDITATION("meditation"),
        AI("ai");

        private final String translationName;

        ConfigCategory(String translationName) {
            this.translationName = translationName;
        }

        private String translationKey() {
            return "screen.ninecun_lingtai.config.category." + this.translationName;
        }

        private String descriptionTranslationKey() {
            return translationKey() + ".description";
        }
    }

    private record DoubleConfigEntry(
            String translationName,
            ForgeConfigSpec.DoubleValue configValue,
            double minimum,
            double maximum
    ) {
        private String translationKey() {
            return "screen.ninecun_lingtai.config.field." + this.translationName;
        }

        private String tooltipTranslationKey() {
            return translationKey() + ".tooltip";
        }
    }
}
