package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyGrade;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyPillEffectData;
import net.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import net.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.client.render.PillItemRenderer;
import net.seraphina.ninecun_lingtai.common.alchemy.PillEffectApplier;
import net.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.seraphina.ninecun_lingtai.common.player.PlayerPillToxinData;
import net.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import net.seraphina.ninecun_lingtai.utility.AlchemyResultCalculator;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class PillItem extends ColorItem {
    public static final String PILL_TOXIN_TAG_KEY = ModMain.MOD_ID + ":alchemy_pill_toxin";
    private static final String PILL_TIER_TAG_KEY = ModMain.MOD_ID + ":alchemy_pill_tier";

    private final ItemFontStyle fontStyle;

    public PillItem(Properties properties, int color, ItemFontStyle fontStyle) {
        super(properties, color);
        this.fontStyle = fontStyle;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack itemStack) {
        return Component.translatable(
                "item.ninecun_lingtai.alchemy_pill.with_grade",
                super.getName(itemStack),
                AlchemyPillUtility.getGrade(itemStack).getDisplayName()
        );
    }

    @Override
    public void appendHoverText(
            @NotNull ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            @NotNull TooltipFlag tooltipFlag
    ) {
        CompoundTag itemData = itemStack.getTag();
        if (itemData == null) {
            return;
        }
        if (itemData.contains(PILL_TIER_TAG_KEY)) {
            AlchemyGrade grade = AlchemyPillUtility.getGrade(itemStack);
            tooltip.add(Component.translatable(
                    "tooltip.ninecun_lingtai.alchemy_pill.grade",
                    grade.getDisplayName()
            ).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                    "tooltip.ninecun_lingtai.alchemy_pill.effect_multiplier",
                    Math.round(grade.getEffectMultiplier() * 100.0D)
            ).withStyle(ChatFormatting.GRAY));
        }
        double pillToxin = itemData.getDouble(PILL_TOXIN_TAG_KEY);
        if (pillToxin > 0.0D) {
            tooltip.add(Component.translatable(
                    "tooltip.ninecun_lingtai.alchemy_pill.toxin",
                    formatToxin(pillToxin)
            ).withStyle(ChatFormatting.DARK_RED));
        }
    }

    private static String formatToxin(double toxin) {
        double rounded = Math.round(toxin * 10.0D) / 10.0D;
        if (rounded == Math.floor(rounded)) {
            return Long.toString((long) rounded);
        }
        return Double.toString(rounded);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
        }

        if (PlayerPillToxinData.refusesPills(player)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.pill_toxin_refused"),
                    true
            );
            return InteractionResultHolder.fail(itemStack);
        }
        Optional<AlchemyRecipeDefinition> recipe = AlchemyRecipeManager.INSTANCE.getRecipeForOutputItem(
                BuiltInRegistries.ITEM.getKey(this)
        );
        if (recipe.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.missing_alchemy_recipe"),
                    true
            );
            return InteractionResultHolder.fail(itemStack);
        }
        if (!CultivationApi.getStage(player)
                .map(stage -> recipe.get().allowedRealms().contains(stage.realm()))
                .orElse(false)) {
            player.displayClientMessage(
                    Component.translatable("message.ninecun_lingtai.pill_realm_not_allowed"),
                    true
            );
            return InteractionResultHolder.fail(itemStack);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            consumePill(serverPlayer, recipe.get(), itemStack);
        }
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(itemStack);
    }

    private void consumePill(ServerPlayer player, AlchemyRecipeDefinition recipe, ItemStack itemStack) {
        int rank = AlchemyPillUtility.getGrade(itemStack).rank();
        String resistanceCategory = recipe.effects().isEmpty()
                ? "misc"
                : recipe.effects().get(0).type();
        double resistanceMultiplier = PlayerPillToxinData.useAndGetResistanceMultiplier(
                player,
                resistanceCategory,
                player.level().getGameTime()
        );
        double multiplier = AlchemyPillUtility.getGrade(itemStack).getEffectMultiplier()
                * PlayerPillToxinData.getPillEffectMultiplier(player)
                * resistanceMultiplier;
        if (resistanceMultiplier < 1.0D) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.ninecun_lingtai.pill_resistance",
                            Math.round(resistanceMultiplier * 100.0D)
                    ),
                    true
            );
        }
        for (AlchemyPillEffectData effect : recipe.effects()) {
            PillEffectApplier.apply(player, effect, multiplier);
        }
        applyToxin(player, recipe, rank);
    }

    private static void applyToxin(ServerPlayer player, AlchemyRecipeDefinition recipe, int rank) {
        double toxin = AlchemyResultCalculator.calculateToxin(recipe, rank);
        if (toxin <= 0.0D) {
            return;
        }
        double previousToxin = PlayerPillToxinData.getToxin(player);
        PlayerPillToxinData.addToxin(player, toxin);
        double currentToxin = PlayerPillToxinData.getToxin(player);
        String warningKey = getCrossedToxinWarning(previousToxin, currentToxin);
        if (warningKey != null) {
            player.displayClientMessage(
                    Component.translatable(warningKey).withStyle(ChatFormatting.RED),
                    false
            );
        }
    }

    private static String getCrossedToxinWarning(double previousToxin, double currentToxin) {
        if (previousToxin < PlayerPillToxinData.OUTBREAK_THRESHOLD
                && currentToxin >= PlayerPillToxinData.OUTBREAK_THRESHOLD) {
            return "message.ninecun_lingtai.pill_toxin_outbreak";
        }
        if (previousToxin < PlayerPillToxinData.MARROW_THRESHOLD
                && currentToxin >= PlayerPillToxinData.MARROW_THRESHOLD) {
            return "message.ninecun_lingtai.pill_toxin_marrow";
        }
        if (previousToxin < PlayerPillToxinData.EROSION_THRESHOLD
                && currentToxin >= PlayerPillToxinData.EROSION_THRESHOLD) {
            return "message.ninecun_lingtai.pill_toxin_erosion";
        }
        return null;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return PillItemRenderer.INSTANCE;
            }

            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(PillItem.this.fontStyle);
            }
        });
    }
}
