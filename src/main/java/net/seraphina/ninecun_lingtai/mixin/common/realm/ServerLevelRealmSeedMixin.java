package net.seraphina.ninecun_lingtai.mixin.common.realm;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.seraphina.ninecun_lingtai.common.realm.ThreeThousandRealmSeedContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelRealmSeedMixin {
    @Unique
    private boolean thisHasThreeThousandRealmSeed;
    @Unique
    private long thisThreeThousandRealmSeed;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    private void storeThreeThousandRealmSeedAfterConstruction(
            MinecraftServer server,
            Executor dispatcher,
            LevelStorageSource.LevelStorageAccess levelStorageAccess,
            ServerLevelData serverLevelData,
            ResourceKey<Level> dimension,
            LevelStem levelStem,
            ChunkProgressListener progressListener,
            boolean isDebug,
            long biomeZoomSeed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            @Nullable RandomSequences randomSequences,
            CallbackInfo callbackInfo
    ) {
        OptionalLong realmSeed = ThreeThousandRealmSeedContext.getCurrentSeed();
        if (realmSeed.isPresent()) {
            this.thisHasThreeThousandRealmSeed = true;
            this.thisThreeThousandRealmSeed = realmSeed.getAsLong();
        }
    }

    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/structure/StructureCheck;<init>(Lnet/minecraft/world/level/chunk/storage/ChunkScanAccess;Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/LevelHeightAccessor;Lnet/minecraft/world/level/biome/BiomeSource;JLcom/mojang/datafixers/DataFixer;)V"
            ),
            index = 8
    )
    private long useThreeThousandRealmSeedForStructureCheck(long originalSeed) {
        OptionalLong realmSeed = ThreeThousandRealmSeedContext.getCurrentSeed();
        return realmSeed.orElse(originalSeed);
    }

    @Inject(method = "getSeed", at = @At("HEAD"), cancellable = true)
    private void getThreeThousandRealmSeed(CallbackInfoReturnable<Long> callbackInfo) {
        OptionalLong realmSeed = ThreeThousandRealmSeedContext.getCurrentSeed();
        if (realmSeed.isPresent()) {
            callbackInfo.setReturnValue(realmSeed.getAsLong());
            return;
        }
        if (this.thisHasThreeThousandRealmSeed) {
            callbackInfo.setReturnValue(this.thisThreeThousandRealmSeed);
        }
    }
}
