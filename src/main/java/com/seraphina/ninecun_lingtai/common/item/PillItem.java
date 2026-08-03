package com.seraphina.ninecun_lingtai.common.item;

import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyPillEffect;
import com.seraphina.ninecun_lingtai.api.alchemy.AlchemyRecipeDefinition;
import com.seraphina.ninecun_lingtai.api.cultivation.CultivationApi;
import com.seraphina.ninecun_lingtai.common.data.AlchemyRecipeManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class PillItem extends Item {
    public PillItem(Properties properties) {
        super(properties);
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

        applyPillEffect(player, recipe.get().pillEffect());
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(itemStack);
    }

    private static void applyPillEffect(Player player, AlchemyPillEffect effect) {
        float healing = (float) Math.max(
                effect.minimumHealing(),
                player.getMaxHealth() * effect.maxHealthFraction()
        );
        player.heal(healing);
        player.addEffect(new MobEffectInstance(
                MobEffects.REGENERATION,
                effect.regenerationDuration(),
                effect.regenerationAmplifier()
        ));
    }
}
