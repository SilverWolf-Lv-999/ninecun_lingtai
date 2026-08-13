package net.seraphina.ninecun_lingtai.common.block.entity;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredientSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

final class AlchemyFurnaceSessionState {
    static final String SELECTED_RECIPE_KEY = "SelectedRecipe";
    private static final String ACTIVE_RECIPE_KEY = "ActiveRecipe";
    private static final String FED_FEED_MASK_KEY = "FedFeedMask";
    private static final String MISSED_FEED_MASK_KEY = "MissedFeedMask";
    private static final String LATE_FEED_MASK_KEY = "LateFeedMask";
    private static final String FEED_SCORE_TOTAL_KEY = "FeedScoreTotal";
    private static final String FEED_SCORE_COUNT_KEY = "FeedScoreCount";
    static final String PLANNED_FEED_MASK_KEY = "PlannedFeedMask";
    private static final int VALID_FEED_MASK =
            (1 << AlchemyIngredientSlot.values().length) - 1;

    static void save(CompoundTag tag, PersistedState state) {
        if (state.selectedRecipeId() != null) {
            tag.putString(SELECTED_RECIPE_KEY, state.selectedRecipeId().toString());
        }
        if (state.activeRecipeId() != null) {
            tag.putString(ACTIVE_RECIPE_KEY, state.activeRecipeId().toString());
        }
        tag.putInt(FED_FEED_MASK_KEY, state.fedFeedMask());
        tag.putInt(MISSED_FEED_MASK_KEY, state.missedFeedMask());
        tag.putInt(LATE_FEED_MASK_KEY, state.lateFeedMask());
        tag.putDouble(FEED_SCORE_TOTAL_KEY, state.feedScoreTotal());
        tag.putInt(FEED_SCORE_COUNT_KEY, state.feedScoreCount());
        tag.putInt(PLANNED_FEED_MASK_KEY, state.plannedFeedMask());
    }

    static PersistedState load(CompoundTag tag) {
        ResourceLocation activeRecipeId = readRecipeId(tag, ACTIVE_RECIPE_KEY);
        ResourceLocation selectedRecipeId = readRecipeId(tag, SELECTED_RECIPE_KEY);
        if (selectedRecipeId == null) {
            selectedRecipeId = activeRecipeId;
        }
        double storedScore = tag.getDouble(FEED_SCORE_TOTAL_KEY);
        double feedScoreTotal = Double.isFinite(storedScore) && storedScore > 0.0D
                ? Math.min(AlchemyIngredientSlot.values().length, storedScore)
                : 0.0D;
        return new PersistedState(
                selectedRecipeId,
                activeRecipeId,
                sanitizeMask(tag.getInt(FED_FEED_MASK_KEY)),
                sanitizeMask(tag.getInt(MISSED_FEED_MASK_KEY)),
                sanitizeMask(tag.getInt(LATE_FEED_MASK_KEY)),
                feedScoreTotal,
                Math.max(
                        0,
                        Math.min(
                                AlchemyIngredientSlot.values().length,
                                tag.getInt(FEED_SCORE_COUNT_KEY)
                        )
                ),
                sanitizeMask(tag.getInt(PLANNED_FEED_MASK_KEY))
        );
    }

    @Nullable
    private static ResourceLocation readRecipeId(CompoundTag tag, String key) {
        return tag.contains(key)
                ? ResourceLocation.tryParse(tag.getString(key))
                : null;
    }

    private static int sanitizeMask(int mask) {
        return mask & VALID_FEED_MASK;
    }

    record PersistedState(
            @Nullable ResourceLocation selectedRecipeId,
            @Nullable ResourceLocation activeRecipeId,
            int fedFeedMask,
            int missedFeedMask,
            int lateFeedMask,
            double feedScoreTotal,
            int feedScoreCount,
            int plannedFeedMask
    ) {
    }
}
