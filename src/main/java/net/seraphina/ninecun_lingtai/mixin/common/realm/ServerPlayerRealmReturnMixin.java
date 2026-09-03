package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.ITeleporter;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaRealmManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerRealmReturnMixin {
    @Inject(
            method = "changeDimension(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraftforge/common/util/ITeleporter;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void saveOrRestoreThreeThousandRealmReturnPosition(
            ServerLevel destinationLevel,
            ITeleporter teleporter,
            CallbackInfoReturnable<Entity> callbackInfo
    ) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        PalmBuddhaRealmManager palmBuddhaRealmManager = PalmBuddhaRealmManager.get(player.server);
        if (!palmBuddhaRealmManager.canMemberTravelTo(player, destinationLevel.dimension())) {
            callbackInfo.setReturnValue(player);
            return;
        }

        ThreeThousandRealmManager realmManager = ThreeThousandRealmManager.get(player.server);
        if (realmManager.isThreeThousandRealm(player.serverLevel().dimension())
                && !realmManager.isThreeThousandRealm(destinationLevel.dimension())) {
            if (!realmManager.savePlayerRealmReturnPosition(player)) {
                callbackInfo.setReturnValue(player);
            }
            return;
        }

        if (destinationLevel.dimension() != Level.OVERWORLD
                || realmManager.isThreeThousandRealm(player.serverLevel().dimension())) {
            return;
        }

        Entity returnedPlayer = realmManager.returnPlayerToSavedRealm(player);
        if (returnedPlayer != null) {
            callbackInfo.setReturnValue(returnedPlayer);
        }
    }
}
