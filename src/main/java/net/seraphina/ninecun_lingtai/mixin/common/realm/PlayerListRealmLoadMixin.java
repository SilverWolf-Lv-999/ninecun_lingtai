package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
        if (realmManager.isThreeThousandRealm(levelKey)) {
            return realmManager.getOrLoadRealm(levelKey.location());
        }
        return server.getLevel(levelKey);
    }
}
