package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerRealmSaveMixin {
    @Redirect(
            method = "saveAllChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
                            + "saveDataTag(Lnet/minecraft/core/RegistryAccess;"
                            + "Lnet/minecraft/world/level/storage/WorldData;"
                            + "Lnet/minecraft/nbt/CompoundTag;)V"
            )
    )
    private void saveWorldDataWithoutLazyRealmStems(
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            RegistryAccess registryAccess,
            WorldData worldData,
            CompoundTag hostPlayerData
    ) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        levelStorageAccess.saveDataTag(
                ThreeThousandRealmManager.createWorldSaveRegistryAccess(server, registryAccess),
                worldData,
                hostPlayerData
        );
    }
}
