package com.seraphina.ninecun_lingtai.utility;

import com.seraphina.ninecun_lingtai.ModMain;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueClientData;
import com.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import com.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class TechniqueItemUtility {
    private static final String TECHNIQUE_ID_KEY = ModMain.MOD_ID + ":technique_id";
    private static final String TECHNIQUE_NAME_KEY = ModMain.MOD_ID + ":technique_name";
    private static final String TECHNIQUE_DESCRIPTION_KEY = ModMain.MOD_ID + ":technique_description";

    private TechniqueItemUtility() {
    }

    public static ItemStack createStack(TechniqueDefinition technique) {
        return createStack(
                technique.id(),
                technique.name(),
                technique.description()
        );
    }

    public static ItemStack createStack(TechniqueClientData technique) {
        return createStack(
                technique.id(),
                technique.name(),
                technique.description()
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
                technique.description()
        );
    }

    private static ItemStack createStack(
            ResourceLocation techniqueId,
            String name,
            String description
    ) {
        ItemStack itemStack = new ItemStack(ModItems.JADE_SLIP.get());
        writeTechniqueData(itemStack, techniqueId, name, description);
        return itemStack;
    }

    private static void writeTechniqueData(
            ItemStack itemStack,
            ResourceLocation techniqueId,
            String name,
            String description
    ) {
        CompoundTag itemData = itemStack.getOrCreateTag();
        itemData.putString(TECHNIQUE_ID_KEY, techniqueId.toString());
        itemData.putString(TECHNIQUE_NAME_KEY, name);
        itemData.putString(TECHNIQUE_DESCRIPTION_KEY, description);
    }

    private static Optional<String> getText(ItemStack itemStack, String key) {
        if (!itemStack.is(ModItems.JADE_SLIP.get()) || !itemStack.hasTag()) {
            return Optional.empty();
        }
        String value = itemStack.getTag().getString(key);
        return value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
