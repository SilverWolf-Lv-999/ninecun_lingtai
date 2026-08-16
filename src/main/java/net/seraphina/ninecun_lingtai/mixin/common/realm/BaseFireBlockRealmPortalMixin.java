package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.seraphina.ninecun_lingtai.utility.ThreeThousandRealmUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseFireBlock.class)
public abstract class BaseFireBlockRealmPortalMixin {
    @Inject(
            method = "inPortalDimension(Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void allowPortalsInThreeThousandRealms(
            Level level,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        if (!callbackInfo.getReturnValue()
                && ThreeThousandRealmUtility.isPortalCompatibleRealm(level)) {
            callbackInfo.setReturnValue(true);
        }
    }
}
