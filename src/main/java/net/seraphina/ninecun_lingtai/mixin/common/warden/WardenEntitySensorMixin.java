package net.seraphina.ninecun_lingtai.mixin.common.warden;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.WardenEntitySensor;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.seraphina.ninecun_lingtai.utility.GreatCausalityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WardenEntitySensor.class)
public abstract class WardenEntitySensorMixin {
    @Inject(method = "doTick*", at = @At("TAIL"))
    private void ninecunLingtai$removeCausalityPlayerFromSniffTargets(
            ServerLevel level,
            Warden warden,
            CallbackInfo callbackInfo
    ) {
        warden.getBrain().getMemory(MemoryModuleType.NEAREST_ATTACKABLE)
                .filter(entity -> entity instanceof Player player
                        && GreatCausalityUtility.hasTechnique(player))
                .ifPresent(entity -> warden.getBrain().eraseMemory(MemoryModuleType.NEAREST_ATTACKABLE));
    }
}
