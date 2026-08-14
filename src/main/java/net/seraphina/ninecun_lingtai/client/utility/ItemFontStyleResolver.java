package net.seraphina.ninecun_lingtai.client.utility;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.api.item.SpiritualItemGrade;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.seraphina.ninecun_lingtai.client.ItemFontTheme;
import net.seraphina.ninecun_lingtai.client.player.ClientAlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.client.player.ClientTechniqueManager;
import net.seraphina.ninecun_lingtai.common.item.ItemFontStyle;
import net.seraphina.ninecun_lingtai.common.item.SpiritualStoneGrade;
import net.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import net.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualItemUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import net.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.Set;

public final class ItemFontStyleResolver {
    private static final Set<ElementType> FIVE_ELEMENTS = Set.copyOf(ElementType.FIVE_ELEMENTS);

    private ItemFontStyleResolver() {
    }

    public static ItemFontTheme resolveTechnique(ItemStack itemStack) {
        Set<ElementType> compatibleElements = TechniqueItemUtility.getCompatibleElements(itemStack);
        if (compatibleElements.isEmpty()) {
            compatibleElements = TechniqueItemUtility.getTechniqueId(itemStack)
                    .flatMap(ClientTechniqueManager::getTechnique)
                    .map(TechniqueClientData::compatibleElements)
                    .orElse(Set.of());
        }
        return ItemFontTheme.of(resolveElementStyle(compatibleElements, ItemFontStyle.JADE_SLIP));
    }

    public static ItemFontTheme resolveAlchemyRecipe(ItemStack itemStack) {
        Optional<AlchemyRecipeClientData> recipeData = AlchemyRecipeItemUtility.getRecipeId(itemStack)
                .flatMap(ClientAlchemyRecipeManager::getRecipe);
        Optional<ElementType> primaryElement = AlchemyRecipeItemUtility.getPrimaryElement(itemStack);
        if (primaryElement.isEmpty()) {
            primaryElement = recipeData.flatMap(AlchemyRecipeClientData::getPrimaryElement);
        }
        AlchemyGrade grade = AlchemyRecipeItemUtility.hasStoredGrade(itemStack)
                ? AlchemyRecipeItemUtility.getGrade(itemStack)
                : recipeData.map(AlchemyRecipeClientData::grade).orElse(AlchemyGrade.LOWEST);
        return resolveAlchemyTheme(primaryElement, grade);
    }

    public static ItemFontTheme resolvePill(ItemStack itemStack) {
        Optional<ElementType> primaryElement = AlchemyPillUtility.getPrimaryElement(itemStack);
        if (primaryElement.isEmpty()) {
            ResourceLocation outputItem = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            primaryElement = ClientAlchemyRecipeManager
                    .getRecipeForOutputItem(outputItem)
                    .flatMap(AlchemyRecipeClientData::getPrimaryElement);
        }
        return resolveAlchemyTheme(primaryElement, AlchemyPillUtility.getGrade(itemStack));
    }

    public static ItemFontTheme resolveSpiritualStone(ItemStack itemStack) {
        SpiritualStoneGrade grade = SpiritualStoneUtility.getGrade(itemStack);
        return ItemFontTheme.of(switch (grade) {
            case HUMAN -> ItemFontStyle.HUMAN_SPIRITUAL_STONE;
            case EARTH -> ItemFontStyle.EARTH_SPIRITUAL_STONE;
            case HEAVEN -> ItemFontStyle.HEAVEN_SPIRITUAL_STONE;
        });
    }

    public static ItemFontTheme resolveSpiritualItem(ItemStack itemStack) {
        SpiritualItemGrade grade = SpiritualItemUtility.getGrade(itemStack);
        return ItemFontTheme.of(switch (grade) {
            case MORTAL -> ItemFontStyle.MORTAL_HAN_SWORD;
            case SPIRITUAL -> ItemFontStyle.SPIRITUAL_HAN_SWORD;
            case IMMORTAL -> ItemFontStyle.IMMORTAL_HAN_SWORD;
        });
    }

    private static ItemFontTheme resolveAlchemyTheme(
            Optional<ElementType> primaryElement,
            AlchemyGrade grade
    ) {
        ItemFontStyle tierStyle = ItemFontStyle.getAlchemyTierStyle(grade.tier());
        ItemFontStyle primaryStyle = primaryElement
                .map(element -> resolveElementStyle(Set.of(element), tierStyle))
                .orElse(tierStyle);
        return ItemFontTheme.withGrade(primaryStyle, tierStyle, grade.rank());
    }

    private static ItemFontStyle resolveElementStyle(
            Set<ElementType> elements,
            ItemFontStyle fallbackStyle
    ) {
        if (elements.contains(ElementType.CHAOS)) {
            return ItemFontStyle.CHAOS;
        }
        if (elements.equals(FIVE_ELEMENTS)) {
            return ItemFontStyle.FIVE_ELEMENTS;
        }
        if (elements.size() == 1) {
            return elements.stream()
                    .findFirst()
                    .map(ItemFontStyleResolver::getElementStyle)
                    .orElse(fallbackStyle);
        }
        return elements.isEmpty() ? fallbackStyle : ItemFontStyle.MULTI_ELEMENT;
    }

    private static ItemFontStyle getElementStyle(ElementType element) {
        return switch (element) {
            case METAL -> ItemFontStyle.METAL;
            case WOOD -> ItemFontStyle.WOOD;
            case WATER -> ItemFontStyle.WATER;
            case FIRE -> ItemFontStyle.FIRE;
            case EARTH -> ItemFontStyle.EARTH;
            case YIN_YANG -> ItemFontStyle.YIN_YANG;
            case THUNDER -> ItemFontStyle.THUNDER;
            case CHAOS -> ItemFontStyle.CHAOS;
        };
    }
}
