package net.seraphina.ninecun_lingtai.mixin.client.health;

import net.minecraft.world.entity.Entity;
import net.seraphina.ninecun_lingtai.client.utility.ClientCultivationDefenseUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ClientEntityHealthMixin {
    @Inject(method = "isAlive()Z", at = @At("RETURN"), cancellable = true)
    private void ninecunLingtai$protectLocalAlive(CallbackInfoReturnable<Boolean> callbackInfo) {
        Entity entity = (Entity) (Object) this;
        callbackInfo.setReturnValue(
                ClientCultivationDefenseUtility.getProtectedAlive(entity, callbackInfo.getReturnValue())
        );
    }
}
