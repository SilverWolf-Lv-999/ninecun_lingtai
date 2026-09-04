package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.ITeleporter;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaRealmManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps ordinary entities inside their authoritative Palm Buddha realm.
 * Players use the dedicated player mixin and are intentionally left untouched.
 */
@Mixin(Entity.class)
public abstract class EntityRealmReturnMixin {
    @Inject(
            method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void ninecunLingtai$restrictPalmBuddhaEntityDimensionTravel(
            ServerLevel destinationLevel,
            ITeleporter teleporter,
            CallbackInfoReturnable<Entity> callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof net.minecraft.server.level.ServerPlayer) {
            return;
        }
        PalmBuddhaRealmManager realmManager = PalmBuddhaRealmManager.get(entity.getServer());
        if (!realmManager.canEntityTravelTo(entity, destinationLevel.dimension())) {
            callbackInfo.setReturnValue(entity);
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void ninecunLingtai$clearTerminalPalmBuddhaEntityState(
            Entity.RemovalReason removalReason,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo callbackInfo
    ) {
        Entity entity = (Entity) (Object) this;
        if (entity.level() instanceof ServerLevel serverLevel) {
            PalmBuddhaRealmManager.get(serverLevel.getServer()).handleEntityRemoval(entity, removalReason);
        }
    }
}
