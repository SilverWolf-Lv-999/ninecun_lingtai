package com.seraphina.ninecun_lingtai.utility;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.seraphina.ninecun_lingtai.api.aptitude.SoulStrength;
import com.seraphina.ninecun_lingtai.api.aptitude.SpiritualRoot;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationStage;
import com.seraphina.ninecun_lingtai.api.element.ElementType;
import com.seraphina.ninecun_lingtai.common.network.ModNetwork;
import com.seraphina.ninecun_lingtai.common.player.PlayerDefenseResourceData;
import com.seraphina.ninecun_lingtai.common.player.PlayerSpiritualEnergyData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class DebugCommandUtility {
    private static final DynamicCommandExceptionType UNKNOWN_ELEMENT = new DynamicCommandExceptionType(
            value -> Component.literal("未知元素：" + value)
    );
    private static final DynamicCommandExceptionType UNKNOWN_REALM = new DynamicCommandExceptionType(
            value -> Component.literal("未知境界：" + value)
    );
    private static final DynamicCommandExceptionType INVALID_ROOT_ELEMENTS = new DynamicCommandExceptionType(
            value -> Component.literal("无效灵根元素：" + value)
    );
    private static final Map<String, ElementType> ELEMENT_ALIASES = createElementAliases();
    private static final Map<String, CultivationRealm> REALM_ALIASES = createRealmAliases();
    private static final List<String> ROOT_ELEMENT_SUGGESTIONS = List.of(
            "metal", "wood", "water", "fire", "earth", "yin_yang", "five", "all", "chaos",
            "金", "木", "水", "火", "土", "阴阳", "五行", "混沌"
    );

    private DebugCommandUtility() {
    }

    public static ElementType getElement(
            CommandContext<CommandSourceStack> context,
            String argumentName
    ) throws CommandSyntaxException {
        String elementName = context.getArgument(argumentName, String.class);
        ElementType element = ELEMENT_ALIASES.get(normalize(elementName));
        if (element == null) {
            throw UNKNOWN_ELEMENT.create(elementName);
        }
        return element;
    }

    public static CultivationRealm getRealm(
            CommandContext<CommandSourceStack> context,
            String argumentName
    ) throws CommandSyntaxException {
        String realmName = context.getArgument(argumentName, String.class);
        CultivationRealm realm = REALM_ALIASES.get(normalize(realmName));
        if (realm == null) {
            throw UNKNOWN_REALM.create(realmName);
        }
        return realm;
    }

    public static Set<ElementType> getSpiritualRootElements(
            CommandContext<CommandSourceStack> context,
            String argumentName
    ) throws CommandSyntaxException {
        String rootElementsValue = context.getArgument(argumentName, String.class);
        String normalizedValue = normalize(rootElementsValue);
        if (normalizedValue.equals("all")
                || normalizedValue.equals("chaos")
                || normalizedValue.equals("混沌")) {
            return SpiritualRoot.SELECTABLE_ELEMENTS;
        }
        if (normalizedValue.equals("five") || normalizedValue.equals("五行")) {
            return Set.copyOf(ElementType.FIVE_ELEMENTS);
        }

        EnumSet<ElementType> elements = EnumSet.noneOf(ElementType.class);
        String[] elementNames = normalizedValue.split("[,+，、/\\s]+");
        for (String elementName : elementNames) {
            ElementType element = ELEMENT_ALIASES.get(elementName);
            if (element == null || !SpiritualRoot.SELECTABLE_ELEMENTS.contains(element)) {
                throw INVALID_ROOT_ELEMENTS.create(rootElementsValue);
            }
            elements.add(element);
        }
        if (elements.isEmpty()) {
            throw INVALID_ROOT_ELEMENTS.create(rootElementsValue);
        }
        return Set.copyOf(elements);
    }

    public static CompletableFuture<Suggestions> suggestElements(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
                ELEMENT_ALIASES.keySet().stream().map(StringArgumentType::escapeIfRequired),
                builder
        );
    }

    public static CompletableFuture<Suggestions> suggestRootElements(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
                ROOT_ELEMENT_SUGGESTIONS.stream().map(StringArgumentType::escapeIfRequired),
                builder
        );
    }

    public static CompletableFuture<Suggestions> suggestRealms(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(
                REALM_ALIASES.keySet().stream().map(StringArgumentType::escapeIfRequired),
                builder
        );
    }

    public static void syncCultivationInfo(Collection<ServerPlayer> players) {
        players.forEach(player -> {
            PlayerDefenseResourceData.refillToMaximum(player);
            ModNetwork.sendCultivationInfo(player);
            ModNetwork.sendDefenseResource(player);
        });
    }

    public static String getPlayerNames(Collection<ServerPlayer> players) {
        return players.stream()
                .map(player -> player.getGameProfile().getName())
                .collect(Collectors.joining("、"));
    }

    public static String formatSpiritualRoot(SpiritualRoot spiritualRoot) {
        return SpiritualRootUtility.getSpiritualRootName(spiritualRoot.elements())
                + "（" + formatNumber(spiritualRoot.lengthInches()) + "寸，元素="
                + Arrays.stream(ElementType.values())
                .filter(spiritualRoot.elements()::contains)
                .map(ElementType::getSerializedName)
                .collect(Collectors.joining("+"))
                + "）";
    }

    public static String formatSoulStrength(SoulStrength soulStrength) {
        return soulStrength.illuminatedStars() + "星";
    }

    public static String formatCultivationStage(CultivationStage stage) {
        return CultivationUtility.getDisplayName(stage)
                + "（" + stage.realm().getSerializedName() + " " + stage.layer() + "）";
    }

    public static String formatSpiritualEnergy(ServerPlayer player) {
        List<String> values = new ArrayList<>();
        for (ElementType element : ElementType.values()) {
            values.add(
                    SpiritualRootUtility.getElementName(element)
                            + "=" + formatNumber(PlayerSpiritualEnergyData.getEnergy(player, element))
            );
        }
        return String.join("，", values);
    }

    public static String formatNumber(double value) {
        if (!Double.isFinite(value)) {
            return String.valueOf(value);
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toEngineeringString();
    }

    private static Map<String, ElementType> createElementAliases() {
        Map<String, ElementType> aliases = new LinkedHashMap<>();
        Arrays.stream(ElementType.values()).forEach(element -> aliases.put(element.getSerializedName(), element));
        aliases.put("金", ElementType.METAL);
        aliases.put("木", ElementType.WOOD);
        aliases.put("水", ElementType.WATER);
        aliases.put("火", ElementType.FIRE);
        aliases.put("土", ElementType.EARTH);
        aliases.put("阴阳", ElementType.YIN_YANG);
        aliases.put("陰陽", ElementType.YIN_YANG);
        aliases.put("雷", ElementType.THUNDER);
        aliases.put("混沌", ElementType.CHAOS);
        return Map.copyOf(aliases);
    }

    private static Map<String, CultivationRealm> createRealmAliases() {
        Map<String, CultivationRealm> aliases = new LinkedHashMap<>();
        Arrays.stream(CultivationRealm.values()).forEach(realm -> {
            aliases.put(realm.getSerializedName(), realm);
            aliases.put(realm.getChineseName(), realm);
        });
        return Map.copyOf(aliases);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
