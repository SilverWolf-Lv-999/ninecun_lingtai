package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import net.seraphina.ninecun_lingtai.common.spell.palm_buddha.PalmBuddhaRealmManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListRealmLoadMixin {
    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;"
                            + "getLevel(Lnet/minecraft/resources/ResourceKey;)"
                            + "Lnet/minecraft/server/level/ServerLevel;"
            )
    )
    private ServerLevel loadSavedThreeThousandRealm(
            MinecraftServer server,
            ResourceKey<Level> levelKey
    ) {
        ThreeThousandRealmManager realmManager = ThreeThousandRealmManager.get(server);
        if (realmManager.isManagedRealm(levelKey)) {
            return realmManager.getOrLoadManagedRealm(levelKey.location());
        }
        return server.getLevel(levelKey);
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void restorePalmBuddhaMemberAfterLogin(
            Connection connection,
            ServerPlayer player,
            CallbackInfo callbackInfo
    ) {
        PalmBuddhaRealmManager.get(player.server).restoreMemberToRealm(player);
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void restorePalmBuddhaMemberAfterRespawn(
            ServerPlayer oldPlayer,
            boolean keepEverything,
            CallbackInfoReturnable<ServerPlayer> callbackInfo
    ) {
        ServerPlayer respawnedPlayer = callbackInfo.getReturnValue();
        if (respawnedPlayer != null) {
            PalmBuddhaRealmManager.get(respawnedPlayer.server).restoreMemberToRealm(respawnedPlayer);
        }
    }
}
