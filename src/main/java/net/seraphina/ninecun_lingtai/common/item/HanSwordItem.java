package net.seraphina.ninecun_lingtai.common.item;

import net.seraphina.ninecun_lingtai.api.item.ISpiritualItem;
import net.seraphina.ninecun_lingtai.api.item.SpiritualItemGrade;
import net.seraphina.ninecun_lingtai.client.SeraFont;
import net.seraphina.ninecun_lingtai.client.render.HanSwordRenderer;
import net.seraphina.ninecun_lingtai.utility.SpiritualItemUtility;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions.FontContext;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

public final class HanSwordItem extends SwordItem implements ISpiritualItem, GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public HanSwordItem() {
        super(Tiers.IRON, 3, -2.4F, new Properties().stacksTo(1));
    }

    @Override
    public SpiritualItemGrade getGrade(ItemStack itemStack) {
        return SpiritualItemUtility.getGrade(itemStack);
    }

    @Override
    public void appendHoverText(
            ItemStack itemStack,
            Level level,
            List<Component> tooltip,
            TooltipFlag tooltipFlag
    ) {
        tooltip.add(Component.translatable(
                "tooltip.ninecun_lingtai.spiritual_item.grade",
                getGrade(itemStack).getDisplayName()
        ));
        super.appendHoverText(itemStack, level, tooltip, tooltipFlag);
    }

    @Override
    public int getMaxDamage(ItemStack itemStack) {
        return getGrade(itemStack).isDamageable() ? super.getMaxDamage(itemStack) : 0;
    }

    @Override
    public int getDamage(ItemStack itemStack) {
        if (!getGrade(itemStack).isDamageable()) {
            return 0;
        }

        return super.getDamage(itemStack);
    }

    @Override
    public boolean isDamageable(ItemStack itemStack) {
        return getGrade(itemStack).isDamageable();
    }

    @Override
    public boolean isDamaged(ItemStack itemStack) {
        return getGrade(itemStack).isDamageable() && getDamage(itemStack) > 0;
    }

    @Override
    public void setDamage(ItemStack itemStack, int damage) {
        if (!getGrade(itemStack).isDamageable()) {
            return;
        }
        super.setDamage(itemStack, damage);
    }

    @Override
    public <T extends LivingEntity> int damageItem(
            ItemStack itemStack,
            int amount,
            T entity,
            Consumer<T> onBroken
    ) {
        return getGrade(itemStack).isDamageable() ? amount : 0;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return new HanSwordRenderer();
            }

            @Override
            public Font getFont(ItemStack itemStack, FontContext context) {
                return SeraFont.get(ItemFontStyle.HAN_SWORD);
            }
        });
    }
}
