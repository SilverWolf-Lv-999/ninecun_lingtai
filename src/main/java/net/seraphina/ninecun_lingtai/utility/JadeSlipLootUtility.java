package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.common.config.CommonConfig;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class JadeSlipLootUtility {
    public static boolean tryAddRandomJadeSlip(Container container, RandomSource random) {
        if (random.nextDouble() >= CommonConfig.JADE_SLIP_LOOT_CHEST_CHANCE.get()) {
            return false;
        }

        ItemStack jadeSlip = createRandomJadeSlip(random);
        return addToRandomEmptySlot(container, jadeSlip, random);
    }

    private static ItemStack createRandomJadeSlip(RandomSource random) {
        List<TechniqueDefinition> techniques = TechniqueManager.INSTANCE.getTechniques();
        List<AlchemyRecipeDefinition> recipes = AlchemyRecipeManager.INSTANCE.getRecipes();
        int selectedItemIndex = random.nextInt(2 + techniques.size() + recipes.size());

        if (selectedItemIndex == 0) {
            return new ItemStack(ModItems.JADE_SLIP.get());
        }
        if (selectedItemIndex == 1) {
            return new ItemStack(ModItems.ALCHEMY_RECIPE.get());
        }
        selectedItemIndex -= 2;
        if (selectedItemIndex < techniques.size()) {
            return TechniqueItemUtility.createStack(techniques.get(selectedItemIndex));
        }
        return AlchemyRecipeItemUtility.createStack(
                recipes.get(selectedItemIndex - techniques.size())
        );
    }

    private static boolean addToRandomEmptySlot(
            Container container,
            ItemStack jadeSlip,
            RandomSource random
    ) {
        int containerSize = container.getContainerSize();
        if (containerSize == 0) {
            return false;
        }

        int startingSlot = random.nextInt(containerSize);
        for (int slotOffset = 0; slotOffset < containerSize; slotOffset++) {
            int slot = (startingSlot + slotOffset) % containerSize;
            if (container.getItem(slot).isEmpty()) {
                container.setItem(slot, jadeSlip);
                return true;
            }
        }
        return false;
    }
}
