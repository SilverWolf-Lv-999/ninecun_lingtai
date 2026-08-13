package net.seraphina.ninecun_lingtai.utility;

import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyFeedStep;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredient;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyIngredientSlot;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeClientData;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeJeiData;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatBand;
import net.seraphina.ninecun_lingtai.api.alchemy.HeatStage;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationRealm;
import net.seraphina.ninecun_lingtai.api.element.ElementType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public final class AlchemyRecipeNetworkUtility {
    private static final int MAXIMUM_RECIPES = 2048;
    private static final int MAXIMUM_NAME_LENGTH = 256;
    private static final int MAXIMUM_DESCRIPTION_LENGTH = 4096;
    private static final int MAXIMUM_CONTENT_LENGTH = 131072;
    private static final int MAXIMUM_HEAT_STAGES = 32;
    private static final int MAXIMUM_FEED_STEPS = AlchemyIngredientSlot.values().length;

    public static void writeRecipe(FriendlyByteBuf buffer, AlchemyRecipeClientData recipe) {
        buffer.writeResourceLocation(recipe.id());
        buffer.writeUtf(recipe.name(), MAXIMUM_NAME_LENGTH);
        buffer.writeUtf(recipe.description(), MAXIMUM_DESCRIPTION_LENGTH);
        buffer.writeUtf(recipe.content(), MAXIMUM_CONTENT_LENGTH);
        buffer.writeVarInt(recipe.grade().tier());
        buffer.writeVarInt(recipe.grade().rank());
        buffer.writeVarInt(recipe.allowedRealms().size());
        recipe.allowedRealms().stream()
                .sorted()
                .forEach(realm -> buffer.writeUtf(realm.getSerializedName(), MAXIMUM_NAME_LENGTH));
        buffer.writeBoolean(recipe.primaryElement() != null);
        if (recipe.primaryElement() != null) {
            buffer.writeEnum(recipe.primaryElement());
        }
        buffer.writeVarInt(recipe.heatStages().size());
        for (HeatStage heatStage : recipe.heatStages()) {
            buffer.writeEnum(heatStage.band());
            buffer.writeDouble(heatStage.portion());
        }
        writeFeedPlan(buffer, recipe.feedPlan());
        buffer.writeVarInt(recipe.brewingDuration());
        buffer.writeDouble(recipe.pillToxin());
    }

    public static AlchemyRecipeClientData readRecipe(FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readResourceLocation();
        String name = buffer.readUtf(MAXIMUM_NAME_LENGTH);
        String description = buffer.readUtf(MAXIMUM_DESCRIPTION_LENGTH);
        String content = buffer.readUtf(MAXIMUM_CONTENT_LENGTH);
        AlchemyGrade grade = new AlchemyGrade(buffer.readVarInt(), buffer.readVarInt());
        int realmCount = buffer.readVarInt();
        if (realmCount <= 0 || realmCount > CultivationRealm.values().length) {
            throw new IllegalArgumentException("Invalid synchronized allowed realm count: " + realmCount);
        }
        EnumSet<CultivationRealm> allowedRealms = EnumSet.noneOf(CultivationRealm.class);
        for (int realmIndex = 0; realmIndex < realmCount; realmIndex++) {
            String realmName = buffer.readUtf(MAXIMUM_NAME_LENGTH);
            CultivationRealm realm = CultivationRealm.fromSerializedName(realmName)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown synchronized cultivation realm: " + realmName
                    ));
            allowedRealms.add(realm);
        }
        ElementType primaryElement = buffer.readBoolean() ? buffer.readEnum(ElementType.class) : null;
        int stageCount = buffer.readVarInt();
        if (stageCount < 0 || stageCount > MAXIMUM_HEAT_STAGES) {
            throw new IllegalArgumentException("Invalid synchronized heat stage count: " + stageCount);
        }
        List<HeatStage> heatStages = new ArrayList<>(stageCount);
        for (int stageIndex = 0; stageIndex < stageCount; stageIndex++) {
            heatStages.add(new HeatStage(buffer.readEnum(HeatBand.class), buffer.readDouble()));
        }
        List<AlchemyFeedStep> feedPlan = readFeedPlan(buffer);
        int brewingDuration = buffer.readVarInt();
        double pillToxin = buffer.readDouble();
        return new AlchemyRecipeClientData(
                recipeId,
                name,
                description,
                content,
                grade,
                allowedRealms,
                primaryElement,
                heatStages,
                feedPlan,
                brewingDuration,
                pillToxin
        );
    }

    public static void writeRecipes(FriendlyByteBuf buffer, List<AlchemyRecipeClientData> recipes) {
        if (recipes.size() > MAXIMUM_RECIPES) {
            throw new IllegalArgumentException("Too many alchemy recipes to synchronize");
        }
        buffer.writeVarInt(recipes.size());
        recipes.forEach(recipe -> writeRecipe(buffer, recipe));
    }

    public static List<AlchemyRecipeClientData> readRecipes(FriendlyByteBuf buffer) {
        int recipeCount = buffer.readVarInt();
        if (recipeCount < 0 || recipeCount > MAXIMUM_RECIPES) {
            throw new IllegalArgumentException("Invalid synchronized alchemy recipe count: " + recipeCount);
        }
        List<AlchemyRecipeClientData> recipes = new ArrayList<>(recipeCount);
        for (int recipeIndex = 0; recipeIndex < recipeCount; recipeIndex++) {
            recipes.add(readRecipe(buffer));
        }
        return List.copyOf(recipes);
    }

    public static void writeJeiRecipes(FriendlyByteBuf buffer, List<AlchemyRecipeJeiData> recipes) {
        if (recipes.size() > MAXIMUM_RECIPES) {
            throw new IllegalArgumentException("Too many JEI alchemy recipes to synchronize");
        }
        buffer.writeVarInt(recipes.size());
        recipes.forEach(recipe -> writeJeiRecipe(buffer, recipe));
    }

    public static List<AlchemyRecipeJeiData> readJeiRecipes(FriendlyByteBuf buffer) {
        int recipeCount = buffer.readVarInt();
        if (recipeCount < 0 || recipeCount > MAXIMUM_RECIPES) {
            throw new IllegalArgumentException("Invalid synchronized JEI alchemy recipe count: " + recipeCount);
        }
        List<AlchemyRecipeJeiData> recipes = new ArrayList<>(recipeCount);
        for (int recipeIndex = 0; recipeIndex < recipeCount; recipeIndex++) {
            recipes.add(readJeiRecipe(buffer));
        }
        return List.copyOf(recipes);
    }

    public static void writeJeiRecipe(FriendlyByteBuf buffer, AlchemyRecipeJeiData recipe) {
        buffer.writeResourceLocation(recipe.id());
        writeIngredient(buffer, recipe.primaryIngredient());
        recipe.secondaryIngredients().forEach(ingredient -> writeIngredient(buffer, ingredient));
        recipe.auxiliaryIngredients().forEach(ingredient -> writeIngredient(buffer, ingredient));
        buffer.writeResourceLocation(recipe.outputItem());
        buffer.writeVarInt(recipe.outputCount());
        buffer.writeVarInt(recipe.grade().tier());
        buffer.writeVarInt(recipe.grade().rank());
    }

    public static AlchemyRecipeJeiData readJeiRecipe(FriendlyByteBuf buffer) {
        ResourceLocation recipeId = buffer.readResourceLocation();
        AlchemyIngredient primaryIngredient = readIngredient(buffer);
        List<AlchemyIngredient> secondaryIngredients = List.of(
                readIngredient(buffer),
                readIngredient(buffer)
        );
        List<AlchemyIngredient> auxiliaryIngredients = List.of(
                readIngredient(buffer),
                readIngredient(buffer)
        );
        ResourceLocation outputItem = buffer.readResourceLocation();
        int outputCount = buffer.readVarInt();
        AlchemyGrade grade = new AlchemyGrade(buffer.readVarInt(), buffer.readVarInt());
        return new AlchemyRecipeJeiData(
                recipeId,
                primaryIngredient,
                secondaryIngredients,
                auxiliaryIngredients,
                outputItem,
                outputCount,
                grade
        );
    }

    private static void writeIngredient(FriendlyByteBuf buffer, AlchemyIngredient ingredient) {
        buffer.writeResourceLocation(ingredient.item());
        buffer.writeVarInt(ingredient.count());
    }

    private static AlchemyIngredient readIngredient(FriendlyByteBuf buffer) {
        return new AlchemyIngredient(buffer.readResourceLocation(), buffer.readVarInt());
    }

    private static void writeFeedPlan(
            FriendlyByteBuf buffer,
            List<AlchemyFeedStep> feedPlan
    ) {
        if (feedPlan.size() > MAXIMUM_FEED_STEPS) {
            throw new IllegalArgumentException("Too many synchronized alchemy feed steps");
        }
        buffer.writeVarInt(feedPlan.size());
        for (AlchemyFeedStep step : feedPlan) {
            buffer.writeEnum(step.slot());
            buffer.writeVarInt(step.stageIndex());
            buffer.writeDouble(step.windowStart());
            buffer.writeDouble(step.idealPoint());
            buffer.writeDouble(step.windowEnd());
        }
    }

    private static List<AlchemyFeedStep> readFeedPlan(FriendlyByteBuf buffer) {
        int stepCount = buffer.readVarInt();
        if (stepCount < 0 || stepCount > MAXIMUM_FEED_STEPS) {
            throw new IllegalArgumentException(
                    "Invalid synchronized alchemy feed step count: " + stepCount
            );
        }
        List<AlchemyFeedStep> feedPlan = new ArrayList<>(stepCount);
        for (int stepIndex = 0; stepIndex < stepCount; stepIndex++) {
            feedPlan.add(new AlchemyFeedStep(
                    buffer.readEnum(AlchemyIngredientSlot.class),
                    buffer.readVarInt(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble()
            ));
        }
        return List.copyOf(feedPlan);
    }

    public static int getMaximumRecipes() {
        return MAXIMUM_RECIPES;
    }
}
