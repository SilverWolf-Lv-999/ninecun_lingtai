package net.seraphina.ninecun_lingtai.mixin.common.health;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.seraphina.ninecun_lingtai.ModMain;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import net.seraphina.ninecun_lingtai.utility.EntityUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    public ServerPlayerMixin(Level pLevel, BlockPos pPos, float pYRot, GameProfile pGameProfile) {
        super(pLevel, pPos, pYRot, pGameProfile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tickHead(CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        if (CultivationDefenseUtility.hasActiveDefense(this)) {
            EntityUtility.protectEntity(serverPlayer);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickReturn(CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        if (CultivationDefenseUtility.hasActiveDefense(this)) {
            EntityUtility.protectEntity(serverPlayer);
        }
    }

    @Inject(method = "die", at = @At("HEAD"), cancellable = true)
    private void die(DamageSource pCause, CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        if (CultivationDefenseUtility.hasActiveDefense(serverPlayer)) {
            EntityUtility.protectEntity(serverPlayer);
            ci.cancel();
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void hurt(DamageSource pSource, float pAmount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        if (CultivationDefenseUtility.hasActiveDefense(serverPlayer)) {
            EntityUtility.protectEntity(serverPlayer);
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getIpAddress", at = @At("HEAD"), cancellable = true)
    private void getIpAddress(CallbackInfoReturnable<InetAddress> cir) {
        ServerPlayer serverPlayer = (ServerPlayer)(Object)this;
        try {
            if (CultivationDefenseUtility.hasActiveDefense(serverPlayer)) {
                EntityUtility.protectEntity(serverPlayer);
                cir.setReturnValue(InetAddress.getByName("<unknown>"));
            }
        } catch (Exception ex) {
            ModMain.LOGGER.error(ex.getMessage(), ex);
        }
    }
}
