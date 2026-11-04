package net.seraphina.ninecun_lingtai.mixin.common;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBusInvokeDispatcher;
import net.seraphina.ninecun_lingtai.utility.CultivationDefenseUtility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("all")
@Mixin(value = EventBus.class, remap = false)
public abstract class MixinEventBus {
    @Inject(
            method = "post(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraftforge/eventbus/api/IEventBusInvokeDispatcher;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void postZ(Event event, IEventBusInvokeDispatcher wrapper, CallbackInfoReturnable<Boolean> cir) {
        if (event instanceof LivingDeathEvent deathEvent) {
            LivingEntity livingEntity = deathEvent.getEntity();
            if (CultivationDefenseUtility.hasActiveDefense(livingEntity)) {
                cir.setReturnValue(false);
            }
        }
        if (event instanceof LivingHurtEvent hurtEvent) {
            LivingEntity livingEntity = hurtEvent.getEntity();
            if (CultivationDefenseUtility.hasActiveDefense(livingEntity)) {
                cir.setReturnValue(false);
            }
        }
        if (event instanceof LivingAttackEvent attackEvent) {
            LivingEntity livingEntity = attackEvent.getEntity();
            if (CultivationDefenseUtility.hasActiveDefense(livingEntity)) {
                cir.setReturnValue(false);
            }
        }
    }

    @Inject(
            method = "post(Lnet/minecraftforge/eventbus/api/Event;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void postZ(Event event, CallbackInfoReturnable<Boolean> cir) {
        if (event instanceof LivingDeathEvent deathEvent) {
            LivingEntity livingEntity = deathEvent.getEntity();
            if (CultivationDefenseUtility.hasActiveDefense(livingEntity)) {
                cir.setReturnValue(false);
            }
        }
        if (event instanceof LivingHurtEvent hurtEvent) {
            LivingEntity livingEntity = hurtEvent.getEntity();
            if (CultivationDefenseUtility.hasActiveDefense(livingEntity)) {
                cir.setReturnValue(false);
            }
        }
        if (event instanceof LivingAttackEvent attackEvent) {
            LivingEntity livingEntity = attackEvent.getEntity();
            if (CultivationDefenseUtility.hasActiveDefense(livingEntity)) {
                cir.setReturnValue(false);
            }
        }
    }
}
