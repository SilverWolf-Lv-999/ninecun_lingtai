package net.seraphina.ninecun_lingtai.client.screen;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.common.ForgeConfigSpec;
import net.seraphina.ninecun_lingtai.api.ai.AiProvider;
import net.seraphina.ninecun_lingtai.client.utility.ConfigScreenUtility;
import net.seraphina.ninecun_lingtai.common.config.ClientConfig;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** A scrollable editor for every value exposed by the mod's Forge config specs. */
public final class CommonConfigScreen extends Screen {
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFE0E0E0;
    private static final int SECONDARY_TEXT_COLOR = 0xFFA0A0A0;
    private static final int ERROR_TEXT_COLOR = 0xFFFF5555;
    private static final int VALID_INPUT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_INPUT_COLOR = 0xFFFF7777;
    private static final int CATEGORY_BUTTON_HEIGHT = 20;
    private static final int INPUT_HEIGHT = 20;
    private static final int ROW_SPACING = 25;
    private static final int CONTENT_TOP = 42;
    private static final int FOOTER_HEIGHT = 28;
    private static final int SIDEBAR_WIDTH = 142;

    private final Screen parentScreen;
    private final List<ConfigEntry> entries = new ArrayList<>();
    private final Map<String, List<ConfigEntry>> entriesByCategory = new LinkedHashMap<>();
    private final Map<ForgeConfigSpec.ConfigValue<?>, Object> draftValues = new LinkedHashMap<>();
    private final List<EditBox> visibleEditBoxes = new ArrayList<>();
    private String selectedCategory;
    private int scrollOffset;
    private int categoryOffset;
    private Component validationMessage = Component.empty();

    public CommonConfigScreen(Screen parentScreen) {
        super(Component.translatable("screen.ninecun_lingtai.config.title"));
        this.parentScreen = parentScreen;
        collectSpec(CommonConfig.SPEC, false);
        collectSpec(ClientConfig.SPEC, true);
        if (!this.entriesByCategory.isEmpty()) {
            this.selectedCategory = this.entriesByCategory.keySet().iterator().next();
        }
    }

    @Override
    protected void init() {
        this.visibleEditBoxes.clear();
        if (this.selectedCategory == null) {
            return;
        }

        int contentLeft = 12;
        int contentWidth = Math.max(180, this.width - 24);
        int editorLeft = contentLeft + SIDEBAR_WIDTH + 12;
        int editorWidth = Math.max(120, contentWidth - SIDEBAR_WIDTH - 12);
        int fieldWidth = Math.min(190, Math.max(110, editorWidth / 2));
        int fieldLeft = editorLeft + editorWidth - fieldWidth;

        addCategoryWidgets(contentLeft);
        List<ConfigEntry> selectedEntries = this.entriesByCategory.getOrDefault(
                this.selectedCategory,
                List.of()
        );
        int visibleRows = getVisibleRows();
        this.scrollOffset = clampScroll(this.scrollOffset, selectedEntries.size(), visibleRows);
        int firstEntry = this.scrollOffset;
        int lastEntry = Math.min(selectedEntries.size(), firstEntry + visibleRows);
        for (int index = firstEntry; index < lastEntry; index++) {
            addEntryWidget(selectedEntries.get(index), editorLeft, fieldLeft, fieldWidth, index - firstEntry);
        }

        int footerWidth = Math.min(360, this.width - 24);
        int footerLeft = (this.width - footerWidth) / 2;
        int buttonWidth = (footerWidth - 8) / 3;
        int footerTop = this.height - FOOTER_HEIGHT;
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

    private void collectSpec(ForgeConfigSpec spec, boolean clientSpec) {
        collectValues(spec, spec.getValues(), List.of(), clientSpec);
    }

    private void collectValues(
            ForgeConfigSpec spec,
            UnmodifiableConfig config,
            List<String> parentPath,
            boolean clientSpec
    ) {
        for (Map.Entry<String, Object> mapEntry : config.valueMap().entrySet()) {
            List<String> path = new ArrayList<>(parentPath);
            path.add(mapEntry.getKey());
            Object rawValue = mapEntry.getValue();
            if (rawValue instanceof UnmodifiableConfig childConfig) {
                collectValues(spec, childConfig, path, clientSpec);
                continue;
            }
            if (!(rawValue instanceof ForgeConfigSpec.ConfigValue<?> configValue)) {
                continue;
            }
            ConfigEntry entry = createEntry(spec, configValue, path, clientSpec);
            this.entries.add(entry);
            this.entriesByCategory.computeIfAbsent(entry.categoryId(), ignored -> new ArrayList<>()).add(entry);
            this.draftValues.put(configValue, initialDraftValue(configValue));
        }
    }

    private ConfigEntry createEntry(
            ForgeConfigSpec spec,
            ForgeConfigSpec.ConfigValue<?> configValue,
            List<String> path,
            boolean clientSpec
    ) {
        Object valueSpec = spec.getSpec().get(path);
        ForgeConfigSpec.Range<?> range = valueSpec instanceof ForgeConfigSpec.ValueSpec specValue
                ? specValue.getRange()
                : null;
        ValueType type = configValue instanceof ForgeConfigSpec.BooleanValue
                ? ValueType.BOOLEAN
                : configValue instanceof ForgeConfigSpec.EnumValue<?>
                        ? ValueType.ENUM
                        : configValue instanceof ForgeConfigSpec.DoubleValue
                                ? ValueType.DOUBLE
                                : configValue instanceof ForgeConfigSpec.IntValue
                                        ? ValueType.INTEGER
                                        : configValue instanceof ForgeConfigSpec.LongValue
                                                ? ValueType.LONG
                                                : ValueType.STRING;
        String categoryPart = path.isEmpty() ? "general" : path.get(0);
        String categoryId = (clientSpec ? "client." : "common.") + categoryPart;
        return new ConfigEntry(spec, configValue, path, categoryId, type, range);
    }

    private Object initialDraftValue(ForgeConfigSpec.ConfigValue<?> configValue) {
        return configValue.get();
    }

    private void addCategoryWidgets(int sidebarLeft) {
        List<String> categories = new ArrayList<>(this.entriesByCategory.keySet());
        int visibleCategories = getVisibleCategoryCount();
        this.categoryOffset = clampCategoryScroll(this.categoryOffset, categories.size(), visibleCategories);
        int lastCategory = Math.min(categories.size(), this.categoryOffset + visibleCategories);
        for (int categoryIndex = this.categoryOffset; categoryIndex < lastCategory; categoryIndex++) {
            String categoryId = categories.get(categoryIndex);
            int categoryTop = 30 + (categoryIndex - this.categoryOffset) * 22;
            Button categoryButton = Button.builder(
                            categoryLabel(categoryId),
                            button -> selectCategory(categoryId)
                    )
                    .bounds(sidebarLeft, categoryTop, SIDEBAR_WIDTH, CATEGORY_BUTTON_HEIGHT)
                    .build();
            categoryButton.active = !categoryId.equals(this.selectedCategory);
            this.addRenderableWidget(categoryButton);
        }
    }

    private void addEntryWidget(
            ConfigEntry entry,
            int editorLeft,
            int fieldLeft,
            int fieldWidth,
            int visibleIndex
    ) {
        int rowTop = CONTENT_TOP + visibleIndex * ROW_SPACING;
        Component label = fieldLabel(entry);
        Tooltip tooltip = Tooltip.create(tooltipText(entry));
        if (entry.type() == ValueType.BOOLEAN) {
            boolean initialValue = (Boolean) this.draftValues.get(entry.configValue());
            CycleButton<Boolean> button = CycleButton.onOffBuilder()
                    .withInitialValue(initialValue)
                    .withTooltip(ignored -> tooltip)
                    .create(fieldLeft, rowTop, fieldWidth, INPUT_HEIGHT, label, (ignored, value) -> {
                        this.draftValues.put(entry.configValue(), value);
                        clearValidationMessage();
                    });
            this.addRenderableWidget(button);
            return;
        }
        if (entry.type() == ValueType.ENUM) {
            addEnumWidget(entry, label, tooltip, fieldLeft, rowTop, fieldWidth);
            return;
        }

        EditBox editBox = new EditBox(this.font, fieldLeft, rowTop, fieldWidth, INPUT_HEIGHT, label);
        editBox.setMaxLength(512);
        if (entry.type().isNumeric()) {
            editBox.setFilter(entry.type() == ValueType.INTEGER || entry.type() == ValueType.LONG
                    ? value -> value != null && value.matches("-?\\d*")
                    : ConfigScreenUtility::isPotentialDecimal);
        }
        editBox.setTooltip(tooltip);
        editBox.setResponder(value -> {
            this.draftValues.put(entry.configValue(), value);
            updateInputColor(editBox, entry, value);
            clearValidationMessage();
        });
        if (entry.isSecret()) {
            editBox.setFormatter((value, characterIndex) -> FormattedCharSequence.forward(
                    editBox.isFocused() ? value : "*".repeat(value.length()),
                    Style.EMPTY
            ));
        }
        editBox.setValue(String.valueOf(this.draftValues.get(entry.configValue())));
        updateInputColor(editBox, entry, editBox.getValue());
        this.visibleEditBoxes.add(editBox);
        this.addRenderableWidget(editBox);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void addEnumWidget(
            ConfigEntry entry,
            Component label,
            Tooltip tooltip,
            int fieldLeft,
            int rowTop,
            int fieldWidth
    ) {
        Enum<?> currentValue = (Enum<?>) this.draftValues.get(entry.configValue());
        Class enumClass = currentValue.getDeclaringClass();
        CycleButton.Builder builder = CycleButton.builder(value -> enumLabel((Enum<?>) value))
                .withValues((Object[]) enumClass.getEnumConstants())
                .withInitialValue(currentValue)
                .withTooltip(ignored -> tooltip)
                .displayOnlyValue();
        CycleButton button = builder.create(fieldLeft, rowTop, fieldWidth, INPUT_HEIGHT, label, (ignored, value) -> {
            this.draftValues.put(entry.configValue(), value);
            clearValidationMessage();
        });
        this.addRenderableWidget(button);
    }

    @Override
    public void tick() {
        for (EditBox editBox : this.visibleEditBoxes) {
            editBox.tick();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX <= 12 + SIDEBAR_WIDTH) {
            List<String> categories = new ArrayList<>(this.entriesByCategory.keySet());
            int oldCategoryOffset = this.categoryOffset;
            this.categoryOffset = clampCategoryScroll(
                    this.categoryOffset - (int) Math.signum(delta),
                    categories.size(),
                    getVisibleCategoryCount()
            );
            if (oldCategoryOffset != this.categoryOffset) {
                this.rebuildWidgets();
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        int visibleRows = getVisibleRows();
        List<ConfigEntry> selectedEntries = this.entriesByCategory.getOrDefault(this.selectedCategory, List.of());
        int oldOffset = this.scrollOffset;
        this.scrollOffset = clampScroll(this.scrollOffset - (int) Math.signum(delta), selectedEntries.size(), visibleRows);
        if (oldOffset != this.scrollOffset) {
            this.rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, TITLE_COLOR);
        Component secondaryMessage = this.validationMessage.getString().isEmpty()
                ? categoryDescription(this.selectedCategory)
                : this.validationMessage;
        int secondaryColor = this.validationMessage.getString().isEmpty()
                ? SECONDARY_TEXT_COLOR
                : ERROR_TEXT_COLOR;
        guiGraphics.drawCenteredString(this.font, secondaryMessage, this.width / 2, 22, secondaryColor);

        int contentLeft = 12;
        int contentWidth = Math.max(180, this.width - 24);
        int editorLeft = contentLeft + SIDEBAR_WIDTH + 12;
        int editorWidth = Math.max(120, contentWidth - SIDEBAR_WIDTH - 12);
        int fieldWidth = Math.min(190, Math.max(110, editorWidth / 2));
        int labelWidth = Math.max(80, editorWidth - fieldWidth - 8);
        List<ConfigEntry> selectedEntries = this.entriesByCategory.getOrDefault(this.selectedCategory, List.of());
        int visibleRows = getVisibleRows();
        for (int index = this.scrollOffset;
             index < Math.min(selectedEntries.size(), this.scrollOffset + visibleRows);
             index++) {
            int rowTop = CONTENT_TOP + (index - this.scrollOffset) * ROW_SPACING;
            Component label = fieldLabel(selectedEntries.get(index));
            List<FormattedCharSequence> lines = this.font.split(FormattedText.of(label.getString()), labelWidth);
            if (lines.isEmpty()) {
                guiGraphics.drawString(this.font, label, editorLeft, rowTop + 6, LABEL_COLOR, false);
            } else {
                guiGraphics.drawString(this.font, lines.get(0), editorLeft, rowTop + 6, LABEL_COLOR, false);
            }
        }
        if (selectedEntries.size() > visibleRows) {
            int trackTop = CONTENT_TOP;
            int trackHeight = visibleRows * ROW_SPACING - 5;
            int thumbHeight = Math.max(12, trackHeight * visibleRows / selectedEntries.size());
            int thumbTop = trackTop + (trackHeight - thumbHeight) * this.scrollOffset
                    / Math.max(1, selectedEntries.size() - visibleRows);
            guiGraphics.fill(editorLeft + editorWidth + 3, trackTop, editorLeft + editorWidth + 5,
                    trackTop + trackHeight, 0x55333333);
            guiGraphics.fill(editorLeft + editorWidth + 3, thumbTop, editorLeft + editorWidth + 5,
                    thumbTop + thumbHeight, 0xFFAAAAAA);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void selectCategory(String categoryId) {
        this.selectedCategory = categoryId;
        this.scrollOffset = 0;
        clearValidationMessage();
        this.rebuildWidgets();
    }

    private void resetSelectedCategory() {
        for (ConfigEntry entry : this.entriesByCategory.getOrDefault(this.selectedCategory, List.of())) {
            this.draftValues.put(entry.configValue(), entry.configValue().getDefault());
        }
        this.scrollOffset = 0;
        clearValidationMessage();
        this.rebuildWidgets();
    }

    private void saveConfiguration() {
        Map<ForgeConfigSpec.ConfigValue<?>, Object> parsedValues = new LinkedHashMap<>();
        for (ConfigEntry entry : this.entries) {
            Object draft = this.draftValues.get(entry.configValue());
            try {
                parsedValues.put(entry.configValue(), parseValue(entry, draft));
            } catch (IllegalArgumentException exception) {
                this.selectedCategory = entry.categoryId();
                List<ConfigEntry> categoryEntries = this.entriesByCategory.get(entry.categoryId());
                this.scrollOffset = Math.max(0, categoryEntries.indexOf(entry));
                this.categoryOffset = categoryIndex(entry.categoryId());
                this.validationMessage = Component.translatable(
                        "screen.ninecun_lingtai.config.error.invalid_number",
                        fieldLabel(entry),
                        rangeMinimum(entry),
                        rangeMaximum(entry)
                );
                this.rebuildWidgets();
                return;
            }
        }

        try {
            for (Map.Entry<ForgeConfigSpec.ConfigValue<?>, Object> parsedEntry : parsedValues.entrySet()) {
                setConfigValue(parsedEntry.getKey(), parsedEntry.getValue());
            }
            CommonConfig.SPEC.save();
            ClientConfig.SPEC.save();
            closeWithoutSaving();
        } catch (RuntimeException exception) {
            this.validationMessage = Component.translatable("screen.ninecun_lingtai.config.error.save_failed");
        }
    }

    private Object parseValue(ConfigEntry entry, Object draft) {
        if (!entry.type().isNumeric()) {
            return entry.type() == ValueType.STRING ? String.valueOf(draft).trim() : draft;
        }
        String text = String.valueOf(draft).trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        try {
            Number parsed = switch (entry.type()) {
                case DOUBLE -> Double.parseDouble(text);
                case INTEGER -> Integer.parseInt(text);
                case LONG -> Long.parseLong(text);
                default -> throw new IllegalArgumentException("not numeric");
            };
            if (!isInRange(entry.range(), parsed)) {
                throw new IllegalArgumentException("out of range");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("invalid number", exception);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setConfigValue(ForgeConfigSpec.ConfigValue<?> configValue, Object value) {
        ((ForgeConfigSpec.ConfigValue) configValue).set(value);
    }

    private void updateInputColor(EditBox editBox, ConfigEntry entry, String value) {
        if (!entry.type().isNumeric()) {
            editBox.setTextColor(VALID_INPUT_COLOR);
            return;
        }
        try {
            parseValue(entry, value);
            editBox.setTextColor(VALID_INPUT_COLOR);
        } catch (IllegalArgumentException exception) {
            editBox.setTextColor(INVALID_INPUT_COLOR);
        }
    }

    private static boolean isInRange(ForgeConfigSpec.Range<?> range, Number value) {
        return range == null || range.test(value);
    }

    private static int clampScroll(int offset, int itemCount, int visibleRows) {
        return Math.max(0, Math.min(offset, Math.max(0, itemCount - visibleRows)));
    }

    private static int clampCategoryScroll(int offset, int categoryCount, int visibleCategories) {
        return Math.max(0, Math.min(offset, Math.max(0, categoryCount - visibleCategories)));
    }

    private int categoryIndex(String categoryId) {
        int index = new ArrayList<>(this.entriesByCategory.keySet()).indexOf(categoryId);
        return clampCategoryScroll(index, this.entriesByCategory.size(), getVisibleCategoryCount());
    }

    private int getVisibleCategoryCount() {
        return Math.max(1, (this.height - 30 - FOOTER_HEIGHT - 8) / 22);
    }

    private int getVisibleRows() {
        return Math.max(1, (this.height - CONTENT_TOP - FOOTER_HEIGHT - 8) / ROW_SPACING);
    }

    private Component fieldLabel(ConfigEntry entry) {
        String name = entry.path().get(entry.path().size() - 1);
        String snakeName = toSnakeCase(name);
        String translatedName = translatedFieldName(entry, snakeName);
        String translationKey = "screen.ninecun_lingtai.config.field." + translatedName;
        return switch (translatedName) {
            case "generation_weakening_multiplier", "restraint_enhancement_multiplier", "suppressed_multiplier",
                    "natural_restraint_advantage", "chaos_enhancement_multiplier", "chaos_suppressed_multiplier",
                    "meditation_base_absorption_per_second", "meditation_defense_resource_repair_percent_per_second",
                    "meditation_generates_multiplier", "meditation_generated_by_multiplier",
                    "meditation_overcomes_multiplier", "meditation_overcome_by_multiplier", "ai_provider",
                    "ai_api_key", "ai_model" -> Component.translatable(translationKey);
            default -> Component.literal(humanize(name));
        };
    }

    private static String translatedFieldName(ConfigEntry entry, String snakeName) {
        if (entry.categoryId().equals("common.ai")) {
            return "ai_" + snakeName;
        }
        if (entry.categoryId().equals("common.meditation")) {
            return "meditation_" + snakeName;
        }
        return snakeName;
    }

    private Component categoryLabel(String categoryId) {
        String[] parts = categoryId.split("\\.", 2);
        String name = toSnakeCase(parts.length == 2 ? parts[1] : parts[0]);
        if (name.equals("element_damage") || name.equals("meditation") || name.equals("ai")) {
            return Component.translatable("screen.ninecun_lingtai.config.category." + name);
        }
        return Component.literal((parts[0].equals("client") ? "Client / " : "") + humanize(parts[parts.length - 1]));
    }

    private Component categoryDescription(String categoryId) {
        if (categoryId == null) {
            return Component.empty();
        }
        String[] parts = categoryId.split("\\.", 2);
        String name = toSnakeCase(parts.length == 2 ? parts[1] : parts[0]);
        if (name.equals("element_damage") || name.equals("meditation") || name.equals("ai")) {
            return Component.translatable("screen.ninecun_lingtai.config.category." + name + ".description");
        }
        return Component.literal("Edit " + humanize(parts[parts.length - 1]) + " settings");
    }

    private static Component tooltipText(ConfigEntry entry) {
        String comment = null;
        Object specValue = entry.spec().getSpec().get(entry.path());
        if (specValue instanceof ForgeConfigSpec.ValueSpec valueSpec) {
            comment = valueSpec.getComment();
        }
        String range = entry.range() == null ? "" : "\nRange: " + rangeMinimum(entry) + " - " + rangeMaximum(entry);
        return Component.literal((comment == null ? "" : comment) + range);
    }

    private static String rangeMinimum(ConfigEntry entry) {
        return entry.range() == null ? "-inf" : String.valueOf(entry.range().getMin());
    }

    private static String rangeMaximum(ConfigEntry entry) {
        return entry.range() == null ? "+inf" : String.valueOf(entry.range().getMax());
    }

    private static Component enumLabel(Enum<?> value) {
        if (value instanceof AiProvider) {
            return Component.translatable("screen.ninecun_lingtai.config.ai_provider." + value.name().toLowerCase(Locale.ROOT));
        }
        return Component.literal(humanize(value.name()));
    }

    private static String humanize(String value) {
        String text = value.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ');
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static String toSnakeCase(String value) {
        return value.replaceAll("([a-z])([A-Z])", "$1_$2").replace('-', '_').toLowerCase(Locale.ROOT);
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

    private enum ValueType {
        BOOLEAN,
        ENUM,
        DOUBLE,
        INTEGER,
        LONG,
        STRING;

        private boolean isNumeric() {
            return this == DOUBLE || this == INTEGER || this == LONG;
        }
    }

    private record ConfigEntry(
            ForgeConfigSpec spec,
            ForgeConfigSpec.ConfigValue<?> configValue,
            List<String> path,
            String categoryId,
            ValueType type,
            ForgeConfigSpec.Range<?> range
    ) {
        private boolean isSecret() {
            String name = path.get(path.size() - 1).toLowerCase(Locale.ROOT);
            return name.contains("apikey") || name.contains("password") || name.contains("token");
        }
    }
}
