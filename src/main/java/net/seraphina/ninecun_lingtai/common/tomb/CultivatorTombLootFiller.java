package net.seraphina.ninecun_lingtai.common.tomb;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.technique.TechniqueDefinition;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.data.TechniqueManager;
import net.seraphina.ninecun_lingtai.common.item.SpiritualStoneGrade;
import net.seraphina.ninecun_lingtai.common.registry.ModTombContent;
import net.seraphina.ninecun_lingtai.utility.AlchemyRecipeItemUtility;
import net.seraphina.ninecun_lingtai.utility.SpiritualStoneUtility;
import net.seraphina.ninecun_lingtai.utility.TechniqueItemUtility;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 墓穴宝箱的分级战利品（M4 墓穴切片，施工图 §7 战利品分级）。
 *
 * <p>玉简/丹方复用 {@code TechniqueItemUtility}/{@code AlchemyRecipeItemUtility} 的
 * 生成入口（与 {@code JadeSlipLootUtility} 同一套底层），但按墓主境界过滤品阶：
 * 筑基/金丹墓出"玄阶"带（练气~金丹需求的功法、二三阶丹方），元婴/化神墓出
 * "地阶"带（金丹~化神需求的功法、三四阶丹方）。灵石：金丹墓起见天字（HEAVEN）。
 * 机缘之物"魂泉水"只在高阶墓穴按概率出现；"墓主残魂"不进宝箱，只由墓主掉落。</p>
 */
public final class CultivatorTombLootFiller {
    /** 魂泉水出现概率（按墓穴档位索引：筑基/金丹/元婴/化神）。建议配置键 tomb.soulSpringWaterChance。 */
    private static final double[] SOUL_SPRING_WATER_CHANCE = {0.0D, 0.15D, 0.35D, 0.60D};

    private CultivatorTombLootFiller() {
    }

    public static void fill(Container container, TombTier tier, RandomSource random) {
        List<ItemStack> loot = new ArrayList<>();
        addJadeSlips(loot, tier, random);
        addRecipePages(loot, tier, random);
        addSpiritStones(loot, tier, random);
        addArtifactMaterials(loot, tier, random);
        addSoulSpringWater(loot, tier, random);
        scatterIntoContainer(container, loot, random);
    }

    /** 玄/地阶功法玉简：按墓主境界带过滤功法的最低修为需求。 */
    private static void addJadeSlips(List<ItemStack> loot, TombTier tier, RandomSource random) {
        List<TechniqueDefinition> allTechniques = TechniqueManager.INSTANCE.getTechniques();
        if (allTechniques.isEmpty()) {
            return;
        }
        int minRealmOrdinal = Math.max(0, tier.getRealm().ordinal() - 1);
        int maxRealmOrdinal = tier.getRealm().ordinal();
        List<TechniqueDefinition> banded = allTechniques.stream()
                .filter(technique -> {
                    int requirementOrdinal = technique.requirements().minimumCultivation()
                            .map(stage -> stage.realm().ordinal())
                            .orElse(0);
                    return requirementOrdinal >= minRealmOrdinal && requirementOrdinal <= maxRealmOrdinal;
                })
                .toList();
        List<TechniqueDefinition> pool = banded.isEmpty() ? allTechniques : banded;
        int count = 1 + random.nextInt(2) + (tier.isHighTier() ? 1 : 0);
        for (int index = 0; index < count; index++) {
            loot.add(TechniqueItemUtility.createStack(pool.get(random.nextInt(pool.size()))));
        }
    }

    /** 丹方残页：丹方 tier 带随墓主境界上移。 */
    private static void addRecipePages(List<ItemStack> loot, TombTier tier, RandomSource random) {
        List<AlchemyRecipeDefinition> allRecipes = AlchemyRecipeManager.INSTANCE.getRecipes();
        if (allRecipes.isEmpty()) {
            return;
        }
        int minGradeTier = 1 + tier.ordinal() / 2;
        int maxGradeTier = 2 + tier.ordinal();
        List<AlchemyRecipeDefinition> banded = allRecipes.stream()
                .filter(recipe -> recipe.grade().tier() >= minGradeTier && recipe.grade().tier() <= maxGradeTier)
                .toList();
        List<AlchemyRecipeDefinition> pool = banded.isEmpty() ? allRecipes : banded;
        int count = 1 + random.nextInt(2);
        for (int index = 0; index < count; index++) {
            loot.add(AlchemyRecipeItemUtility.createStack(pool.get(random.nextInt(pool.size()))));
        }
    }

    /** 灵石：筑基墓地字为主，金丹墓起见天字，化神墓天字成把。 */
    private static void addSpiritStones(List<ItemStack> loot, TombTier tier, RandomSource random) {
        int heavenCount = switch (tier) {
            case FOUNDATION -> 0;
            case GOLDEN_CORE -> 1;
            case NASCENT_SOUL -> 1 + random.nextInt(2);
            case SPIRIT_TRANSFORMATION -> 2 + random.nextInt(2);
        };
        for (int index = 0; index < heavenCount; index++) {
            loot.add(SpiritualStoneUtility.createStack(SpiritualStoneGrade.HEAVEN));
        }
        int earthCount = 2 + random.nextInt(3) - (tier.isHighTier() ? 1 : 0);
        for (int index = 0; index < earthCount; index++) {
            loot.add(SpiritualStoneUtility.createStack(SpiritualStoneGrade.EARTH));
        }
    }

    private static void addArtifactMaterials(List<ItemStack> loot, TombTier tier, RandomSource random) {
        int count = 1 + random.nextInt(tier.ordinal() + 2);
        loot.add(new ItemStack(ModTombContent.ARTIFACT_MATERIAL_FRAGMENT.get(), count));
    }

    private static void addSoulSpringWater(List<ItemStack> loot, TombTier tier, RandomSource random) {
        if (random.nextDouble() < SOUL_SPRING_WATER_CHANCE[tier.ordinal()]) {
            loot.add(new ItemStack(ModTombContent.SOUL_SPRING_WATER.get()));
        }
    }

    /** 随机散入空槽（照 JadeSlipLootUtility 的散布思路，本类自有副本）。 */
    private static void scatterIntoContainer(Container container, List<ItemStack> loot, RandomSource random) {
        int containerSize = container.getContainerSize();
        if (containerSize == 0) {
            return;
        }
        for (ItemStack stack : loot) {
            int startingSlot = random.nextInt(containerSize);
            for (int slotOffset = 0; slotOffset < containerSize; slotOffset++) {
                int slot = (startingSlot + slotOffset) % containerSize;
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, stack);
                    break;
                }
            }
        }
    }
}
