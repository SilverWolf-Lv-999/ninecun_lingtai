package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyPillEffect;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import com.seraphina.ninecun_lingtai.utility.AlchemyPillUtility;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public final class PillItem extends ColorItem {
    public PillItem(Properties properties, int color) {
        super(properties, color);
    }

    @Override
    public Component getName(ItemStack itemStack) {
        return Component.translatable(
                "item.ninecun_lingtai.alchemy_pill.with_grade",
                super.getName(itemStack),
                AlchemyPillUtility.getGrade(itemStack).getDisplayName()
        );
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        tooltip.add(Component.translatable(
                "tooltip.ninecun_lingtai.alchemy_pill.grade",
                AlchemyPillUtility.getGrade(itemStack).getDisplayName()
        ));
        tooltip.add(Component.translatable(
                "tooltip.ninecun_lingtai.alchemy_pill.effect_multiplier",
                Math.round(AlchemyPillUtility.getGrade(itemStack).getEffectMultiplier() * 100.0D)
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(itemStack);
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

        applyPillEffect(
                player,
                recipe.get().pillEffect(),
                AlchemyPillUtility.getGrade(itemStack).getEffectMultiplier()
        );
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(itemStack);
    }

    private static void applyPillEffect(Player player, AlchemyPillEffect effect, double effectMultiplier) {
        float healing = (float) (
                player.getMaxHealth() * effect.maxHealthFraction()
                        + effect.flatHealing()
        ) * (float) effectMultiplier;
        player.heal(healing);
        if (effect.regenerationDuration() > 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.REGENERATION,
                    scaleDuration(effect.regenerationDuration(), effectMultiplier),
                    effect.regenerationAmplifier()
            ));
        }
    }

    private static int scaleDuration(int duration, double effectMultiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.round(duration * effectMultiplier));
    }
}
