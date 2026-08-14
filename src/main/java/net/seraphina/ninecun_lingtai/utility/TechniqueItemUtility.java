package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class TechniqueItemUtility {
    private static final String TECHNIQUE_ID_KEY = ModMain.MOD_ID + ":technique_id";
    private static final String TECHNIQUE_NAME_KEY = ModMain.MOD_ID + ":technique_name";
    private static final String TECHNIQUE_DESCRIPTION_KEY = ModMain.MOD_ID + ":technique_description";
    private static final String TECHNIQUE_COMPATIBLE_ELEMENTS_KEY =
            ModMain.MOD_ID + ":technique_compatible_elements";

    public static ItemStack createStack(TechniqueDefinition technique) {
        return createStack(
                technique.id(),
                technique.name(),
                technique.description(),
                technique.compatibleElements()
        );
    }

    public static ItemStack createStack(TechniqueClientData technique) {
        return createStack(
                technique.id(),
                technique.name(),
                technique.description(),
                technique.compatibleElements()
        );
    }

    public static Optional<ResourceLocation> getTechniqueId(ItemStack itemStack) {
        if (!itemStack.is(ModItems.JADE_SLIP.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ResourceLocation.tryParse(
                itemStack.getTag().getString(TECHNIQUE_ID_KEY)
        ));
    }

    public static Optional<String> getTechniqueName(ItemStack itemStack) {
        return getText(itemStack, TECHNIQUE_NAME_KEY);
    }

    public static Optional<String> getTechniqueDescription(ItemStack itemStack) {
        return getText(itemStack, TECHNIQUE_DESCRIPTION_KEY);
    }

    public static Set<ElementType> getCompatibleElements(ItemStack itemStack) {
        if (!itemStack.is(ModItems.JADE_SLIP.get()) || !itemStack.hasTag()) {
            return Set.of();
        }
        ListTag elementNames = itemStack.getTag().getList(TECHNIQUE_COMPATIBLE_ELEMENTS_KEY, 8);
        Set<ElementType> compatibleElements = EnumSet.noneOf(ElementType.class);
        for (int elementIndex = 0; elementIndex < elementNames.size(); elementIndex++) {
            ElementType.fromSerializedName(elementNames.getString(elementIndex))
                    .ifPresent(compatibleElements::add);
        }
        return Set.copyOf(compatibleElements);
    }

    public static boolean isTechnique(ItemStack itemStack, ResourceLocation techniqueId) {
        return getTechniqueId(itemStack).filter(techniqueId::equals).isPresent();
    }

    public static boolean isEmptyJadeSlip(ItemStack itemStack) {
        return itemStack.is(ModItems.JADE_SLIP.get()) && getTechniqueId(itemStack).isEmpty();
    }

    public static void imprintTechnique(ItemStack itemStack, TechniqueDefinition technique) {
        if (!itemStack.is(ModItems.JADE_SLIP.get())) {
            throw new IllegalArgumentException("Only jade slips can carry techniques");
        }
        writeTechniqueData(
                itemStack,
                technique.id(),
                technique.name(),
                technique.description(),
                technique.compatibleElements()
        );
    }

    private static ItemStack createStack(
            ResourceLocation techniqueId,
            String name,
            String description,
            Set<ElementType> compatibleElements
    ) {
        ItemStack itemStack = new ItemStack(ModItems.JADE_SLIP.get());
        writeTechniqueData(itemStack, techniqueId, name, description, compatibleElements);
        return itemStack;
    }

    private static void writeTechniqueData(
            ItemStack itemStack,
            ResourceLocation techniqueId,
            String name,
            String description,
            Set<ElementType> compatibleElements
    ) {
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putString(TECHNIQUE_ID_KEY, techniqueId.toString());
        itemData.putString(TECHNIQUE_NAME_KEY, name);
        itemData.putString(TECHNIQUE_DESCRIPTION_KEY, description);
        ListTag elementNames = new ListTag();
        compatibleElements.stream()
                .sorted()
                .forEach(element -> elementNames.add(
                        net.minecraft.nbt.StringTag.valueOf(element.getSerializedName())
                ));
        itemData.put(TECHNIQUE_COMPATIBLE_ELEMENTS_KEY, elementNames);
    }

    private static Optional<String> getText(ItemStack itemStack, String key) {
        if (!itemStack.is(ModItems.JADE_SLIP.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        String value = itemStack.getTag().getString(key);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
