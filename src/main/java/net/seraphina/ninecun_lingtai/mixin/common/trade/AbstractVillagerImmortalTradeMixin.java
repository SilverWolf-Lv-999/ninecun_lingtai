package net.seraphina.ninecun_lingtai.mixin.common.trade;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.seraphina.ninecun_lingtai.utility.ImmortalTradeUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class AbstractVillagerImmortalTradeMixin {
    @Shadow
    protected MerchantOffers offers;

    @Inject(method = "getOffers", at = @At("TAIL"))
    private void ninecunLingtai$appendImmortalTrade(
            CallbackInfoReturnable<MerchantOffers> callbackInfo
    ) {
        ImmortalTradeUtility.appendTrade((AbstractVillager) (Object) this, this.offers);
    }

    @Inject(method = "notifyTrade", at = @At("TAIL"))
    private void ninecunLingtai$refreshImmortalTrade(
            MerchantOffer offer,
            CallbackInfo callbackInfo
    ) {
        ImmortalTradeUtility.refreshTrade((AbstractVillager) (Object) this, offer);
    }
}
